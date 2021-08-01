package optimisation;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringStyle;

import uk.ac.york.safire.metrics.Configuration;

///////////////////////////////////

public final class ObjectiveFunctionResult {

	private final Configuration configuration;
	private final List<Double> objectiveValues;

	///////////////////////////////

	public ObjectiveFunctionResult(Configuration configuration, List<Double> objectiveValues) {
		this.configuration = configuration;
		this.objectiveValues = Collections.unmodifiableList(objectiveValues);
	}

	///////////////////////////////

	public Configuration getConfiguration() {
		return configuration;
	}

	public List<Double> getObjectiveValues() {
		return objectiveValues;
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
