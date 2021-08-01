package uk.ac.york.safire.metrics;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringStyle;

///////////////////////////////////

public final class RecipeInfo {
	
	final String name;
	final List< Integer > compatibleResources;
	final int instances;		
	// final int commodityProduced; 
	// final int executionTime;	
	
	///////////////////////////
	
	public RecipeInfo(
		String name,
		int instances,			
		List< Integer > compatibleResources) {
		this.name = name;
		this.instances = instances;
		this.compatibleResources = compatibleResources;
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
		return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(
			this,ToStringStyle.SHORT_PREFIX_STYLE); 
	}
}

// End ///////////////////////////////////////////////////////////////
