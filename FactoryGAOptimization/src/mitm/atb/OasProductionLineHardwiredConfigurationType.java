package mitm.atb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import metrics.ConfigurationType;
import metrics.ControlledMetricType;
import metrics.KeyObjectiveType;
import metrics.ObservableMetricType;
import metrics.SampleRate;
import metrics.SearchDirection;
import metrics.ValueType;

///////////////////////////////////

public class OasProductionLineHardwiredConfigurationType {

	public static final int[][] ExecutionTime = { { 100, 100, 100, 100 }, { 100, 100, 45, 45 }, { 120, 90, 60, 60 },
			{ 60, 45, 30, 30 } };

	private static int randomInRange(int min, int max, Random rng) {
		if (min >= max)
			throw new IllegalArgumentException();

		return rng.nextInt((max - min) + 1) + min;
	}

	public static final class ProductionLineInfo {
		final String name;
		final int siloID;
		final int mixerID;
		final int tankID;

		public ProductionLineInfo(String name, int siloID, int mixerIndex, int tankID) {
			this.name = name;
			this.siloID = siloID;
			this.mixerID = mixerIndex;
			this.tankID = tankID;
		}

		public String resourceName() {
			return "Silo(" + siloID + ") Mixer(" + mixerID + ") Tank(" + tankID + ")";
		}

		public String getName() {
			return name;
		}
	}

	public static List<ProductionLineInfo> generateProductionLines() {
		List<ProductionLineInfo> result = new ArrayList<>();
		// for (String recipeId : recipeIds)
		// for (int i = 0; i < numMixers; ++i)
		// result.add(new ProductionLineInfo(recipeId, i));

		result.add(new ProductionLineInfo("ProductionLine 1", 1, 1, 1));
		result.add(new ProductionLineInfo("ProductionLine 2", 1, 2, 1));
		result.add(new ProductionLineInfo("ProductionLine 3", 2, 3, 1));
		result.add(new ProductionLineInfo("ProductionLine 4", 2, 4, 1));
		result.add(new ProductionLineInfo("ProductionLine 5", 3, 5, 1));
		result.add(new ProductionLineInfo("ProductionLine 6", 3, 6, 2));
		result.add(new ProductionLineInfo("ProductionLine 7", 4, 7, 2));
		result.add(new ProductionLineInfo("ProductionLine 8", 4, 8, 2));
		result.add(new ProductionLineInfo("ProductionLine 9", 5, 9, 2));
		result.add(new ProductionLineInfo("ProductionLine 10", 5, 10, 2));

		return result;
	}

	///////////////////////////////

	public static Map<String, Integer> hardwiredAmountsProduced() {
		Map<String, Integer> amounts = new HashMap<>();
		// FIXME: values as per D3.1 p32
		amounts.put("Std Weiss A", 5);
		amounts.put("Std Weiss B", 10);
		amounts.put("Std Weiss C", 10);
		amounts.put("Std Weiss D", 10);

		amounts.put("Weiss Matt A", 5);
		amounts.put("Weiss Matt B", 10);
		amounts.put("Weiss Matt C", 10);
		amounts.put("Weiss Matt D", 10);

		amounts.put("Super Glanz A", 4);
		amounts.put("Super Glanz B", 8);
		amounts.put("Super Glanz C", 8);
		amounts.put("Super Glanz D", 8);

		amounts.put("Weiss Basis A", 6);
		amounts.put("Weiss Basis B", 12);
		amounts.put("Weiss Basis C", 12);
		amounts.put("Weiss Basis D", 12);

		return amounts;
	}

	public static int[] AmountRequired = { 45, 40, 30, 20 };

