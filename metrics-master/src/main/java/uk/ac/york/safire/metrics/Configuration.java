package uk.ac.york.safire.metrics;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringStyle;

///////////////////////////////////

public final class Configuration {

	private final ConfigurationType configurationType;
	private Map<String, Value> controlledMetrics;
	private Map<String, Value> keyObjectives;

	///////////////////////////////

	public Configuration(ConfigurationType configurationType, Map<String, Value> controlledMetrics,
			Map<String, Value> keyObjectives) {

		this.configurationType = configurationType;
		this.controlledMetrics = Collections.unmodifiableMap(controlledMetrics);
		this.keyObjectives = Collections.unmodifiableMap(keyObjectives);
	}

	public static Configuration update(Configuration current, Map<String, Value> proposedControlledMetrics,
			Map<String, Value> predictedKeyObjectives) {
		return new Configuration(current.getConfigurationType(), proposedControlledMetrics, predictedKeyObjectives); // current.keyObjectives);
	}

	public static boolean equivalentModuloControlsAndKeyObjectives(Configuration a, Configuration b) {
		return a.configurationType.equals(b.configurationType);
	}

	public static boolean equivalentConfigurations(List<Configuration> config) {
		for (int i = 1; i < config.size(); ++i)
			if (!Configuration.equivalentModuloControlsAndKeyObjectives(config.get(i - 1), config.get(i)))
				return false;
		return true;
	}

	public ConfigurationType getConfigurationType() {
		return configurationType;
	}

	public Map<String, Value> getControlledMetrics() {
		return controlledMetrics;
	}

	public Map<String, Value> getKeyObjectives() {
		return keyObjectives;
	}

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
