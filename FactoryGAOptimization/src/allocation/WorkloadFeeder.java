package allocation;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import allocation.ONA.Device;
import allocation.ONA.ONAFactoryModel;
import allocation.ONA.ProductionProcess;
import allocation.ONA.SubProcess;
import metrics.Configuration;
import metrics.ConfigurationType;
import metrics.ControlledMetricType;
import metrics.KeyObjectiveType;
import metrics.ObservableMetricType;
import metrics.SampleRate;
import metrics.SearchDirection;
import metrics.Utility;
import metrics.ValueType;
import mitm.atb.Interval;
import mitm.atb.RecipeInfo;
import mitm.atb.UoYEarlyPrototypeDemo;

public class WorkloadFeeder extends Thread {

	int scalePart;
	int numberOfParts;
	int scaleMechaine;
	int period;
	Workload workload;
	Random rng;

	List<WorkloadConsumer> consumer;

	long time = 0;

	boolean shouldFinish = false;

	public WorkloadFeeder(int numberOfParts, int scalePart, int scaleMechaine, int workloadPeriod,
			List<WorkloadConsumer> consumer, int seed) {
		this.numberOfParts = numberOfParts;
		this.scalePart = scalePart;
		this.scaleMechaine = scaleMechaine;
		this.period = workloadPeriod;
		this.rng = new Random(seed);
		this.consumer = consumer;
	}

	@Override
	public void run() {
		int id = 0;
		while (!shouldFinish) {
			AllocationFactory.workloads.add(generate(rng, id));

			for (int i = 0; i < consumer.size(); i++) {
				wakeUpConsumer(consumer.get(i));
			}

			time += period;
			id++;

			try {
				Thread.sleep(period * 1000);
			} catch (InterruptedException e) {
				System.err.println("Feeder is signalled to finish");
			}

		}

		System.err.println("Feeder Finished. Total time: " + time);
	}

	public void wakeUpConsumer(WorkloadConsumer consumer) {
		synchronized (consumer) {
			consumer.notify();
			System.out.println("notify " + consumer.toString());
		}

	}

	public void signalToFinish() {
		shouldFinish = true;
		this.interrupt();

		System.err.println("Feeder is singalled to finish. ");
	}

	public Workload generate(Random rdm, int id) {
		ONAFactoryModel.productScale = scalePart;
		ONAFactoryModel.NoPoduction = numberOfParts;
		ONAFactoryModel.machineScale = scaleMechaine;

		ONAFactoryModel ONAFactory = new ONAFactoryModel();
		ONAFactory.getRandomConfiguration(rdm);

		List<Device> devices = ONAFactory.devices.stream().flatMap(List::stream).collect(Collectors.toList());
		List<ProductionProcess> processes = ONAFactory.processes;

		final Pair<Map<String, List<RecipeInfo>>, String[]> recipesAndResources = recipesAndResourceNames(devices,
				processes);
		final Map<String, List<RecipeInfo>> recipeInfo = recipesAndResources.getLeft();
		final String[] resourceNames = recipesAndResources.getRight();

		List<Task> tasks = generateTasks(processes, recipeInfo, resourceNames);
		Configuration config = generateConfiguration(Arrays.asList(ONAFactoryModel.objectives), processes, recipeInfo,
				resourceNames, rdm);

		Configuration ruleConfig = generateRullConfiguration(tasks, Arrays.asList(ONAFactoryModel.objectives),
				processes, recipeInfo, resourceNames, rdm);

		workload = new Workload(id, tasks, config, ruleConfig, Arrays.asList(resourceNames));

		return workload;
	}

	public Configuration generateRullConfiguration(List<Task> tasks, List<String> objectives,
			List<ProductionProcess> processes, Map<String, List<RecipeInfo>> recipeInfo, String[] resourceNames,
			Random rdm) {

		ConfigurationType ct = rullConfigurationType(tasks, objectives, processes, recipeInfo, resourceNames, true,
				rdm);

		Configuration template = Utility.randomConfiguration(ct, rdm);
		return template;
	}

