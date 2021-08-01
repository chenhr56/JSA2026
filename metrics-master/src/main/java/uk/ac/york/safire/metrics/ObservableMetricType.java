package uk.ac.york.safire.metrics;

import org.apache.commons.lang3.builder.ToStringStyle;

public final class ObservableMetricType {

	public final String			name;	
	public final ValueType		valueType;	
	public final String 		units;	
	public final SampleRate		sampleRate;
	
	///////////////////////////////	
	
	public ObservableMetricType(String name, ValueType valueType, String units, SampleRate sampleRate) {
		this.name = name;
		this.valueType = valueType;
		this.units = units;		
		this.sampleRate = sampleRate; 
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
		return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this,ToStringStyle.SHORT_PREFIX_STYLE);			
	}
}

// End ///////////////////////////////////////////////////////////////
