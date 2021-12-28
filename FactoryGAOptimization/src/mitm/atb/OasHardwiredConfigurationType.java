/********************************
package uk.ac.york.safire.optimisation.mitm.atb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import uk.ac.york.safire.metrics.ConfigurationType;
import uk.ac.york.safire.metrics.ControlledMetricType;
import uk.ac.york.safire.metrics.KeyObjectiveType;
import uk.ac.york.safire.metrics.ObservableMetricType;
import uk.ac.york.safire.metrics.SampleRate;
import uk.ac.york.safire.metrics.SearchDirection;
import uk.ac.york.safire.metrics.ValueType;
import uk.ac.york.safire.optimisation.ia.Interval;
import uk.ac.york.safire.optimisation.mitm.atb.ATBSimulatorKafkaProducer.RecipeInfo;

///////////////////////////////////

public class OasHardwiredConfigurationType {

	private static int randomInRange(int min, int max, Random rng) {
	    if (min >= max)
	        throw new IllegalArgumentException();

	    return rng.nextInt((max - min) + 1) + min;
	}
	    		
	///////////////////////////
	
	public static Set<String> hardwiredCommodities() {
		final Set<String> result = new HashSet<>();
		result.add( "Std Weiss" );
		result.add( "Weiss Matt" );
		result.add( "Super Glanz" );
		result.add( "Weiss Basis" );		
		return result;
	}
	
//	public static Map< String, Integer > hardwiredAmountsRequired() { 
//		Map< String, Integer > amounts = new HashMap<>();
//		amounts.put( "Std Weiss", 145000 ); 
//		// amounts.put( "Std Weiss", 50 ); 
//		amounts.put( "Weiss Matt",  165000 ); 
//		amounts.put( "Super Glanz", 56000 ); 
//		amounts.put( "Weiss Basis",	126000 );
//		return amounts;
//	}

	public static Map< String, Integer > hardwiredAmountsRequired() { 
		Map< String, Integer > amounts = new HashMap<>();
		amounts.put( "Std Weiss", 50 ); 
		amounts.put( "Weiss Matt",  1650 ); 
		amounts.put( "Super Glanz", 560 ); 
		amounts.put( "Weiss Basis",	1260 );
		return amounts;
	}
	

//	public static Map< String, Integer > hardwiredAmountsProduced() { 
//		Map< String, Integer > amounts = new HashMap<>();
//		// FIXME: values as per D3.1 p32		
//		amounts.put( "Std Weiss A", 500 );
//		amounts.put( "Std Weiss B", 1000 );	   
//		amounts.put( "Std Weiss C", 1000 );
//		amounts.put( "Std Weiss D", 1000 ); 
//		   
//		amounts.put( "Weiss Matt A", 500 );
//		amounts.put( "Weiss Matt B", 1000 );	   
//		amounts.put( "Weiss Matt C", 1000 );
//		amounts.put( "Weiss Matt D", 1000 ); 
//
//		amounts.put( "Super Glanz A", 400 );
//		amounts.put( "Super Glanz B", 800 );	   
//		amounts.put( "Super Glanz C", 800 );
//		amounts.put( "Super Glanz D", 800 );
//
//		amounts.put( "Weiss Basis A", 600 );
//		amounts.put( "Weiss Basis B", 1200 );   
//		amounts.put( "Weiss Basis C", 1200 );
//		amounts.put( "Weiss Basis D", 1200 );
//		
//		return amounts;
//	}

	
	// As per Piotr's email of 12:11 26th September 2018:
	
	public static Map< String, Integer > hardwiredAmountsProduced() { 
		Map< String, Integer > amounts = new HashMap<>();
		// FIXME: values as per D3.1 p32		
		amounts.put( "Std Weiss A", 28 );
		amounts.put( "Std Weiss B", 10 );	   
		amounts.put( "Std Weiss C", 10 );
		amounts.put( "Std Weiss D", 10 ); 
		   
		amounts.put( "Weiss Matt A", 5 );
		amounts.put( "Weiss Matt B", 10 );	   
		amounts.put( "Weiss Matt C", 10 );
		amounts.put( "Weiss Matt D", 10 ); 

		amounts.put( "Super Glanz A", 4 );
		amounts.put( "Super Glanz B", 8 );	   
		amounts.put( "Super Glanz C", 8 );
		amounts.put( "Super Glanz D", 8 );

		amounts.put( "Weiss Basis A", 6 );
		amounts.put( "Weiss Basis B", 12 );   
		amounts.put( "Weiss Basis C", 12 );
		amounts.put( "Weiss Basis D", 12 );
		
		return amounts;
	}	
	
	///////////////////////////
	
	private static ConfigurationType 
	makeConfigurationType(
		Map< String, List<RecipeInfo> > recipeInfo, 
		String [] resourceNames,
		Map< String, Integer > mapRecipeNameToAmountRequired,		
		Map< String, Integer > mapRecipeInfoNameToAmountProduced, 
		Interval taskDurationInterval, int percentAvailability, Random random) {
		
		if( percentAvailability < 0 || percentAvailability > 100 )
			throw new IllegalArgumentException("Expected percentage for percentAvailability, found: " + percentAvailability );
		
		final List< KeyObjectiveType > keyObjectiveTypes = new ArrayList<>();
		keyObjectiveTypes.add( new KeyObjectiveType("makespan", ValueType.realType(0, Double.MAX_VALUE), "n/a", SearchDirection.MINIMIZING ) );
		
		// Key Objective to minimise surplus for each paint in recipe:
		for( Map.Entry<String, List<RecipeInfo> > e: recipeInfo.entrySet() ) {
			// keyObjectiveTypes.add( new KeyObjectiveType( e.getKey() + " surplus", ValueType.realType(0, Double.MAX_VALUE), "n/a", SearchDirection.MINIMIZING ) );
			keyObjectiveTypes.add( 
				new KeyObjectiveType( e.getKey() + " amount discrepancy score", ValueType.realType(0.0, Double.MAX_VALUE), "n/a", SearchDirection.MINIMIZING ) );
		}
				
		///////////////////////////
		
		final List< ControlledMetricType > controlledMetricTypes = new ArrayList<>();
		final List< ObservableMetricType > observableMetricTypes = new ArrayList<>();

		// 1. Observable metric for availability of each resource:
		int numMixersUnavailable = 0;
		for( String resourceName: resourceNames ) {
			// e.g. Mixer 1 availability (int type, domain: 0,1)
			if( random.nextInt( 100 ) < percentAvailability )
				observableMetricTypes.add( new ObservableMetricType(resourceName + " availability", ValueType.intType(1, 1), "n/a", SampleRate.eventDriven ) );
			else {
				numMixersUnavailable += 1;				
				observableMetricTypes.add( new ObservableMetricType(resourceName + " availability", ValueType.intType(0, 0), "n/a", SampleRate.eventDriven ) );
			}
		}

		System.out.println();
		System.out.println( "Mixers available: " + ( resourceNames.length - numMixersUnavailable ) + " (of " + resourceNames.length + ")" );
		
		///////////////////////////

		for( Map.Entry<String, Integer > e: mapRecipeInfoNameToAmountProduced.entrySet() ) {
			final Integer quantity = e.getValue(); 
			observableMetricTypes.add( new ObservableMetricType(e.getKey() + " amount produced", ValueType.intType(quantity, quantity), "n/a", SampleRate.eventDriven ) );				
		}

		for( Map.Entry<String, Integer > e: mapRecipeNameToAmountRequired.entrySet() ) {
			final Integer quantity = e.getValue(); 
			observableMetricTypes.add( new ObservableMetricType(e.getKey() + " amount required", ValueType.intType(quantity, quantity), "n/a", SampleRate.eventDriven ) );				
		}
		
		///////////////////////////
		
		// count instances (used to set an upper bound on priority):
		int totalInstances = 0;
		for( Map.Entry<String, List<RecipeInfo> > e: recipeInfo.entrySet() )
			for( RecipeInfo r: e.getValue() )
				totalInstances += r.instances;

		// create a randomised list of priorities:
		List<Integer> priorities = IntStream.rangeClosed(0, totalInstances)
			    .boxed().collect(Collectors.toList());
		Collections.shuffle(priorities, random);

		// create a randomised list of (start,end) intervals:
//		final List<Integer> ends = random.ints( totalInstances, 1, maxIntervalDuration * totalInstances ).sorted().mapToObj( i -> new Integer(i)).collect(Collectors.toList());
//		final List< Interval > intervals = StreamUtils.zip( 
//			Stream.concat(Stream.of(0), ends.stream()), ends.stream(), 
//			(start, end) -> new Interval(start,end)
//		).collect(Collectors.toList());
		final List< Interval > intervals = new ArrayList<>();
		for( int i=0; i<totalInstances; ++i ) {
			// final int a = random.nextInt( ( maxDuration * totalInstances ) / resourceNames.length );
			// final int b = random.nextInt( ( maxDuration * totalInstances ) / resourceNames.length );
			// intervals.add( new Interval( Math.min(a,b), Math.max(a,b) ) );
			final int duration = randomInRange(taskDurationInterval.lower(), taskDurationInterval.upper(),random);
			intervals.add( new Interval( 0, duration ) );			
		}
		
		// Add types for controlled and observable metrics:
		int instanceCount = 0;
		for( Map.Entry<String, List<RecipeInfo> > e: recipeInfo.entrySet() ) {
			for( RecipeInfo r: e.getValue() ) {
				for( int i=0; i<r.instances; ++i ) {
					final String instanceName = r.name + " " + i;

					// 2. Controlled metrics for allocation and for priority:
					
					final ValueType allocationValueType = ValueType.nominalType( instanceName + " allocation type ", r.compatibleResources.stream().map( index -> resourceNames[ index ] ).toArray(String[]::new) );
					// e.g. Std Weiss A 1 allocation (nominal type, domain: Mixer 1, Mixer 2, Mixer 3, Mixer 4, Mixer 5} :
					controlledMetricTypes.add( new ControlledMetricType(instanceName + " allocation", allocationValueType, "n/a") );

					// e.g. Std Weiss A 1 priority (Int type)
					final int priorityValue = priorities.get(instanceCount);
					// ensure each priority value is unique - otherwise scheduling can get in an infinite loop:
					controlledMetricTypes.add( new ControlledMetricType(instanceName + " priority", 
							ValueType.intType(priorityValue, priorityValue), "n/a") );
					
					// 3. Observable metric for start and end time of each recipe instance:

//					final int start = intervals.get(instanceCount).lower();
//					final int end = intervals.get(instanceCount).upper();					
//					observableMetricTypes.add( new ObservableMetricType(instanceName + " start", 
//							ValueType.intType(start, start), "n/a", SampleRate.eventDriven ) );
//					observableMetricTypes.add( new ObservableMetricType(instanceName + " end", 
//							ValueType.intType(end, end), "n/a", SampleRate.eventDriven ) );

					for( String resourceName: resourceNames ) {
						final String recipeAndResourceNamePrefix = instanceName + " " + resourceName;

						final int start = intervals.get(instanceCount).lower();
						final int end = intervals.get(instanceCount).upper();					
						observableMetricTypes.add( new ObservableMetricType(
								// instanceName + " start",
								recipeAndResourceNamePrefix + " start",								
								ValueType.intType(start, start), "n/a", SampleRate.eventDriven ) );
						observableMetricTypes.add( new ObservableMetricType(
								// instanceName + " end", 
								recipeAndResourceNamePrefix + " end",								
							ValueType.intType(end, end), "n/a", SampleRate.eventDriven ) );
					}
					
					instanceCount += 1;
				}
			}
		}

		return new ConfigurationType.Explicit(keyObjectiveTypes, controlledMetricTypes, observableMetricTypes );
	}

	///////////////////////////////

	public static ConfigurationType 
	hardwiredConfigurationType(int percentAvailability, Random random) {
		final String [] resourceNames = { 
			"Mixer 1", "Mixer 2", "Mixer 3", "Mixer 4", "Mixer 5", 
			"Mixer 6", "Mixer 7", "Mixer 8", "Mixer 9", "Mixer 10",
			"No allocation"
		};
		
	   final Map< String, List< RecipeInfo > > recipeInfo = new HashMap<>();
	   recipeInfo.put( "Std Weiss",	Lists.newArrayList( 
		   new RecipeInfo( "Std Weiss A", 9, Ints.asList(0,1,2,3,4,10) ), // , 5000, 90 ),
		   new RecipeInfo( "Std Weiss B", 2, Ints.asList(5,6,10) ), // 10000, 60 ),	   
		   new RecipeInfo( "Std Weiss C", 3, Ints.asList(7,8,10) ), //  10000, 45 ),
		   new RecipeInfo( "Std Weiss D", 4 , Ints.asList(7,8,10) ) //  10000, 45 ) ) 
	   ) );	   

	   recipeInfo.put( "Weiss Matt", Lists.newArrayList(	   
			   // new RecipeInfo( "Weiss Matt A", 11, Ints.asList(0, 1,2,3,4,10) ), //  5000, 90 ),
			   new RecipeInfo( "Weiss Matt A", 9, Ints.asList(0, 1,2,3,4,10) ), //  5000, 90 ),
		   new RecipeInfo( "Weiss Matt B", 4, Ints.asList(5,6,10) ), //  10000, 60 ),	   
		   new RecipeInfo( "Weiss Matt C", 5, Ints.asList(7,8,10) ), //  10000, 45 ),
		   new RecipeInfo( "Weiss Matt D", 2, Ints.asList(7,8,10) ) // , 10000, 45 ) ) 
		) );	   
	   recipeInfo.put( "Super Glanz", Lists.newArrayList(
			   new RecipeInfo( "Super Glanz A", 2, Ints.asList(0,1,2,3,4,10) ), // , 4000, 120 ),
			   new RecipeInfo( "Super Glanz B", 4, Ints.asList(5,6,10) ), // , 8000, 90 ),	   
			   new RecipeInfo( "Super Glanz C", 2, Ints.asList(7,8,10) ), // , 8000, 60 ),
			   new RecipeInfo( "Super Glanz D", 0, Ints.asList(7,8,10) ) // , 10000, 60 ) ) 
		) );	   
	   recipeInfo.put( "Weiss Basis", Lists.newArrayList(
				  // new RecipeInfo( "Weiss Basis A", 11, Ints.asList(0,1,2,3,4,10) ), // , 6000, 60 ),
				  new RecipeInfo( "Weiss Basis A", 9, Ints.asList(0,1,2,3,4,10) ), // , 6000, 60 ),				  
				  new RecipeInfo( "Weiss Basis B", 3, Ints.asList(5,6,10) ), // , 12000, 45 ),	   
				  new RecipeInfo( "Weiss Basis C", 1, Ints.asList(7,8,10) ), // , 12000, 30 ),
				  new RecipeInfo( "Weiss Basis D", 1, Ints.asList(7,8,10) ) // , 12000, 30 ) ) );
	   ) );			   

	   final Interval taskDurationInterval = new Interval( 30 * 60, 120 * 60); // 0.5 to 2 hours

	   ////////////////////////////
	   
	   return makeConfigurationType( recipeInfo, resourceNames, 
			   hardwiredAmountsRequired(),
			   hardwiredAmountsProduced(),			   
			   taskDurationInterval, percentAvailability, random ); 
	}
}

// End ///////////////////////////////////////////////////////////////

********************************/

