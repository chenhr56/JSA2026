package allocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutablePair;

import metrics.Configuration;
import metrics.Value;
import metrics.ValueType;
import metrics.Value.Nominal;
import optimisation.ObjectiveFunction.LocalObjectiveFunction;
import optimisation.ObjectiveFunctionArguments;
import optimisation.ObjectiveFunctionResult;
import util.Utils;

class FastFitnessFunction extends LocalObjectiveFunction {

	private List<Task> tasks;
	private List<String> resources;

	public FastFitnessFunction(List<Task> tasks, List<String> resources) {
		this.tasks = tasks;
		this.resources = resources;
	}

	@Override
	public ObjectiveFunctionResult evaluate(ObjectiveFunctionArguments args) {

		Result result = predictKeyObjectives(args.getConfiguration(), args.getProposedControlMetrics());

		Configuration newConfig = Configuration.update(args.getConfiguration(), args.getProposedControlMetrics(),
				result.keyObjectiveMetrics);
		return new ObjectiveFunctionResult(newConfig, objectiveValues(result));

	}

	@Override
	public Result predictKeyObjectives(Configuration current, Map<String, Value> proposedControlMetrics) {
		// List<String> sortedResources = Date20.resources;
		// List<Task> tasks = Date20.tasks;

		List<List<Task>> allocatedTasks = new ArrayList<>();
		for (int i = 0; i < resources.size(); i++) {
			List<Task> tasksOneParition = new ArrayList<>();
			allocatedTasks.add(tasksOneParition);
		}

		List<MutablePair<String, Value>> controlsList = proposedControlMetrics.entrySet().stream()
				.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList());

		for (int i = 0; i < controlsList.size(); i++) {
			if (controlsList.get(i).left.contains("allocation")) {
				Nominal allocationV = (Nominal) controlsList.get(i).right;
				String allocation = allocationV.value;
				String[] nameArray = controlsList.get(i).left.split(" ");
				String[] nameArrayProcessed = Arrays.copyOf(nameArray, nameArray.length - 1);
				String name = Arrays.stream(nameArrayProcessed).collect(Collectors.joining(" "));
				int index = resources.indexOf(allocation);

				Task t = getTaskByID(tasks, name);

				allocatedTasks.get(index).add(t);
			}
		}

		List<Integer> comptuationTimes = new ArrayList<>();
		List<Integer> costs = new ArrayList<>();

		for (int i = 0; i < allocatedTasks.size(); i++) {
			final int allocation = i;
			int time = allocatedTasks.get(i).stream().map(t -> t.computations.get(allocation))
					.collect(Collectors.toList()).stream().mapToInt(t -> t.intValue()).sum();
			int cost = allocatedTasks.get(i).stream().map(t -> t.costs.get(allocation)).collect(Collectors.toList())
					.stream().mapToInt(t -> t.intValue()).sum();
			comptuationTimes.add(time);
			costs.add(cost);
		}

		int makespan = Collections.max(comptuationTimes);
		int cost = costs.stream().mapToInt(t -> t.intValue()).sum();

		Map<String, Value> keyObjectiveMetrics = new HashMap<>();
		keyObjectiveMetrics.put("makespan", new Value.Real(makespan, new ValueType.Real(makespan, makespan)));
		keyObjectiveMetrics.put("montary", new Value.Real(cost, new ValueType.Real(cost, cost)));

		String reportStr = "Status: Succeeded.\n";
		return new Result(keyObjectiveMetrics, reportStr);

	}

	private Task getTaskByID(List<Task> tasks, String name) {
		for (int i = 0; i < tasks.size(); i++) {
			if (name.equals(tasks.get(i).name))
				return tasks.get(i);
		}

		System.err.println("cannot get task by id: " + name);
		System.exit(-1);
		return null;
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
