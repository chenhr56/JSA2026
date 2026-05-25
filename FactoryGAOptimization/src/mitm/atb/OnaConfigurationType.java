package mitm.atb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import factoryModel.ONA.Device;
import factoryModel.ONA.ONAXMLReader;
import factoryModel.ONA.ProductionProcess;
import factoryModel.ONA.SubProcess;
import factoryModel.ONA.SubProcessRelation;
import metrics.ConfigurationType;
import metrics.ControlledMetricType;
import metrics.KeyObjectiveType;
import metrics.ObservableMetricType;
import metrics.SampleRate;
import metrics.SearchDirection;
import metrics.ValueType;


///////////////////////////////////

public final class OnaConfigurationType {

	// [多线程修复] ONAReader 保持 public 以兼容旧代码直接置 null 的调用方，但 presetup() 不再仅依赖 null 判断。
	public static ONAXMLReader ONAReader = null;

	public static List<String> objectives;
	public static List<Device> devices;
	public static List<String> devicesName;
	public static List<ProductionProcess> processes;
	public static List<SubProcessRelation> relation;
	private static List<SequenceDependentTaskInfo> setUps = null;

	// [多线程修复] 记录当前已加载的 scale，用于检测 scale 变化并触发重新初始化。
	// 原代码仅判断 ONAReader==null，多线程下不同 scale 切换时无法自动重新加载。
	private static int currentScale = -1;

	public static Map<String, List<String>> unAvailableTimes = new HashMap<>();

	public static synchronized void presetup() {
		// [多线程修复] 原条件: if (ONAReader == null)
		// 问题1: 仅判断 null，不感知 scale 变化。线程 A 加载 scale=3 后，线程 B 切换到 scale=8 时不会重新初始化。
		// 问题2: 外部代码（如 Para_Test_FactoryScale）直接 ONAReader=null 可在 presetup() 执行中途置空，
		//        导致后续 ONAReader.getObjectivesList() 抛出 NullPointerException。
		// 修复: (1) 增加 currentScale 跟踪，scale 变化时强制重新加载。
		//       (2) 使用局部变量 reader，最后才赋值给 ONAReader，防止中途被外部置 null。
		if (ONAReader == null || currentScale != factoryModel.ONA.ONAFactoryModel.scale) {
			ONAXMLReader reader = new ONAXMLReader();
			reader.readOASInput();

			objectives = ONAReader.getObjectivesList();
			devices = ONAReader.getResources();
			processes = ONAReader.getProcesses();
			relation = ONAReader.getRelations();
			setUps = ONAReader.getSetUps();

			devicesName = new ArrayList<>();
			for (Device d : devices) {
				String deviceID = d.getName();

				devicesName.add(deviceID);

				if (!unAvailableTimes.containsKey(deviceID) && d.getNotavailbaileTime().length() > 1) {
					unAvailableTimes.put(deviceID, Arrays.asList(d.getNotavailbaileTime().split(" ")));
				}
			}

			// [多线程修复] 在方法末尾、所有字段设置完毕后再赋值 ONAReader 和 currentScale，
			// 确保外部代码读到非 null 的 ONAReader 时，processes/devices 等字段已完整就绪。
			currentScale = factoryModel.ONA.ONAFactoryModel.scale;
			ONAReader = reader;
		}
	}

	public static List<SequenceDependentTaskInfo> getSetUps() {
		presetup();
		return setUps;
	}

	public static List<String> getObjectives() {
		presetup();
		return objectives;
	}

