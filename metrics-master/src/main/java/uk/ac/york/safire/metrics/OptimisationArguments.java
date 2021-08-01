package uk.ac.york.safire.metrics;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringStyle;

public final class OptimisationArguments {

	private List< Configuration >	configurations;	
	private final double urgency;
	private final double quality;
	private final int engine;
	
	///////////////////////////////
	
	public OptimisationArguments(List<Configuration> configurations, double urgency, double quality) {	
		if( urgency < 0.0 || urgency > 1.0 ) throw new IllegalArgumentException("Expected: urgency in [0.0,1.0], found: " + urgency );
		if( quality < 0.0 || quality > 1.0 ) throw new IllegalArgumentException("Expected: quality in [0.0,1.0], found: " + quality );
		if( !Configuration.equivalentConfigurations( configurations ) )
			throw new IllegalArgumentException("Expected: configurations differing only in controlled metrics");

		this.urgency = urgency;
		this.quality = quality;		
		this.configurations = Collections.unmodifiableList( configurations );
		this.engine = 0;
	}
	
	public OptimisationArguments(List<Configuration> configurations, double urgency, double quality, int engine) {	
		if( urgency < 0.0 || urgency > 1.0 ) throw new IllegalArgumentException("Expected: urgency in [0.0,1.0], found: " + urgency );
		if( quality < 0.0 || quality > 1.0 ) throw new IllegalArgumentException("Expected: quality in [0.0,1.0], found: " + quality );
		if( !Configuration.equivalentConfigurations( configurations ) )
			throw new IllegalArgumentException("Expected: configurations differing only in controlled metrics");

		this.urgency = urgency;
		this.quality = quality;		
		this.configurations = Collections.unmodifiableList( configurations );
		this.engine = engine;
	}
	
	///////////////////////////////
	
	public double getUrgency() { return urgency; }
	public double getQuality() { return quality; }	
	public int getEngine() { return engine; }
	public List< Configuration > getConfigurations() { return configurations; }	
	
	///////////////////////////////
	
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
		return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);			
	}
}

// End ///////////////////////////////////////////////////////////////
