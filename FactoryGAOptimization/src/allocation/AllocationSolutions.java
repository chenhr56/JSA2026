package allocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import aura.PopulationEntry;
import metrics.Configuration;
import metrics.Value;
import metrics.Value.Int;
import metrics.Value.Nominal;
import metrics.ValueType;
import mitm.atb.UoYEarlyPrototypeDemo;
import optimisation.ObjectiveFunction;
import optimisation.ObjectiveFunction.LocalObjectiveFunction;
import optimisation.ObjectiveFunctionArguments;
import optimisation.ObjectiveFunctionResult;
import optimisation.OptimisationIslandResult;

public class AllocationSolutions {

	public static int populations = 300; // 50
	public static int iterations = 500; // 100

	public Pair<List<List<Double>>, Long> allocate(int allocationID, Workload workload, boolean isSpeedUp, Random rng) {
		Pair<List<List<Double>>, Long> res = null;
		switch (allocationID) {
		case 0:
			res = worstFit(workload.getTasks(), workload.getResources(), workload.getConfig(),
					workload.getObjectiveFunction(false, isSpeedUp));
			break;
		case 1:
			res = bestFit(workload.getTasks(), workload.getResources(), workload.getMaxUtilPerCore(),
					workload.getConfig(), workload.getObjectiveFunction(false, isSpeedUp));
			break;
		case 2:
			res = firstFit(workload.getTasks(), workload.getResources(), workload.getMaxUtilPerCore(),
					workload.getConfig(), workload.getObjectiveFunction(false, isSpeedUp));
			break;
		case 3:
			res = nextFit(workload.getTasks(), workload.getResources(), workload.getMaxUtilPerCore(),
					workload.getConfig(), workload.getObjectiveFunction(false, isSpeedUp));
			break;
		case 4:
			RuleOptimisationResult tasksRule = ruleOptimisation(workload.getRullConfig(),
					workload.getObjectiveFunction(true, isSpeedUp), workload.getTasks(), workload.getResources(), rng);

			res = Pair.of(tasksRule.objectives, tasksRule.time);
			break;
		case 5:
			List<Task> t = new ArrayList<>(workload.getTasks());
			t.sort((c1, c2) -> Double.compare(c1.maxUtil, c2.maxUtil));

			int firstSize = t.size() / 3 - 1;
			int l1 = t.get(firstSize - 1).computations.get(t.get(firstSize - 1).computations.size() - 1);
			int l2 = t.get(firstSize * 2 - 1).computations.get(t.get(firstSize * 2 - 1).computations.size() - 1);

			List<Integer> l = new ArrayList<>();
			l.add(l1);
			l.add(l2);

			List<List<Integer>> ls = new ArrayList<>();
			ls.add(l);

			res = ruleAllocation(ls, workload.getObjectiveFunction(true, isSpeedUp), workload.getTasks());
			break;
		case 6:
			res = deepOptimisation(workload.getConfig(), workload.getObjectiveFunction(false, isSpeedUp), rng);
			break;
		case 7:
			res = randomAllocation(workload.getConfig(), workload.getObjectiveFunction(false, isSpeedUp), rng);
			break;

		default:
			break;
		}

		return res;
	}

	public Pair<List<List<Double>>, Long> worstFit(List<Task> workload, List<String> resources, Configuration template,
			LocalObjectiveFunction of) {
		long start = System.currentTimeMillis();
		int partitions = resources.size();
		// clear tasks' partitions
		for (int i = 0; i < workload.size(); i++) {
			workload.get(i).allocation = -1;
		}

		// Init allocated tasks array
		List<List<Task>> tasks = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			ArrayList<Task> task = new ArrayList<>();
			tasks.add(task);
		}

