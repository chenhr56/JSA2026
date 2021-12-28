package aura;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import metrics.Configuration;
import metrics.ConfigurationType;
import metrics.SearchDirection;
import metrics.Utility;
import metrics.Value;
import metrics.ValueType;
import metrics.ValueTypeVisitor;
import metrics.ValueVisitor;
import metrics.Value.Int;
import metrics.Value.Nominal;
import metrics.Value.Real;
import mitm.atb.UoYEarlyPrototypeDemo;

public class Operators {

	static final List<Double> getControlsNamedVar(Configuration c) {
		final Function<Value, Double> valueToDouble = (v) -> new ValueVisitor.NumberVisitor().visit(v).doubleValue();
		return c.getControlledMetrics().entrySet().stream().filter(e -> e.getKey().contains("var"))
				.map((e) -> valueToDouble.apply(e.getValue())).collect(Collectors.toList());
	}

	public static List<Configuration> initialPopulation(int populationSize, ConfigurationType ct, Random rng) {
		final List<Configuration> initialPopulation = new ArrayList<>();
		for (int i = 0; i < populationSize; ++i)
			initialPopulation.add(Utility.randomConfiguration(ct, rng));
		UoYEarlyPrototypeDemo.observableMetrics = Utility
				.makeObservableMetrics(UoYEarlyPrototypeDemo.observableMetricTypes, rng);
		return initialPopulation;
	}

	///////////////////////////////

	public static Comparator<List<Double>> compareMultiObjective(SearchDirection direction) {
		return new Comparator<List<Double>>() {
			@Override
			public int compare(List<Double> o1, List<Double> o2) {
				if (o1.size() != o2.size())
					throw new IllegalArgumentException();

				int acc = 0;
				for (int i = 0; i < o1.size(); ++i) {
					final int cmp = direction == SearchDirection.MINIMIZING ? -Double.compare(o1.get(i), o2.get(i))
							: Double.compare(o1.get(i), o2.get(i));
					if (cmp < 0)
						return -1;
					else {
						acc = Math.min(acc, cmp);
					}
				}

				return acc;
			}
		};
	}

	///////////////////////////////

