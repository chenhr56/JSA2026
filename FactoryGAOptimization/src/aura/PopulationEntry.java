package aura;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringStyle;

import metrics.Configuration;

///////////////////////////////////

public final class PopulationEntry {

	private final Configuration configuration;
	private final List<Double> objectives;

	///////////////////////////////

	public PopulationEntry(Configuration configuration, List<Double> objectives) {
		this.configuration = configuration;
		this.objectives = Collections.unmodifiableList(objectives);
	}

	///////////////////////////////

	public Configuration getConfiguration() {
		return configuration;
	}

	public List<Double> getObjectives() {
		return objectives;
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
				ToStringStyle.SHORT_PREFIX_STYLE);
	}
}

// End ///////////////////////////////////////////////////////////////