	private static ConfigurationType makeConfigurationType(Map<String, List<RecipeInfo>> recipeInfo,
			String[] resourceNames, Map<Pair<String, String>, Double> recipeAndResourceNameToEnergy,
			Map<Pair<String, String>, Double> recipeAndResourceNameToMontary,
			// int percentAvailability,
			Map<Pair<String, String>, Interval> recipeAndResourceNameToInterval,
			Map<Pair<String, String>, Boolean> mutices,
			Map<Pair<String, String>, Integer> recipeAndResourceNameToPreemptionPoints, boolean isMultiobjective,
			Random random) {
		// if( percentAvailability < 0 || percentAvailability > 100 )
		// throw new IllegalArgumentException("Expected percentage for
		// percentAvailability, found: " + percentAvailability );

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

		for (Entry<String, List<String>> entry : unAvailableTimes.entrySet()) {
			String resourceKey = entry.getKey();
			if (entry.getValue().size() > 0) {
				observableMetricTypes.add(new ObservableMetricType(
						resourceKey + " unAvailabileTime " + String.join("_", entry.getValue()),
						ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
			}
		}

		for (Map.Entry<Pair<String, String>, Boolean> e : mutices.entrySet()) {

			final ValueType vt = e.getValue() ? ValueType.intType(1, 1) : ValueType.intType(0, 0);
			observableMetricTypes.add(new ObservableMetricType(e.getKey().getLeft() + " mutex " + e.getKey().getRight(),
					vt, "n/a", SampleRate.eventDriven));
		}

		///////////////////////////

		// count instances (used to set an upper bound on priority):
//		int totalInstances = 0;
//		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
//			for (RecipeInfo r : e.getValue())
//				totalInstances += r.instances;
//		}

//		List<Integer> priorities = IntStream.rangeClosed(0, totalInstances).boxed().collect(Collectors.toList());

		// int productsCount = 0;
//		int instanceCount = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			for (RecipeInfo r : e.getValue()) {

				int productIndex = getProductIndex(r.name);
				List<SubProcess> subProcesses = processes.get(productIndex).getSubProcesses();

				for (int i = 0; i < r.instances; ++i) {
					final String instanceName = r.name + " " + i;

					// 2. Controlled metrics for allocation and for priority:

					List<Integer> allComptiables = r.compatibleResources;
					List<Integer> compatiables = new ArrayList<>();

					for (int j = 0; j < allComptiables.size(); j++) {
						String deviceName = resourceNames[allComptiables.get(j)];
						String[] deviceNameS = deviceName.split(" ");

						int mode = Integer.parseInt(deviceNameS[1]);

						if (mode >= i * 4 + 1 && mode <= i * 4 + 4) {
							compatiables.add(allComptiables.get(j));
						}
					}

					final ValueType allocationValueType = ValueType.nominalType(instanceName + " allocation type ",
							compatiables.stream().map(index -> resourceNames[index]).toArray(String[]::new));
					// e.g. Std Weiss A 1 allocation (nominal type, domain: Mixer 1, Mixer 2, Mixer
					// 3, Mixer 4, Mixer 5} :
					controlledMetricTypes
							.add(new ControlledMetricType(instanceName + " allocation", allocationValueType, "n/a"));

					// e.g. Std Weiss A 1 priority (Int type)
//					final int priorityValue = priorities.get(instanceCount);
					// ensure each priority value is unique - otherwise scheduling can get in an
					// infinite loop:
					controlledMetricTypes.add(new ControlledMetricType(instanceName + " priority",
							ValueType.intType(1, 1000), "n/a"));

					///////////////

					// Observable metric for start and end time of each (resource,recipe instance) :

					for (int j = 0; j < processes.get(productIndex).getCompitableResource().size(); j++) {

						String resourceName = processes.get(productIndex).getCompitableResource().get(j).getName();

						for (SubProcess sp : subProcesses) {
//							String spName = sp.getName().replaceFirst("cut", "preempt");
//							String[] spNameArray = spName.split(" ");

							String recipeAndResourceNamePrefix = instanceName + " " + resourceName
							/*
							 * + spNameArray[spNameArray.length - 2] + " " + spNameArray[spNameArray.length
							 * - 1]
							 */;

							int spProcessingTime = sp.getProcessingTime().get(j);
							int spEnergy = sp.getEnergyCost().get(j);
							int spMontary = sp.getMontaryCost().get(j);

							SubProcess spRelation = null;
							for (SubProcessRelation relation : relation) {
								if (relation.getDestination().getName().equals(sp.getName())) {
									spRelation = relation.getSource();
								}
							}

							if (spRelation != null) {
								String sourceName = spRelation.getName().replaceFirst("cut", "preempt");
								String[] sourceNameArray = sourceName.split(" ");
								String sourcePrefix = instanceName + " " + resourceName + " "
										+ sourceNameArray[sourceNameArray.length - 2] + " "
										+ sourceNameArray[sourceNameArray.length - 1];

								observableMetricTypes.add(new ObservableMetricType(
										recipeAndResourceNamePrefix + " executedAfter " + sourcePrefix,
										ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
							}

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

//					instanceCount += 1;
				}
			}
			// productsCount++;
		}

		///////////////////////////

		// Add a controlled metric which encodes a permutation of the user-specified
		// priority ordering
		// final double numPermutations = factorial(instanceCount);
		// if (numPermutations <= Integer.MAX_VALUE) {
		// final ValueType priorityPermutationIndexType = ValueType.realType(0,
		// numPermutations - 1);
		//
		// controlledMetricTypes
		// .add(new ControlledMetricType("priority-permutation-index",
		// priorityPermutationIndexType, "n/a"));
		// }

		// System.out.println(Integer.MAX_VALUE);

		///////////////////////////
		UoYEarlyPrototypeDemo.observableMetricTypes = observableMetricTypes;
		return new ConfigurationType.Explicit(keyObjectiveTypes, controlledMetricTypes);
	}

	private static int getProductIndex(String name) {

		for (int i = 0; i < processes.size(); i++) {
			if (name.equals(processes.get(i).getName())) {
				return i;
			}
		}

		System.err.print("cannot by product via given name.");
		System.exit(-1);
		return -1;
	}

	///////////////////////////////

	public static Pair<Map<String, List<RecipeInfo>>, String[]> recipesAndResourceNames() {
		presetup();
		final String[] resourceNames = devices.stream().map(d -> d.getName()).toArray(String[]::new);

		final java.util.function.Function<String[], List<Integer>> resourceIndices = (String[] names) -> {
			List<Integer> result = Arrays.asList(names).stream()
					.map((String nm) -> Arrays.asList(resourceNames).indexOf(nm)).collect(Collectors.toList());
			if (result.contains(-1))
				System.out.println(Arrays.toString(names) + " contains bad string:\n" + result);

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

	///////////////////////////////

	public static ConfigurationType configurationType(Map<String, List<RecipeInfo>> recipeInfo, String[] resourceNames,
			boolean isMultiobjective, Random random) {
		presetup();
		// Convention: resource names prefixes (up to the first space) denote the same
		// resource
		// hence they are mutually exclusive:
		final BiPredicate<String, String> mutex = (String resource1, String resource2) -> {
			final String[] s1 = resource1.split(" ");
			final String[] s2 = resource2.split(" ");
			return (s1.length == 0 && s2.length == 0) || (s1[1].equals(s2[1]));
		};

		final Map<Pair<String, String>, Boolean> mutices = new HashMap<>();
		for (String r1 : resourceNames)
			for (String r2 : resourceNames)
				mutices.put(Pair.of(r1, r2), mutex.test(r1, r2));

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

		final ConfigurationType ct = makeConfigurationType(recipeInfo, resourceNames, recipeAndResourceToEnergy,
				recipeAndResourceToMontary, recipeAndResourceToProcessingTime, mutices,
				recipeAndResourceNameToPreemptionPoints, isMultiobjective, random);

		// final SequenceDependentSetup sds =
		// makeSequenceDependantSetup(sequenceDependentTasks);
		// return Pair.of( ct, sequenceDependentTasks );
		return ct;
	}
}

// End ///////////////////////////////////////////////////////////////
