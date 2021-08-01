package uk.ac.york.safire.metrics;

import org.apache.commons.lang3.builder.ToStringStyle;

public interface SampleRate {

	public static SampleRate eventDriven = new EventDriven();
	public static SampleRate periodic(double frequencyInMillisecondsLo,double frequencyInMillisecondsHi) {
		return new Periodic(frequencyInMillisecondsLo,frequencyInMillisecondsHi);	
	}
	
	///////////////////////////////
	
	public static final class EventDriven implements SampleRate {
		
		@Override
		public boolean equals(Object rhs) {
			return EventDriven.class.isInstance(rhs);			
		}

		@Override
		public int hashCode() { return 0; }

		@Override
		public String toString() {
			return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this,ToStringStyle.SHORT_PREFIX_STYLE);			
		}
	}
	
	///////////////////////////////	
	
	public static final class Periodic implements SampleRate {
		
		public final double frequencyInMillisecondsLo;
		public final double frequencyInMillisecondsHi;
		
		///////////////////////////
		
		public Periodic(double frequencyInMillisecondsLo,double frequencyInMillisecondsHi) {
			if(frequencyInMillisecondsLo > frequencyInMillisecondsHi)
				throw new IllegalArgumentException();
				
			this.frequencyInMillisecondsLo = frequencyInMillisecondsLo;
			this.frequencyInMillisecondsHi = frequencyInMillisecondsHi;			
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
			return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this,ToStringStyle.SHORT_PREFIX_STYLE);			
		}
	}	
}

// End ///////////////////////////////////////////////////////////////
