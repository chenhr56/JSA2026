package metrics;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Streams;



///////////////////////////////////

public final class ValueTrace {

	private final List< Value >		history;
	private final List< Double > 	timestamps;
	
	///////////////////////////////

	public static boolean isSorted(List<Pair< Value, Double > > history) {
		for( int i=1; i<history.size(); ++i )
			if(history.get(i-1).getRight() >= history.get(i).getRight() )
				return false;
		return true;
	}

	///////////////////////////
	
	public ValueTrace(List<Pair< Value,Double> >history) {
		if( history.isEmpty() || !isSorted(history) )
			throw new IllegalArgumentException();
		
		this.history = history.stream().map( x -> x.getLeft() ).collect(Collectors.toList());
		this.timestamps = history.stream().map( x -> x.getRight() ).collect(Collectors.toList());			
	}
	
	///////////////////////////

	public Pair< Value,Double > 
	get(double timeInMilliseconds) {
		
		final int index = Collections.binarySearch(timestamps, timeInMilliseconds);
		if( index >= 0 ) {
			assert timestamps.get(index) <= timeInMilliseconds;
			return Pair.of( history.get(index), timestamps.get(index) );				
		}
		else {
			final int insertionPointPlusOne = -index;
			assert timestamps.get(insertionPointPlusOne - 2) <= timeInMilliseconds;				
			return Pair.of( history.get(insertionPointPlusOne - 2), timestamps.get(insertionPointPlusOne - 2) );
		}
	}
	
	///////////////////////////
	
	@Override
	public boolean equals(Object rhs) {
		return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, rhs);			
	}

	@Override
	public int hashCode() {
		return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);			
	}
	
	@Override
	public String toString() {
		return "ValueTrace( " + 
			Streams.zip( history.stream(), timestamps.stream(), (h,t) -> Pair.of( h, t ) ).collect(Collectors.toList()) + 
		")";
	}
}

// End ///////////////////////////////////////////////////////////////

