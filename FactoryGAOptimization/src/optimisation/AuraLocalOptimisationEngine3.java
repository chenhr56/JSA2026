package optimisation;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import aura.EAIsland;
import aura.MOEADIslandMulti;
import aura.NSGA2;
import aura.Operators;
import aura.PopulationEntry;
import metrics.Configuration;
import metrics.ConfigurationType;
import metrics.OptimisationArguments;
import metrics.SearchDirection;

///////////////////////////////////

public final class AuraLocalOptimisationEngine3 extends OptimisationEngine3.LocalOptimisationEngine {

	private static final int TOURNAMENT_SIZE = 3;
	// private static final SearchDirection SEARCH_DIRECTION =
	// SearchDirection.MAXIMIZING;
	private static final SearchDirection SEARCH_DIRECTION = SearchDirection.MINIMIZING;

	///////////////////////////////

	private final Random rng;
	private final int maxGenerations;
	private int engine = 0;

	///////////////////////////////

	public AuraLocalOptimisationEngine3(ObjectiveFunction objectiveFunction, int maxGenerations, Random rng,
			int engine) {

		super(objectiveFunction);

		// if( minEvaluations < 1 || minEvaluations > maxEvaluations )
		// throw new IllegalArgumentException();
		if (maxGenerations < 1)
			throw new IllegalArgumentException();
		this.rng = rng;
		this.maxGenerations = maxGenerations;
		this.engine = engine;
	}

	public AuraLocalOptimisationEngine3(ObjectiveFunction objectiveFunction, int maxGenerations, Random rng) {

		super(objectiveFunction);

		// if( minEvaluations < 1 || minEvaluations > maxEvaluations )
		// throw new IllegalArgumentException();
		if (maxGenerations < 1)
			throw new IllegalArgumentException();
		this.rng = rng;
		this.maxGenerations = maxGenerations;
		
		
	}

	///////////////////////////////

	private static List<Double> objectiveValueRanges(ConfigurationType ct) {
		return ct.getKeyObjectiveMetrics().stream().map(e -> e.valueType.max - e.valueType.min)
				.collect(Collectors.toList());
	}

	///////////////////////////////

	@Override
	public OptimisationIslandResult optimise(OptimisationArguments args) {

		// final int maxEval = maxGenerations * populationSize; //
		// OptmisationUtility.maxEvaluations(args.getUrgency(), args.getQuality(),
		// minEvaluations, maxEvaluations);

		// final int maxIterations = 10;

		// new
		// RandomSearchLocalOptimisationEngine(of,minEvaluations,maxEvaluations,rng);
		// JMetalLocalOptimisationEngine.nsga3(minEvaluations, maxEvaluations,of,rng);
		// final OptimisationEngine oe = new
		// ExhaustiveSearchLocalOptimisationEngine(of,rng);

		final Comparator<List<Double>> compareMultiObjective = Operators.compareMultiObjective(SEARCH_DIRECTION);
		final BiFunction<Configuration, Configuration, Configuration> crossover = Operators.onePointCrossover(rng);
		final BiFunction<List<PopulationEntry>, Random, PopulationEntry> select = Operators.proportionalSelectionFirstObjective;
		final Function<Configuration, Configuration> mutation = Operators.hyperMutation(0.1, rng); // //Operators.uniformMutation(rng);
		// //
		//

		// jeep.lang.Diag.println("maxGen: " + maxGenerations);
		
//		System.out.println("Geneartion in engine: " + maxGenerations);
		
		
		final Predicate<List<List<PopulationEntry>>> isFinished = Operators.maxIterTermination(maxGenerations);

		///////////////////////////

		/*
		 * apply( List< Configuration > initialPopulation,
		 * ObjectiveFunction.LocalObjectiveFunction of, Comparator< List< Double > >
		 * compareMultiObjective, BiFunction< List< PopulationEntry >, Random,
		 * PopulationEntry > select, BiFunction< Configuration, Configuration,
		 * Configuration > crossover, Function< Configuration, Configuration > mutation,
		 * Predicate< List< List< PopulationEntry > > > isFinished, Random rng ) {
		 */

		OptimisationIslandResult or = null;
		if (args.getConfigurations().get(0).getKeyObjectives().size() == 1) {
			// System.out.println("Single Objective Optimsation Start");
			or = EAIsland.apply(args.getConfigurations(),
					(ObjectiveFunction.LocalObjectiveFunction) getObjectiveFunction(), compareMultiObjective, select,
					crossover, mutation, isFinished, rng);
			// System.out.println();
		} else {
			// System.out.println("Multi-Objective Optimsation Start, OE number: " +
			// args.getEngine());
			if (engine == 0) {
				MOEADIslandMulti moeadIsland = new MOEADIslandMulti(args.getConfigurations(),
						(ObjectiveFunction.LocalObjectiveFunction) getObjectiveFunction(), crossover, mutation,
						isFinished, objectiveValueRanges(args.getConfigurations().get(0).getConfigurationType()),
						TOURNAMENT_SIZE, SEARCH_DIRECTION, rng);
				List<PopulationEntry> bestFront = moeadIsland.apply();
				or = new OptimisationIslandResult(bestFront, moeadIsland.currentPopulation,
						moeadIsland.getCapsForNormalization());
			} else {
				List<PopulationEntry> bestFront = NSGA2.apply(args.getConfigurations(),
						(ObjectiveFunction.LocalObjectiveFunction) getObjectiveFunction(), compareMultiObjective,
						crossover, mutation, isFinished,
						objectiveValueRanges(args.getConfigurations().get(0).getConfigurationType()), TOURNAMENT_SIZE,
						SEARCH_DIRECTION, rng);
				or = new OptimisationIslandResult(bestFront, NSGA2.currentPopulation, null);

			}
		}

		return or;
	}
}

// End ///////////////////////////////////////////////////////////////
