#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Solve an ONAFactoryModel XML scheduling instance with OR-Tools CP-SAT.

Features:
- Reads productionProcesses, processingDevices, compatibleDevices, and sequenceDependentSetups.
- Expands productionProcess instances into separate jobs.
- Each job is assigned to exactly one compatible device.
- Smaller priority value must complete before larger priority value starts.
- Jobs assigned to the same device are sequenced by an arc-based disjunctive model.
- Sequence-dependent setup time and setup cost are charged on immediate arcs.
- Supports weighted and two-phase lexicographic objectives.

Install:
    pip install ortools

Examples:
    python solve_ona_ortools.py ONAConfiguration2.xml --mode weighted --time-weight 1 --cost-weight 1 --log
    python solve_ona_ortools.py ONAConfiguration2.xml --mode lex_time --time-limit 300 --gap 0.01 --workers 8 --log
"""

from __future__ import annotations

import argparse
import csv
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

try:
    from ortools.sat.python import cp_model
except ImportError as exc:  # pragma: no cover
    raise SystemExit(
        "OR-Tools is not installed. Install it with:\n"
        "    pip install ortools\n"
    ) from exc


@dataclass(frozen=True)
class Option:
    job_id: str
    base_process: str
    device: str
    processing_time: int
    monetary_cost: int
    energy: int


@dataclass(frozen=True)
class Job:
    job_id: str
    base_process: str
    instance: int
    priority: int


@dataclass(frozen=True)
class Setup:
    source: str
    destination: str
    device: str
    extra_time: int
    extra_cost: int
    extra_energy: int


@dataclass
class InstanceData:
    devices: List[str]
    jobs: List[Job]
    options_by_job: Dict[str, List[Option]]
    option_by_job_device: Dict[Tuple[str, str], Option]
    setup_by_key: Dict[Tuple[str, str, str], Setup]


@dataclass
class ModelBundle:
    model: cp_model.CpModel
    data: InstanceData
    start: Dict[str, cp_model.IntVar]
    end: Dict[str, cp_model.IntVar]
    duration: Dict[str, cp_model.IntVar]
    x: Dict[Tuple[str, str], cp_model.IntVar]
    arc: Dict[Tuple[str, str, str], cp_model.IntVar]
    start_arc: Dict[Tuple[str, str], cp_model.IntVar]
    end_arc: Dict[Tuple[str, str], cp_model.IntVar]
    makespan: cp_model.IntVar
    processing_cost: cp_model.IntVar
    setup_cost: cp_model.IntVar
    total_cost: cp_model.IntVar
    horizon: int
    cost_upper: int


def safe_name(text: str) -> str:
    return re.sub(r"[^0-9A-Za-z_]+", "_", text)


def get_int_attr(elem: ET.Element, name: str, default: int = 0) -> int:
    raw = elem.get(name)
    if raw is None or raw == "":
        return default
    return int(float(raw))


def parse_xml(xml_path: str | Path) -> InstanceData:
    tree = ET.parse(xml_path)
    root = tree.getroot()

    devices: List[str] = []
    for dev in root.findall("./processingDevices/processingDevice"):
        if dev.get("availability", "1") != "0":
            name = dev.get("name")
            if name:
                devices.append(name)

    jobs: List[Job] = []
    options_by_job: Dict[str, List[Option]] = {}
    option_by_job_device: Dict[Tuple[str, str], Option] = {}

    for proc in root.findall("./productionProcesses/productionProcess"):
        base = proc.get("name")
        if not base:
            continue
        instances = get_int_attr(proc, "instances", 1)
        priority = get_int_attr(proc, "priority", 0)

        base_options: List[Tuple[str, int, int, int]] = []
        for opt in proc.findall("./comptiableDevices/comptiableDevice"):
            dev = opt.get("name")
            if not dev or dev not in devices:
                continue
            p = get_int_attr(opt, "processingTime")
            c = get_int_attr(opt, "montary")  # The XML uses this spelling.
            e = get_int_attr(opt, "energy")
            base_options.append((dev, p, c, e))

        if not base_options:
            raise ValueError(
                f"Process {base} has no compatible available device.")

        for inst in range(instances):
            jid = f"{base}_{inst}" if instances > 1 else base
            job = Job(job_id=jid, base_process=base,
                      instance=inst, priority=priority)
            jobs.append(job)
            opts: List[Option] = []
            for dev, p, c, e in base_options:
                option = Option(jid, base, dev, p, c, e)
                opts.append(option)
                option_by_job_device[(jid, dev)] = option
            options_by_job[jid] = opts

    setup_by_key: Dict[Tuple[str, str, str], Setup] = {}
    for st in root.findall("./sequenceDependentSetups/sequenceDependentSetup"):
        src = st.get("source")
        dst = st.get("destination")
        dev = st.get("processingDevice")
        if not src or not dst or not dev:
            continue
        setup = Setup(
            source=src,
            destination=dst,
            device=dev,
            extra_time=get_int_attr(st, "extraProcessingTime"),
            extra_cost=get_int_attr(st, "extraMonetaryCost"),
            extra_energy=get_int_attr(st, "extraEnergyConsumption"),
        )
        setup_by_key[(src, dst, dev)] = setup

    if not jobs:
        raise ValueError("No productionProcess/job found in XML.")

    return InstanceData(
        devices=devices,
        jobs=jobs,
        options_by_job=options_by_job,
        option_by_job_device=option_by_job_device,
        setup_by_key=setup_by_key,
    )


def setup_time(data: InstanceData, pred: Job, succ: Job, device: str) -> int:
    setup = data.setup_by_key.get(
        (pred.base_process, succ.base_process, device))
    return setup.extra_time if setup else 0


def setup_cost(data: InstanceData, pred: Job, succ: Job, device: str) -> int:
    setup = data.setup_by_key.get(
        (pred.base_process, succ.base_process, device))
    return setup.extra_cost if setup else 0


def compatible_devices(data: InstanceData, job_id: str) -> List[str]:
    return [opt.device for opt in data.options_by_job[job_id]]


def allowed_arc(pred: Job, succ: Job) -> bool:
    """Whether pred can immediately precede succ on a device.

    Because smaller priority values must complete before larger priority values,
    an arc from a larger-priority job to a smaller-priority job is impossible.
    Same-priority arcs are allowed and decide the local order on a device.
    """
    return pred.job_id != succ.job_id and pred.priority <= succ.priority


def estimate_bounds(data: InstanceData) -> Tuple[int, int]:
    max_setup_t = max(
        (s.extra_time for s in data.setup_by_key.values()), default=0)
    max_setup_c = max(
        (s.extra_cost for s in data.setup_by_key.values()), default=0)

    horizon = 0
    cost_upper = 0
    for job in data.jobs:
        opts = data.options_by_job[job.job_id]
        horizon += max(o.processing_time for o in opts)
        cost_upper += max(o.monetary_cost for o in opts)

    # Sequential schedule is a safe upper bound; add setup allowance between jobs.
    horizon += max(0, len(data.jobs) - 1) * max_setup_t + 100
    cost_upper += max(0, len(data.jobs) - 1) * max_setup_c + 100
    return horizon, cost_upper


def build_model(
    data: InstanceData,
    objective: str,
    time_weight: int = 1,
    cost_weight: int = 1,
    makespan_upper: Optional[int] = None,
    cost_upper_bound: Optional[int] = None,
) -> ModelBundle:
    model = cp_model.CpModel()
    horizon, cost_upper = estimate_bounds(data)

    jobs_by_id = {j.job_id: j for j in data.jobs}

    start: Dict[str, cp_model.IntVar] = {}
    end: Dict[str, cp_model.IntVar] = {}
    duration: Dict[str, cp_model.IntVar] = {}

    for job in data.jobs:
        s = model.NewIntVar(0, horizon, f"S_{safe_name(job.job_id)}")
        e = model.NewIntVar(0, horizon, f"C_{safe_name(job.job_id)}")
        max_p = max(o.processing_time for o in data.options_by_job[job.job_id])
        dur = model.NewIntVar(0, max_p, f"D_{safe_name(job.job_id)}")
        start[job.job_id] = s
        end[job.job_id] = e
        duration[job.job_id] = dur

    x: Dict[Tuple[str, str], cp_model.IntVar] = {}
    for job in data.jobs:
        lits = []
        duration_terms = []
        for opt in data.options_by_job[job.job_id]:
            lit = model.NewBoolVar(
                f"x_{safe_name(job.job_id)}_{safe_name(opt.device)}")
            x[(job.job_id, opt.device)] = lit
            lits.append(lit)
            duration_terms.append(opt.processing_time * lit)
        model.Add(sum(lits) == 1)
        model.Add(duration[job.job_id] == sum(duration_terms))
        model.Add(end[job.job_id] == start[job.job_id] + duration[job.job_id])

    # Global priority precedence: all jobs with smaller priority complete before
    # any job with larger priority can start.
    for pred in data.jobs:
        for succ in data.jobs:
            if pred.priority < succ.priority:
                model.Add(start[succ.job_id] >= end[pred.job_id])

    arc: Dict[Tuple[str, str, str], cp_model.IntVar] = {}
    start_arc: Dict[Tuple[str, str], cp_model.IntVar] = {}
    end_arc: Dict[Tuple[str, str], cp_model.IntVar] = {}

    setup_cost_terms = []

    for dev in data.devices:
        jobs_on_dev = [j for j in data.jobs if (j.job_id, dev) in x]
        if not jobs_on_dev:
            continue

        start_lits = []
        end_lits = []

        for job in jobs_on_dev:
            a0 = model.NewBoolVar(
                f"arc_START_{safe_name(dev)}_{safe_name(job.job_id)}")
            a1 = model.NewBoolVar(
                f"arc_{safe_name(dev)}_{safe_name(job.job_id)}_END")
            start_arc[(dev, job.job_id)] = a0
            end_arc[(dev, job.job_id)] = a1
            start_lits.append(a0)
            end_lits.append(a1)

        for pred in jobs_on_dev:
            for succ in jobs_on_dev:
                if not allowed_arc(pred, succ):
                    continue
                lit = model.NewBoolVar(
                    f"arc_{safe_name(dev)}_{safe_name(pred.job_id)}_{safe_name(succ.job_id)}"
                )
                arc[(dev, pred.job_id, succ.job_id)] = lit
                st = setup_time(data, pred, succ, dev)
                sc = setup_cost(data, pred, succ, dev)
                model.Add(start[succ.job_id] >=
                          end[pred.job_id] + st).OnlyEnforceIf(lit)
                if sc:
                    setup_cost_terms.append(sc * lit)

        # Each selected job on this device has exactly one predecessor and one successor
        # in the device-local sequence. Unselected jobs have none.
        for job in jobs_on_dev:
            incoming = [start_arc[(dev, job.job_id)]]
            outgoing = [end_arc[(dev, job.job_id)]]
            for other in jobs_on_dev:
                if (dev, other.job_id, job.job_id) in arc:
                    incoming.append(arc[(dev, other.job_id, job.job_id)])
                if (dev, job.job_id, other.job_id) in arc:
                    outgoing.append(arc[(dev, job.job_id, other.job_id)])
            model.Add(sum(incoming) == x[(job.job_id, dev)])
            model.Add(sum(outgoing) == x[(job.job_id, dev)])

        # One chain per device. If no job is assigned to a device, both sums are zero.
        model.Add(sum(start_lits) <= 1)
        model.Add(sum(end_lits) <= 1)

    makespan = model.NewIntVar(0, horizon, "makespan")
    for job in data.jobs:
        model.Add(makespan >= end[job.job_id])

    processing_cost_expr = []
    for opt in data.option_by_job_device.values():
        processing_cost_expr.append(
            opt.monetary_cost * x[(opt.job_id, opt.device)])

    processing_cost_var = model.NewIntVar(0, cost_upper, "processing_cost")
    setup_cost_var = model.NewIntVar(0, cost_upper, "setup_cost")
    total_cost_var = model.NewIntVar(0, cost_upper, "total_cost")

    model.Add(processing_cost_var == sum(processing_cost_expr))
    model.Add(setup_cost_var == sum(setup_cost_terms)
              if setup_cost_terms else 0)
    model.Add(total_cost_var == processing_cost_var + setup_cost_var)

    if makespan_upper is not None:
        model.Add(makespan <= int(makespan_upper))
    if cost_upper_bound is not None:
        model.Add(total_cost_var <= int(cost_upper_bound))

    if objective == "weighted":
        model.Minimize(int(time_weight) * makespan +
                       int(cost_weight) * total_cost_var)
    elif objective == "makespan":
        model.Minimize(makespan)
    elif objective == "cost":
        model.Minimize(total_cost_var)
    else:
        raise ValueError(f"Unknown objective: {objective}")

    return ModelBundle(
        model=model,
        data=data,
        start=start,
        end=end,
        duration=duration,
        x=x,
        arc=arc,
        start_arc=start_arc,
        end_arc=end_arc,
        makespan=makespan,
        processing_cost=processing_cost_var,
        setup_cost=setup_cost_var,
        total_cost=total_cost_var,
        horizon=horizon,
        cost_upper=cost_upper,
    )


def configure_solver(args: argparse.Namespace) -> cp_model.CpSolver:
    solver = cp_model.CpSolver()
    if args.time_limit is not None and args.time_limit > 0:
        solver.parameters.max_time_in_seconds = float(args.time_limit)
    if args.gap is not None and args.gap >= 0:
        solver.parameters.relative_gap_limit = float(args.gap)
    if args.workers is not None and args.workers > 0:
        solver.parameters.num_search_workers = int(args.workers)
    if args.log:
        solver.parameters.log_search_progress = True
    return solver


def solve_bundle(bundle: ModelBundle, args: argparse.Namespace) -> Tuple[int, cp_model.CpSolver]:
    solver = configure_solver(args)
    print(
        f"Jobs: {len(bundle.data.jobs)}, devices: {len(bundle.data.devices)}, "
        f"assignment options: {len(bundle.x)}, arcs: {len(bundle.arc)}"
    )
    print(
        f"Horizon upper bound: {bundle.horizon}, cost upper bound: {bundle.cost_upper}, "
        f"CP-SAT variables: {len(bundle.model.Proto().variables)}, "
        f"constraints: {len(bundle.model.Proto().constraints)}"
    )
    status = solver.Solve(bundle.model)
    print(f"Status: {solver.StatusName(status)}")
    return status, solver


def selected_device(bundle: ModelBundle, solver: cp_model.CpSolver, job_id: str) -> str:
    for opt in bundle.data.options_by_job[job_id]:
        if solver.Value(bundle.x[(job_id, opt.device)]) == 1:
            return opt.device
    return ""


def extract_machine_sequences(bundle: ModelBundle, solver: cp_model.CpSolver) -> Dict[str, List[str]]:
    sequences: Dict[str, List[str]] = {}
    for dev in bundle.data.devices:
        first: Optional[str] = None
        for (d, jid), lit in bundle.start_arc.items():
            if d == dev and solver.Value(lit) == 1:
                first = jid
                break
        if first is None:
            sequences[dev] = []
            continue

        seq = [first]
        current = first
        seen = {first}
        while True:
            if (dev, current) in bundle.end_arc and solver.Value(bundle.end_arc[(dev, current)]) == 1:
                break
            nxt = None
            for (d, i, j), lit in bundle.arc.items():
                if d == dev and i == current and solver.Value(lit) == 1:
                    nxt = j
                    break
            if nxt is None or nxt in seen:
                break
            seq.append(nxt)
            seen.add(nxt)
            current = nxt
        sequences[dev] = seq
    return sequences


def write_outputs(bundle: ModelBundle, solver: cp_model.CpSolver, output_prefix: str) -> None:
    schedule_path = Path(f"{output_prefix}_schedule.csv")
    sequence_path = Path(f"{output_prefix}_machine_sequences.csv")

    rows = []
    for job in bundle.data.jobs:
        dev = selected_device(bundle, solver, job.job_id)
        opt = bundle.data.option_by_job_device[(job.job_id, dev)]
        rows.append(
            {
                "job": job.job_id,
                "base_process": job.base_process,
                "instance": job.instance,
                "priority": job.priority,
                "device": dev,
                "start": solver.Value(bundle.start[job.job_id]),
                "end": solver.Value(bundle.end[job.job_id]),
                "processing_time": opt.processing_time,
                "processing_cost": opt.monetary_cost,
            }
        )

    rows.sort(key=lambda r: (int(r["start"]),
              int(r["priority"]), str(r["job"])))
    with schedule_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)

    seqs = extract_machine_sequences(bundle, solver)
    with sequence_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["device", "sequence"])
        for dev, seq in seqs.items():
            if seq:
                writer.writerow([dev, " -> ".join(seq)])

    print(f"Schedule written to: {schedule_path}")
    print(f"Machine sequences written to: {sequence_path}")


def print_solution(bundle: ModelBundle, solver: cp_model.CpSolver) -> None:
    print("\n========== Solution ==========")
    print(f"Makespan:        {solver.Value(bundle.makespan)}")
    print(f"Processing cost: {solver.Value(bundle.processing_cost)}")
    print(f"Setup cost:      {solver.Value(bundle.setup_cost)}")
    print(f"Total cost:      {solver.Value(bundle.total_cost)}")
    print("==============================\n")

    rows = []
    for job in bundle.data.jobs:
        dev = selected_device(bundle, solver, job.job_id)
        rows.append(
            (
                solver.Value(bundle.start[job.job_id]),
                solver.Value(bundle.end[job.job_id]),
                job.priority,
                job.job_id,
                dev,
            )
        )
    for s, e, pr, jid, dev in sorted(rows):
        print(f"{jid:10s} priority={pr:3d} device={dev:12s} start={s:8d} end={e:8d}")


def solve_lexicographic(data: InstanceData, args: argparse.Namespace, first: str) -> Tuple[ModelBundle, cp_model.CpSolver, int]:
    assert first in {"makespan", "cost"}

    if first == "makespan":
        first_obj = "makespan"
        second_obj = "cost"
    else:
        first_obj = "cost"
        second_obj = "makespan"

    print(f"Phase 1: minimize {first_obj}")
    bundle1 = build_model(data, first_obj)
    status1, solver1 = solve_bundle(bundle1, args)
    if status1 not in (cp_model.OPTIMAL, cp_model.FEASIBLE):
        return bundle1, solver1, status1

    best_makespan = solver1.Value(bundle1.makespan)
    best_cost = solver1.Value(bundle1.total_cost)
    if status1 != cp_model.OPTIMAL:
        print(
            "Warning: phase 1 did not prove optimality. "
            "Phase 2 will optimize within the best phase-1 value found."
        )

    print(f"Phase 1 result: makespan={best_makespan}, total_cost={best_cost}")
    print(f"Phase 2: minimize {second_obj} with phase-1 bound fixed")

    if first == "makespan":
        bundle2 = build_model(data, "cost", makespan_upper=best_makespan)
    else:
        bundle2 = build_model(data, "makespan", cost_upper_bound=best_cost)

    status2, solver2 = solve_bundle(bundle2, args)
    return bundle2, solver2, status2


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(
        description="Solve ONA XML scheduling model with OR-Tools CP-SAT.")
    parser.add_argument("xml", help="Input ONAFactoryModel XML file.")
    parser.add_argument(
        "--mode",
        choices=["weighted", "lex_time", "lex_cost"],
        default="weighted",
        help="weighted: minimize weighted makespan+cost; lex_time: time first; lex_cost: cost first.",
    )
    parser.add_argument("--time-weight", type=int, default=1,
                        help="Weight of makespan in weighted mode.")
    parser.add_argument("--cost-weight", type=int, default=1,
                        help="Weight of total monetary cost in weighted mode.")
    parser.add_argument("--time-limit", type=float,
                        default=None, help="Solver time limit in seconds.")
    parser.add_argument("--gap", type=float, default=None,
                        help="Relative MIP/CP-SAT gap limit, e.g., 0.01.")
    parser.add_argument("--workers", type=int, default=8,
                        help="Number of CP-SAT search workers.")
    parser.add_argument("--log", action="store_true",
                        help="Enable CP-SAT search log.")
    parser.add_argument("--output-prefix",
                        default="ona_result", help="Output CSV prefix.")
    args = parser.parse_args(argv)

    start_time = time.time()

    data = parse_xml(args.xml)

    if args.mode == "weighted":
        bundle = build_model(
            data,
            "weighted",
            time_weight=args.time_weight,
            cost_weight=args.cost_weight,
        )
        status, solver = solve_bundle(bundle, args)
    elif args.mode == "lex_time":
        bundle, solver, status = solve_lexicographic(
            data, args, first="makespan")
    else:
        bundle, solver, status = solve_lexicographic(data, args, first="cost")

    end_time = time.time()
    print(f"Solve time: {end_time - start_time:.3f} seconds")

    if status not in (cp_model.OPTIMAL, cp_model.FEASIBLE):
        print("No feasible solution found.")
        return 2

    print_solution(bundle, solver)
    write_outputs(bundle, solver, args.output_prefix)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
