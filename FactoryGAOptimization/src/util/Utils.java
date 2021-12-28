package util;

import metrics.Value;
import metrics.ValueVisitor;

public class Utils {

	public static double doubleValue(Value v) {
		return new ValueVisitor.NumberVisitor().visit(v).doubleValue();
	}
	
	public static int intValue(Value v) {
		return new ValueVisitor.NumberVisitor().visit(v).intValue();
	}
	
	public static String stringValue(Value v) {
		return new ValueVisitor.ValueStringVisitor().visit(v);
	}
}
