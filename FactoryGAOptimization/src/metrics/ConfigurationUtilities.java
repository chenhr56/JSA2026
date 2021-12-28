package metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import mitm.atb.UoYEarlyPrototypeDemo;

///////////////////////////////////

public final class ConfigurationUtilities {

	private static ConfigurationType makeConfigurationTypeImpl(Map<String, List<RecipeInfo>> recipeInfo,
			String[] resourceNames, List<KeyObjectiveType> keyObjectiveTypes, double maxCost, int maxExecutionTime,
			int maxPriority) {
		if (maxCost < 0)
			throw new IllegalArgumentException("Expected positive maxCost, found " + maxCost);
		if (maxExecutionTime < 0)
			throw new IllegalArgumentException("Expected positive maxExecutionTime, found " + maxExecutionTime);
		if (maxPriority < 0)
			throw new IllegalArgumentException("Expected positive maxPriority, found " + maxPriority);

		///////////////////////////

		final List<ControlledMetricType> controlledMetricTypes = new ArrayList<>();
		final List<ObservableMetricType> observableMetricTypes = new ArrayList<>();

		///////////////////////////

		// Observable metric for availability of each resource:

		for (String resourceName : resourceNames) {
			// e.g. Mixer 1 availability (int type, domain: 0,1)
			observableMetricTypes.add(new ObservableMetricType(resourceName + " availability", ValueType.intType(0, 1),
					"n/a", SampleRate.eventDriven));
		}

		///////////////////////////

		// Observable metric for mutual exclusiveness:

		for (String resourceName1 : resourceNames) {
			for (String resourceName2 : resourceNames) {
				observableMetricTypes.add(new ObservableMetricType(resourceName1 + " " + resourceName2 + " mutex",
						ValueType.intType(0, 1), "n/a", SampleRate.eventDriven));
			}
		}

		///////////////////////////

		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			for (RecipeInfo r : e.getValue()) {
				for (int i = 0; i < r.instances; ++i) {

					final String instanceName = r.name + " " + i;

					// Controlled metrics for allocation of resources:
					// e.g. Std Weiss A 1 allocation (nominal type, domain: Mixer 1, Mixer 2, Mixer
					// 3, Mixer 4, Mixer 5} :

					final ValueType allocationValueType = ValueType.nominalType(instanceName + " allocation type ",
							r.compatibleResources.stream().map(index -> resourceNames[index]).toArray(String[]::new));
					controlledMetricTypes
							.add(new ControlledMetricType(instanceName + " allocation", allocationValueType, "n/a"));

					// Controlled metrics for priority:
					// e.g. Std Weiss A 1 priority (Int type)
					controlledMetricTypes.add(new ControlledMetricType(instanceName + " priority",
							ValueType.intType(0, maxPriority), "n/a"));

					///////////////

					// Observable metric for start and end time of each (resource,recipe instance) :

					for (String resourceName : resourceNames) {
						final String recipeAndResourceNamePrefix = instanceName + " " + resourceName;

						final int start = 0;
						final int end = maxExecutionTime;

						observableMetricTypes.add(new ObservableMetricType(recipeAndResourceNamePrefix + " start",
								ValueType.intType(start, end), "n/a", SampleRate.eventDriven));
						observableMetricTypes.add(new ObservableMetricType(recipeAndResourceNamePrefix + " end",
								ValueType.intType(start, end), "n/a", SampleRate.eventDriven));
					}

					///////////////

					// Observable metric for cost for each (resource,recipe instance) :

					for (String resourceName : resourceNames) {
						final String recipeAndResourceNamePrefix = instanceName + " " + resourceName;

						observableMetricTypes.add(new ObservableMetricType(recipeAndResourceNamePrefix + " cost",
								ValueType.realType(0, maxCost), "n/a", SampleRate.eventDriven));
					}
				}
			}
		}