	public static ConfigurationType hardwiredConfigurationType(Random random, List<RecipeInfo> recipeInfoList) {
		// final String[] recipeIds = { "Std Weiss", "Weiss Matt", "Super Glanz", "Weiss
		// Basis" };

		final List<ProductionLineInfo> productionLineInfo = generateProductionLines();
		productionLineInfo.forEach(x -> System.out.println(x.resourceName()));

		final List<String> resourceNames = productionLineInfo.stream().map(x -> x.resourceName())
				.collect(Collectors.toList());

		final Map<String, List<RecipeInfo>> recipeInfo = new HashMap<>();

		recipeInfo.put("Std Weiss", Lists.newArrayList(
				new RecipeInfo("Std Weiss A", 9, IntStream.rangeClosed(0, 4).boxed().collect(Collectors.toList())),
				new RecipeInfo("Std Weiss B", 5, IntStream.rangeClosed(5, 6).boxed().collect(Collectors.toList())),
				new RecipeInfo("Std Weiss C", 5, IntStream.rangeClosed(7, 9).boxed().collect(Collectors.toList())),
				new RecipeInfo("Std Weiss D", 5, IntStream.rangeClosed(7, 9).boxed().collect(Collectors.toList()))));

		recipeInfo.put("Weiss Matt", Lists.newArrayList(
				new RecipeInfo("Weiss Matt A", 5, IntStream.rangeClosed(0, 4).boxed().collect(Collectors.toList())),
				new RecipeInfo("Weiss Matt B", 2, IntStream.rangeClosed(5, 6).boxed().collect(Collectors.toList())),
				new RecipeInfo("Weiss Matt C", 5, IntStream.rangeClosed(7, 9).boxed().collect(Collectors.toList())),
				new RecipeInfo("Weiss Matt D", 2, IntStream.rangeClosed(7, 9).boxed().collect(Collectors.toList()))));

		// recipeInfo.put("Super Glanz", Lists.newArrayList(
		// new RecipeInfo("Super Glanz A", 2, IntStream.rangeClosed(0,
		// 4).boxed().collect(Collectors.toList())), // ,
		// // 4000,
		// // 120
		// // ),
		// new RecipeInfo("Super Glanz B", 4, IntStream.rangeClosed(5,
		// 6).boxed().collect(Collectors.toList())), // ,
		// // 8000,
		// // 90
		// // ),
		// new RecipeInfo("Super Glanz C", 2, IntStream.rangeClosed(7,
		// 9).boxed().collect(Collectors.toList())), // ,
		// // 8000,
		// // 60
		// // ),
		// new RecipeInfo("Super Glanz D", 0, IntStream.rangeClosed(7,
		// 9).boxed().collect(Collectors.toList())) // ,
		// // 10000,
		// // 60
		// // )
		// // )
		// ));
		//
		// recipeInfo.put("Weiss Basis", Lists.newArrayList(
		// new RecipeInfo("Weiss Basis A", 11, IntStream.rangeClosed(0,
		// 4).boxed().collect(Collectors.toList())), // ,
		// // 6000,
		// // 60
		// // ),
		// new RecipeInfo("Weiss Basis B", 3, IntStream.rangeClosed(5,
		// 6).boxed().collect(Collectors.toList())), // ,
		// // 12000,
		// // 45
		// // ),
		// new RecipeInfo("Weiss Basis C", 1, IntStream.rangeClosed(7,
		// 9).boxed().collect(Collectors.toList())), // ,
		// // 12000,
		// // 30
		// // ),
		// new RecipeInfo("Weiss Basis D", 1, IntStream.rangeClosed(7,
		// 9).boxed().collect(Collectors.toList())) // ,
		// // 12000,
		// // 30
		// // )
		// // )
		// // );
		// ));

		recipeInfo.values().stream().forEach(r -> recipeInfoList.addAll(r));

		final Interval taskDurationInterval = new Interval(30 * 60, 120 * 60); // 0.5 to 2 hours
		return makeConfigurationType(recipeInfo, resourceNames, taskDurationInterval, random);
	}

	///////////////////////////

