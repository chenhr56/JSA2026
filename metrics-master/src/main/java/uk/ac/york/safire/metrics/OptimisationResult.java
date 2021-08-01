package uk.ac.york.safire.metrics;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringStyle;

public final class OptimisationResult {

	private List< Configuration >	proposedReconfigurations;	
	public double optimisationTime = -1;

	public OptimisationResult(List<Configuration> proposedReconfigurations) {
		this.proposedReconfigurations = Collections.unmodifiableList( proposedReconfigurations );
	}
	
	public List< Configuration > getReconfigurations() { return proposedReconfigurations; }	
	
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
