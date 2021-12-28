package metrics;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;



///////////////////////////////////

public interface Process {
	
	public ConfigurationType getConfigurationType();
	
	public Pair<Value,Double> getControlledMetric(ControlledMetricType typ,double timeInMilliseconds);
	public Optional< Pair<Value,Double> > setControlledMetric(ControlledMetricType typ, ValueType value, double timeInMilliseconds);	
//	public Pair<Value,Double> getObservableMetric(ObservableMetricType typ,double timeInMilliseconds);
	public Optional<Pair<Value,Double>> getKeyObjectiveMetric(KeyObjectiveType typ,double timeInMilliseconds);
	
	///////////////////////////////
	
	public static class ExplicitHistorical implements Process {

		private ConfigurationType configurationType;
		private final Map< ControlledMetricType, ValueTrace > 	controlledMetrics;
		private final Map< KeyObjectiveType, ValueTrace >		keyObjectives;		
		
		///////////////////////////

		public ExplicitHistorical(ConfigurationType configurationType,
				Map< ControlledMetricType, ValueTrace > controlledMetrics,
				Map< KeyObjectiveType, ValueTrace >	keyObjectives) {

			this.configurationType = configurationType; 
			this.controlledMetrics = Collections.unmodifiableMap( controlledMetrics );
			this.keyObjectives = Collections.unmodifiableMap( keyObjectives );
		}
		
		///////////////////////////
		
		@Override
		public ConfigurationType getConfigurationType() { return configurationType; }

		@Override
		public Pair<Value,Double> 
		getControlledMetric(ControlledMetricType typ, double timeInMilliseconds) {
			return controlledMetrics.get(typ).get(timeInMilliseconds);
		}

		@Override
		public Optional<Pair<Value,Double>> 
		setControlledMetric(ControlledMetricType typ, ValueType value, double timeInMilliseconds) {
			return Optional.empty();
		}
		
		@Override
		public Optional<Pair<Value,Double>> 
		getKeyObjectiveMetric(KeyObjectiveType typ, double timeInMilliseconds) {
			final Pair<Value,Double> x = keyObjectives.get(typ).get(timeInMilliseconds);
			return x == null ? Optional.empty() : Optional.of(x); 
		}

		///////////////////////////
		
		public Map< ControlledMetricType, ValueTrace > 
		getcontrolledMetrics() { return controlledMetrics; }
		
		public Map< KeyObjectiveType, ValueTrace >	
		getKeyObjectives() { return	keyObjectives; }		
		
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
			return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);			
		}
	}
}

// End ///////////////////////////////////////////////////////////////