		UoYEarlyPrototypeDemo.observableMetricTypes = observableMetricTypes;
		return new ConfigurationType.Explicit(keyObjectiveTypes, controlledMetricTypes);
	}

	///////////////////////////////

	public static ConfigurationType makeConfigurationType(String[] resourceNames,
			Map<String, List<RecipeInfo>> recipeInfo) {

		final List<KeyObjectiveType> keyObjectiveTypes = new ArrayList<>();
		keyObjectiveTypes.add(new KeyObjectiveType("makespan", ValueType.realType(0, Double.MAX_VALUE), "n/a",
				SearchDirection.MINIMIZING));

		final double maxCost = Double.MAX_VALUE;
		final int maxExecutionTime = Integer.MAX_VALUE;
		final int maxPriority = Integer.MAX_VALUE;

		return ConfigurationUtilities.makeConfigurationTypeImpl(recipeInfo, resourceNames, keyObjectiveTypes, maxCost,
				maxExecutionTime, maxPriority);
	}

	///////////////////////////////

	public static Map<String, Value> defaultKeyObjectives(ConfigurationType ct) {
		final Random rng = new Random(0xDEADBEEF);
		final Map<String, Value> result = new HashMap<>();
		for (KeyObjectiveType kot : ct.getKeyObjectiveMetrics()) {
			result.put(kot.name, metrics.Utility.randomValue(kot.valueType, rng));
		}

		return result;
	}

	///////////////////////////////

	public static Configuration makeConfiguration(ConfigurationType ct, Map<String, List<RecipeInfo>> recipeInfo,
			String[] resourceNames, Predicate<String> resourceAvailability,
			BiFunction<RecipeInstanceId, String, Double> recipeInstanceAndResourceNameToCost,
			BiFunction<RecipeInstanceId, String, Integer> recipeInstanceAndResourceNameToExecutionTime,
			Function<RecipeInstanceId, Integer> recipeInstancePriority,
			BiFunction<String, String, Boolean> resourceMutex, Map<String, Value> keyObjectives) {

		///////////////////////////

		final Random rng = new Random(0xDEADBEEF);

		final Map<String, Value> controlledMetrics = new HashMap<>();
		final Map<String, Value> observableMetrics = new HashMap<>();

		///////////////////////////

		// Observable metric for availability of each resource:

		for (String resourceName : resourceNames) {
			// e.g. Mixer 1 availability (int type, domain: 0,1)
			final String key = resourceName + " availability";
			observableMetrics.put(key,
					Value.intValue(resourceAvailability.test(resourceName) ? 1 : 0, ValueType.intType(0, 1)));
		}

		///////////////////////////

		// Observable metric for mutual exclusiveness:

		for (String resourceName1 : resourceNames) {
			for (String resourceName2 : resourceNames) {
				final String key = resourceName1 + " " + resourceName2 + " mutex";
				observableMetrics.put(key, Value.intValue(resourceMutex.apply(resourceName1, resourceName2) ? 1 : 0,
						ValueType.intType(0, 1)));

			}
		}

		///////////////////////////

		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			for (RecipeInfo r : e.getValue()) {
				for (int i = 0; i < r.instances; ++i) {

					// final String instanceName = r.name + " " + i;
					final RecipeInstanceId instanceName = RecipeInstanceId.mk(r, i);

					// Controlled metrics for allocation:
					// e.g. Std Weiss A 1 allocation (nominal type, domain: Mixer 1, Mixer 2, Mixer
					// 3, Mixer 4, Mixer 5} :

					final String allocationKey = instanceName + " allocation";
					final ValueType allocationValueType = ValueType.nominalType(instanceName + " allocation type ",
							r.compatibleResources.stream().map(index -> resourceNames[index]).toArray(String[]::new));

					controlledMetrics.put(allocationKey, metrics.Utility.randomValue(allocationValueType, rng));

					// Controlled metrics for priority:
					// e.g. Std Weiss A 1 priority (Int type)
					final String priorityKey = instanceName + " priority";
					controlledMetrics.put(priorityKey, Value.intValue(recipeInstancePriority.apply(instanceName),
							ValueType.intType(0, Integer.MAX_VALUE)));

					///////////////

					// Observable metric for start and end time of each (resource,recipe instance) :

					for (String resourceName : resourceNames) {

						final String recipeAndResourceNamePrefix = instanceName + " " + resourceName;

//						final int start = 0;

						final String startKey = recipeAndResourceNamePrefix + " start";
						final String endKey = recipeAndResourceNamePrefix + " end";

						final Integer end = recipeInstanceAndResourceNameToExecutionTime.apply(instanceName,
								resourceName);

						observableMetrics.put(startKey, Value.intValue(0, ValueType.intType(0, Integer.MAX_VALUE)));
						observableMetrics.put(endKey, Value.intValue(end, ValueType.intType(0, Integer.MAX_VALUE)));
					}

					///////////////

					// Observable metric for cost for each (resource,recipe instance) :

					for (String resourceName : resourceNames) {
						final String recipeAndResourceNamePrefix = instanceName + " " + resourceName;

						final String costKey = recipeAndResourceNamePrefix + " cost";

						observableMetrics.put(costKey,
								Value.realValue(recipeInstanceAndResourceNameToCost.apply(instanceName, resourceName),
										ValueType.realType(0, Double.MAX_VALUE)));
					}
				}
			}
		}

		return new Configuration(ct, controlledMetrics, keyObjectives);
	}
}

// End ///////////////////////////////////////////////////////////////