	public ConfigurationType rullConfigurationType(List<Task> tasks, List<String> objectives,
			List<ProductionProcess> processes, Map<String, List<RecipeInfo>> recipeInfo, String[] resourceNames,
			boolean isMultiobjective, Random random) {

		final Map<Pair<String, String>, Double> recipeAndResourceToEnergy = new HashMap<>();

		for (ProductionProcess process : processes) {
			for (int i = 0; i < process.getCompitableResource().size(); i++) {
				recipeAndResourceToEnergy.put(
						Pair.of(process.getName(), process.getCompitableResource().get(i).getName()),
						(double) process.getEnergys().get(i));
			}
		}

		final Map<Pair<String, String>, Double> recipeAndResourceToMontary = new HashMap<>();

		for (ProductionProcess process : processes) {
			for (int i = 0; i < process.getCompitableResource().size(); i++) {
				recipeAndResourceToMontary.put(
						Pair.of(process.getName(), process.getCompitableResource().get(i).getName()),
						(double) process.getMontarys().get(i));
			}
		}

		final Map<Pair<String, String>, Interval> recipeAndResourceToProcessingTime = new HashMap<>();

		for (ProductionProcess process : processes) {
			for (int i = 0; i < process.getCompitableResource().size(); i++) {
				recipeAndResourceToProcessingTime.put(
						Pair.of(process.getName(), process.getCompitableResource().get(i).getName()),
						new Interval(0, process.getprocessingTime().get(i)));
			}
		}

		final Map<Pair<String, String>, Integer> recipeAndResourceNameToPreemptionPoints = new HashMap<>();
		for (ProductionProcess process : processes) {
			for (Device d : process.getCompitableResource()) {
				recipeAndResourceNameToPreemptionPoints.put(Pair.of(process.getName(), d.getName()),
						process.getNumberOfCuts());
			}
		}

		final ConfigurationType ct = makeRullConfigurationType(tasks, objectives, processes, recipeInfo, resourceNames,
				recipeAndResourceToEnergy, recipeAndResourceToMontary, recipeAndResourceToProcessingTime, null,
				recipeAndResourceNameToPreemptionPoints, isMultiobjective, rng);
		return ct;
	}

