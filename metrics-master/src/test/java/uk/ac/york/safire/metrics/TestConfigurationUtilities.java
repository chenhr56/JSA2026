package uk.ac.york.safire.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

///////////////////////////////////

public final class TestConfigurationUtilities {

	private static final String [] resourceNames = { 
		"Mixer 1", "Mixer 2", "Mixer 3", "Mixer 4", "Mixer 5", 
		"Mixer 6", "Mixer 7", "Mixer 8", "Mixer 9", "Mixer 10" 
	};

	private final static Map< String, List< RecipeInfo > > recipeInfo = new HashMap<>();
	
	static {

		final Map< String, List< RecipeInfo > > recipeInfo = new HashMap<>();
		recipeInfo.put( "Std Weiss",	Lists.newArrayList( 
			new RecipeInfo( "Std Weiss A", 9, Ints.asList(0,1,2,3,4) ), // , 5000, 90 ),
			new RecipeInfo( "Std Weiss B", 2, Ints.asList(5,6) ), // 10000, 60 ),	   
			new RecipeInfo( "Std Weiss C", 3, Ints.asList(7,8) ), //  10000, 45 ),
			new RecipeInfo( "Std Weiss D", 4 , Ints.asList(7,8) ) //  10000, 45 ) ) 
		) );	   

		recipeInfo.put( "Weiss Matt", Lists.newArrayList(	   
			new RecipeInfo( "Weiss Matt A", 11, Ints.asList(0, 1,2,3,4) ), //  5000, 90 ),
			new RecipeInfo( "Weiss Matt B", 4, Ints.asList(5,6) ), //  10000, 60 ),	   
			new RecipeInfo( "Weiss Matt C", 5, Ints.asList(7,8) ), //  10000, 45 ),
			new RecipeInfo( "Weiss Matt D", 2, Ints.asList(7,8) ) // , 10000, 45 ) ) 
		) );	   

		recipeInfo.put( "Super Glanz", Lists.newArrayList(
			new RecipeInfo( "Super Glanz A", 2, Ints.asList(0,1,2,3,4) ), // , 4000, 120 ),
			new RecipeInfo( "Super Glanz B", 4, Ints.asList(5,6) ), // , 8000, 90 ),	   
			new RecipeInfo( "Super Glanz C", 2, Ints.asList(7,8) ), // , 8000, 60 ),
			new RecipeInfo( "Super Glanz D", 0, Ints.asList(7,8) ) // , 10000, 60 ) ) 
		) );	   

		recipeInfo.put( "Weiss Basis", Lists.newArrayList(
			new RecipeInfo( "Weiss Basis A", 11, Ints.asList(0,1,2,3,4) ), // , 6000, 60 ),
			new RecipeInfo( "Weiss Basis B", 3, Ints.asList(5,6) ), // , 12000, 45 ),	   
			new RecipeInfo( "Weiss Basis C", 1, Ints.asList(7,8) ), // , 12000, 30 ),
			new RecipeInfo( "Weiss Basis D", 1, Ints.asList(7,8) ) // , 12000, 30 ) ) );
		) );			   
		
	}; 
	
	///////////////////////////////
	
	@Test
	public void testOasConfiguration() {
	
		final ConfigurationType ct = 
			ConfigurationUtilities.makeConfigurationType(resourceNames,recipeInfo);
		
		final Predicate< String > resourceAvailability = (r) -> true;
		final BiFunction< RecipeInstanceId, String, Double 
			> recipeInstanceAndResourceNameToCost = (r,s) -> 0.0;
		final BiFunction< RecipeInstanceId, String, Integer
			> recipeInstanceAndResourceNameToExecutionTime = (r,s) -> 100;
		final BiFunction< String, String, Boolean > resourceMutexes = (r1,r2) -> false;
		final Function< RecipeInstanceId, Integer > recipeInstancePriority = (r) -> r.hashCode();
		
		final Map< String, Value > keyObjectives = ConfigurationUtilities.defaultKeyObjectives(ct);

		final Configuration config = ConfigurationUtilities.makeConfiguration(
			ct,	
			recipeInfo, resourceNames, 
			resourceAvailability,
			recipeInstanceAndResourceNameToCost,			
			recipeInstanceAndResourceNameToExecutionTime,
			recipeInstancePriority,
			resourceMutexes, keyObjectives );
		
		jeep.lang.Diag.println( config );
	}
}

// End ///////////////////////////////////////////////////////////////
