package aura;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import optimisation.ObjectiveFunction;
import optimisation.ObjectiveFunctionArguments;
import optimisation.ObjectiveFunctionResult;
import optimisation.OptimisationIslandResult;
import uk.ac.york.safire.metrics.Configuration;

///////////////////////////////////

public final class EAIsland {

	private static void removeEntryWithHighestFirstObjective(List<PopulationEntry> population) {

		if (population.size() < 2)
			throw new InvalidParameterException();

		PopulationEntry entryWithHighestFirstObjective = null;
		int entryWithHighestFirstObjectiveIndex = -1;
		for (int i = 0; i < population.size(); i++) {
			if (entryWithHighestFirstObjective == null || entryWithHighestFirstObjective.getObjectives()
					.get(0) < population.get(i).getObjectives().get(0)) {
				entryWithHighestFirstObjective = population.get(i);
				entryWithHighestFirstObjectiveIndex = i;
			}
		}
		assert (entryWithHighestFirstObjectiveIndex != -1);

		population.remove(entryWithHighestFirstObjectiveIndex);
	}

	private static PopulationEntry getEntryWithLowestFirstObjective(List<PopulationEntry> population) {

		if (population.size() < 2)
			throw new InvalidParameterException();

		PopulationEntry entryWithLowestFirstObjective = null;
		for (PopulationEntry populationEntry : population) {
			if (entryWithLowestFirstObjective == null
					|| entryWithLowestFirstObjective.getObjectives().get(0) > populationEntry.getObjectives().get(0)) {
				entryWithLowestFirstObjective = populationEntry;
			}
		}

		return entryWithLowestFirstObjective;
	}

	public static OptimisationIslandResult apply(List<Configuration> initialPopulation,
			ObjectiveFunction.LocalObjectiveFunction of, Comparator<List<Double>> compareMultiObjective,
			BiFunction<List<PopulationEntry>, Random, PopulationEntry> select,
			BiFunction<Configuration, Configuration, Configuration> crossover,
			Function<Configuration, Configuration> mutation, Predicate<List<List<PopulationEntry>>> isFinished,
			Random rng) {

		if (initialPopulation.size() < 2) {
			System.out.println("initialPopulation.size() < 2");
			throw new IllegalArgumentException();

		}

		///////////////////////////

		final Comparator<PopulationEntry> cmpPE = new Comparator<PopulationEntry>() {
			@Override
			public int compare(PopulationEntry a, PopulationEntry b) {
				return compareMultiObjective.compare(a.getObjectives(), b.getObjectives());
			}
		};

		// final Stream<PopulationEntry> populationWithObjectiveValues =
		// initialPopulation.stream()
		// .map((Configuration c) -> {
		// ObjectiveFunctionResult evaled = ObjectiveFunction.evaluateNowHelper(of,
		// new ObjectiveFunctionArguments(c, c.getControlledMetrics()));
		//
		// return new PopulationEntry(evaled.getConfiguration(),
		// evaled.getObjectiveValues());
		// });

		final Stream<PopulationEntry> population = initialPopulation.parallelStream().map((Configuration c) -> {

			ObjectiveFunctionResult evaled = null;
			try {
				evaled = of.evaluate(new ObjectiveFunctionArguments(c, c.getControlledMetrics()));
			} catch (Exception e) {
				e.printStackTrace();
			}

			return new PopulationEntry(evaled.getConfiguration(), evaled.getObjectiveValues());
		});

		///////////////////////////

		List<PopulationEntry> currentPopulation = population.collect(Collectors.toList());
		PopulationEntry bestEver = currentPopulation.stream().max(cmpPE).get();

		List<List<PopulationEntry>> history = new ArrayList<>();
		history.add(currentPopulation);
		while (!isFinished.test(history)) {

			final List<PopulationEntry> newPopulation = new ArrayList<>();

			final List<PopulationEntry> thisGen = currentPopulation;

			// IntStream.range(0, initialPopulation.size() / 2).parallel().forEach( i -> {

			IntStream.range(0, initialPopulation.size() / 2).forEach(i -> {
				final PopulationEntry p1 = select.apply(thisGen, rng);
				final PopulationEntry p2 = select.apply(thisGen, rng);
				final Configuration c1 = mutation.apply(crossover.apply(p1.getConfiguration(), p2.getConfiguration()));
				final Configuration c2 = mutation.apply(crossover.apply(p1.getConfiguration(), p2.getConfiguration()));

				// final ObjectiveFunctionResult evaled1 =
				// ObjectiveFunction.evaluateNowHelper(of,
				// new ObjectiveFunctionArguments(c1, c1.getControlledMetrics()));
				// final ObjectiveFunctionResult evaled2 =
				// ObjectiveFunction.evaluateNowHelper(of,
				// new ObjectiveFunctionArguments(c2, c2.getControlledMetrics()));

				ObjectiveFunctionResult evaled1 = null;
				try {
					evaled1 = of.evaluate(new ObjectiveFunctionArguments(c1, c1.getControlledMetrics()));
				} catch (Exception e) {
					e.printStackTrace();
				}
				ObjectiveFunctionResult evaled2 = null;
				try {
					evaled2 = of.evaluate(new ObjectiveFunctionArguments(c2, c2.getControlledMetrics()));
				} catch (Exception e) {
					e.printStackTrace();
				}

				final PopulationEntry e1 = new PopulationEntry(evaled1.getConfiguration(),
						evaled1.getObjectiveValues());
				final PopulationEntry e2 = new PopulationEntry(evaled2.getConfiguration(),
						evaled2.getObjectiveValues());
				newPopulation.add(e1);
				newPopulation.add(e2);
			});

			// elistism
			removeEntryWithHighestFirstObjective(newPopulation);
			newPopulation.add(getEntryWithLowestFirstObjective(thisGen));

			// jeep.lang.Diag.println( initialPopulation.size() );
			// jeep.lang.Diag.println( newPopulation.size() );
			assert (newPopulation.size() == initialPopulation.size());
			final PopulationEntry newBest = newPopulation.parallelStream().max(cmpPE).get();
			bestEver = cmpPE.compare(bestEver, newBest) < 0 ? newBest : bestEver;
			currentPopulation = newPopulation;
			history.add(newPopulation);
		}
		List<PopulationEntry> bestEvers = new ArrayList<PopulationEntry>();
		bestEvers.add(bestEver);
		double[][] caps = new double[1][2];
		caps[0][0] = bestEver.getObjectives().get(0);
		caps[0][1] = bestEver.getObjectives().get(0);
		return new OptimisationIslandResult(bestEvers, currentPopulation, caps);
	}
}

// End ///////////////////////////////////////////////////////////////
