package restCloud;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import aura.PopulationEntry;
import factoryModel.ONA.ONAFactoryModel;
import indicator.HyperVolume;
import metrics.Configuration;
import metrics.ConfigurationType;
import metrics.Utility;
import metrics.Value;
import metrics.ValueType;
import metrics.Value.Nominal;
import mitm.atb.BusinessCase;
import mitm.atb.UoYEarlyPrototypeDemo;
import optimisation.ONAFitnessFunction;
import optimisation.ObjectiveFunction;

public class LinkageFactory {

	BusinessCase bc;
	Random random;
	ObjectiveFunction.LocalObjectiveFunction of;

	ArrayList<int[]> allScraps = new ArrayList<>();

	List<PopulationEntry> best = new ArrayList<>();
	// double bestFitness=0;

	DecimalFormat df = new DecimalFormat("0.00");

	public LinkageFactory(BusinessCase bc, int seed) {
		this.bc = bc;
		this.random = new Random(seed);
		of = (ObjectiveFunction.LocalObjectiveFunction) bc.getObjectiveFunction();

	}

	public int compareGene(String gene1, String gene2) {

		String id1_string = gene1.split(" ")[0];
		String part1_string = gene1.split(" ")[1];
		String content1 = gene1.split(" ")[2];

		String id2_string = gene2.split(" ")[0];
		String part2_string = gene2.split(" ")[1];
		String content2 = gene2.split(" ")[2];

		if (content1.equals("allocation") && content2.equals("priority")) {
			return -1;
		} else if (content1.equals("priority") && content2.equals("allocation")) {
			return 1;
		} else {

			int compare = Integer.compare(Integer.parseInt(part1_string), Integer.parseInt(part2_string));

			if (compare != 0) {
				return compare;
			} else {
				int id1 = Integer.parseInt(id1_string.substring(1));
				int id2 = Integer.parseInt(id2_string.substring(1));

				return Integer.compare(id1, id2);
			}

		}

	}

	public Tree getLinkage(PopulationEntry entry) {

		double weightO1 = (double) random.nextInt(101) / (double) 100;
		double weightO2 = 1 - weightO1;

//		System.out.println("Weight 1: " + weightO1 + " Weight 2: " + weightO2);

		Configuration config = entry.getConfiguration();

		Map<String, Value> controls = config.getControlledMetrics();
		List<MutablePair<String, Value>> controlsList = controls.entrySet().stream()
				.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList());

		controlsList.sort((p1, p2) -> compareGene(p1.getKey(), p2.getKey()));

		int startingPoint = random.nextInt(controlsList.size() / 2);

		List<MutablePair<String, Value>> bestConfig = getBestConfig(config, controlsList, weightO1, weightO2,
				startingPoint);

		ArrayList<int[]> scarps = getScrap(config, controlsList, bestConfig, weightO1, weightO2, startingPoint);

		allScraps.addAll(scarps);

//		System.out.println("Scraps");
//		for (int i = 0; i < allScraps.size(); i++) {
//			int[] s = allScraps.get(i);
//			for (int j = 0; j < s.length; j++) {
//				System.out.print(s[j] + " ");
//			}
//
//			System.out.println();
//			// System.out.println(Arrays.toString(allScraps.get(i)));
//		}

		double[][] lllo = get3L0(allScraps);

//		System.out.println("3LO-DSM:");
//		for (int i = 0; i < lllo.length; i++) {
//
//			double[] d = lllo[i];
//			for (int j = 0; j < d.length; j++) {
//				System.out.print(df.format(d[j]) + " ");
//			}
//
//			System.out.println();
////			System.out.println(Arrays.toString(lllo[i]));
//		}

		List<Node> roots = new ArrayList<>();
		for (int i = 0; i < lllo.length; i++) {
			List<Integer> id = new ArrayList<>();
			id.add(i);
			Node node = new Node(id);
			roots.add(node);
		}

		Tree linkageTree = new Tree(roots, lllo);

//		System.out.println("Linkage Tree:");
//		linkageTree.printTree();

