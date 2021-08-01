package optimisation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringStyle;

import uk.ac.york.safire.metrics.Configuration;
import uk.ac.york.safire.metrics.Value;

public abstract class ObjectiveFunction {

	public static final class Result {

		public final String objectiveFunctionStatusReport;
		public final Map<String, Value> keyObjectiveMetrics;
		public final List<Double> objectives;

		///////////////////////////

		public Result(Map<String, Value> keyObjectiveMetrics, String objectiveFunctionStatusReport) {
			this(keyObjectiveMetrics, objectiveFunctionStatusReport, null);
		}

		public Result(Map<String, Value> keyObjectiveMetrics, String objectiveFunctionStatusReport,
				List<Double> objectives) {
			this.keyObjectiveMetrics = Collections.unmodifiableMap(keyObjectiveMetrics);
			this.objectiveFunctionStatusReport = objectiveFunctionStatusReport;
			this.objectives = objectives;
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
			return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this,
					ToStringStyle.MULTI_LINE_STYLE);
		}
	}

	public abstract int numObjectives();

	public abstract ObjectiveFunctionResult evaluate(ObjectiveFunctionArguments args);

	public abstract Result predictKeyObjectives(Configuration current, Map<String, Value> proposedControlMetrics);

	public static abstract class LocalObjectiveFunction extends ObjectiveFunction {

		public abstract Result predictKeyObjectives(Configuration current, Map<String, Value> proposedControlMetrics);

		@Override
		public ObjectiveFunctionResult evaluate(ObjectiveFunctionArguments args) {

			Result res = predictKeyObjectives(args.getConfiguration(), args.getProposedControlMetrics());

			final Configuration newConfig = Configuration.update(args.getConfiguration(),
					args.getProposedControlMetrics(), res.keyObjectiveMetrics);

			ObjectiveFunctionResult or = new ObjectiveFunctionResult(newConfig, res.objectives);

			return or;

		}
	}

	///////////////////////////////

}

// End ///////////////////////////////////////////////////////////////
