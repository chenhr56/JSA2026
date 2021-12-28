package metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;



///////////////////////////////////

public class Utility {

	public static Value randomValue(ValueType typ, Random random) {
		return new ValueTypeVisitor.RandomValueVisitor(random).visit(typ);
	}

	public static Process.ExplicitHistorical randomProcess(ConfigurationType typ, int numSamples, double startTime,
			double endTime, Random random) {
		if (numSamples <= 0)
			throw new IllegalArgumentException();
		if (startTime >= endTime)
			throw new IllegalArgumentException();

		final Map<ControlledMetricType, ValueTrace> controlledMetrics = Utility
				.randomControlledMetricsValueTrace(typ.getControlledMetrics(), numSamples, startTime, endTime, random);
		final Map<KeyObjectiveType, ValueTrace> keyObjectives = Utility.randomKeyObjectiveMetricsValueTrace(
				typ.getKeyObjectiveMetrics(), numSamples, startTime, endTime, random);

		return new Process.ExplicitHistorical(typ, controlledMetrics, keyObjectives);
	}

	///////////////////////////////

	public static Configuration randomConfiguration(ConfigurationType ct, Random random) {
		final Map<String, Value> controlledMetrics = new HashMap<>();
		for (ControlledMetricType t : ct.getControlledMetrics()) {
			controlledMetrics.put(t.name, randomValue(t.valueType, random));
		}

		final Map<String, Value> keyObjectives = new HashMap<>();
		for (KeyObjectiveType t : ct.getKeyObjectiveMetrics()) {
			keyObjectives.put(t.name, randomValue(t.valueType, random));
		}

		return new Configuration(ct, controlledMetrics, keyObjectives);
	}

	public static Map<String, Value> makeObservableMetrics(List<ObservableMetricType> observableMetricTypes,
			Random random) {
		final Map<String, Value> observableMetrics = new HashMap<>();
		for (ObservableMetricType t : observableMetricTypes) {
			observableMetrics.put(t.name, randomValue(t.valueType, random));
		}

		return observableMetrics;
	}

	///////////////////////////////

	static ValueTrace randomValueTrace(ValueType typ, int numSamples, double startTime, double endTime, Random random) {
		if (numSamples <= 0)
			throw new IllegalArgumentException();
		if (startTime >= endTime)
			throw new IllegalArgumentException();

		final List<Double> timestamps = random.doubles().limit(numSamples).boxed().collect(Collectors.toList());
		Collections.sort(timestamps);
		List<Pair<Value, Double>> trace = timestamps.stream().map(t -> Pair.of(randomValue(typ, random), t))
				.collect(Collectors.toList());
		return new ValueTrace(trace);
	}

	static Map<ControlledMetricType, ValueTrace> randomControlledMetricsValueTrace(List<ControlledMetricType> types,
			int numSamples, double startTime, double endTime, Random random) {
		if (numSamples <= 0)
			throw new IllegalArgumentException();
		if (startTime >= endTime)
			throw new IllegalArgumentException();

		return types.stream().map(
				typ -> Pair.of(typ, Utility.randomValueTrace(typ.valueType, numSamples, startTime, endTime, random)))
				.collect(Collectors.toMap(p -> p.getLeft(), p -> p.getRight()));
	}

	static Map<ObservableMetricType, ValueTrace> randomObservableMetricsValueTrace(List<ObservableMetricType> types,
			int numSamples, double startTime, double endTime, Random random) {
		if (numSamples <= 0)
			throw new IllegalArgumentException();
		if (startTime >= endTime)
			throw new IllegalArgumentException();

		return types.stream()
				.map((ObservableMetricType typ) -> Pair.of(typ,
						Utility.randomValueTrace(typ.valueType, numSamples, startTime, endTime, random)))
				.collect(Collectors.toMap(p -> p.getLeft(), p -> p.getRight()));
	}

	static Map<KeyObjectiveType, ValueTrace> randomKeyObjectiveMetricsValueTrace(List<KeyObjectiveType> types,
			int numSamples, double startTime, double endTime, Random random) {
		if (numSamples <= 0)
			throw new IllegalArgumentException();
		if (startTime >= endTime)
			throw new IllegalArgumentException();

		return types.stream()
				.map((KeyObjectiveType typ) -> Pair.of(typ,
						Utility.randomValueTrace(typ.valueType, numSamples, startTime, endTime, random)))
				.collect(Collectors.toMap(p -> p.getLeft(), p -> p.getRight()));
	}
}

// End ///////////////////////////////////////////////////////////////
