package mitm.atb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.util.Pair;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import metrics.ConfigurationType;
import metrics.ControlledMetricType;
import metrics.KeyObjectiveType;
import metrics.ObservableMetricType;
import metrics.SampleRate;
import metrics.SearchDirection;
import metrics.ValueType;

///////////////////////////////////

public class OnaHardwiredConfigurationType {

	/**********************
	 * // Version without explicit unavailability times: public static
	 * ConfigurationType makeConfigurationType(Map< String, List<RecipeInfo> >
	 * recipeInfo, String [] resourceNames, Map< Pair< String, String >, Double >
	 * recipeAndResourceNameToCost, int percentAvailability, Map< Pair< String,
	 * String >, Integer > recipeAndResourceNameToCuttingTime, Map< Pair< String,
	 * String >, Boolean > mutices, boolean isMultiobjective, Random random) { if(
	 * percentAvailability < 0 || percentAvailability > 100 ) throw new
	 * IllegalArgumentException("Expected percentage for percentAvailability, found:
	 * " + percentAvailability );
	 * 
	 * final List< KeyObjectiveType > keyObjectiveTypes = new ArrayList<>();
	 * keyObjectiveTypes.add( new KeyObjectiveType("makespan", ValueType.realType(0,
	 * Double.MAX_VALUE), "n/a", SearchDirection.MINIMIZING ) ); if(
	 * isMultiobjective ) keyObjectiveTypes.add( new KeyObjectiveType("totalcost",
	 * ValueType.realType(0, Double.MAX_VALUE), "n/a", SearchDirection.MINIMIZING )
	 * );
	 * 
	 * final List< ControlledMetricType > controlledMetricTypes = new ArrayList<>();
	 * final List< ObservableMetricType > observableMetricTypes = new ArrayList<>();
	 * 
	 * ///////////////////////////
	 * 
	 * // Observable metric for availability of each resource: int
	 * numResourcesUnavailable = 0; for( String resourceName: resourceNames ) { //
	 * e.g. Mixer 1 availability (int type, domain: 0,1) if( true ) //
	 * random.nextInt( 100 ) < percentAvailability ) observableMetricTypes.add( new
	 * ObservableMetricType(resourceName + " availability", ValueType.intType(1, 1),
	 * "n/a", SampleRate.eventDriven ) ); else { numResourcesUnavailable += 1;
	 * observableMetricTypes.add( new ObservableMetricType(resourceName + "
	 * availability", ValueType.intType(0, 0), "n/a", SampleRate.eventDriven ) ); }
	 * }
	 * 
	 * System.out.println(); System.out.println( "Resources available: " + (
	 * resourceNames.length - numResourcesUnavailable ) + " (of " +
	 * resourceNames.length + ")" );
	 * 
	 * ///////////////////////////
	 * 
	 * // Observable metric for mutual exclusiveness:
	 * 
	 * for( Map.Entry< Pair< String, String >, Boolean > e: mutices.entrySet() ) {
	 * 
	 * // final String resourcePairPrefix = e.getKey().getFirst() + " " +
	 * e.getKey().getSecond();
	 * 
	 * final ValueType vt = e.getValue() ? ValueType.intType(1, 1) :
	 * ValueType.intType(0, 0); // observableMetricTypes.add( // new
	 * ObservableMetricType(resourcePairPrefix + " mutex", // vt, "n/a",
	 * SampleRate.eventDriven ) ); observableMetricTypes.add( new
	 * ObservableMetricType( e.getKey().getFirst() + " mutex " +
	 * e.getKey().getSecond(), vt, "n/a", SampleRate.eventDriven ) ); }
	 * 
	 * ///////////////////////////
	 * 
	 * // count instances (used to set an upper bound on priority): int
	 * totalInstances = 0; for( Map.Entry<String, List<RecipeInfo> > e:
	 * recipeInfo.entrySet() ) for( RecipeInfo r: e.getValue() ) totalInstances +=
	 * r.instances;
	 * 
	 * // create a randomised list of priorities: List<Integer> priorities =
	 * IntStream.rangeClosed(0, totalInstances)
	 * .boxed().collect(Collectors.toList()); Collections.shuffle(priorities,
	 * random);
	 * 
	 * ///////////////////////////
	 * 
	 * // Add types for controlled and observable metrics: int instanceCount = 0;
	 * for( Map.Entry<String, List<RecipeInfo> > e: recipeInfo.entrySet() ) { for(
	 * RecipeInfo r: e.getValue() ) { for( int i=0; i<r.instances; ++i ) { final
	 * String instanceName = r.name + " " + i;
	 * 
	 * // 2. Controlled metrics for allocation and for priority:
	 * 
	 * final ValueType allocationValueType = ValueType.nominalType( instanceName + "
	 * allocation type ", r.compatibleResources.stream().map( index ->
	 * resourceNames[ index ] ).toArray(String[]::new) ); // e.g. Std Weiss A 1
	 * allocation (nominal type, domain: Mixer 1, Mixer 2, Mixer 3, Mixer 4, Mixer
	 * 5} : controlledMetricTypes.add( new ControlledMetricType(instanceName + "
	 * allocation", allocationValueType, "n/a") );
	 * 
	 * // e.g. Std Weiss A 1 priority (Int type) final int priorityValue =
	 * priorities.get(instanceCount); // ensure each priority value is unique -
	 * otherwise scheduling can get in an infinite loop: controlledMetricTypes.add(
	 * new ControlledMetricType(instanceName + " priority",
	 * ValueType.intType(priorityValue, priorityValue), "n/a") );
	 * 
	 * ///////////////
	 * 
	 * // Observable metric for start and end time of each (resource,recipe
	 * instance) :
	 * 
	 * for( String resourceName: resourceNames ) { final String
	 * recipeAndResourceNamePrefix = instanceName + " " + resourceName; // r.name +
	 * " " + resourceName;
	 * 
	 * // if( recipeAndResourceNameToCuttingTime.get(
	 * Pair.create(r.name,resourceName ) ) == null ) // jeep.lang.Diag.println( "No
	 * recipeAndResourceNameToCuttingTime entry for " +
	 * Pair.create(r.name,resourceName ) );
	 * 
	 * final Integer end = recipeAndResourceNameToCuttingTime.get(
	 * Pair.create(r.name,resourceName ) ); // intervals.get(instanceCount).upper();
	 * if( end != null ) { final int start = 0; observableMetricTypes.add( new
	 * ObservableMetricType( // metricNamePrefix + " start",
	 * recipeAndResourceNamePrefix + " start", ValueType.intType(start, start),
	 * "n/a", SampleRate.eventDriven ) ); observableMetricTypes.add( new
	 * ObservableMetricType( // metricNamePrefix + " end",
	 * recipeAndResourceNamePrefix + " end", ValueType.intType(end, end), "n/a",
	 * SampleRate.eventDriven ) ); } }
	 * 
	 * ///////////////
	 * 
	 * // Observable metric for cost for each (resource,recipe instance) :
	 * 
	 * for( String resourceName: resourceNames ) { final String
	 * recipeAndResourceNamePrefix = instanceName + " " + resourceName; // r.name +
	 * " " + resourceName;
	 * 
	 * final Double cost = recipeAndResourceNameToCost.get(
	 * Pair.create(r.name,resourceName ) ); if( cost != null ) {
	 * observableMetricTypes.add( new ObservableMetricType( // metricNamePrefix + "
	 * cost", recipeAndResourceNamePrefix + " cost", ValueType.realType(cost, cost),
	 * "n/a", SampleRate.eventDriven ) ); } }
	 * 
	 * ///////////////
	 * 
	 * instanceCount += 1; } } }
	 * 
	 * return new ConfigurationType.Explicit(keyObjectiveTypes,
	 * controlledMetricTypes, observableMetricTypes ); }
	 **********************/

	///////////////////////////////

