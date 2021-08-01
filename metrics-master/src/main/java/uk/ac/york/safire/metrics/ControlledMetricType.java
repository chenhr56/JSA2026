package uk.ac.york.safire.metrics;

import org.apache.commons.lang3.builder.ToStringStyle;

public final class ControlledMetricType {
	
	public final String name;	
	public final ValueType valueType;
	public final String units;	
	
	///////////////////////////////
	
	public ControlledMetricType(String name, ValueType valueType, String units) {
		this.name = name;
		this.valueType = valueType;
		this.units = units;		
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
