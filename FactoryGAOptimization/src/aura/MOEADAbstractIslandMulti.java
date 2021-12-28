package aura;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.primes.Primes;

import metrics.Configuration;
import metrics.SearchDirection;
import optimisation.ObjectiveFunction;
import optimisation.ObjectiveFunctionArguments;
import optimisation.ObjectiveFunctionResult;

/**
 * This class provide the abstraction towards various versions of MOEA\D
 * algorithms.
 */

public abstract class MOEADAbstractIslandMulti {

	/**
	 * The supported decomposition approaches.
	 */
	private static enum DecompositionType {
		WEIGHTEDSUM, TCHEBYCHEFF;
	}

	/********************* Configurable MOEA/D Parameters *********************/

	/** By default the Tchebycheff decomposition approach is applied. */
	static final DecompositionType decomposition = DecompositionType.TCHEBYCHEFF;

	/** The notion T in Zhang & Li paper, specifying the size of neighborhood */
	static final int neighborhoodSize = 30;

	/** The size of the external population i.e., the Pareto Front */
	static int SIZE_OF_EP = 100;

	/****************** Configurable MOEA/D Parameters Ends ********************/

	/************************ Fixed MOEA/D Parameters *************************/

	/* Weight vectors */
	List<double[]> lambda;

	/* The neighborhood of each individual */
	List<int[]> neighborhood;

	/*
	 * The fitness value of each individual calculated by a certain decomposition
	 * approach. This implementation supports the Weighted Sum and Tchebycheff
	 * decomposition approaches described in Zhang & Li paper.
	 */
	List<Double> fitness;

	/*
	 * The notation of Z in in Zhang & Li paper, stores the best results of each
	 * objective.
	 */
	List<Double> idealPoint;
	List<Double> worstPoint;

	/*
	 * The external population stores the non-dominated solutions (PF) found during
	 * the search. The size of this list is strictly limited to @param SIZE_OF_EP.
	 */
	List<PopulationEntry> externalPopulation;

	List<PopulationEntry> bestSolutions;

	/********************** Fixed MOEA/D Parameters Ends **********************/

	/************************** Generic GA Parameters *************************/
	int populationSize;
	int numberOfObjectives;
	int tournamentSize;

	SearchDirection dir;
	Random rng;
	List<Configuration> initialPopulation;
	ObjectiveFunction.LocalObjectiveFunction of;
	BiFunction<Configuration, Configuration, Configuration> crossover;
	Function<Configuration, Configuration> mutation;
	List<Double> objectiveValueRanges;
	Predicate<List<List<PopulationEntry>>> isFinished;

	public List<PopulationEntry> currentPopulation;

	/************************ Generic GA Parameters Ends ***********************/

	public MOEADAbstractIslandMulti(List<Configuration> initialPopulation, ObjectiveFunction.LocalObjectiveFunction of,
			BiFunction<Configuration, Configuration, Configuration> crossover,
			Function<Configuration, Configuration> mutation, Predicate<List<List<PopulationEntry>>> isFinished,
			List<Double> objectiveValueRanges, int tournamentSize, SearchDirection dir, Random rng) {
		this.initialPopulation = initialPopulation;
		this.of = of;
		this.crossover = crossover;
		this.mutation = mutation;
		this.isFinished = isFinished;
		this.objectiveValueRanges = objectiveValueRanges;

		this.populationSize = initialPopulation.size();
		this.numberOfObjectives = initialPopulation.get(0).getKeyObjectives().size();
		this.tournamentSize = tournamentSize;
		this.dir = dir;
		this.rng = rng;

		this.lambda = new ArrayList<double[]>(populationSize);
		this.neighborhood = new ArrayList<int[]>(populationSize);
		this.fitness = new ArrayList<Double>(populationSize);
		this.idealPoint = new ArrayList<Double>(numberOfObjectives);
		this.worstPoint = new ArrayList<Double>(numberOfObjectives);

		this.externalPopulation = new ArrayList<PopulationEntry>();
		this.bestSolutions = new ArrayList<PopulationEntry>();

		List<String> keys = initialPopulation.get(0).getKeyObjectives().keySet().stream()
				.filter(k -> k.contains("discrepancy")).collect(Collectors.toList());

		for (int i = 0; i < keys.size(); i++) {
			this.bestSolutions.add(null);
		}
	}

