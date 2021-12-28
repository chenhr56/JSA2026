package allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import metrics.Configuration;
import metrics.Value;
import metrics.ValueType;
import optimisation.ObjectiveFunction.LocalObjectiveFunction;
import optimisation.ObjectiveFunctionArguments;
import optimisation.ObjectiveFunctionResult;
import util.Utils;

class RuleFitnessFunction extends LocalObjectiveFunction {

	private List<Task> tasks;
	private List<String> resources;
	boolean speedUp;

	public RuleFitnessFunction(List<Task> tasks, List<String> resources, boolean speedUp) {
		this.tasks = tasks;
		this.resources = resources;
		this.speedUp = speedUp;
	}

	@Override
	public ObjectiveFunctionResult evaluate(ObjectiveFunctionArguments args) {

		Result result = predictKeyObjectives(args.getConfiguration(), args.getProposedControlMetrics());

		Configuration newConfig = null;

		// long start = System.currentTimeMillis();
//				if (speedUp) {
		newConfig = Configuration.update(args.getConfiguration(), args.getProposedControlMetrics(),
				result.keyObjectiveMetrics);
//				} else {
//					newConfig = IAObjectiveFunction.addOptimisationStatusToConfiguration(
//							result.objectiveFunctionStatusReport, Configuration.update(args.getConfiguration(),
//									args.getProposedControlMetrics(), result.keyObjectiveMetrics));
//				}
		// long end = System.currentTimeMillis() - start;
		ObjectiveFunctionResult res = new ObjectiveFunctionResult(newConfig, objectiveValues(result));

		// System.out.println("rule of time: " + end);
		return res;

	}

	@Override
	public Result predictKeyObjectives(Configuration current, Map<String, Value> proposedControlMetrics) {

		int l1 = Utils.intValue(proposedControlMetrics.get("l1"));
		int l2 = Utils.intValue(proposedControlMetrics.get("l2"));

		List<Double> objectives = getObjective(l1, l2, tasks);

		Map<String, Value> keyObjectiveMetrics = new HashMap<>();
		keyObjectiveMetrics.put("makespan",
				new Value.Real(objectives.get(0), new ValueType.Real(objectives.get(0), objectives.get(0))));
		keyObjectiveMetrics.put("montary",
				new Value.Real(objectives.get(1), new ValueType.Real(objectives.get(1), objectives.get(1))));

		String reportStr = "Status: Succeeded.\n";
		return new Result(keyObjectiveMetrics, reportStr);

	}

	public List<Double> getObjective(int l1, int l2, List<Task> tasks) {
		List<List<Task>> tasksByType = classifyTasks(l1, l2, tasks);
		List<List<Task>> allocatedTasks = allocateTasks(tasksByType, resources);

		List<Integer> comptuationTimes = new ArrayList<>();
		List<Integer> costs = new ArrayList<>();

		for (int i = 0; i < allocatedTasks.size(); i++) {

			int time = allocatedTasks.get(i).stream().map(t -> t.computations.get(t.allocation))
					.collect(Collectors.toList()).stream().mapToInt(t -> t.intValue()).sum();
			int cost = allocatedTasks.get(i).stream().map(t -> t.costs.get(t.allocation)).collect(Collectors.toList())
					.stream().mapToInt(t -> t.intValue()).sum();
			comptuationTimes.add(time);
			costs.add(cost);
		}

		int makespan = Collections.max(comptuationTimes);
		int cost = costs.stream().mapToInt(t -> t.intValue()).sum();

		List<Double> objectives = new ArrayList<>();
		objectives.add(makespan + 0.0);
		objectives.add(cost + 0.0);

		return objectives;
	}

