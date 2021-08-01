package uk.ac.york.safire.metrics;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringStyle;

///////////////////////////////////

public interface ConfigurationType {

	public List<ControlledMetricType> getControlledMetrics();
	public List<KeyObjectiveType> getKeyObjectiveMetrics();

	///////////////////////////////

	public static final class Explicit implements ConfigurationType {

		private final List<KeyObjectiveType> keyObjectiveMetricTypes;
		private final List<ControlledMetricType> controlledMetricTypes;

		private static <T> boolean uniqueNames(List<T> l, Function<T, String> f) {
			return l.stream().map(f).collect(Collectors.toSet()).size() == l.size();
		}

		public Explicit(List<KeyObjectiveType> keyObjectiveMetrics, List<ControlledMetricType> controlledMetrics) {
			if (!uniqueNames(keyObjectiveMetrics, x -> x.name))
				throw new IllegalArgumentException();
			if (!uniqueNames(controlledMetrics, x -> x.name))
				throw new IllegalArgumentException();

			this.keyObjectiveMetricTypes = Collections.unmodifiableList(keyObjectiveMetrics);
			this.controlledMetricTypes = Collections.unmodifiableList(controlledMetrics);
		}

		@Override
		public List<KeyObjectiveType> getKeyObjectiveMetrics() {
			return keyObjectiveMetricTypes;
		}

		@Override
		public List<ControlledMetricType> getControlledMetrics() {
			return controlledMetricTypes;
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
					ToStringStyle.MULTI_LINE_STYLE);
		}
	}
}

// End ///////////////////////////////////////////////////////////////