	public static ConfigurationType makeConfigurationType(Map<String, List<RecipeInfo>> recipeInfo,
			String[] resourceNames, Map<Pair<String, String>, Double> recipeAndResourceNameToCost,
			int percentAvailability, Map<Pair<String, String>, Integer> recipeAndResourceNameToCuttingTime,
			Map<Pair<String, String>, Boolean> mutices, boolean isMultiobjective, Random random) {
		if (percentAvailability < 0 || percentAvailability > 100)
			throw new IllegalArgumentException(
					"Expected percentage for percentAvailability, found: " + percentAvailability);

		final List<KeyObjectiveType> keyObjectiveTypes = new ArrayList<>();
		keyObjectiveTypes.add(new KeyObjectiveType("makespan", ValueType.realType(0, Double.MAX_VALUE), "n/a",
				SearchDirection.MINIMIZING));
		if (isMultiobjective)
			keyObjectiveTypes.add(new KeyObjectiveType("totalcost", ValueType.realType(0, Double.MAX_VALUE), "n/a",
					SearchDirection.MINIMIZING));

		final List<ControlledMetricType> controlledMetricTypes = new ArrayList<>();
		final List<ObservableMetricType> observableMetricTypes = new ArrayList<>();

		for (String resourceName : resourceNames) {
			if (true)
				observableMetricTypes.add(new ObservableMetricType(resourceName + " availability",
						ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
		}

		int totalCuttingTimes = 0;
		for (Map.Entry<Pair<String, String>, Integer> e : recipeAndResourceNameToCuttingTime.entrySet())
			totalCuttingTimes += e.getValue();

		final Multiset<String> unavailableResources = HashMultiset.create();
		for (int i = 0; i < (100 - percentAvailability) * resourceNames.length; ++i)
			unavailableResources.add(resourceNames[random.nextInt(resourceNames.length)]);

		// Observable metric for each unavailability time of each resource:
		for (Multiset.Entry<String> e : unavailableResources.entrySet()) {
			final String resourceName = e.getElement();
			for (int i = 0; i < e.getCount(); ++i) {

				final int r1 = random.nextInt(totalCuttingTimes);
				final int r2 = random.nextInt(totalCuttingTimes);
				final int start = Math.min(r1, r2);
				final int end = Math.max(r1, r2);

				observableMetricTypes.add(new ObservableMetricType(resourceName + " " + i + " unavailable start",
						ValueType.intType(start, start), "n/a", SampleRate.eventDriven));
				observableMetricTypes.add(new ObservableMetricType(resourceName + " " + i + " unavailable end",
						ValueType.intType(end, end), "n/a", SampleRate.eventDriven));
			}
		}

		for (Map.Entry<Pair<String, String>, Boolean> e : mutices.entrySet()) {

			final ValueType vt = e.getValue() ? ValueType.intType(1, 1) : ValueType.intType(0, 0);
			observableMetricTypes.add(new ObservableMetricType(
					e.getKey().getFirst() + " mutex " + e.getKey().getSecond(), vt, "n/a", SampleRate.eventDriven));
		}

		int totalInstances = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet())
			for (RecipeInfo r : e.getValue())
				totalInstances += r.instances;

		// create a randomised list of priorities:
		List<Integer> priorities = IntStream.rangeClosed(0, totalInstances).boxed().collect(Collectors.toList());
		Collections.shuffle(priorities, random);

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

//						if( recipeAndResourceNameToCuttingTime.get( Pair.create(r.name,resourceName ) ) == null )
//							jeep.lang.Diag.println( "No recipeAndResourceNameToCuttingTime entry for " +  Pair.create(r.name,resourceName ) );

						final Integer end = recipeAndResourceNameToCuttingTime.get(Pair.create(r.name, resourceName)); // intervals.get(instanceCount).upper();
						if (end != null) {
							final int start = 0;
							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " start",
									recipeAndResourceNamePrefix + " start", ValueType.intType(start, start), "n/a",
									SampleRate.eventDriven));
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

						final Double cost = recipeAndResourceNameToCost.get(Pair.create(r.name, resourceName));
						if (cost != null) {
							observableMetricTypes.add(new ObservableMetricType(
									// metricNamePrefix + " cost",
									recipeAndResourceNamePrefix + " cost", ValueType.realType(cost, cost), "n/a",
									SampleRate.eventDriven));
						}
					}

					///////////////