	public List<List<Task>> classifyTasks(int l1, int l2, List<Task> tasks) {
		List<List<Task>> sortedTasks = new ArrayList<>();

		List<Task> tasksCopy = new ArrayList<>(tasks);

		for (int i = 0; i < tasksCopy.size(); i++) {
			tasksCopy.get(i).allocation = -1;
		}

		List<Integer> splitors = new ArrayList<>();
		splitors.add(l1);
		splitors.add(l2);
		splitors.sort((c1, c2) -> Integer.compare(c1, c2));

		int i = 0;
		for (; i < splitors.size(); i++) {
			List<Task> taskInOneType = new ArrayList<>();
			int split = splitors.get(i);

			for (int j = 0; j < tasksCopy.size(); j++) {
				Task t = tasksCopy.get(j);
				if (t.computations.get(t.computations.size() - 1) < split) {
					taskInOneType.add(t);
					t.type = i;
					tasksCopy.remove(j);
					j--;
				}
			}

			sortedTasks.add(taskInOneType);
		}

		final int typeValue = i;
		tasksCopy.stream().forEach(t -> t.type = typeValue);
		sortedTasks.add(tasksCopy);

		List<Task> forCheck = sortedTasks.stream().flatMap(List::stream).collect(Collectors.toList());

		assert (tasksCopy.size() == 0);
		assert (forCheck.size() == tasks.size());

		return sortedTasks;
	}

	public List<List<Task>> allocateTasks(List<List<Task>> tasksByType, List<String> resources) {
		for (int i = 0; i < tasksByType.size(); i++) {
			for (int j = 0; j < tasksByType.get(i).size(); j++)
				tasksByType.get(i).get(j).allocation = -1;
		}

		// Init allocated tasks array
		List<List<Task>> tasks = new ArrayList<>();

		// init util array
		ArrayList<Integer> timePerPartition = new ArrayList<>();
		for (int i = 0; i < resources.size(); i++) {
			timePerPartition.add(0);
		}

		List<Integer> resourceIndex = resources.stream().map(r -> resources.indexOf(r)).collect(Collectors.toList());
		List<List<Integer>> resourcesByType = new ArrayList<>();
		resourcesByType.add(resourceIndex.subList(0, 4));
		resourcesByType.add(resourceIndex.subList(4, 8));
		resourcesByType.add(resourceIndex.subList(8, 12));

		for (int i = 0; i < tasksByType.size(); i++) {
			List<Task> tasksOneType = tasksByType.get(i);
			List<Integer> resourceOneType = resourcesByType.get(i);

			// if (i == 2)
			// resourceOneType.addAll(resourcesByType.get(1));

			tasks.addAll(balancedFit(tasksOneType, resourceOneType));
		}

		ArrayList<Integer> times = new ArrayList<>();
		for (int i = 0; i < tasks.size(); i++) {
			int time = tasks.get(i).stream().map(t -> t.computations.get(t.allocation)).collect(Collectors.toList())
					.stream().mapToInt(t -> t).sum();
			times.add(time);
		}

		// System.out.println("allocate times: " + times);

		return tasks;

	}

	public List<List<Task>> balancedFit(List<Task> workload, List<Integer> resourceIndex) {

		// clear tasks' partitions
		for (int i = 0; i < workload.size(); i++) {
			workload.get(i).allocation = -1;
		}

		// Init allocated tasks array
		List<List<Task>> tasks = new ArrayList<>();
		for (int i = 0; i < resourceIndex.size(); i++) {
			ArrayList<Task> task = new ArrayList<>();
			tasks.add(task);
		}

		// init util array
		ArrayList<Double> timePerPartition = new ArrayList<>();
		for (int i = 0; i < resourceIndex.size(); i++) {
			timePerPartition.add(0.0);
		}

		for (int i = 0; i < workload.size(); i++) {
			Task task = workload.get(i);
			int target = -1;
			double minUtil = Integer.MAX_VALUE;
			for (int j = 0; j < resourceIndex.size(); j++) {
				if (minUtil > timePerPartition.get(j)) {
					minUtil = timePerPartition.get(j);
					target = j;
				}
			}

			task.allocation = resourceIndex.get(target);
			timePerPartition.set(target, timePerPartition.get(target) + task.maxUtil);
			tasks.get(target).add(workload.get(i));
		}

		return tasks;
	}

	public List<Double> objectiveValues(Result result) {
		double cost = Utils.doubleValue(result.keyObjectiveMetrics.get("montary"));
		double makespan = Utils.doubleValue(result.keyObjectiveMetrics.get("makespan"));

		List<Double> objectives = new ArrayList<>();
		objectives.add(makespan + 0.0);
		objectives.add(cost + 0.0);
		return objectives;
	}

	@Override
	public int numObjectives() {
		return 2;
	}

}
