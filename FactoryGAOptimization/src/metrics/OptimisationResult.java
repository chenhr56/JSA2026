package metrics;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringStyle;



public final class OptimisationResult {

	// private Map< String, Value > controlledMetrics;
	private List<Configuration> proposedReconfigurations;
	private long time;

	public double optimisationTime = -1;

	///////////////////////////////

	// public OptimisationResult(Map< String, Value > controlledMetrics) {
	public OptimisationResult(List<Configuration> proposedReconfigurations, long time) {
		// if( !Configuration.equivalentConfigurations( proposedReconfigurations ) )
		// throw new IllegalArgumentException("Expected: configurations differing only
		// in proposed controlled metrics");
		// ^ Since the objective function is now allowed to modify observable metrics,
		// we can no longer enforce this.
		this.time = time;
		this.proposedReconfigurations = Collections.unmodifiableList(proposedReconfigurations);
	}
	
	public OptimisationResult(List<Configuration> proposedReconfigurations) {
		// if( !Configuration.equivalentConfigurations( proposedReconfigurations ) )
		// throw new IllegalArgumentException("Expected: configurations differing only
		// in proposed controlled metrics");
		// ^ Since the objective function is now allowed to modify observable metrics,
		// we can no longer enforce this.
		this(proposedReconfigurations, -1);
	}

	///////////////////////////////

	// public Map< String, Value > getControlledMetrics() { return
	// controlledMetrics; }
	public List<Configuration> getReconfigurations() {
		return proposedReconfigurations;
	}

	public long getTimeMilliseconds() {
		return time;
	}
	
	public void setTime(long time) {
		this.time = time;
	}
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
		return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}
}