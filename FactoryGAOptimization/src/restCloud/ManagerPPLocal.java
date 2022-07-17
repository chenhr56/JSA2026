package restCloud;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import aura.PopulationEntry;
import indicator.Indicators;
import mitm.atb.BusinessCase;

public class ManagerPPLocal {
	

	
	public enum ADD {
		New, Random, Best, Diverse
	}

	public enum REMOVE {
		None, Random, Worst
	}

	public static int numberOfPush = 0;
	public static int numberOfPull = 0;

	private ResultBundle startManager(BusinessCase bc, int ONAfactoryScale, int numberOfIslandsParam, int seeds,
			int notImprovedInARowLimit, int numberOfReplacement, int RemoveMethod, int addMethod) {

//		System.out.println(getName(RemoveMethod, addMethod));

		int numberOfIslands = (RemoveMethod == -2 || RemoveMethod == -3) ? 1 : numberOfIslandsParam;
		Random ran = new Random(seeds);
		List<ParetoFrontCapsule> globalCaps = new ArrayList<>();
		List<List<Integer>> numberOfActions = new ArrayList<>();
		List<PopulationEntry> dummy = new ArrayList<>();

		List<ResultBundle> results = new ArrayList<>();

		for (int i = 0; i < numberOfIslands; i++) {
			LinkageFactory factory = new LinkageFactory(bc, seeds + numberOfIslands);
			globalCaps.add(new ParetoFrontCapsule(dummy, factory));
			numberOfActions.add(new ArrayList<>());
			results.add(null);
		}

		List<Thread> islands = new ArrayList<>();
		for (int i = 0; i < numberOfIslands; i++) {
			final int id = i;
			Thread island = new Thread(new Runnable() {
				@Override
				public void run() {
					ResultBundle res = new ManagerPP(bc, ONAfactoryScale, ran.nextInt(), notImprovedInARowLimit,
							numberOfReplacement, RemoveMethod, addMethod, numberOfIslandsParam).start(id, globalCaps, numberOfActions, false);

					results.set(id, res);
				}
			});

			islands.add(island);
		}

		for (Thread island : islands) {
			island.start();
		}

		for (Thread island : islands) {
			try {
				island.join();
			} catch (InterruptedException e) {
			}
		}

		ParetoFrontsKeeper PFKeeper = new ParetoFrontsKeeper(ManagerPP.SIZE_OF_EP * numberOfIslands);

		List<PopulationEntry> globalPF = new ArrayList<>();
		for (ParetoFrontCapsule c : globalCaps) {
			for (PopulationEntry e : c.get()) {
				PFKeeper.updateExternalPopulation(globalPF, e);
			}
		}

		List<List<Double>> objectives = new ArrayList<>();

		for (int i = 0; i < globalPF.size(); i++) {
			objectives.add(globalPF.get(i).getObjectives());
		}

		objectives.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));

		long timeSum = 0;
		numberOfPush = 0;
		numberOfPull = 0;
		for (int i = 0; i < results.size(); i++) {

			numberOfPush += results.get(i).push;
			numberOfPull += results.get(i).pull;
			timeSum += results.get(i).time;
		}

		long time = (long) Math.ceil(timeSum / (double) results.size());

//		System.out.println("time: " + time);
		return new ResultBundle(objectives, numberOfPush, numberOfPull, time, getName(RemoveMethod, addMethod));
	}

//	public static void main(String args[]) {
//		new ManagerPPLocal().startPPLocal(1000, 1, 5, 1, 3);
//	}