					instanceCount += 1;
				}
			}
		}

		UoYEarlyPrototypeDemo.observableMetricTypes = observableMetricTypes;
		return new ConfigurationType.Explicit(keyObjectiveTypes, controlledMetricTypes);
	}

	///////////////////////////////

	public static ConfigurationType toyConfigurationType(int percentageAvailability, boolean isMultiobjective,
			Random random) {

		final String[] resourceNames = { "Small 1", "Medium 1", "Large 1", "Small 2", "Medium 2", "Large 2", "Small 3",
				"Medium 3", "Large 3", "Small 4", "Medium 4", "Large 4" };

		// Convention: resource names prefixes (up to the first space) denote the same
		// resource
		// hence they are mutually exclusive:
		final BiPredicate<String, String> mutex = (String resource1, String resource2) -> {
			final String[] s1 = resource1.split(" ");
			final String[] s2 = resource2.split(" ");
			return (s1.length == 0 && s2.length == 0) || (s1[0].equals(s2[0]));
		};

		final java.util.function.Function<String[], List<Integer>> resourceIndices = (String[] names) -> {
			List<Integer> result = Arrays.asList(names).stream()
					.map((String nm) -> Arrays.asList(resourceNames).indexOf(nm)).collect(Collectors.toList());
			if (result.contains(-1))
				System.out.println(Arrays.toString(names) + " contains bad string:\n" + result);

			return result;
		};

		final Map<Pair<String, String>, Boolean> mutices = new HashMap<>();
		for (String r1 : resourceNames)
			for (String r2 : resourceNames)
				mutices.put(Pair.create(r1, r2), mutex.test(r1, r2));

		final Map<String, List<RecipeInfo>> recipeInfo = new HashMap<>();

		recipeInfo.put("P4", Lists.newArrayList(new RecipeInfo("P4", 1, resourceIndices.apply(new String[] { "Medium 1",
				"Large 1", "Medium 2", "Large 2", "Medium 3", "Large 3", "Medium 4", "Large 4" }))));

		recipeInfo.put("P12", Lists.newArrayList(new RecipeInfo("P12", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		recipeInfo.put("P13", Lists.newArrayList(new RecipeInfo("P13", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		recipeInfo.put("P14", Lists.newArrayList(new RecipeInfo("P14", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		final Map<Pair<String, String>, Double> recipeAndResourceNameToCost = new HashMap<>();

		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 1"), 1004.3 * 10);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 1"), 2185.7);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 2"), 1224.4 * 10);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 2"), 2676.2);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 3"), 956.4 * 10);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 3"), 2004.6);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 4"), 1015.4 * 10);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 4"), 2062.6);

		recipeAndResourceNameToCost.put(Pair.create("P12", "Large 1"), 2058.4);
		recipeAndResourceNameToCost.put(Pair.create("P12", "Large 2"), 2908.8);
		recipeAndResourceNameToCost.put(Pair.create("P12", "Large 3"), 1883.1);
		recipeAndResourceNameToCost.put(Pair.create("P12", "Large 4"), 1816.1);

		recipeAndResourceNameToCost.put(Pair.create("P13", "Large 1"), 4651.4);
		recipeAndResourceNameToCost.put(Pair.create("P13", "Large 2"), 6573.1);
		recipeAndResourceNameToCost.put(Pair.create("P13", "Large 3"), 4255.4);
		recipeAndResourceNameToCost.put(Pair.create("P13", "Large 4"), 3566.2);

		recipeAndResourceNameToCost.put(Pair.create("P14", "Large 1"), 6201.9);
		recipeAndResourceNameToCost.put(Pair.create("P14", "Large 2"), 8764.1);
		recipeAndResourceNameToCost.put(Pair.create("P14", "Large 3"), 5673.8);
		recipeAndResourceNameToCost.put(Pair.create("P14", "Large 4"), 4754.9);

		///////////////////////////

		final Map<Pair<String, String>, Integer> recipeAndResourceNameToCuttingTime = new HashMap<>();
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 1"), (int) (60 * 3544.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 1"), (int) (60 * 3544.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 2"), (int) (60 * 4355.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 2"), (int) (60 * 4355.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 3"), (int) (60 * 3144.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 3"), (int) (60 * 3144.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 4"), (int) (60 * 3141.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 4"), (int) (60 * 3141.7));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large 1"), (int) (60 * 3337.9));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large 2"), (int) (60 * 4768.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large 3"), (int) (60 * 2953.9));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large 4"), (int) (60 * 2688.9));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large 1"), (int) (60 * 7542.9));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large 2"), (int) (60 * 10775.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large 3"), (int) (60 * 6675.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large 4"), (int) (60 * 5280.0));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large 1"), (int) (60 * 10057.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large 2"), (int) (60 * 14367.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large 3"), (int) (60 * 8900.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large 4"), (int) (60 * 7040.0));

		///////////////////////////

		return makeConfigurationType(recipeInfo, resourceNames, recipeAndResourceNameToCost, percentageAvailability,
				recipeAndResourceNameToCuttingTime, mutices, isMultiobjective, random);
	}

	///////////////////////////////

	public static ConfigurationType hardwiredConfigurationType(int percentageAvailability, boolean isMultiobjective,
			Random random) {

		final String[] resourceNames = { "Small 1", "Medium 1", "Large 1", "Small 2", "Medium 2", "Large 2", "Small 3",
				"Medium 3", "Large 3", "Small 4", "Medium 4", "Large 4" };

		// Convention: resource names prefixes (up to the first space) denote the same
		// resource
		// hence they are mutually exclusive:
		final BiPredicate<String, String> mutex = (String resource1, String resource2) -> {
			final String[] s1 = resource1.split(" ");
			final String[] s2 = resource2.split(" ");
			return (s1.length == 0 && s2.length == 0) || (s1[0].equals(s2[0]));
		};

		final java.util.function.Function<String[], List<Integer>> resourceIndices = (String[] names) -> {
			List<Integer> result = Arrays.asList(names).stream()
					.map((String nm) -> Arrays.asList(resourceNames).indexOf(nm)).collect(Collectors.toList());
			if (result.contains(-1))
				System.out.println(Arrays.toString(names) + " contains bad string:\n" + result);

			return result;
		};

		final Map<Pair<String, String>, Boolean> mutices = new HashMap<>();
		for (String r1 : resourceNames)
			for (String r2 : resourceNames)
				mutices.put(Pair.create(r1, r2), mutex.test(r1, r2));

// Ints.asList(0,1,2,3,4,5,6,7,8);

		final Map<String, List<RecipeInfo>> recipeInfo = new HashMap<>();
		// recipeInfo.put( "P1", Lists.newArrayList( new RecipeInfo( "P1 recipe", 1,
		// Ints.asList(0,1,2,3,4,5,6,7,8) ) ) );

		recipeInfo.put("P1", Lists.newArrayList(new RecipeInfo("P1", 1, resourceIndices.apply(resourceNames))));
		recipeInfo.put("P2", Lists.newArrayList(new RecipeInfo("P2", 1, resourceIndices.apply(resourceNames))));
		recipeInfo.put("P3", Lists.newArrayList(new RecipeInfo("P3", 1, resourceIndices.apply(new String[] { "Medium 1",
				"Large 1", "Medium 2", "Large 2", "Medium 3", "Large 3", "Medium 4", "Large 4" }))));

		recipeInfo.put("P4", Lists.newArrayList(new RecipeInfo("P4", 1, resourceIndices.apply(new String[] { "Medium 1",
				"Large 1", "Medium 2", "Large 2", "Medium 3", "Large 3", "Medium 4", "Large 4" }))));

		recipeInfo.put("P5", Lists.newArrayList(new RecipeInfo("P5", 1, resourceIndices.apply(new String[] { "Medium 1",
				"Large 1", "Medium 2", "Large 2", "Medium 3", "Large 3", "Medium 4", "Large 4" }))));

		recipeInfo.put("P6", Lists.newArrayList(new RecipeInfo("P6", 1, resourceIndices.apply(new String[] { "Medium 1",
				"Large 1", "Medium 2", "Large 2", "Medium 3", "Large 3", "Medium 4", "Large 4" }))));

		/*
		 * recipeInfo.put( "P7", Lists.newArrayList( new RecipeInfo( "P7", 1,
		 * resourceIndices.apply(new String [] { "Large 1", "Large 2", "Large 3",
		 * "Large 4" } ) ) ) );
		 * 
		 * 
		 * recipeInfo.put( "P8", Lists.newArrayList( new RecipeInfo( "P8", 1,
		 * resourceIndices.apply(new String [] { "Large 1", "Large 2", "Large 3",
		 * "Large 4" } ) ) ) );
		 * 
		 * 
		 * recipeInfo.put( "P9", Lists.newArrayList( new RecipeInfo( "P9", 1,
		 * resourceIndices.apply(new String [] { "Large 1", "Large 2", "Large 3",
		 * "Large 4" } ) ) ) );
		 * 
		 * recipeInfo.put( "P10", Lists.newArrayList( new RecipeInfo( "P10", 1,
		 * resourceIndices.apply(new String [] { "Large 1", "Large 2", "Large 3",
		 * "Large 4" } ) ) ) );
		 * 
		 * 
		 * recipeInfo.put( "P11", Lists.newArrayList( new RecipeInfo( "P11", 1,
		 * resourceIndices.apply(new String [] { "Large 1", "Large 2", "Large 3",
		 * "Large 4" } ) ) ) );
		 * 
		 * recipeInfo.put( "P12", Lists.newArrayList( new RecipeInfo( "P12", 1,
		 * resourceIndices.apply(new String [] { "Large 1", "Large 2", "Large 3",
		 * "Large 4" } ) ) ) );
		 * 
		 * 
		 * recipeInfo.put( "P13", Lists.newArrayList( new RecipeInfo( "P13", 1,
		 * resourceIndices.apply(new String [] { "Large 1", "Large 2", "Large 3",
		 * "Large 4" } ) ) ) );
		 * 
		 * 
		 * recipeInfo.put( "P14", Lists.newArrayList( new RecipeInfo( "P14", 1,
		 * resourceIndices.apply(new String [] { "Large 1", "Large 2", "Large 3",
		 * "Large 4" } ) ) ) );
		 */
		final Map<Pair<String, String>, Double> recipeAndResourceNameToCost = new HashMap<>();
		recipeAndResourceNameToCost.put(Pair.create("P1", "Small 1"), 168.4);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Medium 1"), 238.6);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Large 1"), 519.3);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Small 2"), 192.1);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Medium 2"), 273.1);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Large 2"), 596.9);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Small 3"), 167.0);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Medium 3"), 230.0);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Large 3"), 482.1);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Small 4"), 175.9);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Medium 4"), 237.1);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Large 4"), 481.6);

		recipeAndResourceNameToCost.put(Pair.create("P2", "Small 1"), 265.6);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Medium 1"), 376.3);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Large 1"), 819.0);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Small 2"), 305.5);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Medium 2"), 434.2);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Large 2"), 949.1);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Small 3"), 262.2);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Medium 3"), 361.1);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Large 3"), 756.9);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Small 4"), 283.1);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Medium 4"), 381.5);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Large 4"), 775.0);

		recipeAndResourceNameToCost.put(Pair.create("P3", "Medium 1"), 903.8);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Large 1"), 1967.2);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Medium 2"), 1096.3);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Large 2"), 2396.2);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Medium 3"), 867.0);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Large 3"), 1817.1);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Medium 4"), 902.8);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Large 4"), 1833.9);

		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 1"), 1004.3);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 1"), 2185.7);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 2"), 1224.4);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 2"), 2676.2);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 3"), 956.4);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 3"), 2004.6);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 4"), 1015.4);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 4"), 2062.6);

		recipeAndResourceNameToCost.put(Pair.create("P5", "Medium 1"), 1399.2);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Large 1"), 3045.3);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Medium 2"), 1737.7);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Large 2"), 3798.3);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Medium 3"), 1344.3);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Large 3"), 2817.6);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Medium 4"), 1370.1);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Large 4"), 2783.1);

		recipeAndResourceNameToCost.put(Pair.create("P6", "Medium 1"), 1770.8);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Large 1"), 3854.2);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Medium 2"), 2225.9);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Large 2"), 4865.4);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Medium 3"), 1703.0);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Large 3"), 3569.4);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Medium 4"), 1687.2);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Large 4"), 3453.0);
		/*
		 * recipeAndResourceNameToCost.put( Pair.create( "P7", "Large 1" ), 3826.5 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P7", "Large 2" ), 4560.0 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P7", "Large 3" ), 3532.6 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P7", "Large 4" ), 3614.3 );
		 * 
		 * recipeAndResourceNameToCost.put( Pair.create( "P8", "Large 1" ), 4325.6 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P8", "Large 2" ), 5175.0 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P8", "Large 3" ), 3982.4 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P8", "Large 4" ), 4274.2 );
		 * 
		 * recipeAndResourceNameToCost.put( Pair.create( "P9", "Large 1" ), 8039.5 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P9", "Large 2" ), 10027.4 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P9", "Large 3" ), 7438.4 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P9", "Large 4" ), 7347.4 );
		 * 
		 * recipeAndResourceNameToCost.put( Pair.create( "P10", "Large 1" ), 10175.0 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P10", "Large 2" ), 12844.6 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P10", "Large 3" ), 9423.1 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P10", "Large 4" ), 9116.0 );
		 * // recipeAndResourceNameToCost.put( Pair.create( "P10", "Large 4" ), 90116.0
		 * );
		 * 
		 * recipeAndResourceNameToCost.put( Pair.create( "P11", "Large 1" ), 1755.7 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P11", "Large 2" ), 2481.0 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P11", "Large 3" ), 1606.2 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P11", "Large 4" ), 1452.9 );
		 * 
		 * recipeAndResourceNameToCost.put( Pair.create( "P12", "Large 1" ), 2058.4 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P12", "Large 2" ), 2908.8 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P12", "Large 3" ), 1883.1 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P12", "Large 4" ), 1816.1 );
		 * 
		 * recipeAndResourceNameToCost.put( Pair.create( "P13", "Large 1" ), 4651.4 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P13", "Large 2" ), 6573.1 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P13", "Large 3" ), 4255.4 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P13", "Large 4" ), 3566.2 );
		 * 
		 * recipeAndResourceNameToCost.put( Pair.create( "P14", "Large 1" ), 6201.9 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P14", "Large 2" ), 8764.1 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P14", "Large 3" ), 5673.8 );
		 * recipeAndResourceNameToCost.put( Pair.create( "P14", "Large 4" ), 4754.9 );
		 */
		///////////////////////////

		final Map<Pair<String, String>, Integer> recipeAndResourceNameToCuttingTime = new HashMap<>();

		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small 2"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium 2"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large 2"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small 3"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium 3"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large 3"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small 4"), (int) (733.5 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium 4"), (int) (733.5 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large 4"), (int) (733.5 * 60));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small 2"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium 2"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large 2"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small 3"), (int) (60 * 1187.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium 3"), (int) (60 * 1187.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large 3"), (int) (60 * 1187.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small 4"), (int) (60 * 1180.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium 4"), (int) (60 * 1180.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large 4"), (int) (60 * 1180.4));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium 1"), (int) (60 * 3190.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large 1"), (int) (60 * 3190.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium 2"), (int) (60 * 3899.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large 2"), (int) (60 * 3899.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium 3"), (int) (60 * 2850.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large 3"), (int) (60 * 2850.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium 4"), (int) (60 * 2793.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large 4"), (int) (60 * 2793.3));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 1"), (int) (60 * 3544.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 1"), (int) (60 * 3544.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 2"), (int) (60 * 4355.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 2"), (int) (60 * 4355.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 3"), (int) (60 * 3144.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 3"), (int) (60 * 3144.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 4"), (int) (60 * 3141.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 4"), (int) (60 * 3141.7));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium 1"), (int) (60 * 4938.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large 1"), (int) (60 * 4938.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium 2"), (int) (60 * 6181.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large 2"), (int) (60 * 6181.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium 3"), (int) (60 * 4419.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large 3"), (int) (60 * 4419.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium 4"), (int) (60 * 4239.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large 4"), (int) (60 * 4239.1));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium 1"), (int) (60 * 6250.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large 1"), (int) (60 * 6250.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium 2"), (int) (60 * 7918.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large 2"), (int) (60 * 7918.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium 3"), (int) (60 * 5599.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large 3"), (int) (60 * 5599.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium 4"), (int) (60 * 5297.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large 4"), (int) (60 * 5297.7));
		/*
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P7", "Large 1" ),
		 * (int)(60 * 6205.1 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P7", "Large 2" ), (int)(60 * 7421.3 ) );
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P7", "Large 3" ),
		 * (int)(60 * 5541.3 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P7", "Large 4" ), (int)(60 * 5505.1 ) );
		 * 
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P8", "Large 1" ),
		 * (int)(60 * 7014.5 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P8", "Large 2" ), (int)(60 * 8422.3 ) );
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P8", "Large 3" ),
		 * (int)(60 * 6247.0 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P8", "Large 4" ), (int)(60 * 6510.3 ) );
		 * 
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P9", "Large 1" ),
		 * (int)(60 * 13037.0 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P9", "Large 2" ), (int)(60 * 16319.5 ) );
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P9", "Large 3" ),
		 * (int)(60 * 11668.1 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P9", "Large 4" ), (int)(60 * 11191.3 ) );
		 * 
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P10", "Large 1" ),
		 * (int)(60 * 16500.0 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P10", "Large 2" ), (int)(60 * 20904.5 ) );
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P10", "Large 3" ),
		 * (int)(60 * 14781.4 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P10", "Large 4" ), (int)(60 * 13985.8 ) );
		 * 
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P11", "Large 1" ),
		 * (int)(60 * 2847.1 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P11", "Large 2" ), (int)(60 * 4067.2 ) );
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P11", "Large 3" ),
		 * (int)(60 * 2519.5 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P11", "Large 4" ), (int)(60 * 2151.1 ) );
		 * 
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P12", "Large 1" ),
		 * (int)(60 * 3337.9 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P12", "Large 2" ), (int)(60 * 4768.5 ) );
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P12", "Large 3" ),
		 * (int)(60 * 2953.9 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P12", "Large 4" ), (int)(60 * 2688.9 ) );
		 * 
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P13", "Large 1" ),
		 * (int)(60 * 7542.9 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P13", "Large 2" ), (int)(60 * 10775.5 ) );
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P13", "Large 3" ),
		 * (int)(60 * 6675.1 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P13", "Large 4" ), (int)(60 * 5280.0 ) );
		 * 
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P14", "Large 1" ),
		 * (int)(60 * 10057.1 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P14", "Large 2" ), (int)(60 * 14367.3 ) );
		 * recipeAndResourceNameToCuttingTime.put( Pair.create( "P14", "Large 3" ),
		 * (int)(60 * 8900.1 ) ); recipeAndResourceNameToCuttingTime.put( Pair.create(
		 * "P14", "Large 4" ), (int)(60 * 7040.0 ) );
		 */
		///////////////////////////

		return makeConfigurationType(recipeInfo, resourceNames, recipeAndResourceNameToCost, percentageAvailability,
				recipeAndResourceNameToCuttingTime, mutices, isMultiobjective, random);
	}

	///////////////////////////////

	public static ConfigurationType hardwiredConfigurationTypeFull(int percentageAvailability, boolean isMultiobjective,
			Random random) {

		final String[] resourceNames = { "Small 1", "Medium 1", "Large 1", "Small 2", "Medium 2", "Large 2", "Small 3",
				"Medium 3", "Large 3", "Small 4", "Medium 4", "Large 4" };

		// Convention: resource names prefixes (up to the first space) denote the same
		// resource
		// hence they are mutually exclusive:
		final BiPredicate<String, String> mutex = (String resource1, String resource2) -> {
			final String[] s1 = resource1.split(" ");
			final String[] s2 = resource2.split(" ");
			return (s1.length == 0 && s2.length == 0) || (s1[0].equals(s2[0]));
		};

		final java.util.function.Function<String[], List<Integer>> resourceIndices = (String[] names) -> {
			List<Integer> result = Arrays.asList(names).stream()
					.map((String nm) -> Arrays.asList(resourceNames).indexOf(nm)).collect(Collectors.toList());
			if (result.contains(-1))
				System.out.println(Arrays.toString(names) + " contains bad string:\n" + result);

			return result;
		};

		final Map<Pair<String, String>, Boolean> mutices = new HashMap<>();
		for (String r1 : resourceNames)
			for (String r2 : resourceNames)
				mutices.put(Pair.create(r1, r2), mutex.test(r1, r2));

// Ints.asList(0,1,2,3,4,5,6,7,8);

		final Map<String, List<RecipeInfo>> recipeInfo = new HashMap<>();
		// recipeInfo.put( "P1", Lists.newArrayList( new RecipeInfo( "P1 recipe", 1,
		// Ints.asList(0,1,2,3,4,5,6,7,8) ) ) );

		recipeInfo.put("P1", Lists.newArrayList(new RecipeInfo("P1", 1, resourceIndices.apply(resourceNames))));
		recipeInfo.put("P2", Lists.newArrayList(new RecipeInfo("P2", 1, resourceIndices.apply(resourceNames))));
		recipeInfo.put("P3", Lists.newArrayList(new RecipeInfo("P3", 1, resourceIndices.apply(new String[] { "Medium 1",
				"Large 1", "Medium 2", "Large 2", "Medium 3", "Large 3", "Medium 4", "Large 4" }))));

		recipeInfo.put("P4", Lists.newArrayList(new RecipeInfo("P4", 1, resourceIndices.apply(new String[] { "Medium 1",
				"Large 1", "Medium 2", "Large 2", "Medium 3", "Large 3", "Medium 4", "Large 4" }))));

		recipeInfo.put("P5", Lists.newArrayList(new RecipeInfo("P5", 1, resourceIndices.apply(new String[] { "Medium 1",
				"Large 1", "Medium 2", "Large 2", "Medium 3", "Large 3", "Medium 4", "Large 4" }))));

		recipeInfo.put("P6", Lists.newArrayList(new RecipeInfo("P6", 1, resourceIndices.apply(new String[] { "Medium 1",
				"Large 1", "Medium 2", "Large 2", "Medium 3", "Large 3", "Medium 4", "Large 4" }))));

		recipeInfo.put("P7", Lists.newArrayList(new RecipeInfo("P7", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		recipeInfo.put("P8", Lists.newArrayList(new RecipeInfo("P8", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		recipeInfo.put("P9", Lists.newArrayList(new RecipeInfo("P9", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		recipeInfo.put("P10", Lists.newArrayList(new RecipeInfo("P10", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		recipeInfo.put("P11", Lists.newArrayList(new RecipeInfo("P11", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		recipeInfo.put("P12", Lists.newArrayList(new RecipeInfo("P12", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		recipeInfo.put("P13", Lists.newArrayList(new RecipeInfo("P13", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		recipeInfo.put("P14", Lists.newArrayList(new RecipeInfo("P14", 1,
				resourceIndices.apply(new String[] { "Large 1", "Large 2", "Large 3", "Large 4" }))));

		final Map<Pair<String, String>, Double> recipeAndResourceNameToCost = new HashMap<>();
		recipeAndResourceNameToCost.put(Pair.create("P1", "Small 1"), 168.4);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Medium 1"), 238.6);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Large 1"), 519.3);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Small 2"), 192.1);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Medium 2"), 273.1);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Large 2"), 596.9);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Small 3"), 167.0);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Medium 3"), 230.0);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Large 3"), 482.1);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Small 4"), 175.9);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Medium 4"), 237.1);
		recipeAndResourceNameToCost.put(Pair.create("P1", "Large 4"), 481.6);

		recipeAndResourceNameToCost.put(Pair.create("P2", "Small 1"), 265.6);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Medium 1"), 376.3);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Large 1"), 819.0);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Small 2"), 305.5);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Medium 2"), 434.2);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Large 2"), 949.1);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Small 3"), 262.2);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Medium 3"), 361.1);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Large 3"), 756.9);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Small 4"), 283.1);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Medium 4"), 381.5);
		recipeAndResourceNameToCost.put(Pair.create("P2", "Large 4"), 775.0);

		recipeAndResourceNameToCost.put(Pair.create("P3", "Medium 1"), 903.8);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Large 1"), 1967.2);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Medium 2"), 1096.3);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Large 2"), 2396.2);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Medium 3"), 867.0);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Large 3"), 1817.1);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Medium 4"), 902.8);
		recipeAndResourceNameToCost.put(Pair.create("P3", "Large 4"), 1833.9);

		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 1"), 1004.3);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 1"), 2185.7);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 2"), 1224.4);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 2"), 2676.2);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 3"), 956.4);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 3"), 2004.6);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Medium 4"), 1015.4);
		recipeAndResourceNameToCost.put(Pair.create("P4", "Large 4"), 2062.6);

		recipeAndResourceNameToCost.put(Pair.create("P5", "Medium 1"), 1399.2);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Large 1"), 3045.3);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Medium 2"), 1737.7);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Large 2"), 3798.3);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Medium 3"), 1344.3);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Large 3"), 2817.6);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Medium 4"), 1370.1);
		recipeAndResourceNameToCost.put(Pair.create("P5", "Large 4"), 2783.1);

		recipeAndResourceNameToCost.put(Pair.create("P6", "Medium 1"), 1770.8);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Large 1"), 3854.2);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Medium 2"), 2225.9);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Large 2"), 4865.4);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Medium 3"), 1703.0);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Large 3"), 3569.4);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Medium 4"), 1687.2);
		recipeAndResourceNameToCost.put(Pair.create("P6", "Large 4"), 3453.0);

		recipeAndResourceNameToCost.put(Pair.create("P7", "Large 1"), 3826.5);
		recipeAndResourceNameToCost.put(Pair.create("P7", "Large 2"), 4560.0);
		recipeAndResourceNameToCost.put(Pair.create("P7", "Large 3"), 3532.6);
		recipeAndResourceNameToCost.put(Pair.create("P7", "Large 4"), 3614.3);

		recipeAndResourceNameToCost.put(Pair.create("P8", "Large 1"), 4325.6);
		recipeAndResourceNameToCost.put(Pair.create("P8", "Large 2"), 5175.0);
		recipeAndResourceNameToCost.put(Pair.create("P8", "Large 3"), 3982.4);
		recipeAndResourceNameToCost.put(Pair.create("P8", "Large 4"), 4274.2);

		recipeAndResourceNameToCost.put(Pair.create("P9", "Large 1"), 8039.5);
		recipeAndResourceNameToCost.put(Pair.create("P9", "Large 2"), 10027.4);
		recipeAndResourceNameToCost.put(Pair.create("P9", "Large 3"), 7438.4);
		recipeAndResourceNameToCost.put(Pair.create("P9", "Large 4"), 7347.4);

		recipeAndResourceNameToCost.put(Pair.create("P10", "Large 1"), 10175.0);
		recipeAndResourceNameToCost.put(Pair.create("P10", "Large 2"), 12844.6);
		recipeAndResourceNameToCost.put(Pair.create("P10", "Large 3"), 9423.1);
		recipeAndResourceNameToCost.put(Pair.create("P10", "Large 4"), 9116.0);
		// recipeAndResourceNameToCost.put( Pair.create( "P10", "Large 4" ), 90116.0 );

		recipeAndResourceNameToCost.put(Pair.create("P11", "Large 1"), 1755.7);
		recipeAndResourceNameToCost.put(Pair.create("P11", "Large 2"), 2481.0);
		recipeAndResourceNameToCost.put(Pair.create("P11", "Large 3"), 1606.2);
		recipeAndResourceNameToCost.put(Pair.create("P11", "Large 4"), 1452.9);

		recipeAndResourceNameToCost.put(Pair.create("P12", "Large 1"), 2058.4);
		recipeAndResourceNameToCost.put(Pair.create("P12", "Large 2"), 2908.8);
		recipeAndResourceNameToCost.put(Pair.create("P12", "Large 3"), 1883.1);
		recipeAndResourceNameToCost.put(Pair.create("P12", "Large 4"), 1816.1);

		recipeAndResourceNameToCost.put(Pair.create("P13", "Large 1"), 4651.4);
		recipeAndResourceNameToCost.put(Pair.create("P13", "Large 2"), 6573.1);
		recipeAndResourceNameToCost.put(Pair.create("P13", "Large 3"), 4255.4);
		recipeAndResourceNameToCost.put(Pair.create("P13", "Large 4"), 3566.2);

		recipeAndResourceNameToCost.put(Pair.create("P14", "Large 1"), 6201.9);
		recipeAndResourceNameToCost.put(Pair.create("P14", "Large 2"), 8764.1);
		recipeAndResourceNameToCost.put(Pair.create("P14", "Large 3"), 5673.8);
		recipeAndResourceNameToCost.put(Pair.create("P14", "Large 4"), 4754.9);

		///////////////////////////

		final Map<Pair<String, String>, Integer> recipeAndResourceNameToCuttingTime = new HashMap<>();

		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small 2"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium 2"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large 2"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small 3"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium 3"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large 3"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small 4"), (int) (733.5 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium 4"), (int) (733.5 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large 4"), (int) (733.5 * 60));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small 2"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium 2"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large 2"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small 3"), (int) (60 * 1187.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium 3"), (int) (60 * 1187.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large 3"), (int) (60 * 1187.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small 4"), (int) (60 * 1180.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium 4"), (int) (60 * 1180.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large 4"), (int) (60 * 1180.4));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium 1"), (int) (60 * 3190.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large 1"), (int) (60 * 3190.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium 2"), (int) (60 * 3899.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large 2"), (int) (60 * 3899.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium 3"), (int) (60 * 2850.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large 3"), (int) (60 * 2850.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium 4"), (int) (60 * 2793.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large 4"), (int) (60 * 2793.3));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 1"), (int) (60 * 3544.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 1"), (int) (60 * 3544.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 2"), (int) (60 * 4355.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 2"), (int) (60 * 4355.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 3"), (int) (60 * 3144.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 3"), (int) (60 * 3144.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium 4"), (int) (60 * 3141.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large 4"), (int) (60 * 3141.7));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium 1"), (int) (60 * 4938.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large 1"), (int) (60 * 4938.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium 2"), (int) (60 * 6181.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large 2"), (int) (60 * 6181.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium 3"), (int) (60 * 4419.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large 3"), (int) (60 * 4419.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium 4"), (int) (60 * 4239.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large 4"), (int) (60 * 4239.1));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium 1"), (int) (60 * 6250.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large 1"), (int) (60 * 6250.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium 2"), (int) (60 * 7918.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large 2"), (int) (60 * 7918.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium 3"), (int) (60 * 5599.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large 3"), (int) (60 * 5599.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium 4"), (int) (60 * 5297.7));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large 4"), (int) (60 * 5297.7));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large 1"), (int) (60 * 6205.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large 2"), (int) (60 * 7421.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large 3"), (int) (60 * 5541.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large 4"), (int) (60 * 5505.1));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large 1"), (int) (60 * 7014.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large 2"), (int) (60 * 8422.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large 3"), (int) (60 * 6247.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large 4"), (int) (60 * 6510.3));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large 1"), (int) (60 * 13037.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large 2"), (int) (60 * 16319.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large 3"), (int) (60 * 11668.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large 4"), (int) (60 * 11191.3));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large 1"), (int) (60 * 16500.0));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large 2"), (int) (60 * 20904.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large 3"), (int) (60 * 14781.4));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large 4"), (int) (60 * 13985.8));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large 1"), (int) (60 * 2847.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large 2"), (int) (60 * 4067.2));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large 3"), (int) (60 * 2519.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large 4"), (int) (60 * 2151.1));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large 1"), (int) (60 * 3337.9));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large 2"), (int) (60 * 4768.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large 3"), (int) (60 * 2953.9));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large 4"), (int) (60 * 2688.9));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large 1"), (int) (60 * 7542.9));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large 2"), (int) (60 * 10775.5));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large 3"), (int) (60 * 6675.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large 4"), (int) (60 * 5280.0));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large 1"), (int) (60 * 10057.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large 2"), (int) (60 * 14367.3));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large 3"), (int) (60 * 8900.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large 4"), (int) (60 * 7040.0));

		///////////////////////////

		return makeConfigurationType(recipeInfo, resourceNames, recipeAndResourceNameToCost, percentageAvailability,
				recipeAndResourceNameToCuttingTime, mutices, isMultiobjective, random);
	}

	///////////////////////////////

	public static ConfigurationType hardwiredConfigurationTypeTwoLevel(int percentageAvailability,
			boolean isMultiobjective, Random random) {

		final String[] resourceNames = { "LargeA 1", "LargeB 1", "LargeC 1", "LargeD 1", "LargeE 1" };

		final String[] resourceNamesLevel1 = { "LargeA 1", "LargeB 1", "LargeC 1" };
		// final int [] resourceIndicesLevel1 = { 0, 1, 2 };
		final String[] resourceNamesLevel2 = { "LargeD 1", "LargeE 1" };
		// final int [] resourceIndicesLevel2 = { 3, 4 };

		// Convention: resource names prefixes (up to the first space) denote the same
		// resource
		// hence they are mutually exclusive:
		final BiPredicate<String, String> mutex = (String resource1, String resource2) -> {
			final String[] s1 = resource1.split(" ");
			final String[] s2 = resource2.split(" ");
			return (s1.length == 0 && s2.length == 0) || (s1[0].equals(s2[0]));
		};

		final java.util.function.Function<String[], List<Integer>> resourceIndices = (String[] names) -> {
			List<Integer> result = Arrays.asList(names).stream()
					.map((String nm) -> Arrays.asList(resourceNames).indexOf(nm)).collect(Collectors.toList());
			if (result.contains(-1))
				throw new RuntimeException(Arrays.toString(names) + " contains bad string:\n" + result);

			return result;
		};

		final Map<Pair<String, String>, Boolean> mutices = new HashMap<>();
		for (String r1 : resourceNames)
			for (String r2 : resourceNames)
				mutices.put(Pair.create(r1, r2), mutex.test(r1, r2));

// Ints.asList(0,1,2,3,4,5,6,7,8);

		final Map<String, List<RecipeInfo>> recipeInfo = new HashMap<>();

//		jeep.lang.Diag.println( resourceIndices.apply(resourceNamesLevel1) );
//		jeep.lang.Diag.println( resourceIndices.apply(resourceNamesLevel2) );

		recipeInfo.put("P1a", Lists.newArrayList(new RecipeInfo("P1a", 1, resourceIndices.apply(resourceNamesLevel1))));
		recipeInfo.put("P2a", Lists.newArrayList(new RecipeInfo("P2a", 1, resourceIndices.apply(resourceNamesLevel1))));
		recipeInfo.put("P3a", Lists.newArrayList(new RecipeInfo("P3a", 1, resourceIndices.apply(resourceNamesLevel1))));
		recipeInfo.put("P4a", Lists.newArrayList(new RecipeInfo("P4a", 1, resourceIndices.apply(resourceNamesLevel1))));
		recipeInfo.put("P5a", Lists.newArrayList(new RecipeInfo("P5a", 1, resourceIndices.apply(resourceNamesLevel1))));
		recipeInfo.put("P6a", Lists.newArrayList(new RecipeInfo("P6a", 1, resourceIndices.apply(resourceNamesLevel1))));
		recipeInfo.put("P7a", Lists.newArrayList(new RecipeInfo("P7a", 1, resourceIndices.apply(resourceNamesLevel1))));
		recipeInfo.put("P8a", Lists.newArrayList(new RecipeInfo("P8a", 1, resourceIndices.apply(resourceNamesLevel1))));
		recipeInfo.put("P9a", Lists.newArrayList(new RecipeInfo("P9a", 1, resourceIndices.apply(resourceNamesLevel1))));
		recipeInfo.put("P10a",
				Lists.newArrayList(new RecipeInfo("P10a", 1, resourceIndices.apply(resourceNamesLevel1))));

		recipeInfo.put("P1b{P1a}",
				Lists.newArrayList(new RecipeInfo("P1b{P1a}", 1, resourceIndices.apply(resourceNamesLevel2))));
		recipeInfo.put("P2b{P2a}",
				Lists.newArrayList(new RecipeInfo("P2b{P2a}", 1, resourceIndices.apply(resourceNamesLevel2))));
		recipeInfo.put("P3b{P3a}",
				Lists.newArrayList(new RecipeInfo("P3b{P3a}", 1, resourceIndices.apply(resourceNamesLevel2))));
		recipeInfo.put("P4b{P4a}",
				Lists.newArrayList(new RecipeInfo("P4b{P4a}", 1, resourceIndices.apply(resourceNamesLevel2))));
		recipeInfo.put("P5b{P5a}",
				Lists.newArrayList(new RecipeInfo("P5b{P5a}", 1, resourceIndices.apply(resourceNamesLevel2))));
		recipeInfo.put("P6b{P6a}",
				Lists.newArrayList(new RecipeInfo("P6b{P6a}", 1, resourceIndices.apply(resourceNamesLevel2))));
		recipeInfo.put("P7b{P7a}",
				Lists.newArrayList(new RecipeInfo("P7b{P7a}", 1, resourceIndices.apply(resourceNamesLevel2))));
		recipeInfo.put("P8b{P8a}",
				Lists.newArrayList(new RecipeInfo("P8b{P8a}", 1, resourceIndices.apply(resourceNamesLevel2))));
		recipeInfo.put("P9b{P9a}",
				Lists.newArrayList(new RecipeInfo("P9b{P9a}", 1, resourceIndices.apply(resourceNamesLevel2))));
		recipeInfo.put("P10b{P10a}",
				Lists.newArrayList(new RecipeInfo("P10b{P10a}", 1, resourceIndices.apply(resourceNamesLevel2))));

		final Map<Pair<String, String>, Double> recipeAndResourceNameToCost = new HashMap<>();
		recipeAndResourceNameToCost.put(Pair.create("P1a", "LargeA 1"), (842.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P1a", "LargeB 1"), (974.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P1a", "LargeC 1"), (756.2 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P2a", "LargeA 1"), (842.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P2a", "LargeB 1"), (974.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P2a", "LargeC 1"), (756.2 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P3a", "LargeA 1"), (842.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P3a", "LargeB 1"), (974.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P3a", "LargeC 1"), (756.2 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P4a", "LargeA 1"), (842.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P4a", "LargeB 1"), (974.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P4a", "LargeC 1"), (756.2 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P5a", "LargeA 1"), (842.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P5a", "LargeB 1"), (974.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P5a", "LargeC 1"), (756.2 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P6a", "LargeA 1"), (842.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P6a", "LargeB 1"), (974.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P6a", "LargeC 1"), (756.2 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P7a", "LargeA 1"), (842.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P7a", "LargeB 1"), (974.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P7a", "LargeC 1"), (756.2 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P8a", "LargeA 1"), (842.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P8a", "LargeB 1"), (974.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P8a", "LargeC 1"), (756.2 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P9a", "LargeA 1"), (842.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P9a", "LargeB 1"), (974.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P9a", "LargeC 1"), (756.2 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P10a", "LargeA 1"), (842.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P10a", "LargeB 1"), (974.1 * 60));
		recipeAndResourceNameToCost.put(Pair.create("P10a", "LargeC 1"), (756.2 * 60));

		recipeAndResourceNameToCost.put(Pair.create("P1b{P1a}", "LargeD 1"), (60 * 1328.1));
		recipeAndResourceNameToCost.put(Pair.create("P1b{P1a}", "LargeE 1"), (60 * 1544.6));
		recipeAndResourceNameToCost.put(Pair.create("P2b{P2a}", "LargeD 1"), (60 * 1328.1));
		recipeAndResourceNameToCost.put(Pair.create("P2b{P2a}", "LargeE 1"), (60 * 1544.6));
		recipeAndResourceNameToCost.put(Pair.create("P3b{P3a}", "LargeD 1"), (60 * 1328.1));
		recipeAndResourceNameToCost.put(Pair.create("P3b{P3a}", "LargeE 1"), (60 * 1544.6));
		recipeAndResourceNameToCost.put(Pair.create("P4b{P4a}", "LargeD 1"), (60 * 1328.1));
		recipeAndResourceNameToCost.put(Pair.create("P4b{P4a}", "LargeE 1"), (60 * 1544.6));
		recipeAndResourceNameToCost.put(Pair.create("P5b{P5a}", "LargeD 1"), (60 * 1328.1));
		recipeAndResourceNameToCost.put(Pair.create("P5b{P5a}", "LargeE 1"), (60 * 1544.6));
		recipeAndResourceNameToCost.put(Pair.create("P6b{P6a}", "LargeD 1"), (60 * 1328.1));
		recipeAndResourceNameToCost.put(Pair.create("P6b{P6a}", "LargeE 1"), (60 * 1544.6));
		recipeAndResourceNameToCost.put(Pair.create("P7b{P7a}", "LargeD 1"), (60 * 1328.1));
		recipeAndResourceNameToCost.put(Pair.create("P7b{P7a}", "LargeE 1"), (60 * 1544.6));
		recipeAndResourceNameToCost.put(Pair.create("P8b{P8a}", "LargeD 1"), (60 * 1328.1));
		recipeAndResourceNameToCost.put(Pair.create("P8b{P8a}", "LargeE 1"), (60 * 1544.6));
		recipeAndResourceNameToCost.put(Pair.create("P9b{P9a}", "LargeD 1"), (60 * 1328.1));
		recipeAndResourceNameToCost.put(Pair.create("P9b{P9a}", "LargeE 1"), (60 * 1544.6));
		recipeAndResourceNameToCost.put(Pair.create("P10b{P10a}", "LargeD 1"), (60 * 1328.1));
		recipeAndResourceNameToCost.put(Pair.create("P10b{P10a}", "LargeE 1"), (60 * 1544.6));

		///////////////////////////

		final Map<Pair<String, String>, Integer> recipeAndResourceNameToCuttingTime = new HashMap<>();

		recipeAndResourceNameToCuttingTime.put(Pair.create("P1a", "LargeA 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1a", "LargeB 1"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1a", "LargeC 1"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2a", "LargeA 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2a", "LargeB 1"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2a", "LargeC 1"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3a", "LargeA 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3a", "LargeB 1"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3a", "LargeC 1"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4a", "LargeA 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4a", "LargeB 1"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4a", "LargeC 1"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5a", "LargeA 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5a", "LargeB 1"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5a", "LargeC 1"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6a", "LargeA 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6a", "LargeB 1"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6a", "LargeC 1"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P7a", "LargeA 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P7a", "LargeB 1"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P7a", "LargeC 1"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P8a", "LargeA 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P8a", "LargeB 1"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P8a", "LargeC 1"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P9a", "LargeA 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P9a", "LargeB 1"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P9a", "LargeC 1"), (int) (756.2 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P10a", "LargeA 1"), (int) (842.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P10a", "LargeB 1"), (int) (974.1 * 60));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P10a", "LargeC 1"), (int) (756.2 * 60));

		recipeAndResourceNameToCuttingTime.put(Pair.create("P1b{P1a}", "LargeD 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P1b{P1a}", "LargeE 1"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2b{P2a}", "LargeD 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P2b{P2a}", "LargeE 1"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3b{P3a}", "LargeD 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P3b{P3a}", "LargeE 1"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4b{P4a}", "LargeD 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P4b{P4a}", "LargeE 1"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5b{P5a}", "LargeD 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P5b{P5a}", "LargeE 1"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6b{P6a}", "LargeD 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P6b{P6a}", "LargeE 1"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P7b{P7a}", "LargeD 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P7b{P7a}", "LargeE 1"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P8b{P8a}", "LargeD 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P8b{P8a}", "LargeE 1"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P9b{P9a}", "LargeD 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P9b{P9a}", "LargeE 1"), (int) (60 * 1544.6));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P10b{P10a}", "LargeD 1"), (int) (60 * 1328.1));
		recipeAndResourceNameToCuttingTime.put(Pair.create("P10b{P10a}", "LargeE 1"), (int) (60 * 1544.6));

		///////////////////////////

		return makeConfigurationType(recipeInfo, resourceNames, recipeAndResourceNameToCost, percentageAvailability,
				recipeAndResourceNameToCuttingTime, mutices, isMultiobjective, random);
	}

	///////////////////////////////

	public static ConfigurationType scalableHardwiredConfigurationTypeTwoLevel(int percentageAvailability,
			boolean isMultiobjective, int numStage1Recipes, int numStage2Recipes, Random random) {

		final String[] resourceNames = { "LargeA 1", "LargeB 1", "LargeC 1", "LargeD 1", "LargeE 1" };

		final String[] resourceNamesLevel1 = { "LargeA 1", "LargeB 1", "LargeC 1" };
		// final int [] resourceIndicesLevel1 = { 0, 1, 2 };
		final String[] resourceNamesLevel2 = { "LargeD 1", "LargeE 1" };
		// final int [] resourceIndicesLevel2 = { 3, 4 };

		// Convention: resource names prefixes (up to the first space) denote the same
		// resource
		// hence they are mutually exclusive:
		final BiPredicate<String, String> mutex = (String resource1, String resource2) -> {
			final String[] s1 = resource1.split(" ");
			final String[] s2 = resource2.split(" ");
			return (s1.length == 0 && s2.length == 0) || (s1[0].equals(s2[0]));
		};

		final java.util.function.Function<String[], List<Integer>> resourceIndices = (String[] names) -> {
			List<Integer> result = Arrays.asList(names).stream()
					.map((String nm) -> Arrays.asList(resourceNames).indexOf(nm)).collect(Collectors.toList());
			if (result.contains(-1))
				throw new RuntimeException(Arrays.toString(names) + " contains bad string:\n" + result);

			return result;
		};

		final Map<Pair<String, String>, Boolean> mutices = new HashMap<>();
		for (String r1 : resourceNames)
			for (String r2 : resourceNames)
				mutices.put(Pair.create(r1, r2), mutex.test(r1, r2));

// Ints.asList(0,1,2,3,4,5,6,7,8);

		final Map<String, List<RecipeInfo>> recipeInfo = new HashMap<>();

//		jeep.lang.Diag.println( resourceIndices.apply(resourceNamesLevel1) );
//		jeep.lang.Diag.println( resourceIndices.apply(resourceNamesLevel2) );

//		recipeInfo.put( "P1a", Lists.newArrayList( new RecipeInfo( "P1a", 1, resourceIndices.apply(resourceNamesLevel1) ) ) );
//		recipeInfo.put( "P2a", Lists.newArrayList( new RecipeInfo( "P2a", 1, resourceIndices.apply(resourceNamesLevel1) ) ) );
//		recipeInfo.put( "P3a", Lists.newArrayList( new RecipeInfo( "P3a", 1, resourceIndices.apply(resourceNamesLevel1) ) ) );
//		recipeInfo.put( "P4a", Lists.newArrayList( new RecipeInfo( "P4a", 1, resourceIndices.apply(resourceNamesLevel1) ) ) );
//		recipeInfo.put( "P5a", Lists.newArrayList( new RecipeInfo( "P5a", 1, resourceIndices.apply(resourceNamesLevel1) ) ) );
//		recipeInfo.put( "P6a", Lists.newArrayList( new RecipeInfo( "P6a", 1, resourceIndices.apply(resourceNamesLevel1) ) ) );
//		recipeInfo.put( "P7a", Lists.newArrayList( new RecipeInfo( "P7a", 1, resourceIndices.apply(resourceNamesLevel1) ) ) );
//		recipeInfo.put( "P8a", Lists.newArrayList( new RecipeInfo( "P8a", 1, resourceIndices.apply(resourceNamesLevel1) ) ) );
//		recipeInfo.put( "P9a", Lists.newArrayList( new RecipeInfo( "P9a", 1, resourceIndices.apply(resourceNamesLevel1) ) ) );
//		recipeInfo.put( "P10a", Lists.newArrayList( new RecipeInfo( "P10a", 1, resourceIndices.apply(resourceNamesLevel1) ) ) );

		for (int i = 0; i < numStage1Recipes; ++i) {
			final String name = "P" + (i + 1) + "a";
			recipeInfo.put(name,
					Lists.newArrayList(new RecipeInfo(name, 1, resourceIndices.apply(resourceNamesLevel1))));
		}

//		recipeInfo.put( "P1b{P1a}", Lists.newArrayList( new RecipeInfo( "P1b{P1a}", 1, resourceIndices.apply(resourceNamesLevel2) ) ) );
//		recipeInfo.put( "P2b{P2a}", Lists.newArrayList( new RecipeInfo( "P2b{P2a}", 1, resourceIndices.apply(resourceNamesLevel2) ) ) );
//		recipeInfo.put( "P3b{P3a}", Lists.newArrayList( new RecipeInfo( "P3b{P3a}", 1, resourceIndices.apply(resourceNamesLevel2) ) ) );
//		recipeInfo.put( "P4b{P4a}", Lists.newArrayList( new RecipeInfo( "P4b{P4a}", 1, resourceIndices.apply(resourceNamesLevel2) ) ) );
//		recipeInfo.put( "P5b{P5a}", Lists.newArrayList( new RecipeInfo( "P5b{P5a}", 1, resourceIndices.apply(resourceNamesLevel2) ) ) );
//		recipeInfo.put( "P6b{P6a}", Lists.newArrayList( new RecipeInfo( "P6b{P6a}", 1, resourceIndices.apply(resourceNamesLevel2) ) ) );
//		recipeInfo.put( "P7b{P7a}", Lists.newArrayList( new RecipeInfo( "P7b{P7a}", 1, resourceIndices.apply(resourceNamesLevel2) ) ) );
//		recipeInfo.put( "P8b{P8a}", Lists.newArrayList( new RecipeInfo( "P8b{P8a}", 1, resourceIndices.apply(resourceNamesLevel2) ) ) );
//		recipeInfo.put( "P9b{P9a}", Lists.newArrayList( new RecipeInfo( "P9b{P9a}", 1, resourceIndices.apply(resourceNamesLevel2) ) ) );
//		recipeInfo.put( "P10b{P10a}", Lists.newArrayList( new RecipeInfo( "P10b{P10a}", 1, resourceIndices.apply(resourceNamesLevel2) ) ) );		

		for (int i = 0; i < numStage2Recipes; ++i) {
			final String name = "P" + (i + 1) + "b{P" + (i + 1) + "a}";
			recipeInfo.put(name,
					Lists.newArrayList(new RecipeInfo(name, 1, resourceIndices.apply(resourceNamesLevel2))));
		}

		final Map<Pair<String, String>, Double> recipeAndResourceNameToCost = new HashMap<>();

//		recipeAndResourceNameToCost.put( Pair.create( "P1a", "LargeA 1" ), (842.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P1a", "LargeB 1" ), (974.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P1a", "LargeC 1" ), (756.2 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P2a", "LargeA 1" ), (842.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P2a", "LargeB 1" ), (974.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P2a", "LargeC 1" ), (756.2 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P3a", "LargeA 1" ), (842.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P3a", "LargeB 1" ), (974.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P3a", "LargeC 1" ), (756.2 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P4a", "LargeA 1" ), (842.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P4a", "LargeB 1" ), (974.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P4a", "LargeC 1" ), (756.2 * 60) );		
//		recipeAndResourceNameToCost.put( Pair.create( "P5a", "LargeA 1" ), (842.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P5a", "LargeB 1" ), (974.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P5a", "LargeC 1" ), (756.2 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P6a", "LargeA 1" ), (842.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P6a", "LargeB 1" ), (974.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P6a", "LargeC 1" ), (756.2 * 60) );	
//		recipeAndResourceNameToCost.put( Pair.create( "P7a", "LargeA 1" ), (842.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P7a", "LargeB 1" ), (974.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P7a", "LargeC 1" ), (756.2 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P8a", "LargeA 1" ), (842.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P8a", "LargeB 1" ), (974.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P8a", "LargeC 1" ), (756.2 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P9a", "LargeA 1" ), (842.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P9a", "LargeB 1" ), (974.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P9a", "LargeC 1" ), (756.2 * 60) );		
//		recipeAndResourceNameToCost.put( Pair.create( "P10a", "LargeA 1" ), (842.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P10a", "LargeB 1" ), (974.1 * 60) );
//		recipeAndResourceNameToCost.put( Pair.create( "P10a", "LargeC 1" ), (756.2 * 60) );

		for (int i = 0; i < numStage1Recipes; ++i) {
			final String name = "P" + (i + 1) + "a";
			recipeAndResourceNameToCost.put(Pair.create(name, "LargeA 1"), (842.1 * 60));
			recipeAndResourceNameToCost.put(Pair.create(name, "LargeB 1"), (974.1 * 60));
			recipeAndResourceNameToCost.put(Pair.create(name, "LargeC 1"), (756.2 * 60));
		}

//		recipeAndResourceNameToCost.put( Pair.create( "P1b{P1a}", "LargeD 1" ), (60 * 1328.1 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P1b{P1a}", "LargeE 1" ), (60 * 1544.6 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P2b{P2a}", "LargeD 1" ), (60 * 1328.1 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P2b{P2a}", "LargeE 1" ), (60 * 1544.6 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P3b{P3a}", "LargeD 1" ), (60 * 1328.1 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P3b{P3a}", "LargeE 1" ), (60 * 1544.6 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P4b{P4a}", "LargeD 1" ), (60 * 1328.1 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P4b{P4a}", "LargeE 1" ), (60 * 1544.6 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P5b{P5a}", "LargeD 1" ), (60 * 1328.1 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P5b{P5a}", "LargeE 1" ), (60 * 1544.6 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P6b{P6a}", "LargeD 1" ), (60 * 1328.1 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P6b{P6a}", "LargeE 1" ), (60 * 1544.6 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P7b{P7a}", "LargeD 1" ), (60 * 1328.1 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P7b{P7a}", "LargeE 1" ), (60 * 1544.6 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P8b{P8a}", "LargeD 1" ), (60 * 1328.1 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P8b{P8a}", "LargeE 1" ), (60 * 1544.6 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P9b{P9a}", "LargeD 1" ), (60 * 1328.1 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P9b{P9a}", "LargeE 1" ), (60 * 1544.6 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P10b{P10a}", "LargeD 1" ), (60 * 1328.1 ) );
//		recipeAndResourceNameToCost.put( Pair.create( "P10b{P10a}", "LargeE 1" ), (60 * 1544.6 ) );

		for (int i = 0; i < numStage2Recipes; ++i) {
			final String name = "P" + (i + 1) + "b{P" + (i + 1) + "a}";
			recipeAndResourceNameToCost.put(Pair.create(name, "LargeD 1"), (60 * 1328.1));
			recipeAndResourceNameToCost.put(Pair.create(name, "LargeE 1"), (60 * 1544.6));
		}

		///////////////////////////

		final Map<Pair<String, String>, Integer> recipeAndResourceNameToCuttingTime = new HashMap<>();

//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P1a", "LargeA 1" ), (int)(842.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P1a", "LargeB 1" ), (int)(974.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P1a", "LargeC 1" ), (int)(756.2 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P2a", "LargeA 1" ), (int)(842.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P2a", "LargeB 1" ), (int)(974.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P2a", "LargeC 1" ), (int)(756.2 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P3a", "LargeA 1" ), (int)(842.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P3a", "LargeB 1" ), (int)(974.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P3a", "LargeC 1" ), (int)(756.2 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P4a", "LargeA 1" ), (int)(842.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P4a", "LargeB 1" ), (int)(974.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P4a", "LargeC 1" ), (int)(756.2 * 60) );		
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P5a", "LargeA 1" ), (int)(842.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P5a", "LargeB 1" ), (int)(974.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P5a", "LargeC 1" ), (int)(756.2 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P6a", "LargeA 1" ), (int)(842.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P6a", "LargeB 1" ), (int)(974.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P6a", "LargeC 1" ), (int)(756.2 * 60) );	
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P7a", "LargeA 1" ), (int)(842.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P7a", "LargeB 1" ), (int)(974.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P7a", "LargeC 1" ), (int)(756.2 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P8a", "LargeA 1" ), (int)(842.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P8a", "LargeB 1" ), (int)(974.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P8a", "LargeC 1" ), (int)(756.2 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P9a", "LargeA 1" ), (int)(842.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P9a", "LargeB 1" ), (int)(974.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P9a", "LargeC 1" ), (int)(756.2 * 60) );		
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P10a", "LargeA 1" ), (int)(842.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P10a", "LargeB 1" ), (int)(974.1 * 60) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P10a", "LargeC 1" ), (int)(756.2 * 60) );

		for (int i = 0; i < numStage1Recipes; ++i) {
			final String name = "P" + (i + 1) + "a";
			recipeAndResourceNameToCuttingTime.put(Pair.create(name, "LargeA 1"), (int) (842.1 * 60));
			recipeAndResourceNameToCuttingTime.put(Pair.create(name, "LargeB 1"), (int) (974.1 * 60));
			recipeAndResourceNameToCuttingTime.put(Pair.create(name, "LargeC 1"), (int) (756.2 * 60));
		}

