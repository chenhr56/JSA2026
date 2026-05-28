#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Solve an ONAFactoryModel XML instance with PuLP/CBC.

This version does NOT use IBM CPLEX. It is intended for the uploaded XML format:
- each productionProcess is treated as one job;
- each job chooses exactly one compatible processingDevice;
- smaller priority values must finish before larger priority values start;
- jobs on the same device have sequence-dependent setup time/cost;
- the XML uses the misspelled attribute name "montary", so the parser reads it as cost.

Because the uploaded instance has a strict priority order and uniform complete setup
values (extraProcessingTime=10 and extraMonetaryCost=1000 for every feasible pair),
this script uses a compact MILP formulation instead of the larger immediate-predecessor
arc formulation. This keeps the model small enough for open-source solvers.

Install:
    pip install pulp

Examples:
    python solve_ona_pulp.py ONAConfiguration1.xml --mode weighted --time-weight 1 --cost-weight 1 --log
    python solve_ona_pulp.py ONAConfiguration1.xml --mode lex_time --log
    python solve_ona_pulp.py ONAConfiguration1.xml --mode lex_cost --log
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
    import pulp
except ImportError as exc:
    raise SystemExit(
        "Cannot import PuLP. Install it with:\n"
        "    pip install pulp\n\n"
        "PuLP normally ships with the open-source CBC solver, so no CPLEX license is required."
    ) from exc


@dataclass(frozen=True)
class Job:
    jid: str       # model job id, e.g., P1 or P1_0
    base: str      # original productionProcess name, e.g., P1
    priority: int


@dataclass(frozen=True)
class Option:
    processing_time: int
    monetary_cost: int
    energy: int


@dataclass(frozen=True)
class Setup:
    extra_time: int
    extra_cost: int
    extra_energy: int


def safe_name(s: str) -> str:
    return re.sub(r"[^0-9A-Za-z_]", "_", s)


def read_ona_xml(xml_path: Path):
    root = ET.parse(xml_path).getroot()
    if root.tag != "ONAFactoryModel":
        raise ValueError(f"Unexpected root tag: {root.tag}")

    devices: List[str] = []
    for dev in root.find("processingDevices") or []:
        if dev.attrib.get("availability", "1") == "1":
            devices.append(dev.attrib["name"])

    jobs: List[Job] = []
    options: Dict[Tuple[str, str], Option] = {}

    for pp in root.find("productionProcesses") or []:
        base = pp.attrib["name"]
        instances = int(pp.attrib.get("instances", "1"))
        priority = int(pp.attrib.get("priority", "0"))

        compatible = pp.find("comptiableDevices")  # XML keeps this misspelling
        if compatible is None:
            compatible = pp.find("compatibleDevices")
        if compatible is None:
            raise ValueError(f"No compatible devices found for {base}")

        for k in range(instances):
            jid = base if instances == 1 else f"{base}_{k}"
            jobs.append(Job(jid=jid, base=base, priority=priority))

            for cd in compatible:
                dev = cd.attrib["name"]
                if dev not in devices:
                    continue
                options[(jid, dev)] = Option(
                    processing_time=int(cd.attrib["processingTime"]),
                    monetary_cost=int(cd.attrib["montary"]),  # XML uses "montary"
                    energy=int(cd.attrib.get("energy", "0")),
                )

    setups: Dict[Tuple[str, str, str], Setup] = {}
    for s in root.find("sequenceDependentSetups") or []:
        src = s.attrib["source"]
        dst = s.attrib["destination"]
        dev = s.attrib["processingDevice"]
        if dev not in devices:
            continue
        setups[(src, dst, dev)] = Setup(
            extra_time=int(s.attrib.get("extraProcessingTime", "0")),
            extra_cost=int(s.attrib.get("extraMonetaryCost", "0")),
            extra_energy=int(s.attrib.get("extraEnergyConsumption", "0")),
        )

    if not jobs:
        raise ValueError("No productionProcess/job found in XML.")
    if not devices:
        raise ValueError("No available processingDevice found in XML.")

    return devices, jobs, options, setups


def setup_is_complete_and_uniform(devices, jobs, options, setups) -> Tuple[bool, Optional[Setup], List[Tuple[str, str, str]]]:
    """Check whether every feasible ordered pair on a common device has the same setup."""
    values = set()
    missing = []
    for i in jobs:
        for j in jobs:
            if i.jid == j.jid:
                continue
            for d in devices:
                if (i.jid, d) in options and (j.jid, d) in options:
                    s = setups.get((i.base, j.base, d))
                    if s is None:
                        missing.append((i.base, j.base, d))
                    else:
                        values.add(s)
    if missing or len(values) != 1:
        return False, None, missing
    return True, next(iter(values)), []