//	public ResultBundle runOneSetting(BusinessCase bc, int ONAfactoryScale, int numberOfIslands, int seeds,
//			int notImprovedInARowLimit, int numberOfReplacement, int remove, int add) {
//
//		String out = "";
//
//		System.out.println(getName(remove, add));
//
//		out += getName(-2, -2) + "\n";
//
//		ResultBundle result = startManager(bc, ONAfactoryScale, numberOfIslands, seeds, notImprovedInARowLimit,
//				numberOfReplacement, -2, -2);
//
//		ObjectiveCapsule normalPF = new ObjectiveCapsule(result.objectives);
//
//		System.out.println("Final PF: ");
//		out += "Final PF: \n";
//		for (List<Double> l : normalPF.get()) {
//			for (Double d : l) {
//				System.out.print(d + " ");
//				out += d + " ";
//			}
//			System.out.println();
//			out += "\n";
//		}
//
//		out += "push: " + result.push + " pull: " + result.pull + "\n";
//		System.out.println("push: " + result.push + " pull: " + result.pull);
//
//		long time = result.time;
//		out += "time: " + time + "\n";
//		System.out.println("time: " + time + "\n\n");
//
//		result.out = out;
//
//		return result;
//	}

	public List<List<Double>> startPPLocal(int seed, int factoryScale, int numberofIsland, int numerOfReplacement,
			int notImprovedInRow, String folder) {

		BusinessCase bc = BusinessCase.ONA;
		int REMOVE_METHOD = 2;
		int ADD_METHOD = 4;

		int seeds = seed;
		int numberOfIslands = numberofIsland;
		int ONAfactoryScale = factoryScale;
		int numberOfReplacement = numerOfReplacement;
		int notImprovedInARowLimit = notImprovedInRow;

		List<ResultBundle> bundles = new ArrayList<>();
		
//		/**
//		 * The NSGA-II
//		 */
		bundles.add(startManager(bc, ONAfactoryScale, numberOfIslands, seeds, notImprovedInARowLimit,
				numberOfReplacement, -3, -3));

		
//		ManagerPP.generations = moead_iter;
//		System.out.println("Start MOEA/D, generation in each stage: " + ManagerPP.generations);
		/**
		 * The traditional MOEA/D
		 */
		
		bundles.add(startManager(bc, ONAfactoryScale, numberOfIslands, seeds, notImprovedInARowLimit,
				numberOfReplacement, -2, -2));

		

//		ManagerPP.generations = individual_iter;
//		System.out.println("Start individual, generation in each stage: " + ManagerPP.generations);
		/**
		 * Island Model without migration
		 */
		bundles.add(startManager(bc, ONAfactoryScale, numberOfIslands, seeds, notImprovedInARowLimit,
				numberOfReplacement, -1, -1));

		/**
		 * Island Model with migrations
		 */
		for (int i = 0; i < REMOVE_METHOD; i++) {
			for (int j = 0; j < ADD_METHOD; j++) {
				bundles.add(startManager(bc, ONAfactoryScale, numberOfIslands, seeds, notImprovedInARowLimit,
						numberOfReplacement, i, j));
			}
		}

		
		
		
//		ManagerPP.generations = linkage_iter;
//		System.out.println("Start linkage, generation in each stage: " + ManagerPP.generations);
		/**
		 * Island Model with linkage
		 */
		bundles.add(startManager(bc, ONAfactoryScale, numberOfIslands, seeds, notImprovedInARowLimit,
				numberOfReplacement, 5, 5));

		List<List<Double>> result = preAnalysis(bundles);

		writeToFile(bundles, numberOfIslands, ONAfactoryScale, seeds, numberOfReplacement, folder);

		return result;
	}

	private List<List<Double>> preAnalysis(List<ResultBundle> bundles) {

		List<List<Double>> allRes = new ArrayList<>();
		List<List<List<Double>>> allFPs = new ArrayList<>();

		for (int i = 0; i < bundles.size(); i++) {
			allFPs.add(bundles.get(i).objectives);
		}

		allRes.addAll(Indicators.compare(allFPs, null));

		List<Double> push = new ArrayList<>();
		List<Double> pull = new ArrayList<>();
		List<Double> time = new ArrayList<>();

		for (int i = 0; i < bundles.size(); i++) {
			push.add((double) bundles.get(i).push);
			pull.add((double) bundles.get(i).pull);
			time.add((double) bundles.get(i).time);
		}

		allRes.add(push);
		allRes.add(pull);
		allRes.add(time);

		return allRes;
	}

	public void writeToFile(List<ResultBundle> bundles, int numberOfIslands, int ONAfactoryScale, int seeds,
			int numberOfReplacement, String folder) {

		/**
		 * Write FPs to the file system
		 */
		try {
			File theDir = new File(folder + "result_PF");
			if (!theDir.exists()) {
				theDir.mkdirs();
			}
		} catch (Exception e) {
		}
		String fileName_PF = folder + "result_PF" + "/" + "PFs " + numberOfIslands + " " + ONAfactoryScale + " "
				+ seeds;
		String out_PF = "";

		for (int i = 0; i < bundles.size(); i++) {
			ResultBundle r = bundles.get(i);

			for (int j = 0; j < r.objectives.size(); j++) {
				for (int k = 0; k < r.objectives.get(j).size(); k++) {
					out_PF += r.objectives.get(j).get(k);

					if (k != r.objectives.get(j).size() - 1)
						out_PF += " ";
				}
				out_PF += "\n";
			}
			out_PF += "\n";
		}

		ResultAnalyser.writeResult(fileName_PF + ".txt", out_PF);

		/**
		 * Write Quality values to the file system
		 */
		try {
			File theDir = new File(folder + "result_QV");
			if (!theDir.exists()) {
				theDir.mkdirs();
			}
		} catch (Exception e) {
		}
		String fileName_QV = folder + "result_QV" + "/" + "Quality Values " + numberOfIslands + " " + ONAfactoryScale
				+ " " + seeds;
		String out_QV = "";

		List<List<List<Double>>> objectives = new ArrayList<>();
		for (int i = 0; i < bundles.size(); i++) {
			objectives.add(bundles.get(i).objectives);
		}

		List<List<Double>> qvs = Indicators.compare(objectives, null);

		for (int i = 0; i < qvs.size(); i++) {
			for (int j = 0; j < qvs.get(i).size(); j++) {
				out_QV += qvs.get(i).get(j);
				if (j != qvs.get(i).size() - 1) {
					out_QV += " ";
				}
			}
			out_QV += "\n";
		}

		ResultAnalyser.writeResult(fileName_QV + ".txt", out_QV);

		/**
		 * Write pull_push and execution time to the system
		 */
		try {
			File theDir = new File(folder + "result_Push_Pull");
			if (!theDir.exists()) {
				theDir.mkdirs();
			}

			File theDir1 = new File(folder + "result_ET");
			if (!theDir1.exists()) {
				theDir1.mkdirs();
			}
		} catch (Exception e) {
		}

		String fileName_PP = folder + "result_Push_Pull" + "/" + "Push and Pull " + numberOfIslands + " "
				+ ONAfactoryScale + " " + seeds;
		String out_PP = "";
		String fileName_ET = folder + "result_ET" + "/" + "Execution Time " + numberOfIslands + " " + ONAfactoryScale
				+ " " + seeds;
		String out_ET = "";

		for (int i = 0; i < bundles.size(); i++) {
			ResultBundle r = bundles.get(i);

			out_PP += r.push + " " + r.pull + "\n";
			out_ET += r.time + "\n";

		}

		ResultAnalyser.writeResult(fileName_PP + ".txt", out_PP);
		ResultAnalyser.writeResult(fileName_ET + ".txt", out_ET);

	}

	public static String getName(int remove, int add) {

		if (remove == -2 && add == -2)
			return "traditional MOEA/D";

		if (remove == -1 && add == -1)
			return "remove and add nothing";

		if (remove == 5 && add == 5)
			return "linkage migration";

		String name = "";
		switch (remove) {
		case 0:
			name += "Remove random   ";
			break;
		case 1:
			name += "Remove worst   ";
			break;
		}

		name += "";
		switch (add) {
		case 0:
			name += "Add new";
			break;
		case 1:
			name += "Add random";
			break;
		case 2:
			name += "Add best";
			break;
		case 3:
			name += "Add diversity";
			break;
		default:
			break;
		}

		return name;
	}

}
