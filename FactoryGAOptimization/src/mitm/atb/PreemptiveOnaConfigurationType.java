package mitm.atb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

import uk.ac.york.safire.metrics.ConfigurationType;
import uk.ac.york.safire.metrics.ControlledMetricType;
import uk.ac.york.safire.metrics.KeyObjectiveType;
import uk.ac.york.safire.metrics.ObservableMetricType;
import uk.ac.york.safire.metrics.SampleRate;
import uk.ac.york.safire.metrics.SearchDirection;
import uk.ac.york.safire.metrics.ValueType;

///////////////////////////////////

public final class PreemptiveOnaConfigurationType {

	private static double factorial(double n) {
		assert (n == Math.floor(n));
		assert (n >= 0.0);
		if (n == 0)
			return 1;
		else
			return n * factorial(n - 1);
	}

	public static ConfigurationType makeConfigurationType(Map<String, List<RecipeInfo>> recipeInfo,
			String[] resourceNames, Map<Pair<String, String>, Double> recipeAndResourceNameToCost,
			// int percentAvailability,
			Map<Pair<String, String>, Interval> recipeAndResourceNameToInterval,
			Map<Pair<String, String>, Boolean> mutices,
			Map<Pair<String, String>, Integer> recipeAndResourceNameToPreemptionPoints, boolean isMultiobjective,
			Random random) {
//		if( percentAvailability < 0 || percentAvailability > 100 )
//			throw new IllegalArgumentException("Expected percentage for percentAvailability, found: " + percentAvailability );

		final List<KeyObjectiveType> keyObjectiveTypes = new ArrayList<>();
		keyObjectiveTypes.add(new KeyObjectiveType("makespan", ValueType.realType(0, Double.MAX_VALUE), "n/a",
				SearchDirection.MINIMIZING));
		if (isMultiobjective)
			keyObjectiveTypes.add(new KeyObjectiveType("totalcost", ValueType.realType(0, Double.MAX_VALUE), "n/a",
					SearchDirection.MINIMIZING));

		keyObjectiveTypes.add(new KeyObjectiveType("user preference violations",
				ValueType.realType(0, Double.MAX_VALUE), "n/a", SearchDirection.MINIMIZING));

		final List<ControlledMetricType> controlledMetricTypes = new ArrayList<>();
		final List<ObservableMetricType> observableMetricTypes = new ArrayList<>();

		///////////////////////////

		// Observable metric for availability of each resource:
		// int numResourcesUnavailable = 0;
		for (String resourceName : resourceNames) {
			// e.g. Mixer 1 availability (int type, domain: 0,1)
			// if( true ) // random.nextInt( 100 ) < percentAvailability )
			observableMetricTypes.add(new ObservableMetricType(resourceName + " availability", ValueType.intType(1, 1),
					"n/a", SampleRate.eventDriven));
//			else {
//				numResourcesUnavailable += 1;				
//				observableMetricTypes.add( new ObservableMetricType(resourceName + " availability", ValueType.intType(0, 0), "n/a", SampleRate.eventDriven ) );
//			}
		}

		// System.out.println();
		// System.out.println( "Resources available: " + ( resourceNames.length -
		// numResourcesUnavailable ) + " (of " + resourceNames.length + ")" );

		///////////////////////////

		// Observable metric for mutual exclusiveness:

		for (Map.Entry<Pair<String, String>, Boolean> e : mutices.entrySet()) {

			// final String resourcePairPrefix = e.getKey().getFirst() + " " +
			// e.getKey().getSecond();

			final ValueType vt = e.getValue() ? ValueType.intType(1, 1) : ValueType.intType(0, 0);
			observableMetricTypes.add(new ObservableMetricType(e.getKey().getLeft() + " mutex " + e.getKey().getRight(),
					vt, "n/a", SampleRate.eventDriven));
		}

		///////////////////////////

		// count instances (used to set an upper bound on priority):
		int totalInstances = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			for (RecipeInfo r : e.getValue())
				totalInstances += r.instances;
		}

		List<Integer> priorities = IntStream.rangeClosed(0, totalInstances).boxed().collect(Collectors.toList());
		// randomise priorities:
		// Collections.shuffle(priorities, random);

		///////////////////////////

