package restCloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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

	private List<List<Double>> startManager(BusinessCase bc, int ONAfactoryScale, int numberOfIslands, int seeds,
			int notImprovedInARowLimit, int numberOfReplacement, int RemoveMethod, int addMethod) {
		Random ran = new Random(seeds);
		List<ParetoFrontCapsule> globalCaps = new ArrayList<>();
		List<List<Integer>> numberOfActions = new ArrayList<>();
		List<PopulationEntry> dummy = new ArrayList<>();

		for (int i = 0; i < numberOfIslands; i++) {
			LinkageFactory factory = new LinkageFactory(bc, seeds + numberOfIslands);
			globalCaps.add(new ParetoFrontCapsule(dummy, factory));
			numberOfActions.add(new ArrayList<>());
		}

		List<Thread> islands = new ArrayList<>();
		for (int i = 0; i < numberOfIslands; i++) {
			final int id = i;
			Thread island = new Thread(new Runnable() {
				@Override
				public void run() {
					new ManagerPP(bc, ONAfactoryScale, ran.nextInt(), notImprovedInARowLimit, numberOfReplacement,
							RemoveMethod, addMethod).startIsland(id, globalCaps, numberOfActions, false);
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

		List<List<Double>> objectives = globalPF.stream().map(e -> e.getObjectives()).collect(Collectors.toList());

		numberOfPush = numberOfActions.stream().map(a -> a.get(0)).collect(Collectors.toList()).stream()
				.mapToInt(Integer::intValue).sum();
		numberOfPull = numberOfActions.stream().map(a -> a.get(1)).collect(Collectors.toList()).stream()
				.mapToInt(Integer::intValue).sum();

		return objectives;
	}

	public static int numberOfPush = 0;
	public static int numberOfPull = 0;

	public static void main(String args[]) {
		new ManagerPPLocal().startPPLocal(1000, 1, 5, 1, 3, "result");
	}

	public List<List<Double>> startPPLocal(int seed, int factoryScale, int numberofIsland, int numerOfReplacement,
			int notImprovedInRow, String directory) {

		BusinessCase bc = BusinessCase.ONA;
		int REMOVE_METHOD = 2;
		int ADD_METHOD = 4;

		int seeds = seed;
		int numberOfIslands = numberofIsland;
		int ONAfactoryScale = factoryScale;
		int numberOfReplacement = numerOfReplacement;
		int notImprovedInARowLimit = notImprovedInRow;

		long t1 = 0;
		long t2 = 0;

		ObjectiveCapsule normalPF = null;
		List<List<ObjectiveCapsule>> objCaps = new ArrayList<>();
		List<Long> time = new ArrayList<>();

		List<int[]> numberOfActions = new ArrayList<>();

		String fileName = directory + "/" + "managerPP " + numberOfIslands + " " + ONAfactoryScale + " " + seeds + " "
				+ numberOfReplacement;

		String out = "seeds " + seeds + " factoryScale" + ONAfactoryScale + " numberOfIslands" + numberofIsland
				+ " numberOfReplacement" + numberOfReplacement + " notImprovedInARow " + notImprovedInARowLimit
				+ "\n\n";

		System.out.println(getName(-1, -1));

		out += getName(-1, -1) + "\n";

		t1 = System.currentTimeMillis();
		normalPF = new ObjectiveCapsule(startManager(bc, ONAfactoryScale, numberOfIslands, seeds,
				notImprovedInARowLimit, numberOfReplacement, -1, -1));
		t2 = System.currentTimeMillis() - t1;
		time.add(t2);

		out += "push: " + numberOfPush + " pull: " + numberOfPull + "\n";
		System.out.println("push: " + numberOfPush + " pull: " + numberOfPull);

		int[] actions = { numberOfPush, numberOfPull };
		numberOfActions.add(actions);
		System.out.println("Final PF: ");
		out += "Final PF: \n";
		for (List<Double> l : normalPF.get()) {
			for (Double d : l) {
				System.out.print(d + " ");
				out += d + " ";
			}
			System.out.println();
			out += "\n";
		}

		System.out.println("\n\n");
		for (int i = 0; i < REMOVE_METHOD; i++) {
			List<ObjectiveCapsule> objC = new ArrayList<>();
			for (int j = 0; j < ADD_METHOD; j++) {
				System.out.println(getName(i, j));
				out += getName(i, j) + "\n";

				t1 = System.currentTimeMillis();
				List<List<Double>> objective = startManager(bc, ONAfactoryScale, numberOfIslands, seeds,
						notImprovedInARowLimit, numberOfReplacement, i, j);
				t2 = System.currentTimeMillis() - t1;
				time.add(t2);

				out += "push: " + numberOfPush + " pull: " + numberOfPull + "\n";
				System.out.println("push: " + numberOfPush + " pull: " + numberOfPull);
				int[] actions1 = { numberOfPush, numberOfPull };
				numberOfActions.add(actions1);
				System.out.println("Final PF: ");
				out += "Final PF: \n";
				for (List<Double> l : objective) {
					for (Double d : l) {
						System.out.print(d + " ");
						out += d + " ";
					}
					System.out.println();
					out += "\n";
				}

				ObjectiveCapsule oc = new ObjectiveCapsule(objective);
				objC.add(oc);
				System.out.println("\n\n");
			}
			objCaps.add(objC);
		}

		System.out.println(getName(5, 5));
		out += getName(5, 5) + "\n";

		t1 = System.currentTimeMillis();

		// double[][] objectiveA = { { 16571.0, 45292.0 }, { 18092.0, 41467.0 }, {
		// 21342.0, 40999.0 },
		// { 24617.0, 40751.0 }, { 25417.0, 40370.0 }, { 32358.0, 40020.0 }, { 16745.0,
		// 44274.0 },
		// { 17089.0, 44129.0 }, { 17345.0, 42408.0 }, { 17516.0, 41720.0 }, { 18596.0,
		// 41349.0 },
		// { 22263.0, 40874.0 } };
		//
		// List<List<Double>> objective = new ArrayList<>();
		// for (int i = 0; i < objectiveA.length; i++) {
		// List<Double> obj = new ArrayList<>();
		//
		// for (int j = 0; j < objectiveA[i].length; j++) {
		// obj.add(objectiveA[i][j]);
		// }
		// objective.add(obj);
		// }

		List<List<Double>> objective = startManager(bc, ONAfactoryScale, numberOfIslands, seeds, notImprovedInARowLimit,
				numberOfReplacement, 5, 5);
		t2 = System.currentTimeMillis() - t1;
		time.add(t2);

		out += "push: " + numberOfPush + " pull: " + numberOfPull + "\n";
		System.out.println("push: " + numberOfPush + " pull: " + numberOfPull);
		int[] actions1 = { numberOfPush, numberOfPull };
		numberOfActions.add(actions1);

		System.out.println("Final PF: ");
		out += "Final PF: \n";
		for (List<Double> l : objective) {
			for (Double d : l) {
				System.out.print(d + " ");
				out += d + " ";
			}
			System.out.println();
			out += "\n";
		}

		ObjectiveCapsule ocLinkage = new ObjectiveCapsule(objective);
		System.out.println("\n\n");

		out += "\n\n\n---------------------------------------\nQuality Indicator Results: \n\n";
		System.out.println("Quality Indicator Results: ");

		out += "for each remove method \n";
		System.out.println("for each remove method \n\n");

		for (int i = 0; i < objCaps.size(); i++) {
			System.out.println(getName(i, -1));
			out += getName(i, -1) + "\n";
			List<List<List<Double>>> objectives = new ArrayList<>();

			for (ObjectiveCapsule oc : objCaps.get(i))
				objectives.add(oc.get());

			String res = Indicators.compareForDisplay(objectives, null);
			System.out.println(res);
			out += res + "\n\n";
			System.out.println("\n");
		}

		out += "\n---------------------------------------\nfor each add method\n";
		System.out.println("for each add method \n\n");
		for (int i = 0; i < objCaps.get(0).size(); i++) {
			System.out.println(getName(-1, i));
			out += getName(-1, i) + "\n";
			List<List<List<Double>>> objectives = new ArrayList<>();

			for (int j = 0; j < objCaps.size(); j++) {
				objectives.add(objCaps.get(j).get(i).get());
			}

			String res = Indicators.compareForDisplay(objectives, null);
			out += res + "\n\n";
			System.out.println(res);
			System.out.println("\n");
		}

		out += "\n---------------------------------------\n results for all together \n\n";
		System.out.println("for all together \n\n");
		List<ObjectiveCapsule> allToGether = new ArrayList<>();
		for (int i = 0; i < objCaps.size(); i++) {
			allToGether.addAll(objCaps.get(i));
		}

		int totalAddMethods = objCaps.get(0).size();
		int removeID = 0;
		int addID = 0;
		System.out.println("Remove nothing   Add nothing");
		out += "Remove nothing   Add nothing\n";
		for (int i = 0; i < allToGether.size(); i++) {
			removeID = i / totalAddMethods;
			addID = i % totalAddMethods;
			System.out.println(getName(removeID, addID));
			out += getName(removeID, addID) + "\n";
		}

		System.out.println("Linkage migration");
		out += "Linkage migration\n";

		List<List<List<Double>>> objectives = new ArrayList<>();
		objectives.add(normalPF.get());
		for (int i = 0; i < allToGether.size(); i++) {
			objectives.add(allToGether.get(i).get());
		}
		objectives.add(ocLinkage.get());

		String res = Indicators.compareForDisplay(objectives, null);
		out += res + "\n";
		System.out.println(res);

		System.out.println("\n---------------------------------------\n actions for all together");
		out += "\n---------------------------------------\n actions for all together \n\n";

		for (int i = 0; i < numberOfActions.size(); i++) {
			int[] action = numberOfActions.get(i);
			System.out.println(action[0] + " " + action[1]);
			out += action[0] + " " + action[1] + "\n";
		}

		ResultAnalyser.writeResult(fileName, out);

		List<List<Double>> result = Indicators.compare(objectives, null);

		List<Double> nopush = new ArrayList<>();
		List<Double> nopull = new ArrayList<>();
		for (int i = 0; i < numberOfActions.size(); i++) {

			nopush.add(numberOfActions.get(i)[0] + 0.0);
			nopull.add(numberOfActions.get(i)[1] + 0.0);
		}
		result.add(nopush);
		result.add(nopull);

		String outForRead = "";

		for (int i = 0; i < result.size(); i++) {
			for (int j = 0; j < result.get(i).size(); j++) {
				if (j < result.get(i).size() - 1)
					outForRead += result.get(i).get(j) + " ";
				else
					outForRead += result.get(i).get(j);
			}

			outForRead += "\n";
		}

		ResultAnalyser.writeResult(fileName + ".txt", outForRead);

		String timeS = "";

		for (int i = 0; i < time.size(); i++) {
			timeS += time.get(i) + "\n";
		}

		ResultAnalyser.writeResult(directory + "/cost.txt", timeS);

		List<Double> timeD = time.stream().map(c -> (double) c).collect(Collectors.toList());

		result.add(timeD);

		return result;
	}

	public static String getName(int remove, int add) {

		if (remove < 0 && add < 0)
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
