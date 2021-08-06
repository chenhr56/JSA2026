package restCloud;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import aura.PopulationEntry;
import factoryModel.ONA.ONAFactoryModel;
import indicator.DCICaculator;
import mitm.atb.BusinessCase;
import mitm.atb.UoYEarlyPrototypeDemo;
import optimisation.AuraLocalOptimisationEngine3;
import optimisation.ONAFitnessFunction;
import optimisation.ObjectiveFunction;
import optimisation.OptimisationEngine3;
import optimisation.OptimisationIslandResult;
import restCloud.LinkageFactory.Node;
import restCloud.LinkageFactory.Tree;
import uk.ac.york.safire.metrics.Configuration;
import uk.ac.york.safire.metrics.ConfigurationType;
import uk.ac.york.safire.metrics.OptimisationArguments;
import uk.ac.york.safire.metrics.Utility;
import uk.ac.york.safire.metrics.Value;
import uk.ac.york.safire.metrics.Value.Nominal;
import uk.ac.york.safire.metrics.ValueType;

public class ManagerPP {

	BusinessCase bc;
	int ONAfactoryScale;
	int seeds;
	int notImprovedInARowLimit;
	int numberOfReplacement;
	int RemoveMethod;
	int addMethod;

	Random random;
	ConfigurationType ct;
	
	int populationSize = 50; // 50
	int generations = 20; // 20
	int NoOfStages = 40; // 40
	int noOfIslands = 1;
	public static int SIZE_OF_EP = 100;
	double urgency = 0.5;
	double quality = 0.5;
	int percentAvailability = 100;

	int numberOfPush = 0;
	int numberOfPull = 0;

	ObjectiveFunction.LocalObjectiveFunction of;

	public ManagerPP(BusinessCase bc, int ONAfactoryScale, int seeds, int notImprovedInARowLimit,
			int numberOfReplacement, int RemoveMethod, int addMethod, int island) {
		this.bc = bc;
		this.ONAfactoryScale = ONAfactoryScale;
		this.seeds = seeds;
		this.notImprovedInARowLimit = notImprovedInARowLimit;
		this.numberOfReplacement = numberOfReplacement;
		this.RemoveMethod = RemoveMethod;
		this.addMethod = addMethod;

		random = new Random(seeds);

		of = (ObjectiveFunction.LocalObjectiveFunction) bc.getObjectiveFunction();

		if (RemoveMethod == -2)
			this.populationSize = populationSize * island;
		
	}

	// private PopulationEntry removeEntryWorstIGD(OptimisationIslandResult or) {
	//
	// if (or.getFinalPopulation().size() < 2)
	// throw new InvalidParameterException();
	//
	// List<List<List<Double>>> fronts = or.getFinalPopulation().stream().map(p -> {
	// List<List<Double>> oneFront = new ArrayList<>();
	// oneFront.add(p.getObjectives());
	// return oneFront;
	// }).collect(Collectors.toList());
	//
	// double[] dcis = new IGDCalculator().applyIGD(fronts);
	// List<List<Double>> d1rIndex = new ArrayList<>();
	// for (int i = 0; i < dcis.length; i++) {
	// List<Double> d1rInx = new ArrayList<>();
	// d1rInx.add(dcis[i]);
	// d1rInx.add(i + 0.0);
	// d1rIndex.add(d1rInx);
	// }
	// d1rIndex.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));
	//
	// int entryWithHighestFirstObjectiveIndex = d1rIndex.get(0).get(1).intValue();
	// assert (entryWithHighestFirstObjectiveIndex != -1);
	//
	// PopulationEntry entryWithHighestFirstObjective = or.getFinalPopulation()
	// .get(entryWithHighestFirstObjectiveIndex);
	// or.getFinalPopulation().remove(entryWithHighestFirstObjectiveIndex);
	//
	// return entryWithHighestFirstObjective;
	// }