		// Add types for controlled and observable metrics:
		int instanceCount = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			for (RecipeInfo r : e.getValue()) {
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

					///////////////

					// Observable metric for start and end time of each (resource,recipe instance) :

					for (String resourceName : resourceNames) {
						final String recipeAndResourceNamePrefix = instanceName + " " + resourceName; // r.name + " " +
																										// resourceName;

						final Interval interval = recipeAndResourceNameToInterval.get(Pair.of(r.name, resourceName));
						if (interval != null) {
							final int start = interval.lower;
							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " start",
									recipeAndResourceNamePrefix + " start", ValueType.intType(start, start), "n/a",
									SampleRate.eventDriven));

							final int end = interval.upper;
							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " end",
									recipeAndResourceNamePrefix + " end", ValueType.intType(end, end), "n/a",
									SampleRate.eventDriven));
						}
					}

					///////////////

					// Observable metric for cost for each (resource,recipe instance) :

					for (String resourceName : resourceNames) {
						final String recipeAndResourceNamePrefix = instanceName + " " + resourceName; // r.name + " " +
																										// resourceName;

						final Double cost = recipeAndResourceNameToCost.get(Pair.of(r.name, resourceName));
						if (cost != null) {
							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " cost",
									recipeAndResourceNamePrefix + " cost", ValueType.realType(cost, cost), "n/a",
									SampleRate.eventDriven));
						}
					}

					///////////////

					// Observable metric for pre-emption points for each (resource,recipe instance)
					// :

					for (String resourceName : resourceNames) {
						final String recipeAndResourceNamePrefix = instanceName + " " + resourceName;

						final Integer numPoints = recipeAndResourceNameToPreemptionPoints
								.get(Pair.of(r.name, resourceName));
						if (numPoints != null) {
							observableMetricTypes
									.add(new ObservableMetricType(recipeAndResourceNamePrefix + " preemption-points",
											ValueType.intType(numPoints, numPoints), "n/a", SampleRate.eventDriven));
						}
					}

					instanceCount += 1;
				}
			}
		}

		///////////////////////////

		// Add a controlled metric which encodes a permutation of the user-specified
		// priority ordering
		final double numPermutations = factorial(instanceCount);
		if (numPermutations > Integer.MAX_VALUE)
			throw new IllegalArgumentException(
					"Too many tasks (" + instanceCount + ") for integer representation of permutations");

		final ValueType priorityPermutationIndexType = ValueType.intType(0, (int) numPermutations - 1);
		// e.g. Std Weiss A 1 allocation (nominal type, domain: Mixer 1, Mixer 2, Mixer
		// 3, Mixer 4, Mixer 5} :
		controlledMetricTypes
				.add(new ControlledMetricType("priority-permutation-index", priorityPermutationIndexType, "n/a"));

		///////////////////////////

		UoYEarlyPrototypeDemo.observableMetricTypes = observableMetricTypes;
		return new ConfigurationType.Explicit(keyObjectiveTypes, controlledMetricTypes);
	}

	///////////////////////////////

	public static Pair<Map<String, List<RecipeInfo>>, String[]> toyExampleRecipesAndResourceNames() {

		final String[] resourceNames = { "Large 1", "Large 2", "Large 3", "Large 4" };

		final java.util.function.Function<String[], List<Integer>> resourceIndices = (String[] names) -> {
			List<Integer> result = Arrays.asList(names).stream()
					.map((String nm) -> Arrays.asList(resourceNames).indexOf(nm)).collect(Collectors.toList());
			if (result.contains(-1))
				System.out.println(Arrays.toString(names) + " contains bad string:\n" + result);

			return result;
		};

		final Map<String, List<RecipeInfo>> recipeInfo = new TreeMap<>(); // new NaturalOrderComparator());

		List<RecipeInfo> reciList = new ArrayList<>();
		reciList.add(new RecipeInfo("P13", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" })));
		recipeInfo.put("P13", reciList);

		List<RecipeInfo> reciList1 = new ArrayList<>();
		reciList1.add(new RecipeInfo("P14", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" })));
		recipeInfo.put("P14", reciList1);

		List<RecipeInfo> reciList2 = new ArrayList<>();
		reciList2.add(new RecipeInfo("P15", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" })));
		recipeInfo.put("P15", reciList2);

		return Pair.of(recipeInfo, resourceNames);
	}

	///////////////////////////////

	public static ConfigurationType configurationType(Map<String, List<RecipeInfo>> recipeInfo, String[] resourceNames,
			boolean isMultiobjective, Random random) {

		// Convention: resource names prefixes (up to the first space) denote the same
		// resource
		// hence they are mutually exclusive:
		final BiPredicate<String, String> mutex = (String resource1, String resource2) -> {
			final String[] s1 = resource1.split(" ");
			final String[] s2 = resource2.split(" ");
			return (s1.length == 0 && s2.length == 0) || (s1[0].equals(s2[0]));
		};

		final Map<Pair<String, String>, Boolean> mutices = new HashMap<>();
		for (String r1 : resourceNames)
			for (String r2 : resourceNames)
				mutices.put(Pair.of(r1, r2), mutex.test(r1, r2));

		final Map<Pair<String, String>, Double> recipeAndResourceNameToCost = new HashMap<>();

		recipeAndResourceNameToCost.put(Pair.of("P13", "Large 1"), 4651.4);
		recipeAndResourceNameToCost.put(Pair.of("P13", "Large 2"), 6573.1);
		recipeAndResourceNameToCost.put(Pair.of("P13", "Large 3"), 4255.4);
		recipeAndResourceNameToCost.put(Pair.of("P13", "Large 4"), 3566.2);

		recipeAndResourceNameToCost.put(Pair.of("P14", "Large 1"), 6201.9);
		recipeAndResourceNameToCost.put(Pair.of("P14", "Large 2"), 8764.1);
		recipeAndResourceNameToCost.put(Pair.of("P14", "Large 3"), 5673.8);
		recipeAndResourceNameToCost.put(Pair.of("P14", "Large 4"), 4754.9);

		recipeAndResourceNameToCost.put(Pair.of("P15", "Large 1"), 6201.9);
		recipeAndResourceNameToCost.put(Pair.of("P15", "Large 2"), 8764.1);
		recipeAndResourceNameToCost.put(Pair.of("P15", "Large 3"), 5673.8);
		recipeAndResourceNameToCost.put(Pair.of("P15", "Large 4"), 4754.9);

		///////////////////////////

		final Map<Pair<String, String>, Interval> recipeAndResourceNameToInterval = new HashMap<>();

		recipeAndResourceNameToInterval.put(Pair.of("P13", "Large 1"), new Interval(0, (int) (60 * 7542.9)));
		recipeAndResourceNameToInterval.put(Pair.of("P13", "Large 2"), new Interval(0, (int) (60 * 10775.5)));
		recipeAndResourceNameToInterval.put(Pair.of("P13", "Large 3"), new Interval(0, (int) (60 * 6675.1)));
		recipeAndResourceNameToInterval.put(Pair.of("P13", "Large 4"), new Interval(0, (int) (60 * 5280.0)));

		recipeAndResourceNameToInterval.put(Pair.of("P14", "Large 1"), new Interval(1, (int) (60 * 10057.1)));
		recipeAndResourceNameToInterval.put(Pair.of("P14", "Large 2"), new Interval(1, (int) (60 * 14367.3)));
		recipeAndResourceNameToInterval.put(Pair.of("P14", "Large 3"), new Interval(1, (int) (60 * 8900.1)));
		recipeAndResourceNameToInterval.put(Pair.of("P14", "Large 4"), new Interval(1, (int) (60 * 7040.0)));

		recipeAndResourceNameToInterval.put(Pair.of("P15", "Large 1"), new Interval(40000, (int) (60 * 10057.1)));
		recipeAndResourceNameToInterval.put(Pair.of("P15", "Large 2"), new Interval(40000, (int) (60 * 14367.3)));
		recipeAndResourceNameToInterval.put(Pair.of("P15", "Large 3"), new Interval(40000, (int) (60 * 8900.1)));
		recipeAndResourceNameToInterval.put(Pair.of("P15", "Large 4"), new Interval(40000, (int) (60 * 7040.0)));

		final Map<Pair<String, String>, Integer> recipeAndResourceNameToPreemptionPoints = new HashMap<>();

		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P13", "Large 1"), 10);
		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P13", "Large 2"), 10);
		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P13", "Large 3"), 10);
		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P13", "Large 4"), 10);

		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P14", "Large 1"), 10);
		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P14", "Large 2"), 10);
		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P14", "Large 3"), 10);
		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P14", "Large 4"), 10);

		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P15", "Large 1"), 10);
		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P15", "Large 2"), 10);
		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P15", "Large 3"), 10);
		recipeAndResourceNameToPreemptionPoints.put(Pair.of("P15", "Large 4"), 10);

		///////////////////////////

		final ConfigurationType ct = makeConfigurationType(recipeInfo, resourceNames, recipeAndResourceNameToCost,
				recipeAndResourceNameToInterval, mutices, recipeAndResourceNameToPreemptionPoints, isMultiobjective,
				random);

		// final SequenceDependentSetup sds =
		// makeSequenceDependantSetup(sequenceDependentTasks);
		// return Pair.of( ct, sequenceDependentTasks );
		return ct;
	}
}

// End ///////////////////////////////////////////////////////////////