	public static BiFunction<List<PopulationEntry>, Random, PopulationEntry> proportionalSelection = (pop, rnd) -> {
		if (pop.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (pop.get(0).getObjectives().size() != 1) {
			throw new IllegalArgumentException();
		}

		final int index = ProportionalSelectionUtil.select(pop, (pe) -> pe.getObjectives().get(0), rnd);
		return pop.get(index);
	};

	public static BiFunction<List<PopulationEntry>, Random, PopulationEntry> proportionalSelectionFirstObjective = (pop,
			rnd) -> {
		if (pop.isEmpty()) {
			throw new IllegalArgumentException();
		}

		final int index = ProportionalSelectionUtil.select(pop, (pe) -> pe.getObjectives().get(0), rnd);
		return pop.get(index);
	};

	public static BiFunction<Configuration, Configuration, Configuration> onePointCrossover(Random rng) {
		return (c1, c2) -> {
			// if( !( c1.getConfigurationType().equals( c2.getConfigurationType() ) ) )
			// throw new IllegalArgumentException();

			List<Map.Entry<String, Value>> linear1 = c1.getControlledMetrics().entrySet().stream()
					.collect(Collectors.toList());
			List<Map.Entry<String, Value>> linear2 = c2.getControlledMetrics().entrySet().stream()
					.collect(Collectors.toList());
			assert (linear1.size() == linear2.size());

			linear1.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));
			linear2.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

			final int index = rng.nextInt(linear1.size());

			final List<Map.Entry<String, Value>> crossed = linear1.subList(0, index);
			crossed.addAll(linear2.subList(index, linear2.size()));

			final Map<String, Value> crossedControlledMetrics = crossed.stream()
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
			return new Configuration(c1.getConfigurationType(), crossedControlledMetrics, c1.getKeyObjectives());
		};
	}

	///////////////////////////////

	static final class PerturbationVisitor extends ValueVisitor<Value> {

		private final Random rng;

		///////////////////////////

		public PerturbationVisitor(Random rng) {
			this.rng = rng;
		}

		@Override
		protected Value onInt(Int x) {
			final Pair<Number, Number> range = new ValueTypeVisitor.NumberRangeVisitor().visit(x.getType());
			final int lower = range.getLeft().intValue();
			final int upper = range.getRight().intValue();
			// final ClosedInterval interval = lower == upper ? ClosedInterval.create(lower,
			// lower + 1) : ClosedInterval.create(lower, upper);
			//
			// final int numBits = 32;
			// final List< Boolean > mutated = mutateUniform(grayEncodeFromReal(x.value,
			// numBits, interval ), rng);
			// final double value = grayDecodeToReal( mutated, numBits, interval );
			// return Value.intValue( (int)value, (ValueType.Integer)x.getType());
			// FIXME: put the above back in
			final int currentValue = new ValueVisitor.NumberVisitor().visit(x).intValue();
			int newValue = currentValue + rng.nextInt(100);
			if (newValue < lower)
				newValue = lower;
			else if (newValue > upper)
				newValue = lower;
			;

			return Value.intValue(newValue, (ValueType.Integer) x.getType());
		}

		@Override
		protected Value onNominal(Nominal x) {
			final ValueType.Nominal vtn = (ValueType.Nominal) x.getType();
			final int index = rng.nextInt(vtn.numValues());
			return Value.nominalValue(index, vtn);
		}

		@Override
		protected Value onReal(Real x) {
			return null;
		}
	}

	///////////////////////////////

	public static Function<Configuration, Configuration> onePointMutation(Random rng) {
		return (c) -> {
			final Map<String, Value> controls = c.getControlledMetrics();
			final List<MutablePair<String, Value>> controlsList = controls.entrySet().stream()
					.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList());
			final int index = rng.nextInt(controlsList.size());
			MutablePair<String, Value> p = controlsList.get(index);
			// p.setValue( new ValueTypeVisitor.RandomValueVisitor(rng).visit(
			// p.getValue().getType() ));
			p.setValue(new PerturbationVisitor(rng).visit(p.getValue()));

			Map<String, Value> proposedControls = controlsList.stream()
					.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

			return new Configuration(c.getConfigurationType(), proposedControls, c.getKeyObjectives());
		};
	}

	public static Function<Configuration, Configuration> uniformMutation(Random rng) {
		return (c) -> {
			final Map<String, Value> controls = c.getControlledMetrics();
			final List<MutablePair<String, Value>> controlsList = controls.entrySet().stream()
					.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList());

			for (int i = 0; i < controlsList.size(); ++i) {
//				System.out.println("aaaa " + (rng.nextDouble() < 1.0 / controlsList.size()));
				if (rng.nextDouble() < 1.0 / controlsList.size()) {
					MutablePair<String, Value> p = controlsList.get(i);
					p.setValue(new PerturbationVisitor(rng).visit(p.getValue()));
				}
			}

			Map<String, Value> proposedControls = controlsList.stream()
					.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

			return new Configuration(c.getConfigurationType(), proposedControls, c.getKeyObjectives());
		};
	}

	public static Function<Configuration, Configuration> hyperMutation(double pm, Random rng) {
		if (pm < 0.0 || pm > 1.0)
			throw new IllegalArgumentException();

		return (c) -> {
			final Map<String, Value> controls = c.getControlledMetrics();
			final List<MutablePair<String, Value>> controlsList = controls.entrySet().stream()
					.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList());

			for (int i = 0; i < controlsList.size(); ++i) {
				if (rng.nextDouble() < pm) {
					MutablePair<String, Value> p = controlsList.get(i);
					p.setValue(new PerturbationVisitor(rng).visit(p.getValue()));
				}
			}

			Map<String, Value> proposedControls = controlsList.stream()
					.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

			return new Configuration(c.getConfigurationType(), proposedControls, c.getKeyObjectives());
		};
	}

	public static Predicate<List<List<PopulationEntry>>> maxIterTermination(int maxIter) {
		return (l) -> l.size() >= maxIter;
	}
}

// End ///////////////////////////////////////////////////////////////