	private PopulationEntry removeEntryWorst(OptimisationIslandResult or) {

		if (or.getFinalPopulation().size() < 2)
			throw new InvalidParameterException();

		List<List<List<Double>>> fronts = or.getFinalPopulation().stream().map(p -> {
			List<List<Double>> oneFront = new ArrayList<>();
			oneFront.add(p.getObjectives());
			return oneFront;
		}).collect(Collectors.toList());

		/*
		 * Evaluate using QI
		 */
		// double[] dcis = new GDCalculator().applyD1R(fronts);

		/*
		 * Evaluate using CD calculation
		 */
		double[] dcis = new double[fronts.size()];

		for (int i = 0; i < fronts.size(); i++) {
			double distance = fronts.get(i).get(0).stream().mapToDouble(f -> f.doubleValue()).sum()
					/ (double) fronts.get(i).get(0).size();
			dcis[i] = distance;
		}

		List<List<Double>> d1rIndex = new ArrayList<>();
		for (int i = 0; i < dcis.length; i++) {
			List<Double> d1rInx = new ArrayList<>();
			d1rInx.add(dcis[i]);
			d1rInx.add(i + 0.0);
			d1rIndex.add(d1rInx);
		}
		d1rIndex.sort((c1, c2) -> -Double.compare(c1.get(0), c2.get(0)));

		int entryWithHighestFirstObjectiveIndex = d1rIndex.get(0).get(1).intValue();
		assert (entryWithHighestFirstObjectiveIndex != -1);

		PopulationEntry entryWithHighestFirstObjective = or.getFinalPopulation()
				.get(entryWithHighestFirstObjectiveIndex);
		or.getFinalPopulation().remove(entryWithHighestFirstObjectiveIndex);

		return entryWithHighestFirstObjective;
	}

	// private PopulationEntry removeEntryWorstDiversity(OptimisationIslandResult
	// or) {
	// if (or.getFinalPopulation().size() < 2)
	// throw new InvalidParameterException();
	//
	// PopulationEntry entryWithHighestFirstObjective = null;
	//
	//
	//
	//
	//
	// return entryWithHighestFirstObjective;
	// }

	private PopulationEntry removeEntryRandom(OptimisationIslandResult or, Random rng) {

		if (or.getFinalPopulation().size() < 2)
			throw new InvalidParameterException();

		PopulationEntry remove = null;

		int entryWithHighestFirstObjectiveIndex = -1;
		while (entryWithHighestFirstObjectiveIndex == -1) {
			entryWithHighestFirstObjectiveIndex = rng.nextInt(or.getFinalPopulation().size());
		}
		remove = or.getFinalPopulation().get(entryWithHighestFirstObjectiveIndex);
		or.getFinalPopulation().remove(entryWithHighestFirstObjectiveIndex);

		return remove;
	}

	private List<PopulationEntry> getEntryRandom(List<PopulationEntry> globalPF, int number) {

		List<PopulationEntry> newMembers = new ArrayList<>();

		if (globalPF.size() > number) {
			for (int i = 0; i < number; i++) {
				PopulationEntry entry = globalPF.get(random.nextInt(globalPF.size()));

				if (!newMembers.contains(entry))
					newMembers.add(entry);
				else
					i--;
			}
		} else
			newMembers = globalPF;

		return newMembers;
	}

	// private List<PopulationEntry> getandRemoveEntryRandom(List<PopulationEntry>
	// globalPF, int number) {
	//
	// List<PopulationEntry> newMembers = new ArrayList<>();
	//
	// if (globalPF.size() > number) {
	// for (int i = 0; i < number; i++) {
	// int index = random.nextInt(globalPF.size());
	// PopulationEntry entry = globalPF.get(index);
	//
	// if (!newMembers.contains(entry)) {
	// newMembers.add(entry);
	// globalPF.remove(index);
	// }
	//
	// else
	// i--;
	// }
	// } else {
	//
	// for (int i = 0; i < globalPF.size(); i++) {
	// newMembers.add(globalPF.get(i));
	// }
	//
	// globalPF.clear();
	//
	// }
	//
	// return newMembers;
	// }

	private List<PopulationEntry> getEntryBaseline(int number) {

		List<PopulationEntry> newMembers = new ArrayList<>();
		for (int i = 0; i < number; i++) {
			Configuration config = Utility.randomConfiguration(ct, random);
			newMembers.add(new PopulationEntry(config, new ArrayList<>()));
		}
		return newMembers;
	}