package mitm.atb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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

public class OasHardwiredConfigurationType {

	///////////////////////////

	public static final double[] MixerConfiguration = { 0.5, 0.2, 0.2 };
	public static final String Mixer_Code = "Mixer ";
	public static final String Void_Mixer = "No allocation";
	public static final int[][] ExecutionTime = { { 60, 45, 30 }, { 90, 60, 45 }, { 100, 80, 60 }, { 120, 90, 60 } };

	public static int Number_Of_Mixers = 10;
	public static int[] AmountRequired = { 80, 60, 45, 40 };

	public static Set<String> hardwiredCommodities() {
		final Set<String> result = new HashSet<>();
		result.add("Std Weiss");
		result.add("Weiss Matt");
		result.add("Super Glanz");
		result.add("Weiss Basis");
		return result;
	}

	public static Map<String, Integer> hardwiredAmountsRequired() {
		Map<String, Integer> amounts = new HashMap<>();
		amounts.put("Std Weiss", AmountRequired[0]);
		amounts.put("Weiss Matt", AmountRequired[1]);
		amounts.put("Super Glanz", AmountRequired[2]);
		amounts.put("Weiss Basis", AmountRequired[3]);
		return amounts;
	}

	public static Map<String, Integer> hardwiredAmountsProduced() {
		Map<String, Integer> amounts = new HashMap<>();
		// FIXME: values as per D3.1 p32
		amounts.put("Std Weiss A", 5);
		amounts.put("Std Weiss B", 10);
		amounts.put("Std Weiss C", 10);
		// amounts.put("Std Weiss D", 10);

		amounts.put("Weiss Matt A", 6);
		amounts.put("Weiss Matt B", 12);
		amounts.put("Weiss Matt C", 12);
		// amounts.put("Weiss Matt D", 10);

		amounts.put("Super Glanz A", 4);
		amounts.put("Super Glanz B", 8);
		amounts.put("Super Glanz C", 8);
		// amounts.put("Super Glanz D", 8);

		amounts.put("Weiss Basis A", 4);
		amounts.put("Weiss Basis B", 8);
		amounts.put("Weiss Basis C", 8);
		// amounts.put("Weiss Basis D", 12);

		return amounts;
	}