def build_compact_model(devices, jobs, options, setups, args):
    job_ids = [j.jid for j in jobs]
    job_by_id = {j.jid: j for j in jobs}
    compatible_devices: Dict[str, List[str]] = {
        j.jid: [d for d in devices if (j.jid, d) in options] for j in jobs
    }
    for jid, ds in compatible_devices.items():
        if not ds:
            raise ValueError(f"Job {jid} has no available compatible device.")

    # The compact model relies on a fixed strict priority order. This is true for
    # the uploaded instance: P1..P14 have priorities 1..14.
    priorities = [j.priority for j in jobs]
    if len(priorities) != len(set(priorities)):
        raise ValueError(
            "The compact PuLP model expects unique priorities. "
            "For duplicate priorities, use a full disjunctive/arc model."
        )

    ok_uniform, uniform_setup, missing = setup_is_complete_and_uniform(devices, jobs, options, setups)
    if not ok_uniform or uniform_setup is None:
        preview = ", ".join(map(str, missing[:5])) if missing else "non-uniform setup values"
        raise ValueError(
            "The compact PuLP model expects complete and uniform setup values for all feasible pairs. "
            f"Problem: {preview}."
        )

    max_processing_sum = sum(max(options[(j.jid, d)].processing_time for d in compatible_devices[j.jid]) for j in jobs)
    horizon_ub = max_processing_sum + max(0, len(jobs) - 1) * uniform_setup.extra_time
    big_m = horizon_ub + uniform_setup.extra_time + 1

    max_processing_cost_sum = sum(max(options[(j.jid, d)].monetary_cost for d in compatible_devices[j.jid]) for j in jobs)
    cost_ub = max_processing_cost_sum + max(0, len(jobs) - 1) * uniform_setup.extra_cost

    prob = pulp.LpProblem("ONA_FJSP_PuLP_CBC", pulp.LpMinimize)

    # x[j,d] = 1 if job j is assigned to device d.
    x = {
        (jid, d): pulp.LpVariable(f"x_{safe_name(jid)}_{safe_name(d)}", cat=pulp.LpBinary)
        for (jid, d) in options
    }
    # used[d] = 1 if at least one job is assigned to d.
    used = {d: pulp.LpVariable(f"used_{safe_name(d)}", cat=pulp.LpBinary) for d in devices}
    start = {jid: pulp.LpVariable(f"S_{safe_name(jid)}", lowBound=0, cat=pulp.LpContinuous) for jid in job_ids}
    end = {jid: pulp.LpVariable(f"C_{safe_name(jid)}", lowBound=0, cat=pulp.LpContinuous) for jid in job_ids}
    makespan = pulp.LpVariable("makespan", lowBound=0, cat=pulp.LpContinuous)
    total_cost = pulp.LpVariable("total_cost", lowBound=0, cat=pulp.LpContinuous)

    # Each job chooses exactly one compatible device; completion time depends on the selected device.
    for j in jobs:
        jid = j.jid
        prob += pulp.lpSum(x[(jid, d)] for d in compatible_devices[jid]) == 1, f"assign_{safe_name(jid)}"
        prob += (
            end[jid] == start[jid] + pulp.lpSum(
                options[(jid, d)].processing_time * x[(jid, d)] for d in compatible_devices[jid]
            )
        ), f"completion_{safe_name(jid)}"
        prob += makespan >= end[jid], f"makespan_{safe_name(jid)}"

    # Device usage definition.
    for d in devices:
        assigned_on_d = [j.jid for j in jobs if (j.jid, d) in options]
        if not assigned_on_d:
            prob += used[d] == 0, f"unused_device_{safe_name(d)}"
            continue
        for jid in assigned_on_d:
            prob += used[d] >= x[(jid, d)], f"used_ge_{safe_name(jid)}_{safe_name(d)}"
        prob += used[d] <= pulp.lpSum(x[(jid, d)] for jid in assigned_on_d), f"used_le_sum_{safe_name(d)}"

    priority_precedence_count = 0
    same_device_order_count = 0

    # Global priority precedence: smaller priority must complete before larger priority starts.
    for pred in jobs:
        for succ in jobs:
            if pred.priority < succ.priority:
                prob += start[succ.jid] >= end[pred.jid], (
                    f"priority_prec_{safe_name(pred.jid)}_before_{safe_name(succ.jid)}"
                )
                priority_precedence_count += 1

                # If both jobs are on the same device, add setup time between them.
                for d in devices:
                    if (pred.jid, d) in options and (succ.jid, d) in options:
                        setup_time = setups[(pred.base, succ.base, d)].extra_time
                        prob += (
                            start[succ.jid]
                            >= end[pred.jid] + setup_time - big_m * (2 - x[(pred.jid, d)] - x[(succ.jid, d)])
                        ), f"same_dev_setup_{safe_name(pred.jid)}_{safe_name(succ.jid)}_{safe_name(d)}"
                        same_device_order_count += 1

    processing_cost = pulp.lpSum(options[(jid, d)].monetary_cost * x[(jid, d)] for (jid, d) in options)
    # With complete uniform setup values, total setup count = number_of_jobs - number_of_used_devices.
    setup_cost = uniform_setup.extra_cost * (len(jobs) - pulp.lpSum(used[d] for d in devices))
    prob += total_cost == processing_cost + setup_cost, "define_total_cost"

    meta = {
        "job_by_id": job_by_id,
        "compatible_devices": compatible_devices,
        "x": x,
        "used": used,
        "start": start,
        "end": end,
        "makespan": makespan,
        "total_cost": total_cost,
        "horizon_ub": horizon_ub,
        "cost_ub": cost_ub,
        "big_m": big_m,
        "uniform_setup": uniform_setup,
        "priority_precedence_count": priority_precedence_count,
        "same_device_order_count": same_device_order_count,
    }
    return prob, meta