	/**
	 * Returns the first @param numOfPrimes prime numbers.
	 */
	private int[] generateFirstKPrimes(int numOfPrimes) {
		int[] primes = new int[numOfPrimes];
		primes[0] = 2;

		for (int i = 1; i < numOfPrimes; i++) {
			primes[i] = Primes.nextPrime(primes[i - 1]);
		}

		return primes;
	}

	/**
	 * Generates weights according to a uniform design of mixtures using the
	 * Hammersley low-discrepancy sequence generator. This algorithm is implemented
	 * by David Hadka from the MOEAFramework project at
	 * https://github.com/dhadka/MOEAFramework.
	 */
	void initializeUniformWeight() {
		if (numberOfObjectives == 1) {
			for (int n = 0; n < populationSize; n++) {
				lambda.add(new double[] { 1 });
			}
			return;
		}
		if (numberOfObjectives == 2) {
			for (int n = 0; n < populationSize; n++) {
				double a = 1.0 * n / (populationSize - 1);

				lambda.add(new double[] { a, 1 - a });
			}

			// for (int n = 0; n < populationSize; n++) {
			// double a = rng.nextGaussian() * 0.1 + UserRoutes.bc.getWeight()[0];
			//
			// assert (a <= 1.0);
			// lambda.add(new double[] { a, 1 - a });
			// }

			lambda.sort((c1, c2) -> Double.compare(c1[0], c2[0]));

			return;
		}

		/* generate uniform design using Hammersley method */
		List<double[]> designs = new ArrayList<double[]>();
		int[] primes = generateFirstKPrimes(numberOfObjectives - 2);

		for (int i = 0; i < populationSize; i++) {
			double[] design = new double[numberOfObjectives - 1];
			design[0] = (2.0 * (i + 1) - 1.0) / (2.0 * populationSize);

			for (int j = 1; j < numberOfObjectives - 1; j++) {
				double f = 1.0 / primes[j - 1];
				int d = i + 1;
				design[j] = 0.0;

				while (d > 0) {
					design[j] += f * (d % primes[j - 1]);
					d = d / primes[j - 1];
					f = f / primes[j - 1];
				}
			}

			designs.add(design);
		}

		/* transform designs into weight vectors (sum to 1) */
		for (double[] design : designs) {
			double[] weight = new double[numberOfObjectives];

			for (int i = 1; i <= numberOfObjectives; i++) {
				if (i == numberOfObjectives) {
					weight[i - 1] = 1.0;
				} else {
					weight[i - 1] = 1.0 - Math.pow(design[i - 1], 1.0 / (numberOfObjectives - i));
				}

				for (int j = 1; j <= i - 1; j++) {
					weight[i - 1] *= Math.pow(design[j - 1], 1.0 / (numberOfObjectives - j));
				}
			}
			lambda.add(weight);
		}

	}

	/**
	 * Initialize neighborhoods of each individual.
	 */
	void initializeNeighborhood() {

		for (int i = 0; i < populationSize; i++) {
			double[] euclideanDistance = new double[populationSize];
			int[] potentialNeighbors = new int[populationSize];

			for (int j = 0; j < populationSize; j++) {

				/****************************************************************
				 * calculate the Euclidean Distances based on weight vectors. see
				 * https://en.wikipedia.org/wiki/Euclidean_distance for details.
				 ***************************************************************/
				int dimension = lambda.get(i).length;
				double sum = 0;
				for (int k = 0; k < dimension; k++) {
					sum += (lambda.get(i)[k] - lambda.get(j)[k]) * (lambda.get(i)[k] - lambda.get(j)[k]);
				}

				/* The Euclidean Distance between individual i and j. */
				euclideanDistance[j] = Math.sqrt(sum);
				potentialNeighbors[j] = j;
			}

			/*
			 * Now we apply the notion of T (the neighborhood size) and get set the T closet
			 * neighbors of individual i.
			 */
			for (int n = 0; n < neighborhoodSize; n++) {
				for (int m = n + 1; m < populationSize; m++) {
					if (euclideanDistance[n] > euclideanDistance[m]) {
						double neighborED = euclideanDistance[n];
						euclideanDistance[n] = euclideanDistance[m];
						euclideanDistance[m] = neighborED;

						int neighborID = potentialNeighbors[n];
						potentialNeighbors[n] = potentialNeighbors[m];
						potentialNeighbors[m] = neighborID;
					}
				}
			}

			int actualNeighborhoodSize = Math.min(neighborhoodSize, populationSize);
			int[] neighbors = new int[actualNeighborhoodSize];
			System.arraycopy(potentialNeighbors, 0, neighbors, 0, actualNeighborhoodSize);
			neighborhood.add(neighbors);
		}
	}