	public static ConfigurationType hardwiredConfigurationType(int percentAvailability, Random random) {
		String[] resourceNames = new String[Number_Of_Mixers + 1];

		for (int i = 0; i < Number_Of_Mixers; i++) {
			resourceNames[i] = Mixer_Code + (i + 1);
		}
		resourceNames[Number_Of_Mixers] = Void_Mixer;
		Map<String, Integer> mapRecipeNameToAmountRequired = hardwiredAmountsRequired();
		Map<String, Integer> mapRecipeInfoNameToAmountProduced = hardwiredAmountsProduced();

		final Map<String, List<RecipeInfo>> recipeInfo = new HashMap<>();

		List<List<Integer>> comptiableMixers = new ArrayList<List<Integer>>();

		List<Integer> recipeAComptiable = new ArrayList<Integer>();
		List<Integer> recipeBComptiable = new ArrayList<Integer>();
		List<Integer> recipeCComptiable = new ArrayList<Integer>();

		// hacks here to simulate new configurations
		for (int k = 0; k < 6; k++) {
			int i = 0;
			for (; i < Number_Of_Mixers * MixerConfiguration[0]; i++) {
				recipeAComptiable.add(i);
			}

			for (; i < Number_Of_Mixers * MixerConfiguration[0] + Number_Of_Mixers * MixerConfiguration[1]; i++) {
				recipeBComptiable.add(i);

			}

			for (; i < Number_Of_Mixers * MixerConfiguration[0] + Number_Of_Mixers * MixerConfiguration[1]
					+ Number_Of_Mixers * MixerConfiguration[2]; i++) {
				recipeCComptiable.add(i);

			}
		}

		recipeAComptiable.add(resourceNames.length - 1);
		comptiableMixers.add(recipeAComptiable);

		recipeBComptiable.add(resourceNames.length - 1);
		comptiableMixers.add(recipeBComptiable);

		recipeCComptiable.add(resourceNames.length - 1);
		comptiableMixers.add(recipeCComptiable);

		List<RecipeInfo> stdWeiss = new ArrayList<>();
		stdWeiss.add(
				new RecipeInfo("Std Weiss A",
						(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Std Weiss")
								/ (double) mapRecipeInfoNameToAmountProduced.get("Std Weiss A")),
						comptiableMixers.get(0)));
		stdWeiss.add(
				new RecipeInfo("Std Weiss B",
						(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Std Weiss")
								/ (double) mapRecipeInfoNameToAmountProduced.get("Std Weiss B")),
						comptiableMixers.get(1)));
		stdWeiss.add(
				new RecipeInfo("Std Weiss C",
						(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Std Weiss")
								/ (double) mapRecipeInfoNameToAmountProduced.get("Std Weiss C")),
						comptiableMixers.get(2)));

		recipeInfo.put("Std Weiss", stdWeiss);

		List<RecipeInfo> weissMatt = new ArrayList<>();
		weissMatt
				.add(new RecipeInfo("Weiss Matt A",
						(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Weiss Matt")
								/ (double) mapRecipeInfoNameToAmountProduced.get("Weiss Matt A")),
						comptiableMixers.get(0)));
		weissMatt
				.add(new RecipeInfo("Weiss Matt B",
						(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Weiss Matt")
								/ (double) mapRecipeInfoNameToAmountProduced.get("Weiss Matt B")),
						comptiableMixers.get(1)));
		weissMatt
				.add(new RecipeInfo("Weiss Matt C",
						(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Weiss Matt")
								/ (double) mapRecipeInfoNameToAmountProduced.get("Weiss Matt C")),
						comptiableMixers.get(2)));

		recipeInfo.put("Weiss Matt", weissMatt);

		
		List<RecipeInfo> superGlanz = new ArrayList<>();
		superGlanz.add(
				new RecipeInfo("Super Glanz A",
						(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Super Glanz")
								/ (double) mapRecipeInfoNameToAmountProduced.get("Super Glanz A")),
						comptiableMixers.get(0)));
		superGlanz.add(new RecipeInfo("Super Glanz B",
				(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Super Glanz")
						/ (double) mapRecipeInfoNameToAmountProduced.get("Super Glanz B")),
				comptiableMixers.get(1)));
		superGlanz.add(new RecipeInfo("Super Glanz C",
				(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Super Glanz")
						/ (double) mapRecipeInfoNameToAmountProduced.get("Super Glanz C")),
				comptiableMixers.get(2)));
				
		recipeInfo.put("Super Glanz", superGlanz);

		List<RecipeInfo> weissBasis = new ArrayList<>();
		weissBasis.add(new RecipeInfo("Weiss Basis A",
								(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Weiss Basis")
										/ (double) mapRecipeInfoNameToAmountProduced.get("Weiss Basis A")),
								comptiableMixers.get(0)));
		weissBasis.add(new RecipeInfo("Weiss Basis B",
								(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Weiss Basis")
										/ (double) mapRecipeInfoNameToAmountProduced.get("Weiss Basis B")),
								comptiableMixers.get(1)));
		weissBasis.add(new RecipeInfo("Weiss Basis C",
								(int) Math.ceil((double) mapRecipeNameToAmountRequired.get("Weiss Basis")
										/ (double) mapRecipeInfoNameToAmountProduced.get("Weiss Basis C")),
								comptiableMixers.get(2)));
		recipeInfo.put("Weiss Basis", weissBasis);

		return makeConfigurationType(recipeInfo, resourceNames, mapRecipeNameToAmountRequired,
				mapRecipeInfoNameToAmountProduced, percentAvailability, random);
	}

