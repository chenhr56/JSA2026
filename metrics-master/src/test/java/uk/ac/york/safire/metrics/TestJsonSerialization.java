package uk.ac.york.safire.metrics;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

///////////////////////////////////

public final class TestJsonSerialization {


	
	///////////////////////////////

	private static ConfigurationType toyConfigurationType() {
		final List< KeyObjectiveType > keyObjectiveMetricTypes = ImmutableList.of( 
			new KeyObjectiveType( "key objective metric 1", ValueType.realType( 0.0, 1.0 ), "scalar", SearchDirection.MINIMIZING ) 
		); 
			
		final List< ControlledMetricType > controlledMetricTypes = ImmutableList.of( 
			new ControlledMetricType( "controlled metric 1", ValueType.realType( 0.0, 1.0 ), "scalar" )			
		);

		return new ConfigurationType.Explicit(keyObjectiveMetricTypes,controlledMetricTypes);
	}

	@Test
	public void testOptimisationResult() {
	
		final ConfigurationType configurationType = toyConfigurationType();
		
		final long randomSeed = 0xDEADBEEF; 
		final Random random = new Random( randomSeed );
		final JsonConverter converter = new JsonConverter();

		final int numConfigurations = 10;
		List< Configuration > configurations = new ArrayList<>();
		for( int i=0; i<numConfigurations; ++i )
			configurations.add( Utility.randomConfiguration(configurationType, random) );
		
		// OptimisationResult original = new OptimisationResult( Utility.randomConfiguration(configurationType, random).getControlledMetrics() );
		OptimisationResult original = new OptimisationResult( configurations );		
//		jeep.lang.Diag.println( original );
		String json = converter.toJson(original);
		jeep.lang.Diag.println( json );		
		OptimisationResult  roundTrip = converter.fromJson(json, OptimisationResult.class );				
		assertEquals(original, roundTrip );
	}
	
	@Test
	public void testConfiguration() {
	
		final ConfigurationType configurationType = toyConfigurationType();
		
		final long randomSeed = 0xDEADBEEF; 
		final Random random = new Random( randomSeed );
		final JsonConverter converter = new JsonConverter();
		
		Configuration original = Utility.randomConfiguration(configurationType, random);
//		jeep.lang.Diag.println( original );
		String json = converter.toJson(original);
		jeep.lang.Diag.println( json );		
		Configuration  roundTrip = converter.fromJson(json,Configuration.class);
		assertEquals(original, roundTrip );
	}

	// These tests won't work: GSon doesn't serialise some types properly unless they are attributes of
	// some enclosing type.
		
//		@Test
//		public void testValue() {
//	
//			final ConfigurationType configurationType = toyConfigurationType();
//			
//			final long randomSeed = 0xDEADBEEF; 
//			final Random random = new Random( randomSeed );
//			final JsonConverter converter = new JsonConverter();
//			
//			Value original = Utility.randomValue(configurationType.getKeyObjectiveMetrics().get(0).valueType, random);
//			jeep.lang.Diag.println( "original: " + original );
//			String json = converter.toJson(original);
//			jeep.lang.Diag.println( json );		
//			Value roundTrip = converter.fromJson(json, Value.class );				
//			assertEquals(original, roundTrip );
//		}
	//
//		@Test
//		public void testKeyObjective() {
	//	
//			final ConfigurationType configurationType = toyConfigurationType();
//			
//			final long randomSeed = 0xDEADBEEF; 
//			final Random random = new Random( randomSeed );
//			final JsonConverter converter = new JsonConverter();
//			
//			Map< String, Value > original = Utility.randomConfiguration(configurationType, random).getKeyObjectives();
////			jeep.lang.Diag.println( original );
//			String json = converter.toJson(original);
//			jeep.lang.Diag.println( json );		
//			Type type = new TypeToken<Map<KeyObjectiveType, Value>>(){}.getType();
//			@SuppressWarnings("unchecked")
//			Map< String, Value > roundTrip = (Map<String, Value>)converter.fromJson(json, type );				
//			assertEquals(original, roundTrip );
//		}
}

// End ///////////////////////////////////////////////////////////////