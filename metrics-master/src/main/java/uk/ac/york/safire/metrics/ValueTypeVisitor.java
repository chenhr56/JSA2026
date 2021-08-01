package uk.ac.york.safire.metrics;

import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import uk.ac.york.safire.metrics.ValueType.Integer;
import uk.ac.york.safire.metrics.ValueType.Real;

public abstract class ValueTypeVisitor< Result > {
	
	public Result visit(ValueType typ) {
		switch( typ.getType() ) {
			case INT:  		return onInt( (ValueType.Integer)typ);
			case REAL: 		return onReal( (ValueType.Real)typ);
			case NOMINAL: 	return onNominal( (ValueType.Nominal)typ);
			default: throw new IllegalStateException("Unknown ValueType:" + typ);
		}
	}
	
	///////////////////////////////
	
	protected abstract Result onInt(ValueType.Integer vt );
	protected abstract Result onReal(ValueType.Real vt );
	protected abstract Result onNominal(ValueType.Nominal vt );
	
	///////////////////////////////
	
	public static final class RandomValueVisitor 
	extends ValueTypeVisitor< Value > {

		private final Random random;
		
		///////////////////////////
		
		public RandomValueVisitor(Random random) {
			this.random = random;
		}
		
		///////////////////////////
		
		@Override
		protected Value onInt(Integer vt) {
			return Value.intValue( vt.min + random.nextInt( vt.max - vt.min + 1 ), vt );
		}

		@Override
		protected Value onReal(Real vt) {
			return Value.realValue( vt.min + ( random.nextDouble() * ( vt.max - vt.min ) ), vt );
		}

		@Override
		protected Value onNominal(ValueType.Nominal vt) {
			return Value.nominalValue( random.nextInt( vt.numValues() ), vt );
		}
	}

	///////////////////////////////
	
	public static final class NumberRangeVisitor 
	extends ValueTypeVisitor< Pair< Number, Number > > {

		@Override
		protected Pair< Number, Number > onInt(Integer vt) {
			return Pair.of( vt.min, vt.max );
		}

		@Override
		protected Pair< Number, Number > onReal(Real vt) {
			return Pair.of( vt.min, vt.max );
		}

		@Override
		protected Pair< Number, Number > onNominal(ValueType.Nominal vt) {
			return Pair.of( 0, vt.numValues() - 1 );			
		}
	}
}

// End ///////////////////////////////////////////////////////////////