	// private PopulationEntry getEntryBestMakeSpan(OptimisationIslandResult result,
	// List<PopulationEntry> globalPF) {
	// globalPF.sort((c1, c2) -> Double.compare(c1.getObjectives().get(0),
	// c2.getObjectives().get(0)));
	// return globalPF.get(0);
	// }

	private List<PopulationEntry> getEntryBestQI(List<PopulationEntry> globalPF, int number) {

		List<List<List<Double>>> fronts = new ArrayList<>();

		for (int i = 0; i < globalPF.size(); i++) {
			List<List<Double>> pf = new ArrayList<>();
			pf.add(globalPF.get(i).getObjectives());
			fronts.add(pf);
		}

//				globalPF.stream().map(p -> {
//			List<List<Double>> oneFront = new ArrayList<>();
//			oneFront.add(p.getObjectives());
//			return oneFront;
//		}).collect(Collectors.toList());

		int sizeTemp = globalPF.size();

		assert (globalPF.size() == fronts.size());

		// double[] dcis = control ? new IGDCalculator().applyIGD(fronts) : new
		// GDCalculator().applyD1R(fronts);

		double[] dcis = new double[fronts.size()];

		for (int i = 0; i < fronts.size(); i++) {
			double distance = fronts.get(i).get(0).stream().mapToDouble(f -> f.doubleValue()).sum()
					/ (double) fronts.get(i).get(0).size();
			dcis[i] = distance;
		}

		List<List<Double>> dciIndex = new ArrayList<>();
		for (int i = 0; i < dcis.length; i++) {
			List<Double> d1rInx = new ArrayList<>();
			d1rInx.add(dcis[i]);
			d1rInx.add(i + 0.0);
			dciIndex.add(d1rInx);
		}

		dciIndex.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));

		if (dciIndex.size() != globalPF.size()) {
			System.err.println("Fatal Error: dci size: " + dciIndex.size() + " fronts size now : " + globalPF.size()
					+ " front size before: " + sizeTemp);
		}

		List<PopulationEntry> sortedGlobal = new ArrayList<>();

		for (int i = 0; i < dciIndex.size(); i++) {
			try {
				sortedGlobal.add(globalPF.get(dciIndex.get(i).get(1).intValue()));
			} catch (IndexOutOfBoundsException e) {
				System.out.println("catch!");
			}
		}

		List<PopulationEntry> newMembers = new ArrayList<>();

		if (sortedGlobal.size() > number) {
			for (int i = 0; i < number; i++) {
				newMembers.add(sortedGlobal.get(i));
			}
		} else {
			newMembers = sortedGlobal;
		}

		return newMembers;
	}

	// private double getSumHammingDistance(Configuration point, List<Configuration>
	// sets) {
	// int diff = 0;
	//
	// final List<Entry<String, Value>> control1 =
	// point.getControlledMetrics().entrySet().stream()
	// .collect(Collectors.toList());
	// control1.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));
	//
	// for (int k = 0; k < sets.size(); k++) {
	// final List<Entry<String, Value>> control2 =
	// sets.get(k).getControlledMetrics().entrySet().stream()
	// .collect(Collectors.toList());
	// control2.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));
	//
	// assert (control1.size() == control2.size());
	//
	// for (int i = 0; i < control1.size(); i++) {
	// String controlUnit1 = control1.get(i).getValue().toString();
	// String controlUnit2 = control2.get(i).getValue().toString();
	//
	// if (!controlUnit1.equals(controlUnit2))
	// diff++;
	// }
	//
	// }
	//
	// return diff;
	// }

	private double getCrowdingDistance(List<Double> ref, List<List<Double>> set) {

		final List<MutablePair<List<Double>, Double>> Fdash = set.stream().map(pe -> MutablePair.of(pe, 0.0))
				.collect(Collectors.toList());

		for (int i = 0; i < ref.size(); ++i) {
			final int iFinal = i;
			Fdash.sort((p1, p2) -> Double.compare(p1.getLeft().get(iFinal), p2.getLeft().get(iFinal)));

			Fdash.get(0).setRight(Double.POSITIVE_INFINITY);
			Fdash.get(Fdash.size() - 1).setRight(Double.POSITIVE_INFINITY);

			for (int j = 1; j < Fdash.size() - 1; ++j) {
				final double inc = Fdash.get(j + 1).getLeft().get(i) - Fdash.get(j - 1).getLeft().get(i);
				Fdash.get(j).setRight(Fdash.get(j).getRight() + inc);
			}
		}

		double distance = 0;
		for (int i = 0; i < Fdash.size(); i++) {
			if (Fdash.get(i).left.equals(ref)) {
				distance = Fdash.get(i).getRight();
			}

		}

		return distance;
	}

	private List<PopulationEntry> getEntryDriverseByCrowding(OptimisationIslandResult localPoP,
			List<PopulationEntry> globalPoP, int number) {

		List<List<Double>> distance = new ArrayList<>();

		for (int i = 0; i < globalPoP.size(); i++) {
			List<List<Double>> set = localPoP.getFront().stream().map(ce -> ce.getObjectives())
					.collect(Collectors.toList());
			set.add(globalPoP.get(i).getObjectives());
			List<Double> l = new ArrayList<>();
			l.add(getCrowdingDistance(globalPoP.get(i).getObjectives(), set));
			l.add(i + 0.0);
			distance.add(l);
		}

		distance.sort((c2, c1) -> Double.compare(c1.get(0), c2.get(0)));

		List<PopulationEntry> newMems = new ArrayList<>();

		for (int i = 0; i < number; i++) {
			newMems.add(globalPoP.get(distance.get(i).get(1).intValue()));
		}

		return newMems;
	}

	private List<PopulationEntry> getNewMemeber(OptimisationIslandResult localPoP, List<PopulationEntry> remotePoP,
			int addMethod, int number) {
		List<PopulationEntry> newMems = new ArrayList<>();

		switch (addMethod) {
		case 0:
			newMems = getEntryBaseline(number);
			break;
		case 1:
			newMems = getEntryRandom(remotePoP, number);
			break;
		case 2:
			// newMems = getEntryBestMakeSpan(result, globalPF);
			newMems = getEntryBestQI(remotePoP, number);
			break;
		case 3:
			// newMems = getEntryDiverseByHamming(result, globalPF, number);
			newMems = getEntryDriverseByCrowding(localPoP, remotePoP, number);
			break;

		default:
			break;
		}

		return newMems;
	}

	private double getFitness(LinkageFactory factory, Tree linkageTree, Random rng,
			List<MutablePair<String, Value>> controlsList, List<MutablePair<String, Value>> srcControlsList,
			Configuration config, double weight1, double weight2) {

		controlsList.sort((p1, p2) -> factory.compareGene(p1.getKey(), p2.getKey()));

		Node n = linkageTree.all.get(rng.nextInt(linkageTree.all.size() - 1));

		for (int j = 0; j < n.ids.size(); j++) {
			int index = n.ids.get(j);

			if (!controlsList.get(index).getKey().equals(srcControlsList.get(index).getKey())) {
				System.err.println("Key does not match");
			}

			Nominal srcV = (Nominal) srcControlsList.get(index).getValue();
			String vNew = srcV.value;

			MutablePair<String, Value> desPair = controlsList.get(index);
			Nominal desV = (Nominal) desPair.getValue();

			Value newAllocValue = new Nominal(vNew, desV.typ);

			desPair.setValue(newAllocValue);
			controlsList.set(index, desPair);
		}

		Map<String, Value> newControlConfig = controlsList.stream()
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

		Configuration bestConfig = new Configuration(config.getConfigurationType(), newControlConfig,
				config.getKeyObjectives());

		List<Double> objectives = ONAFitnessFunction.getFitness(bestConfig);

		// ObjectiveFunctionResult evaled = null;
		// try {
		// evaled = of.evaluate(new ObjectiveFunctionArguments(bestConfig,
		// bestConfig.getControlledMetrics())).call();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		double fitness = objectives.get(0) * weight1 + objectives.get(1) * weight2;

		return fitness;
	}

	// private List<PopulationEntry> getEntryBest(List<PopulationEntry> entry, int
	// number){
	// List<PopulationEntry> best = new ArrayList<>();
	//
	//
	//
	// return best;
	// }

	private void linkageMigration(OptimisationIslandResult localPF, List<PopulationEntry> globalPF, int number,
			Random rng, int islandID, LinkageFactory factory) {

		List<PopulationEntry> source = getEntryDriverseByCrowding(localPF, globalPF, 1);
		Configuration srcConfig = source.get(0).getConfiguration();
		Map<String, Value> srcControls = srcConfig.getControlledMetrics();
		List<MutablePair<String, Value>> srcControlsList = srcControls.entrySet().stream()
				.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList());
		srcControlsList.sort((p1, p2) -> factory.compareGene(p1.getKey(), p2.getKey()));

		// List<PopulationEntry> dess =
		// getandRemoveEntryRandom(result.getFinalPopulation(), 5);

		List<PopulationEntry> dess = new ArrayList<>();

		for (int i = 0; i < number + 1; i++) {
			dess.add(removeEntryWorst(localPF));
		}

		Tree linkageTree = factory.getLinkage(source.get(0));

		for (int i = 0; i < number; i++) {
			PopulationEntry des = dess.get(i);

			double weight1 = random.nextDouble();
			double weight2 = 1 - weight1;

			double originalFitness = des.getObjectives().get(0) * weight1 + des.getObjectives().get(1) * weight2;

			Configuration config = des.getConfiguration();
			Map<String, Value> controls = config.getControlledMetrics();
			List<MutablePair<String, Value>> controlsList = controls.entrySet().stream()
					.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList());
			controlsList.sort((p1, p2) -> factory.compareGene(p1.getKey(), p2.getKey()));

			List<MutablePair<String, Value>> copyConfig = deepCopyConfig(controlsList, factory);
			List<MutablePair<String, Value>> lastConfig = deepCopyConfig(controlsList, factory);

			double fitness = getFitness(factory, linkageTree, rng, copyConfig, srcControlsList, config, weight1,
					weight2);

			int count = 0;
			int maxCount = linkageTree.all.size();
			while (fitness <= originalFitness) {

				if (fitness == originalFitness) {
					lastConfig = deepCopyConfig(copyConfig, factory);
					if (maxCount < count)
						break;
					fitness = getFitness(factory, linkageTree, rng, copyConfig, srcControlsList, config, weight1,
							weight2);
				}

				if (fitness < originalFitness) {
					copyConfig = deepCopyConfig(lastConfig, factory);
					if (maxCount < count)
						break;
					fitness = getFitness(factory, linkageTree, rng, copyConfig, srcControlsList, config, weight1,
							weight2);
				}
				count++;
			}

			Map<String, Value> newConfigList = copyConfig.stream()
					.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
			Configuration newconfig = new Configuration(config.getConfigurationType(), newConfigList,
					config.getKeyObjectives());
			PopulationEntry newEntry = new PopulationEntry(newconfig, des.getObjectives());
			localPF.getFinalPopulation().add(newEntry);

		}

		// TODO: This bit utilizes the greedy optimization results