	private static ConfigurationType makeRullConfigurationType(List<Task> tasks, List<String> objectives,
			List<ProductionProcess> processes, Map<String, List<RecipeInfo>> recipeInfo, String[] resourceNames,
			Map<Pair<String, String>, Double> recipeAndResourceNameToEnergy,
			Map<Pair<String, String>, Double> recipeAndResourceNameToMontary,
			Map<Pair<String, String>, Interval> recipeAndResourceNameToInterval,
			Map<Pair<String, String>, Boolean> mutices,
			Map<Pair<String, String>, Integer> recipeAndResourceNameToPreemptionPoints, boolean isMultiobjective,
			Random random) {

		final List<KeyObjectiveType> keyObjectiveTypes = new ArrayList<>();

		for (int i = 0; i < objectives.size(); i++) {
			keyObjectiveTypes.add(new KeyObjectiveType(objectives.get(i), ValueType.realType(0, Double.MAX_VALUE),
					"n/a", SearchDirection.MINIMIZING));
		}

		final List<ControlledMetricType> controlledMetricTypes = new ArrayList<>();
		final List<ObservableMetricType> observableMetricTypes = new ArrayList<>();

		for (String resourceName : resourceNames) {
			observableMetricTypes.add(new ObservableMetricType(resourceName + " availability", ValueType.intType(1, 1),
					"n/a", SampleRate.eventDriven));
		}

		for (int i = 0; i < processes.size(); i++) {
			String productInfo = processes.get(i).getName();
			int urgency = processes.get(i).getUrgency();

			observableMetricTypes.add(new ObservableMetricType(productInfo + " urgency " + urgency,
					ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
		}

		List<Integer> maxTimes = tasks.stream().map(t -> t.computations.get(t.computations.size() - 1))
				.collect(Collectors.toList());
		int maxTime = Collections.max(maxTimes);
		int minTime = Collections.min(maxTimes);

		controlledMetricTypes.add(new ControlledMetricType("l1", ValueType.intType(minTime, maxTime), "n/a"));
		controlledMetricTypes.add(new ControlledMetricType("l2", ValueType.intType(minTime, maxTime), "n/a"));

		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			for (RecipeInfo r : e.getValue()) {

				int productIndex = getProductIndex(r.name, processes);
				List<SubProcess> subProcesses = processes.get(productIndex).getSubProcesses();

				for (int i = 0; i < r.instances; ++i) {
					final String instanceName = r.name + " " + i;

					for (int j = 0; j < processes.get(productIndex).getCompitableResource().size(); j++) {

						String resourceName = processes.get(productIndex).getCompitableResource().get(j).getName();

						for (SubProcess sp : subProcesses) {

							String recipeAndResourceNamePrefix = instanceName + " " + resourceName;

							int spProcessingTime = sp.getProcessingTime().get(j);
							int spEnergy = sp.getEnergyCost().get(j);
							int spMontary = sp.getMontaryCost().get(j);

							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " start",
									recipeAndResourceNamePrefix + " start", ValueType.intType(0, 0), "n/a",
									SampleRate.eventDriven));

							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " end",
									recipeAndResourceNamePrefix + " end",
									ValueType.intType(spProcessingTime, spProcessingTime), "n/a",
									SampleRate.eventDriven));

							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " cost",
									recipeAndResourceNamePrefix + " energy", ValueType.realType(spEnergy, spEnergy),
									"n/a", SampleRate.eventDriven));

							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " cost",
									recipeAndResourceNamePrefix + " montary", ValueType.realType(spMontary, spMontary),
									"n/a", SampleRate.eventDriven));

						}

					}
				}
			}
		}

		observableMetricTypes.add(new ObservableMetricType(
				// metricNamePrefix + " start",
				"min time", ValueType.intType(minTime, minTime), "n/a", SampleRate.eventDriven));

		observableMetricTypes.add(new ObservableMetricType(
				// metricNamePrefix + " start",
				"max time", ValueType.intType(maxTime, maxTime), "n/a", SampleRate.eventDriven));

		UoYEarlyPrototypeDemo.observableMetricTypes = observableMetricTypes;
		return new ConfigurationType.Explicit(keyObjectiveTypes, controlledMetricTypes);
	}

	public Configuration generateConfiguration(List<String> objectives, List<ProductionProcess> processes,
			Map<String, List<RecipeInfo>> recipeInfo, String[] resourceNames, Random rdm) {

		ConfigurationType ct = configurationType(objectives, processes, recipeInfo, resourceNames, true, rdm);

		Configuration template = Utility.randomConfiguration(ct, rdm);
		return template;
	}

	public ConfigurationType configurationType(List<String> objectives, List<ProductionProcess> processes,
			Map<String, List<RecipeInfo>> recipeInfo, String[] resourceNames, boolean isMultiobjective, Random random) {

		final Map<Pair<String, String>, Double> recipeAndResourceToEnergy = new HashMap<>();

		for (ProductionProcess process : processes) {
			for (int i = 0; i < process.getCompitableResource().size(); i++) {
				recipeAndResourceToEnergy.put(
						Pair.of(process.getName(), process.getCompitableResource().get(i).getName()),
						(double) process.getEnergys().get(i));
			}
		}

		final Map<Pair<String, String>, Double> recipeAndResourceToMontary = new HashMap<>();

		for (ProductionProcess process : processes) {
			for (int i = 0; i < process.getCompitableResource().size(); i++) {
				recipeAndResourceToMontary.put(
						Pair.of(process.getName(), process.getCompitableResource().get(i).getName()),
						(double) process.getMontarys().get(i));
			}
		}

		final Map<Pair<String, String>, Interval> recipeAndResourceToProcessingTime = new HashMap<>();

		for (ProductionProcess process : processes) {
			for (int i = 0; i < process.getCompitableResource().size(); i++) {
				recipeAndResourceToProcessingTime.put(
						Pair.of(process.getName(), process.getCompitableResource().get(i).getName()),
						new Interval(0, process.getprocessingTime().get(i)));
			}
		}

		final Map<Pair<String, String>, Integer> recipeAndResourceNameToPreemptionPoints = new HashMap<>();
		for (ProductionProcess process : processes) {
			for (Device d : process.getCompitableResource()) {
				recipeAndResourceNameToPreemptionPoints.put(Pair.of(process.getName(), d.getName()),
						process.getNumberOfCuts());
			}
		}

		final ConfigurationType ct = makeConfigurationType(objectives, processes, recipeInfo, resourceNames,
				recipeAndResourceToEnergy, recipeAndResourceToMontary, recipeAndResourceToProcessingTime, null,
				recipeAndResourceNameToPreemptionPoints, isMultiobjective, random);

		return ct;
	}

	private ConfigurationType makeConfigurationType(List<String> objectives, List<ProductionProcess> processes,
			Map<String, List<RecipeInfo>> recipeInfo, String[] resourceNames,
			Map<Pair<String, String>, Double> recipeAndResourceNameToEnergy,
			Map<Pair<String, String>, Double> recipeAndResourceNameToMontary,
			Map<Pair<String, String>, Interval> recipeAndResourceNameToInterval,
			Map<Pair<String, String>, Boolean> mutices,
			Map<Pair<String, String>, Integer> recipeAndResourceNameToPreemptionPoints, boolean isMultiobjective,
			Random random) {

		final List<KeyObjectiveType> keyObjectiveTypes = new ArrayList<>();

		for (int i = 0; i < objectives.size(); i++) {

			keyObjectiveTypes.add(new KeyObjectiveType(objectives.get(i), ValueType.realType(0, Double.MAX_VALUE),
					"n/a", SearchDirection.MINIMIZING));

		}

		final List<ControlledMetricType> controlledMetricTypes = new ArrayList<>();
		final List<ObservableMetricType> observableMetricTypes = new ArrayList<>();

		for (String resourceName : resourceNames) {

			observableMetricTypes.add(new ObservableMetricType(resourceName + " availability", ValueType.intType(1, 1),
					"n/a", SampleRate.eventDriven));
		}

		for (int i = 0; i < processes.size(); i++) {
			String productInfo = processes.get(i).getName();
			int urgency = processes.get(i).getUrgency();

			observableMetricTypes.add(new ObservableMetricType(productInfo + " urgency " + urgency,
					ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
		}

		int totalInstances = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			for (RecipeInfo r : e.getValue())
				totalInstances += r.instances;
		}

		List<Integer> priorities = IntStream.rangeClosed(0, totalInstances).boxed().collect(Collectors.toList());

		int instanceCount = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			for (RecipeInfo r : e.getValue()) {

				int productIndex = getProductIndex(r.name, processes);
				List<SubProcess> subProcesses = processes.get(productIndex).getSubProcesses();

				for (int i = 0; i < r.instances; ++i) {
					final String instanceName = r.name + " " + i;

					// 2. Controlled metrics for allocation and for priority:

					final ValueType allocationValueType = ValueType.nominalType(instanceName + " allocation type ",
							r.compatibleResources.stream().map(index -> resourceNames[index]).toArray(String[]::new));
					// e.g. Std Weiss A 1 allocation (nominal type, domain: Mixer 1, Mixer 2, Mixer
					// 3, Mixer 4, Mixer 5} :
					controlledMetricTypes
							.add(new ControlledMetricType(instanceName + " allocation", allocationValueType, "n/a"));

					// e.g. Std Weiss A 1 priority (Int type)
					final int priorityValue = priorities.get(instanceCount);
					// ensure each priority value is unique - otherwise scheduling can get in an
					// infinite loop:
					controlledMetricTypes.add(new ControlledMetricType(instanceName + " priority",
							ValueType.intType(priorityValue, priorityValue), "n/a"));

					// Observable metric for start and end time of each (resource,recipe instance) :

					for (int j = 0; j < processes.get(productIndex).getCompitableResource().size(); j++) {

						String resourceName = processes.get(productIndex).getCompitableResource().get(j).getName();

						for (SubProcess sp : subProcesses) {

							String recipeAndResourceNamePrefix = instanceName + " " + resourceName;

							int spProcessingTime = sp.getProcessingTime().get(j);
							int spEnergy = sp.getEnergyCost().get(j);
							int spMontary = sp.getMontaryCost().get(j);

							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " start",
									recipeAndResourceNamePrefix + " start", ValueType.intType(0, 0), "n/a",
									SampleRate.eventDriven));

							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " end",
									recipeAndResourceNamePrefix + " end",
									ValueType.intType(spProcessingTime, spProcessingTime), "n/a",
									SampleRate.eventDriven));

							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " cost",
									recipeAndResourceNamePrefix + " energy", ValueType.realType(spEnergy, spEnergy),
									"n/a", SampleRate.eventDriven));

							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " cost",
									recipeAndResourceNamePrefix + " montary", ValueType.realType(spMontary, spMontary),
									"n/a", SampleRate.eventDriven));

						}

					}

					instanceCount += 1;
				}
			}
		}

		UoYEarlyPrototypeDemo.observableMetricTypes = observableMetricTypes;
		return new ConfigurationType.Explicit(keyObjectiveTypes, controlledMetricTypes);
	}

	public static List<Task> generateTasks(List<ProductionProcess> processes, Map<String, List<RecipeInfo>> recipeInfo,
			String[] resourceNames) {
		List<Task> tasks = new ArrayList<>();

		int id = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			for (RecipeInfo r : e.getValue()) {

				int productIndex = getProductIndex(r.name, processes);
				List<SubProcess> subProcesses = processes.get(productIndex).getSubProcesses();

				int instanceCount = 0;
				for (int i = 0; i < r.instances; ++i) {
					final String instanceName = r.name + " " + i;

					String[] affinity = r.compatibleResources.stream().map(index -> resourceNames[index])
							.toArray(String[]::new);
					int priority = instanceCount;

					ArrayList<Integer> computations = new ArrayList<>();
					ArrayList<Integer> energys = new ArrayList<>();
					ArrayList<Integer> costs = new ArrayList<>();

					for (int j = 0; j < processes.get(productIndex).getCompitableResource().size(); j++) {
						ArrayList<Integer> subComputations = new ArrayList<>();
						ArrayList<Integer> subEnergys = new ArrayList<>();
						ArrayList<Integer> subCosts = new ArrayList<>();

						for (SubProcess sp : subProcesses) {
							subComputations.add(sp.getProcessingTime().get(j));
							subEnergys.add(sp.getEnergyCost().get(j));
							subCosts.add(sp.getMontaryCost().get(j));
						}

						int computation = subComputations.stream().mapToInt(a -> a).sum();
						int energy = subEnergys.stream().mapToInt(a -> a).sum();
						int cost = subCosts.stream().mapToInt(a -> a).sum();

						computations.add(computation);
						energys.add(energy);
						costs.add(cost);
					}

					Task t = new Task(id, instanceName, priority, computations, energys, costs, new ArrayList<>(),
							Arrays.asList(affinity), -1);
					tasks.add(t);
					instanceCount += 1;
					id += 1;
				}
			}
		}

		//TODO: This utilization computation does not seem right, come back and double check later.
		int totalComputation = 0;

		for (int i = 0; i < tasks.size(); i++) {
			int maxComputation = Collections.max(tasks.get(i).computations);
			totalComputation += maxComputation;
		}

		NumberFormat formatter = new DecimalFormat("#.##");
		for (int i = 0; i < tasks.size(); i++) {
			ArrayList<Double> utils = new ArrayList<>();

			for (int j = 0; j < tasks.get(i).computations.size(); j++) {
				double util = (double) tasks.get(i).computations.get(j) / (double) totalComputation
						* resourceNames.length * 100;
				int utilCut = (int) util;
				double utilFormat = Double.parseDouble(formatter.format((double) utilCut / (double) 100));
				utils.add(utilFormat);
				if (utilFormat > 1) {
					System.err.println("Util hihger than 1");
					System.exit(-1);
				}
			}

			tasks.get(i).util = utils;
			tasks.get(i).maxUtil = Collections.max(utils);
		}

		double u = 0;
		for (int i = 0; i < tasks.size(); i++) {
			u += Collections.max(tasks.get(i).util);
		}

		System.out.println("total Utilsation: " + u);

		return tasks;
	}

	private static int getProductIndex(String name, List<ProductionProcess> processes) {

		for (int i = 0; i < processes.size(); i++) {
			if (name.equals(processes.get(i).getName())) {
				return i;
			}
		}

		System.err.print("cannot by product via given name.");
		System.exit(-1);
		return -1;
	}

	public static Pair<Map<String, List<RecipeInfo>>, String[]> recipesAndResourceNames(List<Device> devices,
			List<ProductionProcess> processes) {
		final String[] resourceNames = devices.stream().map(d -> d.getName()).toArray(String[]::new);

		final java.util.function.Function<String[], List<Integer>> resourceIndices = (String[] names) -> {
			List<Integer> result = Arrays.asList(names).stream()
					.map((String nm) -> Arrays.asList(resourceNames).indexOf(nm)).collect(Collectors.toList());
			if (result.contains(-1))
				jeep.lang.Diag.println(Arrays.toString(names) + " contains bad string:\n" + result);

			return result;
		};

		final Map<String, List<RecipeInfo>> recipeInfo = new TreeMap<>(); // new NaturalOrderComparator());

		for (ProductionProcess process : processes) {
			recipeInfo.put(process.getName(), Lists
					.newArrayList(new RecipeInfo(process.getName(), process.getInstanceNumber(), resourceIndices.apply(
							process.getCompitableResource().stream().map(d -> d.getName()).toArray(String[]::new)))));
		}

		return Pair.of(recipeInfo, resourceNames);
	}

	public synchronized Workload getTasks() {
		return workload;
	}

}
