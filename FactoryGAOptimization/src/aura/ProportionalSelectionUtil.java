package aura;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

//////////////////////////////////////////////////////////////////////

public final class ProportionalSelectionUtil {

	public static int select( double [] values, Random random ) {
		final int numSelections = 1;
		return select( values, numSelections, random ).get( 0 );
	}

	///////////////////////////////

	public static List< Integer >
	select( double [] values, int numSelections, Random random ) {
		if( values.length == 0 )
			throw new IllegalArgumentException();

		///////////////////////////

		double [] cumulativeFitness = cumulativeFitness( values );
		return selectImpl( cumulativeFitness, numSelections, random );
	}

	///////////////////////////////

	public static < T >
	int select( List< T > values, ToDoubleFunction< T > f, Random random ) {
		final int numSelections = 1;
		return select( values, numSelections, f, random ).get( 0 );
	}

	///////////////////////////////

	public static < T > List< Integer >
	select( List< T > values, int numSelections, ToDoubleFunction< T > f, Random random ) {
		if( values.isEmpty() )
			throw new IllegalArgumentException();

		///////////////////////////

		double [] cumulativeFitness = cumulativeFitness( values, f );
		return selectImpl( cumulativeFitness, numSelections, random );
	}

	///////////////////////////////

	public static double []
	cumulativeFitness( double [] values ) {
		if( values.length == 0 )
			throw new IllegalArgumentException();

		///////////////////////////

		double [] cumulativeFitness = new double[ values.length ];
		cumulativeFitness[ 0 ] = values[ 0 ];
		for( int i=1; i<cumulativeFitness.length; ++i ) {
			final double fitness = values[ i ];
			if( fitness < 0 )
				throw new IllegalArgumentException();

			cumulativeFitness[ i ] = cumulativeFitness[ i - 1 ] + fitness;
		}

		return cumulativeFitness;
	}

	///////////////////////////////

	static < T > List< Integer >
	selectImpl( double [] cumulativeFitness, int numSelections, Random random )	{
		if( cumulativeFitness[ cumulativeFitness.length - 1 ] == 0.0 ) {
			List< Integer > result = new ArrayList< Integer >();
			for( int i=0; i<numSelections; ++i )
				result.add( random.nextInt( cumulativeFitness.length ) );
			return result;
		}

		///////////////////////////

		List< Integer > result = new ArrayList< Integer >();
		for( int i=0; i<numSelections; ++i ) {
			double randomFitness = random.nextDouble() * cumulativeFitness[ cumulativeFitness.length - 1 ];
			int index = Arrays.binarySearch( cumulativeFitness, randomFitness );
			if( index < 0 )
				index = Math.abs( index + 1 );

			result.add( index );
		}

		assert result.size() == numSelections;
		return result;
	}


	///////////////////////////////

	public static < T > double []
	cumulativeFitness( List< T > values, ToDoubleFunction< T > f ) {
		if( values.isEmpty() )
			throw new IllegalArgumentException();

		///////////////////////////

		double [] cumulativeFitness = new double[ values.size() ];
		cumulativeFitness[ 0 ] = f.applyAsDouble( values.get( 0 ) );
		for( int i=1; i<cumulativeFitness.length; ++i ) {
			final double fitness = f.applyAsDouble( values.get( i ) );
			if( fitness < 0 )
				throw new IllegalArgumentException();

			cumulativeFitness[ i ] = cumulativeFitness[ i - 1 ] + fitness;
		}

		return cumulativeFitness;
	}
}

// End ///////////////////////////////////////////////////////////////

