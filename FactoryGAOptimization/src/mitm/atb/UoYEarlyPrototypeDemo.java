package mitm.atb;

import java.util.List;
import java.util.Map;

import uk.ac.york.safire.metrics.ObservableMetricType;
import uk.ac.york.safire.metrics.Value;

///////////////////////////////////

/**
 * From:
 * https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.4/bk_kafka-component-guide/content/ch_kafka-development.html
 */

public final class UoYEarlyPrototypeDemo {

	public static List<ObservableMetricType> observableMetricTypes;
	public static Map<String, Value> observableMetrics;

	public static Map<String, Value> getObservableMetrics() {
		return observableMetrics;
	}

	public static List<ObservableMetricType> getObservableMetricTypes() {
		return observableMetricTypes;
	}

}

// End ///////////////////////////////////////////////////////////////
