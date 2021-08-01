package uk.ac.york.safire.metrics;

///////////////////////////////////

public abstract class ValueVisitor< Result > {
	
	public Result visit(Value v) {
		switch( v.getType().getType() ) {
			case INT:  		return onInt( (Value.Int)v );
			case REAL: 		return onReal( (Value.Real)v );
			case NOMINAL: 	return onNominal( (Value.Nominal)v);
			default: throw new IllegalStateException("Unknown ValueType:" + v.getType() );
		}
	}
	
	///////////////////////////////
	
	protected abstract Result onInt(Value.Int vt );
	protected abstract Result onReal(Value.Real vt );
	protected abstract Result onNominal(Value.Nominal vt );
	
	///////////////////////////////
	
	public static final class NumberVisitor 
	extends ValueVisitor< Number > {

		@Override
		protected Number onInt(Value.Int v) {
			return new Integer(v.value);
		}

		@Override
		protected Number onReal(Value.Real v) {
			return new Double(v.value);
		}

		@Override
		protected Number onNominal(Value.Nominal v) {
			return new Integer(v.getIndex());			
		}
	}

	///////////////////////////////
	
	public static final class ValueStringVisitor 
	extends ValueVisitor< String > {

		@Override
		protected String onInt(Value.Int v) {
			return v.toString();
		}

		@Override
		protected String onReal(Value.Real v) {
			return v.toString();
		}

		@Override
		protected String onNominal(Value.Nominal v) {
			return v.getValue();			
		}
	}
}

// End ///////////////////////////////////////////////////////////////
