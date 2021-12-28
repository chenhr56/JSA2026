package optimisation;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringStyle;

import metrics.Configuration;
import metrics.Value;

///////////////////////////////////

public final class ObjectiveFunctionArguments {

	private final Configuration configuration;
	private final Map<String, Value> proposedControlMetrics;

	///////////////////////////////

	public ObjectiveFunctionArguments(Configuration configuration, Map<String, Value> proposedControlMetrics) {
		this.configuration = configuration;
		this.proposedControlMetrics = Collections.unmodifiableMap(proposedControlMetrics);
	}

	///////////////////////////////

	public Configuration getConfiguration() {
		return configuration;
	}

	public Map<String, Value> getProposedControlMetrics() {
		return proposedControlMetrics;
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
