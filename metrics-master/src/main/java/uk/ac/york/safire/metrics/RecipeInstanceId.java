package uk.ac.york.safire.metrics;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

///////////////////////////////////

public final class RecipeInstanceId {
	
	private final RecipeInfo info;
	private final int id;
	
	///////////////////////////
	
	public static 
	RecipeInstanceId
	mk(RecipeInfo info, int id) {
		return new RecipeInstanceId(info, id); 
	}

	///////////////////////////
	
	public RecipeInstanceId(RecipeInfo info, int id) {
		this.info = info;
		this.id = id;
	}

	///////////////////////////

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	@Override
	public boolean equals(Object o) {
		return EqualsBuilder.reflectionEquals(this,o);
	}
	
	@Override
	public String toString() {
		return info.name + " " + id;
	}
}

// End ///////////////////////////////////////////////////////////////