	/**
	 * By default the ideal point is initialized to the positive infinity.
	 */
	void initializeIdealPoint() {
		for (int i = 0; i < numberOfObjectives; i++) {
			idealPoint.add(Double.POSITIVE_INFINITY);
		}
	}

	void initializeWorstPoint() {
		for (int i = 0; i < numberOfObjectives; i++) {
			worstPoint.add(Double.NEGATIVE_INFINITY);
		}
	}

	/**
	 * Update the ideal point to the best observed values so far.
	 */
	void updateIdealPoint(List<PopulationEntry> population) {
		for (PopulationEntry entry : population)
			updateIdealPoint(entry);
	}

	void updateIdealPoint(PopulationEntry individual) {
		for (int i = 0; i < individual.getObjectives().size(); i++) {
			double ideal = idealPoint.get(i);
			double objective = individual.getObjectives().get(i);
			if (objective < ideal) {
				idealPoint.set(i, objective);
				if (bestSolutions.size() > 0 && i >= individual.getObjectives().size() - bestSolutions.size()) {
					bestSolutions.set(i - (individual.getObjectives().size() - bestSolutions.size()), individual);
				}

			}
			// idealPoint.set(i, Math.min(idealPoint.get(i),
			// individual.getObjectives().get(i)));
		}
	}

	void updateWorstPoint(List<PopulationEntry> population) {
		for (PopulationEntry entry : population)
			updateWorstPoint(entry);
	}

	void updateWorstPoint(PopulationEntry individual) {
		for (int i = 0; i < individual.getObjectives().size(); i++) {
			double worst = worstPoint.get(i);
			double objective = individual.getObjectives().get(i);
			if (objective > worst) {
				worstPoint.set(i, objective);
			}
		}
	}

	/**
	 * Here we compute the fitness value for each individual in the given population
	 * via the defined decomposition approach.
	 */
	double getFitness(PopulationEntry individual, int index) {
		double Fitness = 0;

		switch (decomposition) {
		case WEIGHTEDSUM:
			Fitness = 0;
			for (int i = 0; i < individual.getObjectives().size(); i++) {
				Fitness += individual.getObjectives().get(i) * lambda.get(index)[i];
			}
			break;

		case TCHEBYCHEFF:
			Fitness = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < individual.getObjectives().size(); i++) {
				Fitness = Math.max(Fitness, Math.max(lambda.get(index)[i], 0.0001)
						* Math.abs(individual.getObjectives().get(i) - idealPoint.get(i)));
			}

			break;
		default:
			break;
		}

