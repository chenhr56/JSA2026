package uk.ac.york.safire.metrics;

import org.apache.commons.lang3.builder.ToStringStyle;

///////////////////////////////////

public final class KeyObjectiveType {
	
	public final String 			name;	
	public final ValueType.Real		valueType;
	public final String 			units;	
	public final SearchDirection 	searchDirection;
	
	///////////////////////////////
	
	public KeyObjectiveType(String name, ValueType.Real valueType, String units, SearchDirection searchDirection) {
		this.name = name;
		this.valueType = valueType;
		this.units = units;		
		this.searchDirection = searchDirection; 
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

