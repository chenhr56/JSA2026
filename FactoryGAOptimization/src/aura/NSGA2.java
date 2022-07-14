package aura;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.MutablePair;

import jeep.lang.Logic;
import metrics.Configuration;
import metrics.SearchDirection;
import optimisation.ObjectiveFunction;
import optimisation.ObjectiveFunctionArguments;
import optimisation.ObjectiveFunctionResult;

///////////////////////////////////

public class NSGA2 {

	static int compareObjectives(double o1, double o2, SearchDirection dir) {
		return dir == SearchDirection.MINIMIZING ? -Double.compare(o1, o2) : Double.compare(o1, o2);
	}

	///////////////////////////////

	/**
	 * Algorithm 98 from "Essentials of Metaheuristics", 2nd Edition, p137.
	 */
	static boolean paretoDominates(List<Double> o1, List<Double> o2, SearchDirection dir) {
		if (o1.size() != o2.size())
			throw new IllegalArgumentException("Unequal sizes: " + o1.size() + " and " + o2.size());

		boolean result = false;
		for (int i = 0; i < o1.size(); ++i) {
			final int cmp = compareObjectives(o1.get(i), o2.get(i), dir);
			if (cmp > 0)
				result = true;
			else {
				if (cmp < 0)
					return false;
			}
		}

		assert (Logic.implies(o1.equals(o2), !result));
		return result;
	}

	///////////////////////////////

	public static boolean isNondominated(List<List<Double>> objectivesList, SearchDirection dir) {
		for (int i = 0; i < objectivesList.size(); ++i)
			for (int j = 0; j < objectivesList.size(); ++j)
				if (j != i && paretoDominates(objectivesList.get(i), objectivesList.get(j), dir))
					return false;

		return true;
	}


	public static boolean isParetoFrontOf(List<List<Double>> front, List<List<Double>> pop, SearchDirection dir) {
		return isNondominated(front, dir) && pop.stream()
				.allMatch(g -> front.contains(g) || !front.stream().anyMatch(f -> paretoDominates(g, f, dir)));
	}

	static List<PopulationEntry> paretoFrontNaive(List<PopulationEntry> G, SearchDirection dir) {

		// naive O(n^2) algorithm

		List<PopulationEntry> F = new ArrayList<>();

		for (int i = 0; i < G.size(); ++i) {
			final PopulationEntry G_i = G.get(i);

			boolean dominated = false;
			for (int j = 0; !dominated && j < G.size(); ++j) {
				if (j == i)
					continue;

				final PopulationEntry G_j = G.get(j);

				dominated = paretoDominates(G_j.getObjectives(), G_i.getObjectives(), dir);
			}
			if (!dominated && !F.stream().anyMatch(F_j -> F_j.getObjectives().equals(G_i.getObjectives())))
				F.add(G_i);
		}

		assert (isParetoFrontOf(F.stream().map(x -> x.getObjectives()).collect(Collectors.toList()),
				G.stream().map(x -> x.getObjectives()).collect(Collectors.toList()), dir));

		return F;
	}

	/**
	 * Adapted from Algorithm 100 from "Essentials of Metaheuristics", 2nd Edition,
	 * p137.
	 */

	static List<PopulationEntry> paretoFront(List<PopulationEntry> G, SearchDirection dir) {

		List<PopulationEntry> F = new ArrayList<>();
		for (int i = 0; i < G.size(); ++i) {

			final PopulationEntry G_i = G.get(i);
			F = F.stream().filter(F_j -> !paretoDominates(G_i.getObjectives(), F_j.getObjectives(), dir))
					.collect(Collectors.toList());
			if (!F.stream().anyMatch(F_j -> F_j.getObjectives().equals(G_i.getObjectives())
					|| paretoDominates(F_j.getObjectives(), G_i.getObjectives(), dir)))
				F.add(G_i);
		}

		assert (F.stream().collect(Collectors.toSet())
				.equals(paretoFrontNaive(G, dir).stream().collect(Collectors.toSet())));
		// return
		// F.stream().collect(Collectors.toSet()).stream().collect(Collectors.toList());
		return F;
	}

	///////////////////////////////

	/**
	 * Algorithm 101 from "Essentials of Metaheuristics", 2nd Edition, p137.
	 */