		return Fitness;
	}

	void getFitnessAll(List<PopulationEntry> population) {
		for (int i = 0; i < populationSize; i++)
			fitness.add(getFitness(population.get(i), i));
	}

	/**
	 * Return true if @param individual1 strictly dominates @param individual2 on
	 * each objective. Return false otherwise.
	 */
	boolean dominate(PopulationEntry individual1, PopulationEntry individual2) {
		boolean isDominate = false;

		if (individual1.getObjectives().size() != individual2.getObjectives().size()) {
			System.out.println("error");
		}

		for (int i = 0; i < individual1.getObjectives().size(); i++) {
			if (individual1.getObjectives().get(i) > individual2.getObjectives().get(i))
				return false;
			if (individual1.getObjectives().get(i) <= individual2.getObjectives().get(i))
				isDominate = true;
		}

		return isDominate;
	}

	/**
	 * Update the external population list. This list will be returned as the final
	 * optimization result. A new @param candidate can join into the list if and
	 * only if no members in the list can dominate the @param candidate. The members
	 * that are dominated by the @param candidate will be removed from the list.
	 */
	void updateExternalPopulation(PopulationEntry candidate) {

		if (externalPopulation.size() == 0)
			externalPopulation.add(candidate);
		else {
			boolean eligibleToJoin = true;

			/*
			 * remove the members that are dominated by the candidate and check the
			 * eligibility of the candidate.
			 */
			for (int i = 0; i < externalPopulation.size(); i++) {
				PopulationEntry member = externalPopulation.get(i);
				if (dominate(candidate, member)) {
					/* the candidate dominates a member */
					externalPopulation.remove(i);
					i--;
				} else if (dominate(member, candidate))
					/* the candidate is dominated by a member */
					eligibleToJoin = false;
			}

			if (eligibleToJoin) {
				if (externalPopulation.size() < SIZE_OF_EP)
					externalPopulation.add(candidate);
				else {
					boolean isVIP = false;

					for (int i = 0; i < idealPoint.size(); i++) {
						if (idealPoint.get(i) >= candidate.getObjectives().get(i)) {
							isVIP = true;
							break;
						}
					}
					if (isVIP) {
						externalPopulation.add(candidate);
						SIZE_OF_EP += 1;
					}
				}
			}
		}

	}

	/**
	 * Step 1: Initialization: initialize everything, includes EP, weights,
	 * neighborhood and ideal point based on the given @param initialPopulation
	 * and @param of.
	 */
	List<PopulationEntry> initialize() {

		/* Step 1.1 initialize EP. Performed in apply() function. */

		/*
		 * Step 1.2 Initialize weights, compute the Euclidean distance and generate
		 * neighborhood.
		 */
		initializeUniformWeight();
		initializeNeighborhood();

		/*
		 * Step 1.3 generate initial population. NOTE: this step is finished beforehand,
		 * the initial population is passed via @param initialPopulation.
		 */

		/* Step 1.4 initialize Z vector, the ideal point. */
		initializeIdealPoint();
		initializeWorstPoint();
		/*
		 * Calculate the objective values of initial population based on objective
		 * functions @param of
		 */

		final Stream<PopulationEntry> populationWithObjectiveValues = initialPopulation.stream()
				.map((Configuration c) -> {
					ObjectiveFunctionResult evaled = null;
					try {
						evaled = of.evaluate(new ObjectiveFunctionArguments(c, c.getControlledMetrics()));
					} catch (Exception e) {
						e.printStackTrace();
					}

					return new PopulationEntry(evaled.getConfiguration(), evaled.getObjectiveValues());
				});

		List<PopulationEntry> populationList = populationWithObjectiveValues.collect(Collectors.toList());

		/* Update FV, Z and EP values based on the initial population. */
		updateIdealPoint(populationList);
		updateWorstPoint(populationList);
		getFitnessAll(populationList);
		for (int i = 0; i < populationList.size(); i++) {
			updateExternalPopulation(populationList.get(i));
		}

		return populationList;
	}

	public abstract void evolve();

	/**
	 * The MOEA/D algorithm.
	 */
	public List<PopulationEntry> apply() {

		if (initialPopulation.size() < 2)
			throw new IllegalArgumentException();

		/* Step 1 Initialization */
		currentPopulation = initialize();

		assert (fitness.size() > 0 && neighborhood.size() > 0 && idealPoint.size() > 0 && lambda.size() > 0);

		/* A recorder that stores all the evolution process. */
		final List<List<PopulationEntry>> history = new ArrayList<>();
		history.add(currentPopulation);

		/* Step 2 Update */
		while (!isFinished.test(history)) {

			// if (current % 10 == 0)

			evolve();

//			int current = history.size();
//			System.out.println("now " + current + " generations");

//			printResult();

			history.add(currentPopulation);
		}

		return externalPopulation;
	}

//	private void printResult() {
//
//		externalPopulation.sort((c1, c2) -> c1.getObjectives().get(0).compareTo(c2.getObjectives().get(0)));
//		for (PopulationEntry entry : externalPopulation) {
//			String[] results = new String[entry.getObjectives().size()];
//			String[] resultsArray = entry.getObjectives().stream().map(d -> d + "").collect(Collectors.toList())
//					.toArray(results);
//			System.out.println(String.join(" ", resultsArray));
//		}
//	}

	public double[][] getCapsForNormalization() {
		double[][] cap_values = new double[idealPoint.size()][2];

		// for (int i = 0; i < entry.get(0).getObjectives().size(); i++) {
		// final int index = i;
		// List<Double> valuesForOne = entry.stream().map(c ->
		// c.getObjectives().get(index))
		// .collect(Collectors.toList());
		// double maxForOne = Collections.max(valuesForOne);
		// double minForOne = Collections.min(valuesForOne);
		// cap_values[i][0] = maxForOne;
		// cap_values[i][1] = minForOne;
		// }

		assert (cap_values.length == worstPoint.size());
		assert (cap_values.length == idealPoint.size());

		for (int i = 0; i < cap_values.length; i++) {
			cap_values[i][0] = worstPoint.get(i);
			cap_values[i][1] = idealPoint.get(i);
		}

		return cap_values;
	}

}
