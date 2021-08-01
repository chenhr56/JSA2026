package aura;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import optimisation.ObjectiveFunction.LocalObjectiveFunction;
import optimisation.ObjectiveFunctionArguments;
import optimisation.ObjectiveFunctionResult;
import uk.ac.york.safire.metrics.Configuration;
import uk.ac.york.safire.metrics.SearchDirection;

/**
 * This class implements the original MOEA/D algorithm proposed by the following
 * article. The implementation follows exactly as the descriptions provided in
 * the paper.
 * 
 * Q. Zhang and H. Li, "MOEA/D: A Multiobjective Evolutionary Algorithm Based on
 * Decomposition," in IEEE Transactions on Evolutionary Computation, vol. 11,
 * no. 6, pp. 712-731, Dec. 2007.
 */

public class MOEADIslandMulti extends MOEADAbstractIslandMulti {

	public MOEADIslandMulti(List<Configuration> initialPopulation, LocalObjectiveFunction of,
			BiFunction<Configuration, Configuration, Configuration> crossover,
			Function<Configuration, Configuration> mutation, Predicate<List<List<PopulationEntry>>> isFinished,
			List<Double> objectiveValueRanges, int tournamentSize, SearchDirection dir, Random rng) {
		super(initialPopulation, of, crossover, mutation, isFinished, objectiveValueRanges, tournamentSize, dir, rng);
	}

	public void evolve() {
		/* for each individual in the current population */
		for (int i = 0; i < populationSize; i++) {
			/*
			 * Step 2.1 Reproduction. Get two random neighbors of individual i and let them
			 * crossover and mutate.
			 */
			int[] neighborIndexes = neighborhood.get(i);

			/* The randomly selection applied in the original MOEA/D algorithm. */
			PopulationEntry corssover1 = currentPopulation.get(neighborIndexes[rng.nextInt(neighborIndexes.length)]);
			PopulationEntry corssover2 = currentPopulation.get(neighborIndexes[rng.nextInt(neighborIndexes.length)]);

			List<Configuration> offSpring = new ArrayList<>();
			offSpring
					.add(mutation.apply(crossover.apply(corssover1.getConfiguration(), corssover2.getConfiguration())));
			offSpring
					.add(mutation.apply(crossover.apply(corssover1.getConfiguration(), corssover2.getConfiguration())));

			for (Configuration newIndividual : offSpring) {
				/*
				 * Step 2.2 Repair and Improvement. Apply the problem-specific heuristic to
				 * improve the new individual. For our application, this should check whether
				 * the solution is schedulable.
				 */

				/* Calculate the objective values of the new individual */
				ObjectiveFunctionResult evaled = null;
				try {
					evaled = of.evaluate(
							new ObjectiveFunctionArguments(newIndividual, newIndividual.getControlledMetrics()));
				} catch (Exception e) {
					e.printStackTrace();
				}

				PopulationEntry newIndividualEntry = new PopulationEntry(evaled.getConfiguration(),
						evaled.getObjectiveValues());

				/*
				 * Step 2.3 Update of Z. Update the Z vector if the new individual contains
				 * better values for any subproblems.
				 */
				updateIdealPoint(newIndividualEntry);
				updateWorstPoint(newIndividualEntry);

				/*
				 * Step 2.4 Update of Neighboring Solution. Iterates through all neighbors of
				 * individual i and replaces the neighbors with the new individual if they have
				 * a lower fitness.
				 */
				for (int j = 0; j < neighborhood.get(i).length; j++) {
					double neighborFitness = getFitness(currentPopulation.get(neighborIndexes[j]), neighborIndexes[j]);

					double newFitness = getFitness(newIndividualEntry, neighborIndexes[j]);
					if (newFitness <= neighborFitness) {
						currentPopulation.set(neighborIndexes[j], newIndividualEntry);
						fitness.set(neighborIndexes[j], newFitness);
					}
				}

				/*
				 * Step 2.5 Update of EP. (1) Remove from EP all the individuals dominated by
				 * the new individual; (2) Add new individual to EP if no individuals in EP
				 * dominates it.
				 */
				updateExternalPopulation(newIndividualEntry);
			}

		}
	}

}