	static List<List<PopulationEntry>> paretoFrontRanks(List<PopulationEntry> P, SearchDirection dir) {

		List<PopulationEntry> Pdash = new ArrayList<>();
		Pdash.addAll(P);
		List<List<PopulationEntry>> ranks = new ArrayList<>();
		do {
			final List<PopulationEntry> R_i = paretoFront(Pdash, dir);
			ranks.add(R_i);
			Pdash.removeAll(R_i);

		} while (!Pdash.isEmpty());

		return ranks;
	}

	///////////////////////////////

	static final class PopulationEntryRankAndSparsity {
		final PopulationEntry populationEntry;
		final int rank;
		final double sparsity;

		///////////////////////////

		public PopulationEntryRankAndSparsity(PopulationEntry pe, int rank, double sparsity) {
			this.populationEntry = pe;
			this.rank = rank;
			this.sparsity = sparsity;
		}
	}

	///////////////////////////////

	/**
	 * Algorithm 102 from "Essentials of Metaheuristics", 2nd Edition, p137.
	 */

	static List<PopulationEntryRankAndSparsity> multiobjectiveSparsityAssignment(List<PopulationEntry> F, int rank,
			List<Double> objectiveValueRange, SearchDirection dir) {

		if (objectiveValueRange.stream().anyMatch(value -> value <= 0.0))
			throw new IllegalArgumentException();
		if (F.isEmpty())
			throw new IllegalArgumentException();
		if (F.stream().anyMatch(pe -> pe.getObjectives().size() != objectiveValueRange.size()))
			throw new IllegalArgumentException("Expected #objectives: " + objectiveValueRange.size() + ", found "
					+ F.stream().map(pe -> pe.getObjectives().size()).collect(Collectors.toList()));

		///////////////////////////

		final List<MutablePair<PopulationEntry, Double>> Fdash = F.stream().map(pe -> MutablePair.of(pe, 0.0))
				.collect(Collectors.toList());

		// final int numObjectives = F.get(0).getObjectives().size();

		for (int i = 0; i < objectiveValueRange.size(); ++i) {
			final int iFinal = i;
			Fdash.sort((p1, p2) -> compareObjectives(p1.getLeft().getObjectives().get(iFinal),
					p2.getLeft().getObjectives().get(iFinal), dir));
			Fdash.get(0).setRight(Double.POSITIVE_INFINITY);
			Fdash.get(Fdash.size() - 1).setRight(Double.POSITIVE_INFINITY);

			for (int j = 1; j < Fdash.size() - 1; ++j) {
				final double inc = Fdash.get(j + 1).getLeft().getObjectives().get(i)
						- Fdash.get(j - 1).getLeft().getObjectives().get(i);
				Fdash.get(j).setRight(Fdash.get(j).getRight() + (inc / objectiveValueRange.get(i)));
			}
		}

		return Fdash.stream().map(p -> new PopulationEntryRankAndSparsity(p.getLeft(), rank, p.getRight()))
				.collect(Collectors.toList());
	}

	///////////////////////////////

	/**
	 * Algorithm 103 from "Essentials of Metaheuristics", 2nd Edition.
	 */

	static PopulationEntry nondominatedLexicographicTournamentSelectionWithSparsity(int tournamentSize,
			List<PopulationEntryRankAndSparsity> P, Random rng) {
		if (tournamentSize < 1)
			throw new IllegalArgumentException();

		PopulationEntryRankAndSparsity best = P.get(rng.nextInt(P.size()));
		for (int i = 2; i < tournamentSize; ++i) {
			final PopulationEntryRankAndSparsity next = P.get(rng.nextInt(P.size()));
			if (next.rank < best.rank || (next.rank == best.rank && next.sparsity > best.sparsity))
				best = next;
		}

		return best.populationEntry;
	}



	///////////////////////////////