//		if (factory.best.size() > 0) {
//			List<PopulationEntry> entry = factory.getBest();
//
//			int index = 0;
//
//			while (localPF.getFinalPopulation().contains(entry.get(index)) && index < entry.size()) {
//				index++;
//			}
//
//			if (index < entry.size())
//				localPF.getFinalPopulation().add(entry.get(index));
//			else
//				localPF.getFinalPopulation().add(dess.get(dess.size() - 1));
//		}

	}

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
	void updateExternalPopulation(List<PopulationEntry> externalPopulation, PopulationEntry candidate) {

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
			}
		}

	}

	private List<MutablePair<String, Value>> deepCopyConfig(List<MutablePair<String, Value>> controlsList,
			LinkageFactory factory) {
		controlsList.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

		List<MutablePair<String, Value>> newControls = new ArrayList<>();
		for (int i = 0; i < controlsList.size(); i++) {
			MutablePair<String, Value> p = controlsList.get(i);

			if (p.getKey().contains("allocation")) {
				String key = p.getKey();

				Nominal allocValue = (Nominal) p.getValue();
				ValueType.Nominal allocType = (ValueType.Nominal) allocValue.getType();
				String value = allocValue.value;

				Value newAllocValue = new Nominal(value, allocType);

				MutablePair<String, Value> newp = new MutablePair<String, Value>(key, newAllocValue);
				newControls.add(newp);
			} else {
				newControls.add(p);
			}
		}

		newControls.sort((c1, c2) -> factory.compareGene(c1.getKey(), c2.getKey()));
		return newControls;
	}

	private void replace(OptimisationIslandResult localPoP, List<PopulationEntry> remotePoP, int removeMethod,
			int addMethod, int number, Random rng, int islandID, boolean useCloud, LinkageFactory factory) {

		if (removeMethod == 5 && addMethod == 5 && !useCloud) {
			linkageMigration(localPoP, remotePoP, number, rng, islandID, factory);
			return;
		}

		for (int i = 0; i < number; i++) {
			switch (removeMethod) {
			case 0:
				removeEntryRandom(localPoP, rng);
				break;
			case 1:
				removeEntryWorst(localPoP);
				break;

			default:
				break;
			}
		}

		List<PopulationEntry> newMemebrs = getNewMemeber(localPoP, remotePoP, addMethod, number);
		if (newMemebrs.size() > 0)
			localPoP.getFinalPopulation().addAll(newMemebrs);

	}

	private List<Configuration> extractFinalPopulationConfigurations(OptimisationIslandResult or,
			Configuration configurationTemplate) {

		List<Configuration> configurations = new ArrayList<Configuration>();
		for (PopulationEntry populationEntry : or.getFinalPopulation()) {
			Configuration config = new Configuration(configurationTemplate.getConfigurationType(),
					populationEntry.getConfiguration().getControlledMetrics(),
					configurationTemplate.getKeyObjectives());
			configurations.add(config);

		}
		return configurations;
	}

	private OptimisationIslandResult execute(OptimisationArguments optimisationArguments) {

		final ObjectiveFunction.LocalObjectiveFunction of = bc.getObjectiveFunction();
		final Random rng = new Random(seeds);
		final int iterations = generations;

		final OptimisationEngine3 oe = new AuraLocalOptimisationEngine3(of, iterations, rng);

		final long startTime = System.currentTimeMillis();
		final OptimisationIslandResult or = oe.optimise(optimisationArguments);
		final long endTime = System.currentTimeMillis();

		or.optimisationTime = (double) (endTime - startTime) / (double) 1000.0;

		return or;
	}

	private OptimisationArguments createIsland(ConfigurationType ct, double urgency, double quality, int populationSize,
			Random random) {

		List<Configuration> configurations = new ArrayList<Configuration>();
		for (int j = 0; j < populationSize; j++) {
			Configuration config = Utility.randomConfiguration(ct, random);
			configurations.add(config);
		}

		return new OptimisationArguments(configurations, urgency, quality);
	}

	public ResultBundle start(int id, List<ParetoFrontCapsule> fpCaps, List<List<Integer>> actions, boolean useCloud) {
		long t1 = System.currentTimeMillis();
		List<List<Double>> objectives = startIsland(id, fpCaps, actions, useCloud);
		long t2 = System.currentTimeMillis();

		long time = t2 - t1;

		return new ResultBundle(objectives, numberOfPush, numberOfPull, time);

	}

	public List<List<Double>> startIsland(int id, List<ParetoFrontCapsule> fpCaps, List<List<Integer>> actions,
			boolean useCloud) {
		int islandID = id;
		String cloudID = System.getenv("ISLAND_ID");
		ONAFactoryModel.scale = ONAfactoryScale;

		ct = bc.getConfigurationType(percentAvailability, random);
		Configuration configurationTemplate = Utility.randomConfiguration(ct, random);
		List<OptimisationArguments> optimisationArguments = new ArrayList<OptimisationArguments>();
		UoYEarlyPrototypeDemo.observableMetrics = Utility
				.makeObservableMetrics(UoYEarlyPrototypeDemo.observableMetricTypes, random);

		ConnectManager connectM = new ConnectManager(useCloud, random);
		ParetoFrontsKeeper PFKeeper = new ParetoFrontsKeeper(SIZE_OF_EP);

		List<PopulationEntry> gloablPF = new ArrayList<>();
		List<PopulationEntry> previousgloablPF;

		double currentDCI = -1;
		double previousDCI = -2;
		int notImprovedInARow = 0;

		double[][] caps = new double[bc.getNumberOfObjectives()][2];
		for (double[] d : caps) {
			d[0] = Double.NEGATIVE_INFINITY;
			d[1] = Double.POSITIVE_INFINITY;
		}

		for (int i = 0; i < noOfIslands; i++) {
			optimisationArguments.add(createIsland(ct, urgency, quality, populationSize, random));
		}

		List<OptimisationIslandResult> optimisationIslandResult = new ArrayList<OptimisationIslandResult>();

		for (int stage = 0; stage < NoOfStages; stage++) {

			/**
			 * Copy and clear global PF
			 */
			previousgloablPF = new ArrayList<PopulationEntry>(gloablPF);
			optimisationIslandResult.clear();

			for (int island = 0; island < noOfIslands; island++) {
				optimisationIslandResult.add(execute(optimisationArguments.get(island)));
			}

			/**
			 * Update Global FP
			 */
			// gloablPF.clear();
			for (int i = 0; i < optimisationIslandResult.size(); i++) {
				OptimisationIslandResult result = optimisationIslandResult.get(i);
				for (PopulationEntry entry : result.getFront()) {
					PFKeeper.updateExternalPopulation(gloablPF, entry);
				}
			}

			boolean isImproved = true;
			if (previousgloablPF.size() != 0) {
				List<List<List<Double>>> PFValues = new ArrayList<>();

				List<List<Double>> globalPFValues = new ArrayList<>();
				List<List<Double>> previousGlobalPFValues = new ArrayList<>();

				for (PopulationEntry entry : previousgloablPF) {
					previousGlobalPFValues.add(entry.getObjectives());
				}
				for (PopulationEntry entry : gloablPF) {
					globalPFValues.add(entry.getObjectives());
				}

				PFValues.add(previousGlobalPFValues);
				PFValues.add(globalPFValues);

				double[] DCIs = new DCICaculator().apply(PFValues);
				currentDCI = DCIs[1];
				previousDCI = DCIs[0];

				if (currentDCI <= previousDCI) {
					notImprovedInARow++;
					isImproved = false;
				} else {
					notImprovedInARow = 0;
					isImproved = true;
				}

//				System.out.println(
//						"island: " + islandID + " stage: " + stage + " Prev: " + previousDCI + " Now: " + currentDCI);
			}

			if (!isImproved) {
				// System.err.println("not improved: " + notImprovedInARow);
				if (notImprovedInARow == notImprovedInARowLimit) {
					notImprovedInARow = 0;
					connectM.push(cloudID, islandID, gloablPF, fpCaps);
					numberOfPush++;
					// System.err.println("PUSH!");
				}

				List<PopulationEntry> pulledGloablPF = connectM.poll(cloudID, islandID, fpCaps);

				if (pulledGloablPF != null && pulledGloablPF.size() > 0) {
					for (OptimisationIslandResult result : optimisationIslandResult) {
						replace(result, pulledGloablPF, RemoveMethod, addMethod, numberOfReplacement, random, islandID,
								useCloud, fpCaps.get(islandID).linkageFactory);
						if (RemoveMethod >= 0 || addMethod >= 0) {
							numberOfPull++;
						}
					}
				}
			}

			/**
			 * Prepare for next stage
			 */
			optimisationArguments.clear();
			for (OptimisationIslandResult result : optimisationIslandResult) {
				optimisationArguments.add(new OptimisationArguments(
						extractFinalPopulationConfigurations(result, configurationTemplate), urgency, quality));

			}

		}

		connectM.push(cloudID, islandID, gloablPF, fpCaps);

		// String out = "ManagerPP seed: " + seeds + "\n";

		List<List<Double>> finalPF = new ArrayList<>();
		gloablPF.sort((c1, c2) -> Double.compare(c1.getObjectives().get(0), c2.getObjectives().get(0)));
		// out += "\n Global Best: \n";
		for (PopulationEntry gb : gloablPF) {
			finalPF.add(gb.getObjectives());
			// for (Double d : gb.getObjectives()) {
			// out += d + " ";
			// }
			// out += "\n";
		}

		// Executor.writeSystem(seeds + "ManagerPP" + ONAFactoryModel.scale, out);

		List<Integer> numberOfActions = new ArrayList<>();
		numberOfActions.add(numberOfPush);
		numberOfActions.add(numberOfPull);

		actions.set(islandID, numberOfActions);

		return finalPF;
	}

}