//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P1b{P1a}", "LargeD 1" ), (int)(60 * 1328.1 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P1b{P1a}", "LargeE 1" ), (int)(60 * 1544.6 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P2b{P2a}", "LargeD 1" ), (int)(60 * 1328.1 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P2b{P2a}", "LargeE 1" ), (int)(60 * 1544.6 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P3b{P3a}", "LargeD 1" ), (int)(60 * 1328.1 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P3b{P3a}", "LargeE 1" ), (int)(60 * 1544.6 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P4b{P4a}", "LargeD 1" ), (int)(60 * 1328.1 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P4b{P4a}", "LargeE 1" ), (int)(60 * 1544.6 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P5b{P5a}", "LargeD 1" ), (int)(60 * 1328.1 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P5b{P5a}", "LargeE 1" ), (int)(60 * 1544.6 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P6b{P6a}", "LargeD 1" ), (int)(60 * 1328.1 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P6b{P6a}", "LargeE 1" ), (int)(60 * 1544.6 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P7b{P7a}", "LargeD 1" ), (int)(60 * 1328.1 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P7b{P7a}", "LargeE 1" ), (int)(60 * 1544.6 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P8b{P8a}", "LargeD 1" ), (int)(60 * 1328.1 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P8b{P8a}", "LargeE 1" ), (int)(60 * 1544.6 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P9b{P9a}", "LargeD 1" ), (int)(60 * 1328.1 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P9b{P9a}", "LargeE 1" ), (int)(60 * 1544.6 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P10b{P10a}", "LargeD 1" ), (int)(60 * 1328.1 ) );
//		recipeAndResourceNameToCuttingTime.put( Pair.create( "P10b{P10a}", "LargeE 1" ), (int)(60 * 1544.6 ) );

		for (int i = 0; i < numStage2Recipes; ++i) {
			final String name = "P" + (i + 1) + "b{P" + (i + 1) + "a}";
			recipeAndResourceNameToCuttingTime.put(Pair.create(name, "LargeD 1"), (int) (60 * 1328.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create(name, "LargeE 1"), (int) (60 * 1544.6));
		}

		///////////////////////////

		return makeConfigurationType(recipeInfo, resourceNames, recipeAndResourceNameToCost, percentageAvailability,
				recipeAndResourceNameToCuttingTime, mutices, isMultiobjective, random);
	}
}

// End ///////////////////////////////////////////////////////////////