	public static ConfigurationType makeConfigurationType(Map<String, List<RecipeInfo>> recipeInfo,
			List<String> resourceNames, Interval taskDurationInterval, Random random) {

		final List<KeyObjectiveType> keyObjectiveTypes = new ArrayList<>();
		keyObjectiveTypes.add(new KeyObjectiveType("makespan", ValueType.realType(0, Double.MAX_VALUE), "n/a",
				SearchDirection.MINIMIZING));

		final List<ControlledMetricType> controlledMetricTypes = new ArrayList<>();
		final List<ObservableMetricType> observableMetricTypes = new ArrayList<>();

		// 1. Observable metric for availability of each resource:
		for (String resourceName : resourceNames)
			observableMetricTypes.add(new ObservableMetricType(resourceName + " availability", ValueType.intType(1, 1),
					"n/a", SampleRate.eventDriven));

		// List<String> singleResourceNames = new ArrayList<>();
		for (String resourceName : resourceNames) {
			String[] resourceArray = resourceName.split(" ");
			for (String name : resourceArray) {
				observableMetricTypes.add(new ObservableMetricType(resourceName + " mutex " + name,
						ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
			}

		}
		//
		// for (String name : singleResourceNames) {
		// observableMetricTypes.add(new ObservableMetricType(name + " mutex " + name,
		// ValueType.intType(1, 1),
		// "n/a", SampleRate.eventDriven));
		// }

		// count instances (used to set an upper bound on priority):
		int totalInstances = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet())
			for (RecipeInfo r : e.getValue())
				totalInstances += r.instances;

		final List<Integer> priorities = IntStream.rangeClosed(0, totalInstances).boxed().collect(Collectors.toList());

		// List<Integer> priorities = new ArrayList<>();
		// priorities.add(9);
		// priorities.add(5);
		// priorities.add(1);

		// create a randomised list of priorities:
		// Collections.shuffle(priorities, random);

		final List<Interval> intervals = new ArrayList<>();
		for (int i = 0; i < totalInstances; ++i) {
			// final int a = random.nextInt( ( maxDuration * totalInstances ) /
			// resourceNames.length );
			// final int b = random.nextInt( ( maxDuration * totalInstances ) /
			// resourceNames.length );
			// intervals.add( new Interval( Math.min(a,b), Math.max(a,b) ) );
			final int duration = randomInRange(taskDurationInterval.lower, taskDurationInterval.upper, random);
			intervals.add(new Interval(0, duration));
		}

		// Add types for controlled and observable metrics:
		int instanceCount = 0;
		int productsCount = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			int receiptCount = 0;
			for (RecipeInfo r : e.getValue()) {
				int interval = ExecutionTime[productsCount][receiptCount];
				for (int i = 0; i < r.instances; ++i) {
					final String instanceName = r.name + " " + i;

					// 2. Controlled metrics for allocation and for priority:

					final ValueType allocationValueType = ValueType.nominalType(instanceName + " allocation type ",
							r.compatibleResources.stream().map(index -> resourceNames.get(index))
									.toArray(String[]::new));
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

					// 3. Observable metric for start and end time of each recipe instance:

					// final int start = intervals.get(instanceCount).lower();
					// final int end = intervals.get(instanceCount).upper();
					// observableMetricTypes.add( new ObservableMetricType(instanceName + " start",
					// ValueType.intType(start, start), "n/a", SampleRate.eventDriven ) );
					// observableMetricTypes.add( new ObservableMetricType(instanceName + " end",
					// ValueType.intType(end, end), "n/a", SampleRate.eventDriven ) );

					for (String resourceName : resourceNames) {
						final String recipeAndResourceNamePrefix = instanceName + " " + resourceName;

						final int start = 0 /* intervals.get(instanceCount).lower() */;
						final int end = interval /* intervals.get(instanceCount).upper() */;
						observableMetricTypes.add(new ObservableMetricType(
								// instanceName + " start",
								recipeAndResourceNamePrefix + " start", ValueType.intType(start, start), "n/a",
								SampleRate.eventDriven));
						observableMetricTypes.add(new ObservableMetricType(
								// instanceName + " end",
								recipeAndResourceNamePrefix + " end", ValueType.intType(end, end), "n/a",
								SampleRate.eventDriven));
					}

					instanceCount += 1;
				}
				receiptCount++;
			}
			productsCount++;
		}

		///////////////////////////

		UoYEarlyPrototypeDemo.observableMetricTypes = observableMetricTypes;

		return new ConfigurationType.Explicit(keyObjectiveTypes, controlledMetricTypes);
	}

	///////////////////////////////

}

// End ///////////////////////////////////////////////////////////////