def set_objective(prob: pulp.LpProblem, objective):
    # PuLP supports setObjective, but assigning objective is more compatible across versions.
    prob.objective = objective


def make_solver(args):
    if args.solver == "cbc":
        kwargs = {"msg": bool(args.log)}
        if args.time_limit is not None:
            kwargs["timeLimit"] = args.time_limit
        if args.mip_gap is not None:
            kwargs["gapRel"] = args.mip_gap
        if args.threads is not None:
            kwargs["threads"] = args.threads
        return pulp.PULP_CBC_CMD(**kwargs)

    if args.solver == "highs":
        if not hasattr(pulp, "HiGHS_CMD"):
            raise ValueError("This PuLP version does not expose HiGHS_CMD. Use --solver cbc.")
        kwargs = {"msg": bool(args.log)}
        if args.time_limit is not None:
            kwargs["timeLimit"] = args.time_limit
        return pulp.HiGHS_CMD(**kwargs)

    raise ValueError(f"Unknown solver: {args.solver}")


def solve_model(prob, meta, args):
    makespan = meta["makespan"]
    total_cost = meta["total_cost"]
    solver = make_solver(args)

    if args.mode == "weighted":
        time_scale = args.time_scale if args.time_scale else max(1.0, float(meta["horizon_ub"]))
        cost_scale = args.cost_scale if args.cost_scale else max(1.0, float(meta["cost_ub"]))
        set_objective(prob, args.time_weight * makespan / time_scale + args.cost_weight * total_cost / cost_scale)
        status = prob.solve(solver)
        return status

    if args.mode == "lex_time":
        set_objective(prob, makespan)
        status = prob.solve(solver)
        if pulp.LpStatus.get(status) not in {"Optimal", "Not Solved", "Undefined"} and pulp.value(makespan) is None:
            return status
        best_makespan = pulp.value(makespan)
        if best_makespan is None:
            return status
        prob += makespan <= best_makespan + args.epsilon, "fix_best_makespan"
        set_objective(prob, total_cost)
        status = prob.solve(solver)
        return status

    if args.mode == "lex_cost":
        set_objective(prob, total_cost)
        status = prob.solve(solver)
        if pulp.LpStatus.get(status) not in {"Optimal", "Not Solved", "Undefined"} and pulp.value(total_cost) is None:
            return status
        best_cost = pulp.value(total_cost)
        if best_cost is None:
            return status
        prob += total_cost <= best_cost + args.epsilon, "fix_best_cost"
        set_objective(prob, makespan)
        status = prob.solve(solver)
        return status

    raise ValueError(f"Unknown mode: {args.mode}")


def selected_value(var) -> float:
    val = pulp.value(var)
    return 0.0 if val is None else float(val)