		// init util array
		ArrayList<Double> utilPerPartition = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			utilPerPartition.add((double) 0);
		}

		for (int i = 0; i < workload.size(); i++) {
			Task task = workload.get(i);
			int target = -1;
			double minUtil = 2;
			for (int j = 0; j < partitions; j++) {
				if (minUtil > utilPerPartition.get(j)) {
					minUtil = utilPerPartition.get(j);
					target = j;
				}
			}

			if (target == -1) {
				System.err.println("WF error!");
				return null;
			}

			task.allocation = target;
			utilPerPartition.set(target, utilPerPartition.get(target) + task.maxUtil);
		}

		for (int i = 0; i < workload.size(); i++) {
			int partition = workload.get(i).allocation;
			tasks.get(partition).add(workload.get(i));
		}

		// System.out.println("WF: " + utilPerPartition);

		List<List<Double>> objectives = objectivesForTasks(tasks, resources, template, of);

		long time = System.currentTimeMillis() - start;

		Pair<List<List<Double>>, Long> res = Pair.of(objectives, time);

		return res;
	}

	public Pair<List<List<Double>>, Long> bestFit(List<Task> workload, List<String> resources, double maxUtilPerCore,
			Configuration template, LocalObjectiveFunction of) {
		long start = System.currentTimeMillis();

		int partitions = resources.size();
		for (int i = 0; i < workload.size(); i++) {
			workload.get(i).allocation = -1;
		}

		List<List<Task>> tasks = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			ArrayList<Task> task = new ArrayList<>();
			tasks.add(task);
		}

		ArrayList<Double> utilPerPartition = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			utilPerPartition.add((double) 0);
		}

		for (int i = 0; i < workload.size(); i++) {
			Task task = workload.get(i);
			int target = -1;
			double maxUtil = -1;
			for (int j = 0; j < partitions; j++) {
				if (maxUtil < utilPerPartition.get(j) && ((maxUtilPerCore - utilPerPartition.get(j) >= task.maxUtil)
						|| (utilPerPartition.get(j) <= maxUtilPerCore))) {
					maxUtil = utilPerPartition.get(j);
					target = j;
				}
			}

			if (target < 0) {
				return null;
			} else {
				task.allocation = target;
				utilPerPartition.set(target, utilPerPartition.get(target) + task.maxUtil);
			}
		}

		for (int i = 0; i < workload.size(); i++) {
			int partition = workload.get(i).allocation;
			tasks.get(partition).add(workload.get(i));
		}

		// System.out.println("BF: " + utilPerPartition);

		List<List<Double>> objectives = objectivesForTasks(tasks, resources, template, of);

		long time = System.currentTimeMillis() - start;

		Pair<List<List<Double>>, Long> res = Pair.of(objectives, time);

		return res;
	}

	public Pair<List<List<Double>>, Long> firstFit(List<Task> workload, List<String> resources, double maxUtilPerCore,
			Configuration template, LocalObjectiveFunction of) {

		long start = System.currentTimeMillis();

		int partitions = resources.size();
		for (int i = 0; i < workload.size(); i++) {
			workload.get(i).allocation = -1;
		}
		List<List<Task>> tasks = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			List<Task> task = new ArrayList<>();
			tasks.add(task);
		}

		ArrayList<Double> utilPerPartition = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			utilPerPartition.add((double) 0);
		}

		for (int i = 0; i < workload.size(); i++) {
			Task task = workload.get(i);
			for (int j = partitions - 1; j > -1; j--) {
				if ((maxUtilPerCore - utilPerPartition.get(j) >= task.maxUtil)
						|| (utilPerPartition.get(j) <= maxUtilPerCore)) {
					task.allocation = j;
					utilPerPartition.set(j, utilPerPartition.get(j) + task.maxUtil);
					break;
				}
			}
			if (task.allocation == -1)
				return null;
		}

		for (int i = 0; i < workload.size(); i++) {
			int partition = workload.get(i).allocation;
			tasks.get(partition).add(workload.get(i));
		}

		// System.out.println("FF: " + utilPerPartition);

		List<List<Double>> objectives = objectivesForTasks(tasks, resources, template, of);

		long time = System.currentTimeMillis() - start;

		Pair<List<List<Double>>, Long> res = Pair.of(objectives, time);

		return res;
	}

	public Pair<List<List<Double>>, Long> nextFit(List<Task> workload, List<String> resources, double maxUtilPerCore,
			Configuration template, LocalObjectiveFunction of) {

		long start = System.currentTimeMillis();

		int partitions = resources.size();

		for (int i = 0; i < workload.size(); i++) {
			workload.get(i).allocation = -1;
		}
		List<List<Task>> tasks = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			List<Task> task = new ArrayList<>();
			tasks.add(task);
		}

		ArrayList<Double> utilPerPartition = new ArrayList<>();
		for (int i = 0; i < partitions; i++) {
			utilPerPartition.add((double) 0);
		}

		int currentIndex = 0;

		for (int i = 0; i < workload.size(); i++) {
			Task task = workload.get(i);

			for (int j = 0; j < partitions; j++) {
				if ((maxUtilPerCore - utilPerPartition.get(currentIndex) >= task.maxUtil)
						|| (utilPerPartition.get(j) <= maxUtilPerCore)) {
					task.allocation = currentIndex;
					utilPerPartition.set(currentIndex, utilPerPartition.get(currentIndex) + task.maxUtil);
					break;
				}
				if (currentIndex == partitions - 1)
					currentIndex = 0;
				else
					currentIndex++;
			}
			if (task.allocation == -1)
				return null;
		}

		for (int i = 0; i < workload.size(); i++) {
			int partition = workload.get(i).allocation;
			tasks.get(partition).add(workload.get(i));
		}

		// System.out.println("NF: " + utilPerPartition);

		List<List<Double>> objectives = objectivesForTasks(tasks, resources, template, of);

		long time = System.currentTimeMillis() - start;

		Pair<List<List<Double>>, Long> res = Pair.of(objectives, time);

		return res;
	}

	public Pair<List<List<Double>>, Long> deepOptimisation(Configuration template,
			ObjectiveFunction.LocalObjectiveFunction of, Random rng) {
		long start = System.currentTimeMillis();

		int engine = 1;
		int urgency = 50;
		int iteration = iterations;
		int populationSize = populations;

		final OptimisationIslandResult or = UoYEarlyPrototypeDemo.invokeOE(template, of, urgency, iteration, populationSize,
				rng, engine);

//		List<List<Value>> results = new ArrayList<List<Value>>();
		
		List<List<Double>> objectives = new ArrayList<>();

		for (PopulationEntry x : or.getFront()) {
			
			objectives.add(x.getObjectives());
//			List<Value> result = new ArrayList<Value>();
//			
//			
//
//			if (c.getKeyObjectives().get("makespan") != null)
//				result.add(c.getKeyObjectives().get("makespan"));
//
//			if (c.getKeyObjectives().get("montary") != null)
//				result.add(c.getKeyObjectives().get("montary"));
//
//			results.add(result);
		}

		
//		for (int i = 0; i < results.size(); i++) {
//			List<Double> objective = new ArrayList<>();
//			for (int j = 0; j < results.get(i).size(); j++) {
//				Real obj = (Real) results.get(i).get(j);
//				double objV = obj.value;
//
//				objective.add(objV);
//			}
//			objectives.add(objective);
//		}

		objectives.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));

		long time = System.currentTimeMillis() - start;

		Pair<List<List<Double>>, Long> res = Pair.of(objectives, time);

		return res;
	}

	public Pair<List<List<Double>>, Long> ruleAllocation(List<List<Integer>> ls,
			ObjectiveFunction.LocalObjectiveFunction of, List<Task> tasks) {

		long start = System.currentTimeMillis();

		RuleFitnessFunction ruleOf = (RuleFitnessFunction) of;

		List<List<Double>> objectives = new ArrayList<>();

		for (int i = 0; i < ls.size(); i++) {
			updateExternalPopulation(objectives, ruleOf.getObjective(ls.get(i).get(0), ls.get(i).get(1), tasks));
		}

		objectives.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));

		long time = System.currentTimeMillis() - start;

		Pair<List<List<Double>>, Long> res = Pair.of(objectives, time);

		return res;
	}

	private boolean dominate(List<Double> individual1, List<Double> individual2) {
		boolean isDominate = false;

		if (individual1.size() != individual2.size()) {
			System.out.println("error");
		}

		for (int i = 0; i < individual1.size(); i++) {
			if (individual1.get(i) > individual2.get(i))
				return false;
			if (individual1.get(i) <= individual2.get(i))
				isDominate = true;
		}

		return isDominate;
	}

	private void updateExternalPopulation(List<List<Double>> externalPopulation, List<Double> candidate) {

		if (externalPopulation.size() == 0)
			externalPopulation.add(candidate);
		else {
			boolean eligibleToJoin = true;

			/*
			 * remove the members that are dominated by the candidate and check the
			 * eligibility of the candidate.
			 */
			for (int i = 0; i < externalPopulation.size(); i++) {
				List<Double> member = externalPopulation.get(i);
				if (dominate(candidate, member)) {
					/* the candidate dominates a member */

					boolean isEqual = true;

					for (int j = 0; j < candidate.size(); j++) {
						if (candidate.get(j).doubleValue() != member.get(j).doubleValue()) {
							isEqual = false;
						}
					}

					if (isEqual) {
						eligibleToJoin = false;
					} else {
						externalPopulation.remove(i);
						i--;
						eligibleToJoin = true;
					}

				} else if (dominate(member, candidate))
					/* the candidate is dominated by a member */
					eligibleToJoin = false;
			}

			if (eligibleToJoin && externalPopulation.size() < 100) {
				externalPopulation.add(candidate);
			}
		}

	}

	public RuleOptimisationResult ruleOptimisation(Configuration template, ObjectiveFunction.LocalObjectiveFunction of,
			List<Task> tasks, List<String> resources, Random rng) {
		long start = System.currentTimeMillis();
		int engine = 1;
		int urgency = 50;
		int iteration = iterations;
		int populationSize = populations;

		final OptimisationIslandResult or = UoYEarlyPrototypeDemo.invokeOE(template, of, urgency, iteration, populationSize,
				rng, engine);

		List<List<Integer>> classification = new ArrayList<>();
		List<Configuration> reconfigurations = new ArrayList<>();
		List<List<Double>> objectives = new ArrayList<>();
		
		for (PopulationEntry x : or.getFront()) {
			objectives.add(x.getObjectives());
			
			reconfigurations.add(x.getConfiguration());
			
			Configuration c = x.getConfiguration();
			Int l1 = (Int) c.getControlledMetrics().get("l1");
			Int l2 = (Int) c.getControlledMetrics().get("l2");

			int l1Value = l1.value;
			int l2Value = l2.value;
			List<Integer> values = new ArrayList<>();
			values.add(l1Value);
			values.add(l2Value);
			values.sort((c1, c2) -> Integer.compare(c1, c2));
			classification.add(values);
		}

//		List<List<Value>> results = new ArrayList<List<Value>>();

//		for (Configuration c : or.getReconfigurations()) {
//			List<Value> result = new ArrayList<Value>();
//
//			if (c.getKeyObjectives().get("makespan") != null)
//				result.add(c.getKeyObjectives().get("makespan"));
//
//			if (c.getKeyObjectives().get("montary") != null)
//				result.add(c.getKeyObjectives().get("montary"));
//
//			results.add(result);
//		}

		long time = System.currentTimeMillis() - start;

		
//		for (int i = 0; i < results.size(); i++) {
//			List<Double> objective = new ArrayList<>();
//			for (int j = 0; j < results.get(i).size(); j++) {
//				Real obj = (Real) results.get(i).get(j);
//				double objV = obj.value;
//
//				objective.add(objV);
//			}
//			objectives.add(objective);
//		}

		double minMakespan = Double.POSITIVE_INFINITY;
		int index = -1;

		for (int i = 0; i < objectives.size(); i++) {
			if (objectives.get(i).get(0) < minMakespan) {
				minMakespan = objectives.get(i).get(0);
				index = i;
			}
		}

		System.err.println("Rule Optimised. L1: " + classification.get(index).get(0) + " L2: "
				+ classification.get(index).get(1) + " objectives: " + objectives.get(index));

		objectives.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));

		RuleOptimisationResult res = new RuleOptimisationResult(classification.get(index), reconfigurations,
				index, classification, objectives, time);

		return res;

	}

	public Pair<List<List<Double>>, Long> randomAllocation(Configuration template,
			ObjectiveFunction.LocalObjectiveFunction of, Random rng) {

		long start = System.currentTimeMillis();

		int engine = 1;
		int urgency = 50;
		int iteration = 1;
		int populationSize = populations * iterations;

		final OptimisationIslandResult or = UoYEarlyPrototypeDemo.invokeOE(template, of, urgency, iteration, populationSize,
				rng, engine);

//		List<List<Value>> results = new ArrayList<List<Value>>();
		
		List<List<Double>> objectives = new ArrayList<>();

		for (PopulationEntry x : or.getFront()) {
			objectives.add(x.getObjectives());
			
//			List<Value> result = new ArrayList<Value>();
//
//			if (c.getKeyObjectives().get("makespan") != null)
//				result.add(c.getKeyObjectives().get("makespan"));
//
//			if (c.getKeyObjectives().get("montary") != null)
//				result.add(c.getKeyObjectives().get("montary"));
//
//			results.add(result);
		}

		
//		for (int i = 0; i < results.size(); i++) {
//			List<Double> objective = new ArrayList<>();
//			for (int j = 0; j < results.get(i).size(); j++) {
//				Real obj = (Real) results.get(i).get(j);
//				double objV = obj.value;
//
//				objective.add(objV);
//			}
//			objectives.add(objective);
//		}

		objectives.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));

		long time = System.currentTimeMillis() - start;

		Pair<List<List<Double>>, Long> res = Pair.of(objectives, time);

		return res;
	}

	private List<List<Double>> objectivesForTasks(List<List<Task>> tasks, List<String> resources,
			Configuration template, LocalObjectiveFunction of) {
		if (tasks == null) {
			List<List<Double>> obj = new ArrayList<>();
			List<Double> objectives = new ArrayList<>();
			objectives.add(Double.MAX_VALUE);
			objectives.add(Double.MAX_VALUE);
			obj.add(objectives);
			return obj;
		}

		List<Task> tasksFlatten = tasks.stream().flatMap(List::stream).collect(Collectors.toList());
		tasksFlatten.sort((c1, c2) -> c1.name.compareTo(c2.name));

		Map<String, Value> control = template.getControlledMetrics();
		List<MutablePair<String, Value>> controlsList = new ArrayList<>(control.entrySet().stream()
				.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList()));
		controlsList.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

		Map<String, Value> objectivesM = template.getKeyObjectives();
		List<MutablePair<String, Value>> objectivesList = new ArrayList<>(objectivesM.entrySet().stream()
				.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList()));

		for (int j = 0; j < tasksFlatten.size(); j++) {
			Task t = tasksFlatten.get(j);
			String name = t.name;
			String allocation = t.affinity.get(t.allocation);
			int prio = t.priority;

			for (int k = 0; k < controlsList.size(); ++k) {
				MutablePair<String, Value> p = controlsList.get(k);
				String key = p.getKey();

				if (key.contains(name) && key.contains("allocation")) {
					Nominal value = (Nominal) p.getValue();
					metrics.ValueType.Nominal allocType = (metrics.ValueType.Nominal) value.getType();
					String allocTypeName = allocType.getName();

					String[] comptiableResources = new String[1];
					comptiableResources[0] = allocation;

					Nominal newValue = new Nominal(allocation,
							ValueType.nominalType(allocTypeName, comptiableResources));
					p.setValue(newValue);
				}

				if (key.contains(name) && key.contains("priority")) {
					Int newValue = new Int(prio, ValueType.intType(prio, prio));
					p.setValue(newValue);
				}
			}

		}

		Map<String, Value> newRanConfigType = controlsList.stream()
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
		Map<String, Value> newObjectives = objectivesList.stream()
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

		Configuration newConfig = new Configuration(template.getConfigurationType(), newRanConfigType, newObjectives);

		ObjectiveFunctionResult evaled = null;
		try {
			evaled = of.evaluate(new ObjectiveFunctionArguments(newConfig, newConfig.getControlledMetrics()));
		} catch (Exception e) {
			e.printStackTrace();
		}

		PopulationEntry newIndividualEntry = new PopulationEntry(evaled.getConfiguration(),
				evaled.getObjectiveValues());

		List<List<Double>> objectives = new ArrayList<>();
		objectives.add(newIndividualEntry.getObjectives());
		return objectives;

	}

}
