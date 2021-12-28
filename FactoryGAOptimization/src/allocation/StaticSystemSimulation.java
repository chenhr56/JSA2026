package allocation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import indicator.Indicators;

public final class StaticSystemSimulation {

	public static void main(String[] args) {
		boolean isSpeedUp = true;
		int numberOfWork = 1000;

		Random rng = new Random(1000);
		WorkloadFeeder feeder = new WorkloadFeeder(20, 3, 1, 10, null, 1000);

		List<Workload> works = new ArrayList<>();
		for (int i = 0; i < numberOfWork; i++) {
			Workload load = feeder.generate(rng, 0);
			works.add(load);
		}

		StaticSystemSimulation d = new StaticSystemSimulation();
		WorkloadConsumer ruleAlloc = new WorkloadConsumer(4, 1000, 30, isSpeedUp);

		List<List<List<List<Double>>>> PFs = new ArrayList<>();

		for (int i = 0; i < 7; i++) {
			PFs.add(new ArrayList<>());
		}

		PFs.set(0, d.test(0, "Fit Worst" + numberOfWork, works, rng, isSpeedUp));
		PFs.set(1, d.test(1, "Fit Best" + numberOfWork, works, rng, isSpeedUp));
		PFs.set(2, d.test(2, "Fit First" + numberOfWork, works, rng, isSpeedUp));
		PFs.set(3, d.test(3, "Fit Next" + numberOfWork, works, rng, isSpeedUp));
		PFs.set(4, d.test(4, "Optimisation Rule" + numberOfWork, works, rng, isSpeedUp));
		PFs.set(5, d.testRuleAlloc(ruleAlloc, "Optimisation Rule Allocation" + numberOfWork, works));
		PFs.set(6, d.test(6, "Optimisation Full" + numberOfWork, works, rng, isSpeedUp));

		List<List<List<List<Double>>>> PFsSorted = new ArrayList<>();

		for (int i = 0; i < PFs.get(0).size(); i++) {
			List<List<List<Double>>> PFforOneWork = new ArrayList<>();
			for (int j = 0; j < PFs.size(); j++) {
				try {
					PFforOneWork.add(PFs.get(j).get(i));
				} catch (Exception e) {
				}

			}
			PFsSorted.add(PFforOneWork);
		}

		List<List<List<Double>>> values = new ArrayList<>();
		System.out.println("\n\n Comparator: ");
		for (int i = 0; i < PFsSorted.size(); i++) {
			String res = Indicators.compareForDisplay(PFsSorted.get(i),null) + "\n";
			values.add(Indicators.compare(PFsSorted.get(i),null));
			System.out.println("\n" + res + "\n");
		}

		String res = "Comparator: \n";
		for (int j = 0; j < values.get(0).size(); j++) {

			for (int i = 0; i < values.size(); i++) {
				List<Double> value = values.get(i).get(j);
				res += "QI" + (j + 1) + ":";
				for (int k = 0; k < value.size(); k++) {
					res += " " + value.get(k);
				}
				res += "\n";
			}
		}
		System.out.println("comparator for data: ");
		System.out.println(res);
		writeSystem("one run" + numberOfWork, res);

	}

	public List<List<List<Double>>> testRuleAlloc(WorkloadConsumer ruleAlloc, String name, List<Workload> works) {
		List<Double> executionTimes = new ArrayList<>();
		List<List<List<Double>>> paretos = new ArrayList<>();

		for (int i = 0; i < works.size(); i++) {

			AllocationResult allocRes = ruleAlloc.runStatic(works.get(i), i);

			List<List<Double>> tasksComplete = allocRes.objectives;
			long time = allocRes.comptuationTime;

			System.out.println("execute:" + i + " our time: " + (time / 1000.0) + " seconds");
			executionTimes.add(time / 1000.0);


			List<List<Double>> PF = new ArrayList<>();
			for (int j = 0; j < tasksComplete.size(); j++) {
				List<Double> pf = new ArrayList<>();
				pf.add(tasksComplete.get(j).get(0) / 10.0);
				pf.add(tasksComplete.get(j).get(1));
				PF.add(pf);
			}

			paretos.add(PF);

			for (int j = 0; j < PF.size(); j++) {
				System.out.println(PF.get(j));
			}
			System.out.println("\n\n");
			// System.out.println("Complete: " + tasksComplete);
		}

		String res = "Execution times in seconds \n";
		System.out.println("Summary: ");
		System.out.println("Execution time");
		for (int i = 0; i < executionTimes.size(); i++) {
			System.out.println(executionTimes.get(i));
			res += executionTimes.get(i) + "\n";
		}

		res += "\n\n Best makespans \n";
		System.out.println("Best makespan");
		for (int i = 0; i < paretos.size(); i++) {
			System.out.println(paretos.get(i).get(0).get(0) + " " + paretos.get(i).get(0).get(1));
			List<List<Double>> onePF = paretos.get(i);
			res += onePF.get(0).get(0) + " " + onePF.get(0).get(1) + "\n";
		}

		res += "\n\n Execution Performance \n";
		System.out.println("Pareto fronts");
		for (int i = 0; i < paretos.size(); i++) {
			System.out.println(paretos.get(i));
			List<List<Double>> onePF = paretos.get(i);
			for (int j = 0; j < onePF.size(); j++) {
				res += onePF.get(j).get(0) + " " + onePF.get(j).get(1) + "\n";
			}
			res += "\n";
		}

		writeSystem(name, res);

		return paretos;
	}

