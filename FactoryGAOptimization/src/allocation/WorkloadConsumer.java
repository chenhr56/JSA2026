package allocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

public class WorkloadConsumer extends Thread {

	int id;
	String name;
	AllocationSolutions solutions;
	Random rng;
	int maxWait;
	boolean speedUp;
	boolean shouldFinish = false;

	// private int l1 = -1;
	// private int l2 = -1;

	List<List<Integer>> ls = new ArrayList<>();

	RuleOptimisor op = null;

	public WorkloadConsumer(int id, int seed, int maxWait, boolean speedUp) {
		this.id = id;
		this.solutions = new AllocationSolutions();
		this.rng = new Random(seed);
		this.maxWait = maxWait;
		this.speedUp = speedUp;

		switch (id) {
		case 0:
			name = "Worst-Fit";
			break;
		case 1:
			name = "Best-Fit";
			break;
		case 2:
			name = "First-Fit";
			break;
		case 3:
			name = "Next-Fit";
			break;
		case 4:
			name = "Rull Allocation";
			break;
		case 5:
			name = "Full Optimisation";
			break;
		case 6:
			name = "Random";
			break;

		default:
			break;
		}
	}

	@Override
	public void run() {
		while (!shouldFinish) {
			waitForNext();

			if (shouldFinish)
				break;

			AllocationResult result = consume(AllocationFactory.feeder.workload);
			AllocationFactory.outputResults.get(id).add(result);

			System.out.println(this.toString());
			for (int i = 0; i < result.objectives.size(); i++) {
				System.out.println(result.objectives.get(i));
			}
		}

		System.out.println("Consumer " + this.name + " is finished.");
	}

	public synchronized void singalToFinish() {

		if (id == 4) {
			op.singalToFinish();
		}

		shouldFinish = true;
		this.notify();

		System.err.println("Consumer " + this.name + " is signalled to finish.");
	}

	public synchronized void waitForNext() {
		try {
			System.out.println(this.toString() + " go to sleep.");
			this.wait(30 * 1000);
			System.out.println(this.toString() + " wake up.");
		} catch (InterruptedException e) {
		}
	}

	public AllocationResult consume(Workload load) {

		Pair<List<List<Double>>, Long> result = null;
		Workload workload = deepCopyWorkload(load);

		List<List<Integer>> cls = new ArrayList<>();

		switch (id) {
		case 0:
			result = solutions.worstFit(workload.getTasks(), workload.getResources(), workload.getConfig(),
					workload.getObjectiveFunction(false, speedUp));
			break;
		case 1:
			result = solutions.bestFit(workload.getTasks(), workload.getResources(), workload.getMaxUtilPerCore(),
					workload.getConfig(), workload.getObjectiveFunction(false, speedUp));
			break;
		case 2:
			result = solutions.firstFit(workload.getTasks(), workload.getResources(), workload.getMaxUtilPerCore(),
					workload.getConfig(), workload.getObjectiveFunction(false, speedUp));
			break;
		case 3:
			result = solutions.nextFit(workload.getTasks(), workload.getResources(), workload.getMaxUtilPerCore(),
					workload.getConfig(), workload.getObjectiveFunction(false, speedUp));
			break;
		case 4:
			if (getLs().size() == 0) {
				List<Task> t = new ArrayList<>(workload.getTasks());
				t.sort((c1, c2) -> Double.compare(c1.maxUtil, c2.maxUtil));

				int firstSize = t.size() / 3 - 1;
				int l1 = t.get(firstSize - 1).computations.get(t.get(firstSize - 1).computations.size() - 1);
				int l2 = t.get(firstSize * 2 - 1).computations.get(t.get(firstSize * 2 - 1).computations.size() - 1);

				setLs(l1, l2);
			}

			List<List<Integer>> ls = new ArrayList<>(getLs());

			cls = ls;

			result = solutions.ruleAllocation(ls, workload.getObjectiveFunction(true, speedUp), workload.getTasks());

			System.err.println("Rull Alloc: " + result.getLeft().get(0) + " LS: " + ls);

			if (op == null) {
				op = new RuleOptimisor(this, 5, rng, solutions, speedUp);
				op.start();
			}

			break;
		case 5:
			result = solutions.deepOptimisation(workload.getConfig(), workload.getObjectiveFunction(false, speedUp),
					rng);
			break;
		case 6:
			result = solutions.randomAllocation(workload.getConfig(), workload.getObjectiveFunction(false, speedUp),
					rng);
			break;

		default:
			break;
		}

		return new AllocationResult(workload.getID(), id, name, result.getRight(), result.getLeft(), workload, cls);
	}

	public static Workload deepCopyWorkload(Workload temp) {

		List<Task> tasks = new ArrayList<>();
		for (int i = 0; i < temp.getTasks().size(); i++) {
			Task tempT = temp.getTasks().get(i);

			int taskID = tempT.id;
			String taskName = tempT.name;
			int taskPrio = tempT.priority;
			List<Integer> taskComputes = new ArrayList<>(tempT.computations);

			// if(this.id == 4)
			// taskComputes.sort((c1,c2) -> Integer.compare(c1, c2));

			List<Integer> taskEnergy = new ArrayList<>(tempT.energys);
			List<Integer> taskCost = new ArrayList<>(tempT.costs);
			List<Double> taskUtil = new ArrayList<>(tempT.util);
			List<String> taskAffinity = new ArrayList<>(tempT.affinity);

			int taskAllocation = tempT.allocation;

			Task t = new Task(taskID, taskName, taskPrio, taskComputes, taskEnergy, taskCost, taskUtil, taskAffinity,
					taskAllocation);
			t.maxUtil = tempT.maxUtil;
			tasks.add(t);
		}

		List<String> resources = new ArrayList<>(temp.getResources());

		Workload deepcopy = new Workload(temp.getID(), tasks, temp.getConfig(), temp.getRullConfig(), resources);

		return deepcopy;
	}

