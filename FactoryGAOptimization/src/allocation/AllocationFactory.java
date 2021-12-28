package allocation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AllocationFactory {

	// public static Workload load;
	public static int seed = 1000;
	public static int numberOfParts = 20;
	public static int partScale = 3;
	public static int machineScale = 1;
	public static int workloadPeriod = 10;

	public static int consumerNumber = 6;
	public static boolean speedUp = true;
	public static int totalTime = 100;

	static WorkloadFeeder feeder;

	static List<Workload> workloads;
	static List<List<AllocationResult>> outputResults;

	public static void main(String args[]) {
		// for (int i = 0; i < 50; i++) {
		executeFactory();
		// }
	}

	public static void executeFactory() {
		workloads = new ArrayList<>();
		outputResults = new ArrayList<>();

		for (int i = 0; i < consumerNumber; i++) {
			outputResults.add(new ArrayList<>());
		}

		List<WorkloadConsumer> consumers = new ArrayList<WorkloadConsumer>();

		for (int i = 0; i < consumerNumber; i++) {
			WorkloadConsumer worker = new WorkloadConsumer(i, seed, 10, speedUp);
			worker.start();
			consumers.add(worker);
		}

		feeder = new WorkloadFeeder(numberOfParts, partScale, machineScale, workloadPeriod, consumers, seed);
		feeder.start();

		while (feeder.time <= totalTime) {
			try {
				System.err.println("System time: " + feeder.time);
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
		}

		System.out.println("Time has passed");

		feeder.signalToFinish();
		for (int i = 0; i < consumers.size(); i++) {
			consumers.get(i).singalToFinish();
		}

		try {
			feeder.join();
			for (WorkloadConsumer con : consumers)
				con.join();
			consumers.get(4).op.join();
		} catch (InterruptedException e) {
		}

		List<List<AllocationResult>> resForData = new ArrayList<>();

		for (int i = 0; i < outputResults.size(); i++) {

			List<AllocationResult> feededRes = new ArrayList<>();

			for (int j = 0; j < totalTime / workloadPeriod + 1; j++) {
				feededRes.add(null);
			}

			for (int j = 0; j < outputResults.get(i).size(); j++) {
				AllocationResult res = outputResults.get(i).get(j);
				feededRes.set(res.workID, res);
			}

			resForData.add(feededRes);
		}

		System.out.println("That's it. We are finished now.");

		System.out.println("Feeder produced " + workloads.size() + " workloads.");
		System.out.println("IDs: " + workloads.stream().map(w -> w.getID()).collect(Collectors.toList()));

		System.out.println("and for each consumer: ");
		for (int i = 0; i < resForData.size(); i++) {
			System.out.println("For consumer " + i + ":" + consumers.get(i).name + ", it produced results for");

			for (int j = 0; j < resForData.get(i).size(); j++) {
				AllocationResult res = resForData.get(i).get(j);
				// System.out.println("work: " + res.workID + " time: " + res.comptuationTime);
				if (res == null) {
					System.out.println("workload " + j + " no");
				} else {
					System.out.println("work " + j + " best: " + res.objectives.get(0).get(0) + " "
							+ res.objectives.get(0).get(1));
				}

				System.out.println("\n\n");
			}
		}

		System.out.println("For data");

		for (int i = 0; i < resForData.size(); i++) {
			System.out.println("consumer: " + consumers.get(i).name);

			for (int j = 0; j < resForData.get(i).size(); j++) {
				AllocationResult res = resForData.get(i).get(j);
				if (res == null) {
					System.out.println("workload " + j + " no");
				} else {
					System.out.println("work " + j + " best: " + res.objectives.get(0).get(0) + " "
							+ res.objectives.get(0).get(1));
					// if (i == 4 && res.objectives.get(0).get(0) > 50000)
					// System.out.println("!!! work: " + j + " best: " + res.objectives.get(0) + ",
					// ls number: "
					// + res.ls.size() + " list: " + res.ls);
					// else {
					// if (i == 4) {
					// System.out.println("work: " + j + " best: " + res.objectives.get(0) + ", ls
					// number: "
					// + res.ls.size() + " list: " + res.ls);
					// }
					// else {
					// System.out.println("work: " + j + " best: " + res.objectives.get(0));
					// }
					// }
				}

			}
			System.out.println();
		}

	}

}