		return linkageTree;
	}

	public List<MutablePair<String, Value>> deepCopyConfig(List<MutablePair<String, Value>> controlsList) {
		controlsList.sort((p1, p2) -> compareGene(p1.getKey(), p2.getKey()));

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
		return newControls;
	}

	public List<MutablePair<String, Value>> getBestConfig(Configuration config,
			List<MutablePair<String, Value>> controlsList, double weightO1, double weightO2, int start) {
		List<MutablePair<String, Value>> newControls = deepCopyConfig(controlsList);

		/* get greedy best configuration */
		for (int i = start; i < newControls.size(); i++) {
			MutablePair<String, Value> p = newControls.get(i);

			if (p.getKey().contains("allocation")) {
				Nominal allocValue = (Nominal) p.getValue();
				ValueType.Nominal allocType = (ValueType.Nominal) allocValue.getType();

				double bestObjective = Double.MAX_VALUE;
				Value bestAllocValue = null;

				List<String> vaildAllocations = new ArrayList<>();
				for (int j = 0; j < allocType.numValues(); j++) {
					vaildAllocations.add(allocType.getValue(j));
				}

				for (int j = 0; j < vaildAllocations.size(); j++) {
					String alloc = vaildAllocations.get(j);
					Value newAllocValue = new Nominal(alloc, allocType);
					p.setValue(newAllocValue);

					Map<String, Value> newControlConfig = newControls.stream()
							.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

					Configuration bestConfig = new Configuration(config.getConfigurationType(), newControlConfig,
							config.getKeyObjectives());

					List<Double> res = ONAFitnessFunction.getFitness(bestConfig);


					double objective = res.get(0) * weightO1 + res.get(1) * weightO2;
					// System.out.println(objective);

					if (objective <= bestObjective) {
						bestObjective = objective;
						bestAllocValue = newAllocValue;
					}

				}

				p.setValue(bestAllocValue);
			}
		}

		for (int i = 0; i < start; i++) {
			MutablePair<String, Value> p = newControls.get(i);

			if (p.getKey().contains("allocation")) {
				Nominal allocValue = (Nominal) p.getValue();
				ValueType.Nominal allocType = (ValueType.Nominal) allocValue.getType();

				double bestObjective = Double.MAX_VALUE;
				Value bestAllocValue = null;

				List<String> vaildAllocations = new ArrayList<>();
				for (int j = 0; j < allocType.numValues(); j++) {
					vaildAllocations.add(allocType.getValue(j));
				}

				for (int j = 0; j < vaildAllocations.size(); j++) {
					String alloc = vaildAllocations.get(j);
					Value newAllocValue = new Nominal(alloc, allocType);
					p.setValue(newAllocValue);

					Map<String, Value> newControlConfig = newControls.stream()
							.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

					Configuration bestConfig = new Configuration(config.getConfigurationType(), newControlConfig,
							config.getKeyObjectives());

					List<Double> res = ONAFitnessFunction.getFitness(bestConfig);

					// ObjectiveFunctionResult evaled = null;
					// try {
					// evaled = of
					// .evaluate(new ObjectiveFunctionArguments(bestConfig,
					// bestConfig.getControlledMetrics()))
					// .call();
					// } catch (Exception e) {
					// e.printStackTrace();
					// }

					double objective = res.get(0) * weightO1 + res.get(1) * weightO2;
					// System.out.println(objective);
					if (objective <= bestObjective) {
						bestObjective = objective;
						bestAllocValue = newAllocValue;
					}

				}

				p.setValue(bestAllocValue);
			}
		}

		Map<String, Value> greedyControlConfig = newControls.stream()
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
		Configuration greedyConfig = new Configuration(config.getConfigurationType(), greedyControlConfig,
				config.getKeyObjectives());

		// ObjectiveFunctionResult evaled = null;
		// try {
		// evaled = of.evaluate(new ObjectiveFunctionArguments(greedyConfig,
		// greedyConfig.getControlledMetrics()))
		// .call();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		List<Double> res = ONAFitnessFunction.getFitness(greedyConfig);
		PopulationEntry bestEntry = new PopulationEntry(greedyConfig, res);

//		System.out.println("best: " + bestEntry.getObjectives().get(0) + ", " + bestEntry.getObjectives().get(1)
//				+ ". With weight: "
//				+ (bestEntry.getObjectives().get(0) * weightO1 + bestEntry.getObjectives().get(1) * weightO2));

		// if (bestFitness <= (bestEntry.getObjectives().get(0) * weightO1
		// + bestEntry.getObjectives().get(1) * weightO2)) {
		// best = bestEntry;
		// bestFitness = (bestEntry.getObjectives().get(0) * weightO1 +
		// bestEntry.getObjectives().get(1) * weightO2);
		// }

		updateExternalPopulation(best, bestEntry);

		return newControls;
	}

	boolean dominate(PopulationEntry individual1, PopulationEntry individual2) {
		boolean isDominate = false;

		if (individual1.getObjectives().size() != individual2.getObjectives().size()) {
			System.out.println("error");
			System.exit(-1);
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
				if (externalPopulation.size() < 100)
					externalPopulation.add(candidate);
			}
		}

	}

	public List<PopulationEntry> getBest() {

		List<List<List<Double>>> bests = new ArrayList<>();
		for (PopulationEntry e : best) {
			List<List<Double>> b = new ArrayList<>();
			b.add(e.getObjectives());
			bests.add(b);
		}

		HyperVolume hv = new HyperVolume();
		double[] res = hv.applyHV(bests);

		List<List<Double>> resInd = new ArrayList<>();
		for (int i = 0; i < res.length; i++) {
			List<Double> r = new ArrayList<>();
			r.add(res[i]);
			r.add(i + 0.0);
			resInd.add(r);
		}

		resInd.sort((c1, c2) -> -Double.compare(c1.get(0), c2.get(0)));

		List<PopulationEntry> entrys = new ArrayList<>();

		for (int i = 0; i < resInd.size(); i++) {
			entrys.add(best.get(resInd.get(i).get(1).intValue()));
		}

		return entrys;
	}

	public ArrayList<int[]> getScrap(Configuration config, List<MutablePair<String, Value>> controlsList,
			List<MutablePair<String, Value>> best, double weightO1, double weightO2, int start) {
		ArrayList<int[]> scarps = new ArrayList<>();

		List<MutablePair<String, Value>> newControls = deepCopyConfig(controlsList);

		for (int i = 0; i < controlsList.size(); i++) {
			MutablePair<String, Value> p = newControls.get(i);

			if (p.getKey().contains("allocation")) {
				Nominal allocValue = (Nominal) p.getValue();
				ValueType.Nominal allocType = (ValueType.Nominal) allocValue.getType();
				String value = allocValue.value;

				String newValue = allocType.getValue(random.nextInt(allocType.numValues()));
				while (newValue.equals(value) && allocType.numValues() > 1) {
					newValue = allocType.getValue(random.nextInt(allocType.numValues()));
				}

				Value newAllocValue = new Nominal(newValue, allocType);
				p.setValue(newAllocValue);

				List<MutablePair<String, Value>> greedycontrol = getBestConfig(config, newControls, weightO1, weightO2,
						start);

				Value oldAllocValue = new Nominal(value, allocType);
				p.setValue(oldAllocValue);

				List<MutablePair<String, Value>> bestControl = best;

				int[] scrap = new int[controlsList.size() / 2];
				for (int j = 0; j < bestControl.size(); j++) {
					MutablePair<String, Value> bestpair = bestControl.get(j);

					if (bestpair.getKey().contains("allocation")) {
						Nominal n = (Nominal) bestpair.getValue();
						String v = n.value;

						MutablePair<String, Value> greedyPair = greedycontrol.get(j);
						Nominal nNew = (Nominal) greedyPair.getValue();
						String vNew = nNew.value;

						if (!v.equals(vNew)) {
							scrap[j] = 1;
						}
					}
				}

				if (scrap[i] == 0)
					scrap[i] = 1;

				scarps.add(scrap);
			}

		}

		return scarps;
	}

	public double[][] get3L0(ArrayList<int[]> scarps) {

		double[][] lllo = new double[scarps.get(0).length][scarps.get(0).length];

		for (int i = 0; i < scarps.size(); i++) {
			int[] scarp = scarps.get(i);
			List<Integer> diffIndex = new ArrayList<>();

			for (int j = 0; j < scarp.length; j++) {
				if (scarp[j] == 1)
					diffIndex.add(j);
			}

			for (int j = 0; j < diffIndex.size() - 1; j++) {
				for (int k = j + 1; k < diffIndex.size(); k++) {
					lllo[diffIndex.get(j)][diffIndex.get(k)] += 1;
					if (diffIndex.get(j) != diffIndex.get(k))
						lllo[diffIndex.get(k)][diffIndex.get(j)] += 1;
				}
			}
		}

//		System.out.println("3LO-DSM (before randomized):");
//		for (int i = 0; i < lllo.length; i++) {
//
//			double[] d = lllo[i];
//			for (int j = 0; j < d.length; j++) {
//				System.out.print((int) d[j] + " ");
//			}
//
//			System.out.println();
////			System.out.println(Arrays.toString(lllo[i]));
//		}

		for (int i = 0; i < lllo.length; i++) {
			for (int j = 0; j < lllo[i].length; j++) {
				double ran = (double) (random.nextInt(25) + 1) / (double) 100;
				double v = Double.parseDouble(df.format(ran + lllo[i][j]));
				lllo[i][j] = v;

				if (i != j) {
					double v1 = Double.parseDouble(df.format(ran + lllo[j][i]));
					lllo[j][i] = v1;
				}
			}
		}

		return lllo;
	}

	class Tree {
		List<Node> roots;
		List<Node> all;
		List<Node> current;
		double[][] lllo;

		public Tree(List<Node> roots, double[][] lllo) {
			this.roots = roots;
			this.lllo = lllo;

			this.all = new ArrayList<>();
			this.current = new ArrayList<>();

			for (int i = 0; i < roots.size(); i++) {
//				all.add(roots.get(i));
				current.add(roots.get(i));
			}

			formTree();
		}

		public void formTree() {
			boolean finish = join();
			while (!finish) {
				finish = join();
			}
		}

		public boolean join() {
			double maxV = 0;
			List<Node> linkedNodes = new ArrayList<>();

			for (int i = 0; i < current.size() - 1; i++) {
				for (int j = i + 1; j < current.size(); j++) {
					Node a = current.get(i);
					Node b = current.get(j);

					double nodeMax = 0;
					int[] maxPair = new int[2];
					for (int k = 0; k < a.ids.size(); k++) {
						int a_id = a.ids.get(k);
						for (int l = 0; l < b.ids.size(); l++) {
							int b_id = b.ids.get(l);

							if (nodeMax <= lllo[a_id][b_id]) {
								nodeMax = lllo[a_id][b_id];
								maxPair[0] = a_id;
								maxPair[1] = b_id;
							}
						}
					}

					if (maxV <= nodeMax) {
						maxV = nodeMax;
						linkedNodes.clear();
						linkedNodes.add(a);
						linkedNodes.add(b);
					}
				}
			}

			Node newNode = new Node(null);
			List<Integer> newIDs = new ArrayList<>();
			int layer = 0;
			for (int i = 0; i < linkedNodes.size(); i++) {
				layer = Integer.max(layer, linkedNodes.get(i).layer);
				current.remove(linkedNodes.get(i));

				newNode.children.add(linkedNodes.get(i));
				linkedNodes.get(i).parent = newNode;

				for (int j = 0; j < linkedNodes.get(i).ids.size(); j++) {
					newIDs.add(linkedNodes.get(i).ids.get(j));
				}
			}

			newNode.ids = newIDs;
			newNode.layer = layer + 1;

			all.add(newNode);
			current.add(newNode);

			for (int i = 0; i < roots.size(); i++) {
				int rootID = roots.get(i).ids.get(0);

				boolean contain = false;
				for (int j = 0; j < newNode.ids.size(); j++) {
					int id = newNode.ids.get(j);

					if (id == rootID)
						contain = true;
				}

				if (!contain)
					return false;
			}

			return true;
		}

		public void printTree() {
			int layer = 1;

			System.out.println("-----------------------------------------");

			List<Node> currentLayer = new ArrayList<>();
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).layer == layer) {
					currentLayer.add(all.get(i));
				}
			}

			while (currentLayer.size() > 0) {

				for (int i = 0; i < currentLayer.size(); i++) {
					System.out.print(Arrays.toString(currentLayer.get(i).ids.toArray()) + "   ");
				}
				System.out.println();

				currentLayer.clear();
				layer++;
				for (int i = 0; i < all.size(); i++) {
					if (all.get(i).layer == layer) {
						currentLayer.add(all.get(i));
					}
				}

			}

			System.out.println("-----------------------------------------");
		}

	}

	class Node {
		List<Integer> ids;
		Node parent = null;
		List<Node> children = new ArrayList<Node>();
		int layer = 0;

		public Node(List<Integer> ids) {
			this.ids = ids;
		}

		public void add(Node node) {
			children.add(node);
		}

		public Node getParent() {
			return parent;
		}

		@Override
		public String toString() {
			return Arrays.toString(ids.toArray());
		}
	}



	public static void main(String args[]) {

		List<Long> times = new ArrayList<>();

		for (int i = 0; i < 100; i++) {

			LinkageFactory factory = new LinkageFactory(BusinessCase.ONA, 1000 + i);

			ONAFactoryModel.scale.set(3);
			ConfigurationType ct = factory.bc.getConfigurationType(100, factory.random);
			Configuration config = Utility.randomConfiguration(ct, factory.random);
			UoYEarlyPrototypeDemo.observableMetrics = Utility
					.makeObservableMetrics(UoYEarlyPrototypeDemo.observableMetricTypes, factory.random);

			List<Double> objectives = ONAFitnessFunction.getFitness(config);
//			System.out.println("Fitness 1: " + objectives);

			PopulationEntry entry = new PopulationEntry(config, objectives);

			long start = System.currentTimeMillis();
			factory.getLinkage(entry);
			long end = System.currentTimeMillis() - start;

			times.add(end);

//			System.out.println(i + " done");
		}

//		System.out.println(Arrays.toString(times.toArray()));
	}

}