	private static List<PopulationEntry> breedSequential(final List<PopulationEntryRankAndSparsity> currentPopulation,
			ObjectiveFunction.LocalObjectiveFunction of,
			BiFunction<List<PopulationEntryRankAndSparsity>, Random, PopulationEntry> select,
			BiFunction<Configuration, Configuration, Configuration> crossover,
			Function<Configuration, Configuration> mutation, Random rng) {

		final List<PopulationEntry> newPopulation = new ArrayList<>();

		// IntStream.range(0, initialPopulation.size() / 2).parallel().forEach( i -> {
		IntStream.range(0, currentPopulation.size() / 2).forEach(i -> {
			final PopulationEntry p1 = select.apply(currentPopulation, rng);
			final PopulationEntry p2 = select.apply(currentPopulation, rng);
			final Configuration c1 = mutation.apply(crossover.apply(p1.getConfiguration(), p2.getConfiguration()));
			final Configuration c2 = mutation.apply(crossover.apply(p1.getConfiguration(), p2.getConfiguration()));
			final ObjectiveFunctionResult evaled1 = ObjectiveFunction.evaluateNowHelper(of,
					new ObjectiveFunctionArguments(p1.getConfiguration(), c1.getControlledMetrics()));
			final ObjectiveFunctionResult evaled2 = ObjectiveFunction.evaluateNowHelper(of,
					new ObjectiveFunctionArguments(p2.getConfiguration(), c2.getControlledMetrics()));
			final PopulationEntry e1 = new PopulationEntry(evaled1.getConfiguration(), evaled1.getObjectiveValues());
			final PopulationEntry e2 = new PopulationEntry(evaled2.getConfiguration(), evaled2.getObjectiveValues());
			newPopulation.add(e1);
			newPopulation.add(e2);
		});

		return newPopulation;
	}

	///////////////////////////////

	public static List<PopulationEntry> currentPopulation = null;
	
	/**
	 * Algorithm 104 from "Essentials of Metaheuristics", 2nd Edition.
	 */
	public static List<PopulationEntry> apply(List<Configuration> initialPopulation,
			ObjectiveFunction.LocalObjectiveFunction of, Comparator<List<Double>> compareMultiObjective,
			BiFunction<Configuration, Configuration, Configuration> crossover,
			Function<Configuration, Configuration> mutation, Predicate<List<List<PopulationEntry>>> isFinished,
			List<Double> objectiveValueRanges, int tournamentSize, SearchDirection dir, Random rng) {

		if (initialPopulation.size() < 2)
			throw new IllegalArgumentException();

		///////////////////////////
		currentPopulation = null;
		final int maxArchiveSize = initialPopulation.size();

		currentPopulation = initialPopulation.stream().map((Configuration c) -> {
			ObjectiveFunctionResult evaled = ObjectiveFunction.evaluateNowHelper(of,
					new ObjectiveFunctionArguments(c, c.getControlledMetrics()));
			return new PopulationEntry(evaled.getConfiguration(), evaled.getObjectiveValues());
		}).collect(Collectors.toList());

		final List<PopulationEntryRankAndSparsity> archive = new ArrayList<>();
		
		final List<Integer> bestFrontSize = new ArrayList<>();
		List<PopulationEntry> bestFront = new ArrayList<>();
		final List<List<PopulationEntry>> history = new ArrayList<>();
		history.add(currentPopulation);

		while (!isFinished.test(history)) {
			currentPopulation.addAll(archive.stream().map(p -> p.populationEntry).collect(Collectors.toList()));
			bestFront = paretoFront(currentPopulation, dir);
		
			bestFrontSize.add(bestFront.size());
			final List<List<PopulationEntry>> R = paretoFrontRanks(bestFront, dir);

			archive.clear();
			for (int i = 0; i < R.size(); ++i) {
				final List<PopulationEntryRankAndSparsity> withSparsity = multiobjectiveSparsityAssignment(R.get(i), i,
						objectiveValueRanges, dir);

				if (R.get(i).size() + archive.size() >= maxArchiveSize) {

					withSparsity.sort((p1, p2) -> Double.compare(p1.sparsity, p2.sparsity));
					final List<PopulationEntryRankAndSparsity> sparsest = withSparsity.subList(0,
							maxArchiveSize - archive.size());
					archive.addAll(sparsest);

					break;
				} else {
					archive.addAll(withSparsity);
				}
			}

			final BiFunction<List<PopulationEntryRankAndSparsity>, Random, PopulationEntry> select = (pop,
					r) -> nondominatedLexicographicTournamentSelectionWithSparsity(tournamentSize, pop, r);
			currentPopulation = breedSequential(archive, of, select, crossover, mutation, rng);
			history.add(currentPopulation);

		}


		return bestFront;
	}
}
