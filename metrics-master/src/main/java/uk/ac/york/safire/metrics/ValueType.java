package uk.ac.york.safire.metrics;

import java.util.Arrays;

import org.apache.commons.lang3.builder.ToStringStyle;

public class ValueType {
	
	private final Type typ;
	
	///////////////////////////////

	public static enum Type { INT, REAL, NOMINAL };
	
	///////////////////////////////	

	protected ValueType(Type type) {
		this.typ = type;
	}
	
	///////////////////////////////
	
	public static ValueType.Integer intType(int min, int max) { 
		return new Integer(min,max);
	}

	public static ValueType.Real realType(double min, double max) { 
		return new Real(min,max);
	}
	
	public static ValueType.Nominal nominalType(String name, String [] values) { 
		return new ValueType.Nominal(name, values.clone());
	}

	///////////////////////////////
	
	public Type getType() { return typ; }
	
	///////////////////////////////
	
	public static final class Nominal extends ValueType {
		
		private String name;
		private String [] values;
		
		public Nominal(String name, String [] values) {
			super(Type.NOMINAL);
			this.name = name;
			this.values = values;
		}
		
		///////////////////////////
		
		public String getName() { return name; }		
		public int numValues() { return values.length; }		
		public String getValue(int index) { return values[index]; }
		
		@Override
		public Type getType() { return Type.NOMINAL; }
		
		boolean isValid(String value) { 
			return Arrays.asList(values).contains( value ); 
		}
		
		int indexOf(String value) {
			return Arrays.asList(values).indexOf( value );
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
	
	///////////////////////////////
	
	public static final class Integer extends ValueType {
		public int min, max;
		
		public Integer(int min, int max) {
			super(Type.INT);
			if( min > max )
				throw new IllegalArgumentException();
			
			this.min = min;
			this.max = max;			
		}

		boolean isValid(double value) { return value >= min && value <= max; }
		
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
	
	///////////////////////////////	

	public static final class Real extends ValueType {
		public final double min, max;
		
		public Real(double min, double max) {
			super(Type.REAL);
			
			if( min > max )
				throw new IllegalArgumentException();
			
			this.min = min;
			this.max = max;			
		}

		@Override
		public Type getType() { return Type.REAL; }
		
		boolean isValid(double value) { return value >= min && value <= max; }
		
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

// End //////////////////////////////////////////////////////////////