	public AllocationResult runStatic(Workload load, int time) {
		AllocationResult result = consumeStatic(load, time);

//		System.out.println(this.toString());
//		for (int i = 0; i < result.objectives.size(); i++) {
//			System.out.println(result.objectives.get(i));
//		}

		return result;
	}

	public AllocationResult consumeStatic(Workload load, int time) {

		Pair<List<List<Double>>, Long> result = null;
		Workload workload = deepCopyWorkload(load);

		List<List<Integer>> cls = new ArrayList<>();

		switch (id) {
		case 0:
			result = solutions.worstFit(workload.getTasks(), workload.getResources(), workload.getConfig(),
					workload.getObjectiveFunction(false, speedUp));
			break;
		case 1:
			result = solutions.bestFit(workload.getTasks(), workload.getResources(), workload.getMaxUtilPerCore(),
					workload.getConfig(), workload.getObjectiveFunction(false, speedUp));
			break;
		case 2:
			result = solutions.firstFit(workload.getTasks(), workload.getResources(), workload.getMaxUtilPerCore(),
					workload.getConfig(), workload.getObjectiveFunction(false, speedUp));
			break;
		case 3:
			result = solutions.nextFit(workload.getTasks(), workload.getResources(), workload.getMaxUtilPerCore(),
					workload.getConfig(), workload.getObjectiveFunction(false, speedUp));
			break;
		case 4:
			if (getLs().size() == 0) {
				List<Task> t = new ArrayList<>(workload.getTasks());
				t.sort((c1, c2) -> Double.compare(c1.maxUtil, c2.maxUtil));

				int firstSize = t.size() / 3 - 1;
				int l1 = t.get(firstSize - 1).computations.get(t.get(firstSize - 1).computations.size() - 1);
				int l2 = t.get(firstSize * 2 - 1).computations.get(t.get(firstSize * 2 - 1).computations.size() - 1);

				setLs(l1, l2);
			}

			List<List<Integer>> ls = new ArrayList<>(getLs());

			cls = ls;

			result = solutions.ruleAllocation(ls, workload.getObjectiveFunction(true, speedUp), workload.getTasks());

			System.err.println("Rull Alloc: " + result.getLeft().get(0) + " LS: " + ls);

			if ((time+1) % 2 == 0) {
				op = new RuleOptimisor(this, 5, rng, solutions, speedUp);
				op.runStatic(load);
			}

			break;
		case 5:
			result = solutions.deepOptimisation(workload.getConfig(), workload.getObjectiveFunction(false, speedUp),
					rng);
			break;
		case 6:
			result = solutions.randomAllocation(workload.getConfig(), workload.getObjectiveFunction(false, speedUp),
					rng);
			break;

		default:
			break;
		}

		return new AllocationResult(workload.getID(), id, name, result.getRight(), result.getLeft(), workload, cls);
	}

	// public Workload deepCopyWorkload() {
	// Workload temp = AllocationFactory.feeder.workload;
	//
	// List<Task> tasks = new ArrayList<>();
	// for (int i = 0; i < temp.getTasks().size(); i++) {
	// Task tempT = temp.getTasks().get(i);
	//
	// int taskID = tempT.id;
	// String taskName = tempT.name;
	// int taskPrio = tempT.priority;
	// List<Integer> taskComputes = new ArrayList<>(tempT.computations);
	//
	// // if(this.id == 4)
	// // taskComputes.sort((c1,c2) -> Integer.compare(c1, c2));
	//
	// List<Integer> taskEnergy = new ArrayList<>(tempT.energys);
	// List<Integer> taskCost = new ArrayList<>(tempT.costs);
	// List<Double> taskUtil = new ArrayList<>(tempT.util);
	// List<String> taskAffinity = new ArrayList<>(tempT.affinity);
	//
	// int taskAllocation = tempT.allocation;
	//
	// Task t = new Task(taskID, taskName, taskPrio, taskComputes, taskEnergy,
	// taskCost, taskUtil, taskAffinity,
	// taskAllocation);
	// t.maxUtil = tempT.maxUtil;
	// tasks.add(t);
	// }
	//
	// List<String> resources = new ArrayList<>(temp.getResources());
	//
	// Workload deepcopy = new Workload(temp.getID(), tasks, temp.getConfig(),
	// temp.getRullConfig(), resources);
	//
	// return deepcopy;
	// }

	public synchronized void setLs(int l1, int l2) {

		List<Integer> l = new ArrayList<>();
		l.add(l1);
		l.add(l2);

		for (int i = 0; i < ls.size(); i++) {
			if(ls.get(i).get(0).intValue() == l1 && ls.get(i).get(1).intValue() == l2 )
				return;
		}

		ls.add(l);
	}

	public synchronized List<List<Integer>> getLs() {
		return ls;
	}

	@Override
	public String toString() {
		return "Consumer " + this.name;
	}
}