	public List<List<List<Double>>> test(int allocation, String name, List<Workload> works, Random rng,
			boolean isSpeedUp) {
		List<Double> executionTimes = new ArrayList<>();
		List<List<List<Double>>> paretos = new ArrayList<>();

		for (int i = 0; i < works.size(); i++) {
			Workload load = WorkloadConsumer.deepCopyWorkload(works.get(i));

			AllocationSolutions alloc = new AllocationSolutions();

			Pair<List<List<Double>>, Long> result = alloc.allocate(allocation, load, isSpeedUp, rng);

			List<List<Double>> tasksComplete = result.getLeft();
			long time = result.getRight();

			System.out.println("execute:" + i + " our time: " + (time / 1000.0) + " seconds");
			executionTimes.add(time / 1000.0);


			List<List<Double>> PF = new ArrayList<>();
			for (int j = 0; j < tasksComplete.size(); j++) {
				List<Double> pf = new ArrayList<>();
				pf.add(tasksComplete.get(j).get(0) / 10.0);
				pf.add(tasksComplete.get(j).get(1));
				PF.add(pf);
			}

			paretos.add(PF);

			for (int j = 0; j < PF.size(); j++) {
				System.out.println(PF.get(j));
			}
			System.out.println("\n\n");
			// System.out.println("Complete: " + tasksComplete);
		}

		String res = "Execution times in seconds \n";
		System.out.println("Summary: ");
		System.out.println("Execution time");
		for (int i = 0; i < executionTimes.size(); i++) {
			System.out.println(executionTimes.get(i));
			res += executionTimes.get(i) + "\n";
		}

		res += "\n\n Best makespans \n";
		System.out.println("Best makespan");
		for (int i = 0; i < paretos.size(); i++) {
			System.out.println(paretos.get(i).get(0).get(0) + " " + paretos.get(i).get(0).get(1));
			List<List<Double>> onePF = paretos.get(i);
			res += onePF.get(0).get(0) + " " + onePF.get(0).get(1) + "\n";
		}

		res += "\n\n Execution Performance \n";
		System.out.println("Pareto fronts");
		for (int i = 0; i < paretos.size(); i++) {
			System.out.println(paretos.get(i));
			List<List<Double>> onePF = paretos.get(i);
			for (int j = 0; j < onePF.size(); j++) {
				res += onePF.get(j).get(0) + " " + onePF.get(j).get(1) + "\n";
			}
			res += "\n";
		}

		writeSystem(name, res);

		return paretos;
	}

	public void begin() {
		Random rng = new Random(1000);
		WorkloadFeeder feeder = new WorkloadFeeder(20, 3, 1, 10, null, 1000);
		Workload load = feeder.generate(rng, 0);
		boolean isSpeedUp = true;

		AllocationSolutions alloc = new AllocationSolutions();

		List<List<Double>> tasksWF = alloc.worstFit(load.getTasks(), load.getResources(), load.getConfig(),
				load.getObjectiveFunction(false, isSpeedUp)).getLeft();
		System.out.println("WF: " + tasksWF);

		List<List<Double>> tasksBF = alloc.bestFit(load.getTasks(), load.getResources(), load.getMaxUtilPerCore(),
				load.getConfig(), load.getObjectiveFunction(false, isSpeedUp)).getLeft();
		System.out.println("BF: " + tasksBF);

		List<List<Double>> tasksFF = alloc.firstFit(load.getTasks(), load.getResources(), load.getMaxUtilPerCore(),
				load.getConfig(), load.getObjectiveFunction(false, isSpeedUp)).getLeft();
		System.out.println("FF: " + tasksFF);

		List<List<Double>> tasksNF = alloc.nextFit(load.getTasks(), load.getResources(), load.getMaxUtilPerCore(),
				load.getConfig(), load.getObjectiveFunction(false, isSpeedUp)).getLeft();
		System.out.println("NF: " + tasksNF);

		List<Task> t = new ArrayList<>(load.getTasks());
		t.sort((c1, c2) -> Double.compare(c1.maxUtil, c2.maxUtil));

		int firstSize = t.size() / 3 - 1;
		int l1 = t.get(firstSize - 1).computations.get(t.get(firstSize - 1).computations.size() - 1);
		int l2 = t.get(firstSize * 2 - 1).computations.get(t.get(firstSize * 2 - 1).computations.size() - 1);
		List<Integer> ls = new ArrayList<>();
		ls.add(l1);
		ls.add(l2);
		List<List<Integer>> cls = new ArrayList<>();
		cls.add(ls);
		List<List<Double>> tasksFirstRule = alloc
				.ruleAllocation(cls, load.getObjectiveFunction(true, isSpeedUp), load.getTasks()).getLeft();
		System.out.println("First Rule Allocation: " + tasksFirstRule);

		RuleOptimisationResult tasksRule = alloc.ruleOptimisation(load.getRullConfig(),
				load.getObjectiveFunction(true, isSpeedUp), load.getTasks(), load.getResources(), rng);

		List<List<Integer>> rules = new ArrayList<>();
		rules.add(tasksRule.bestMakespanResults);

		List<List<Double>> tasksRuleResults = alloc
				.ruleAllocation(rules, load.getObjectiveFunction(true, isSpeedUp), load.getTasks()).getLeft();
		System.out.println("Rule Optimisation: " + tasksRuleResults);

		List<List<Double>> tasksComplete = alloc
				.deepOptimisation(load.getConfig(), load.getObjectiveFunction(false, isSpeedUp), rng).getLeft();
		System.out.println("Complete: " + tasksComplete);

		List<List<Double>> taskRandom = alloc
				.randomAllocation(load.getConfig(), load.getObjectiveFunction(false, isSpeedUp), rng).getLeft();
		System.out.println("Random: " + taskRandom);

		jeep.lang.Diag.println("All done.");
	}

	public static void writeSystem(String filename, String result) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File("resultDate20/" + filename + ".txt"), false));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writer.println(result);
		writer.close();
	}

}

// End ///////////////////////////////////////////////////////////////