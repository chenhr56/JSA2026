package uk.ac.york.safire.metrics;

import java.util.Arrays;

///////////////////////////////////

public interface Value {

	public static Value.Int intValue(int value, ValueType.Integer typ) {
		return new Value.Int(value,typ);
	}

	public static Value.Real realValue(double value, ValueType.Real typ) {
		return new Value.Real(value,typ);
	}

	public static Value.Nominal nominalValue(int index, ValueType.Nominal typ) {
		return new Value.Nominal(typ.getValue(index),typ);
	}
	
	///////////////////////////////
	
	public ValueType getType();
	
	///////////////////////////////	
	
	public static final class Nominal implements Value {
		
		public final String value;		
		public final ValueType.Nominal typ;
		
		///////////////////////////
		
		public Nominal(String value, ValueType.Nominal typ) {
			if(!typ.isValid(value))
				throw new IllegalArgumentException();
			
			this.value = value;
			this.typ = typ;
		}
		
		///////////////////////////
		
		public int getIndex() { 
			return typ.indexOf(value);			
		}
		
		public String getValue() { return value; }
		
		@Override
		public ValueType getType() { return typ; }
		
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
			return getClass().getSimpleName() + "(value=" + getValue() + ",type=" + typ + ")";			
		}
	}

	///////////////////////////////
	
	public static final class Int implements Value {
		
		public final int value;
		public final ValueType.Integer typ;		
		
		///////////////////////////		
		
		public Int(int value, ValueType.Integer typ) {
			if(!typ.isValid(value))
				throw new IllegalArgumentException("Expected value of type: " + typ + ", found: " + value );
			
			this.value = value;
			this.typ = typ;
		}

		///////////////////////////
		
		@Override
		public ValueType getType() { return typ; }

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
			return getClass().getSimpleName() + "(value=" + value + ",type=" + typ + ")";			
		}
	}
	
	///////////////////////////////	

	public static final class Real implements Value {
		
		public final double value;
		public final ValueType.Real typ;		
		
		///////////////////////////		
		
		public Real(double value, ValueType.Real typ) {
			if(!typ.isValid(value))
				throw new IllegalArgumentException("Expected value of type: " + typ + ", found: " + value );
			
			this.value = value;
			this.typ = typ;			
		}
		
		///////////////////////////
		
		@Override
		public ValueType getType() { return typ; }

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
			return getClass().getSimpleName() + "(value=" + value + ",type=" + typ + ")";			
		}
	}
}

// End ///////////////////////////////////////////////////////////////


