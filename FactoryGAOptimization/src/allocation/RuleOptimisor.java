package allocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import metrics.Configuration;

class RuleOptimisor extends Thread {
	WorkloadConsumer consumer;
	int period;
	Random rng;
	List<Long> time = new ArrayList<>();

	boolean shouldFinish = false;
	AllocationSolutions solutions;
	boolean speedUp;
	Configuration config = null;

	public RuleOptimisor(WorkloadConsumer consumer, int period, Random rng, AllocationSolutions solutions,
			boolean speedUp) {
		this.consumer = consumer;
		this.period = period;
		this.rng = rng;
		this.solutions = solutions;
		this.speedUp = speedUp;
	}

	@Override
	public void run() {

		try {
			Thread.sleep(period * 1000);
		} catch (InterruptedException e) {
			System.out.println("Rule optimistor is signalled to finish");
		}

		System.out.println("Rule optimistor is started.");

		while (!shouldFinish) {

			Workload workload = WorkloadConsumer.deepCopyWorkload(AllocationFactory.feeder.workload);

			long start = System.currentTimeMillis();

			Configuration template = config == null ? workload.getRullConfig() : config;

			RuleOptimisationResult opRes = solutions.ruleOptimisation(template,
					workload.getObjectiveFunction(true, speedUp), workload.getTasks(), workload.getResources(), rng);

			config = opRes.externalPops.get(opRes.bestIndex);

			for (int i = 0; i < opRes.paretoFrontResults.size(); i++) {
				consumer.setLs(opRes.paretoFrontResults.get(i).get(0), opRes.paretoFrontResults.get(i).get(1));
			}

			time.add(System.currentTimeMillis() - start);

			try {
				Thread.sleep(period * 1000);
			} catch (InterruptedException e) {
				System.out.println("Rule optimistor is signalled to finish");
			}
		}

		System.err.println("Rule optimistor is finished.");

	}

	public void runStatic(Workload load) {
		Workload workload = WorkloadConsumer.deepCopyWorkload(load);

		long start = System.currentTimeMillis();

		Configuration template = config == null ? workload.getRullConfig() : config;

		RuleOptimisationResult opRes = solutions.ruleOptimisation(template,
				workload.getObjectiveFunction(true, speedUp), workload.getTasks(), workload.getResources(), rng);

		config = opRes.externalPops.get(opRes.bestIndex);

		for (int i = 0; i < opRes.paretoFrontResults.size(); i++) {
			consumer.setLs(opRes.paretoFrontResults.get(i).get(0), opRes.paretoFrontResults.get(i).get(1));
		}
		time.add(System.currentTimeMillis() - start);
	}

	public void singalToFinish() {

		shouldFinish = true;
		this.interrupt();

		System.err.println("Rule optimistor is signalled to finish.");
	}
}

class RuleOptimisationResult {

	List<List<Integer>> paretoFrontResults;

	List<Integer> bestMakespanResults;

	List<Configuration> externalPops;

	int bestIndex;
	List<List<Double>> objectives;
	Long time;

	public RuleOptimisationResult(List<Integer> results, List<Configuration> externalPops, int bestIndex,
			List<List<Integer>> paretoFrontResults, List<List<Double>> objectives, Long time) {
		this.bestMakespanResults = results;
		this.paretoFrontResults = paretoFrontResults;
		this.externalPops = externalPops;
		this.bestIndex = bestIndex;
		this.objectives = objectives;
		this.time = time;
	}

}