	///////////////////////////

	private static ConfigurationType makeConfigurationType(Map<String, List<RecipeInfo>> recipeInfo,
			String[] resourceNames, Map<String, Integer> mapRecipeNameToAmountRequired,
			Map<String, Integer> mapRecipeInfoNameToAmountProduced, int percentAvailability, Random random) {

		if (percentAvailability < 0 || percentAvailability > 100)
			throw new IllegalArgumentException(
					"Expected percentage for percentAvailability, found: " + percentAvailability);

		final List<KeyObjectiveType> keyObjectiveTypes = new ArrayList<>();

		keyObjectiveTypes.add(new KeyObjectiveType("makespan", ValueType.realType(0, Double.MAX_VALUE), "n/a",
				SearchDirection.MINIMIZING));
		// Key Objective to minimise surplus for each paint in recipe:
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet())
			keyObjectiveTypes.add(new KeyObjectiveType(e.getKey() + " amount discrepancy score",
					ValueType.realType(0, Double.MAX_VALUE), "n/a", SearchDirection.MINIMIZING));

		///////////////////////////

		final List<ControlledMetricType> controlledMetricTypes = new ArrayList<>();
		final List<ObservableMetricType> observableMetricTypes = new ArrayList<>();

		// 1. Observable metric for availability of each resource:
		int numMixersUnavailable = 0;
		for (String resourceName : resourceNames) {
			// e.g. Mixer 1 availability (int type, domain: 0,1)
			if (random.nextInt(100) < percentAvailability)
				observableMetricTypes.add(new ObservableMetricType(resourceName + " availability",
						ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
			else {
				numMixersUnavailable += 1;
				observableMetricTypes.add(new ObservableMetricType(resourceName + " availability",
						ValueType.intType(0, 0), "n/a", SampleRate.eventDriven));
			}
		}

		System.out.println();
		System.out.println("Mixers available: " + (resourceNames.length - numMixersUnavailable) + " (of "
				+ resourceNames.length + ")");

		///////////////////////////

		for (Map.Entry<String, Integer> e : mapRecipeInfoNameToAmountProduced.entrySet()) {
			final Integer quantity = e.getValue();
			observableMetricTypes.add(new ObservableMetricType(e.getKey() + " amount produced",
					ValueType.intType(quantity, quantity), "n/a", SampleRate.eventDriven));
		}

		for (Map.Entry<String, Integer> e : mapRecipeNameToAmountRequired.entrySet()) {
			final Integer quantity = e.getValue();
			observableMetricTypes.add(new ObservableMetricType(e.getKey() + " amount required",
					ValueType.intType(quantity, quantity), "n/a", SampleRate.eventDriven));
		}

		///////////////////////////

		// count instances (used to set an upper bound on priority):
		int totalInstances = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet())
			for (RecipeInfo r : e.getValue())
				totalInstances += r.instances;

		// create a randomised list of priorities:
		List<Integer> priorities = IntStream.rangeClosed(0, totalInstances).boxed().collect(Collectors.toList());
		Collections.shuffle(priorities, random);

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

					// 3. Observable metric for start and end time of each recipe instance:

					// final int start = intervals.get(instanceCount).lower();
					// final int end = intervals.get(instanceCount).upper();
					// observableMetricTypes.add( new ObservableMetricType(instanceName + " start",
					// ValueType.intType(start, start), "n/a", SampleRate.eventDriven ) );
					// observableMetricTypes.add( new ObservableMetricType(instanceName + " end",
					// ValueType.intType(end, end), "n/a", SampleRate.eventDriven ) );

					for (String resourceName : resourceNames) {
						final String recipeAndResourceNamePrefix = instanceName + " " + resourceName;

						final int start = 0; // intervals.get(instanceCount).lower();
						final int end = interval; // intervals.get(instanceCount).upper();
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

		UoYEarlyPrototypeDemo.observableMetricTypes = observableMetricTypes;
		return new ConfigurationType.Explicit(keyObjectiveTypes, controlledMetricTypes);
	}

	///////////////////////////////

}

// End ///////////////////////////////////////////////////////////////
