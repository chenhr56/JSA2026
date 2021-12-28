package mitm.atb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import aura.Operators;
import aura.PopulationEntry;
import metrics.Configuration;
import metrics.ObservableMetricType;
import metrics.OptimisationArguments;
import metrics.Value;
import optimisation.AuraLocalOptimisationEngine3;
import optimisation.ObjectiveFunction.LocalObjectiveFunction;
import optimisation.OptimisationEngine3;
import optimisation.OptimisationIslandResult;

///////////////////////////////////

/**
 * From:
 * https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.4/bk_kafka-component-guide/content/ch_kafka-development.html
 */

public final class UoYEarlyPrototypeDemo {

	public static List<ObservableMetricType> observableMetricTypes;
	public static Map<String, Value> observableMetrics;

	public static List<PopulationEntry> lastGeneration = new ArrayList<>();

	public static Map<String, Value> getObservableMetrics() {
		return observableMetrics;
	}

	public static List<ObservableMetricType> getObservableMetricTypes() {
		return observableMetricTypes;
	}

	public static OptimisationIslandResult invokeOE(Configuration template, LocalObjectiveFunction of, int urgency,
			int iteration, int populationSize, Random rng, int engine) {

		final OptimisationEngine3 oe = new AuraLocalOptimisationEngine3(of, iteration, rng);

		final List<Configuration> configs = new ArrayList<>(); // Collections.singletonList( config );
		configs.add(template);
		final Function<Configuration, Configuration> mutation = Operators.hyperMutation(1.0, rng);
		for (int i = 0; i < populationSize - 1; ++i) {
			// jeep.lang.Diag.println(i);
			configs.add(mutation.apply(template));
		}

		List<Map.Entry<String, Value>> linear1 = template.getControlledMetrics().entrySet().stream()
				.collect(Collectors.toList());
		linear1.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

		// final double urgencyNormalised = jeep.math.LinearInterpolation.apply(urgency,
		// 0.0, 100.0, 0.0, 1.0);
		// OnaSingleObjectiveFunction.W1_$eq(urgencyNormalised);
		// OnaSingleObjectiveFunction.W2_$eq(1.0 - urgencyNormalised);

		final long startTime = System.currentTimeMillis();
		final OptimisationIslandResult or = oe.optimise(new OptimisationArguments(configs, 0.5, 0.5, engine));

		final long endTime = System.currentTimeMillis();
		System.out.println("Optmisation took: " + ((endTime - startTime) / 1000.0) + " seconds");

		return or;

	}

}

// End ///////////////////////////////////////////////////////////////