def extract_schedule(meta, options):
    rows = []
    x = meta["x"]
    start = meta["start"]
    end = meta["end"]
    job_by_id = meta["job_by_id"]

    for jid, job in job_by_id.items():
        selected_device = None
        selected_time = None
        selected_cost = None
        for d in meta["compatible_devices"][jid]:
            if selected_value(x[(jid, d)]) > 0.5:
                selected_device = d
                selected_time = options[(jid, d)].processing_time
                selected_cost = options[(jid, d)].monetary_cost
                break
        rows.append({
            "job": jid,
            "base_process": job.base,
            "priority": job.priority,
            "device": selected_device,
            "start": selected_value(start[jid]),
            "end": selected_value(end[jid]),
            "processing_time": selected_time,
            "processing_cost": selected_cost,
        })
    rows.sort(key=lambda r: (r["priority"], r["start"], r["job"]))
    return rows


def write_csv(path: Path, rows: Iterable[dict]):
    rows = list(rows)
    if not rows:
        return
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("xml", type=Path, help="Path to ONAFactoryModel XML file")
    parser.add_argument("--mode", choices=["weighted", "lex_time", "lex_cost"], default="weighted",
                        help="weighted: normalized weighted sum; lex_time: first minimize makespan then cost; lex_cost: first minimize cost then makespan")
    parser.add_argument("--time-weight", type=float, default=1.0)
    parser.add_argument("--cost-weight", type=float, default=1.0)
    parser.add_argument("--time-scale", type=float, default=None, help="Manual normalization scale for makespan")
    parser.add_argument("--cost-scale", type=float, default=None, help="Manual normalization scale for total cost")
    parser.add_argument("--epsilon", type=float, default=1e-6, help="Tolerance for lexicographic second-stage solve")
    parser.add_argument("--solver", choices=["cbc", "highs"], default="cbc")
    parser.add_argument("--time-limit", type=float, default=None, help="Solver time limit in seconds")
    parser.add_argument("--mip-gap", type=float, default=None, help="Relative MIP gap, e.g., 0.01")
    parser.add_argument("--threads", type=int, default=None)
    parser.add_argument("--log", action="store_true")
    parser.add_argument("--csv", type=Path, default=Path("schedule.csv"))
    args = parser.parse_args(argv)

    devices, jobs, options, setups = read_ona_xml(args.xml)
    prob, meta = build_compact_model(devices, jobs, options, setups, args)

    print(f"Jobs: {len(jobs)}, devices: {len(devices)}, assignment options: {len(options)}, setups: {len(setups)}")
    print(f"Uniform setup: time={meta['uniform_setup'].extra_time}, cost={meta['uniform_setup'].extra_cost}")
    print(f"Horizon upper bound: {meta['horizon_ub']}, cost upper bound: {meta['cost_ub']}, Big-M: {meta['big_m']}")
    print(f"Priority precedence constraints: {meta['priority_precedence_count']}")
    print(f"Same-device setup-order constraints: {meta['same_device_order_count']}")
    print(f"Model size before solve: variables={len(prob.variables())}, constraints={len(prob.constraints)}")

    status = solve_model(prob, meta, args)
    status_name = pulp.LpStatus.get(status, str(status))

    if status_name not in {"Optimal", "Not Solved", "Undefined"} and pulp.value(meta["makespan"]) is None:
        print(f"No feasible solution found. Solver status: {status_name}", file=sys.stderr)
        return 2

    # CBC may return "Not Solved" when stopped by time limit but still has an incumbent.
    if pulp.value(meta["makespan"]) is None or pulp.value(meta["total_cost"]) is None:
        print(f"No incumbent solution available. Solver status: {status_name}", file=sys.stderr)
        return 2

    print("\n===== Solution =====")
    print(f"Solve status : {status_name}")
    print(f"Objective    : {pulp.value(prob.objective):.6f}")
    print(f"Makespan     : {pulp.value(meta['makespan']):.6f}")
    print(f"Total cost   : {pulp.value(meta['total_cost']):.6f}")

    rows = extract_schedule(meta, options)
    write_csv(args.csv, rows)
    print(f"Schedule CSV : {args.csv}")

    print("\njob,device,start,end,processing_time,processing_cost")
    for r in rows:
        print(f"{r['job']},{r['device']},{r['start']:.3f},{r['end']:.3f},{r['processing_time']},{r['processing_cost']}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
