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

public final class OneRun {

	public static void main(String[] args) {
		boolean isSpeedUp = true;

		Random rng = new Random(1000);
		WorkloadFeeder feeder = new WorkloadFeeder(20, 3, 1, 10, null, 1000);

		List<Workload> works = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Workload load = feeder.generate(rng, i);
			works.add(load);
		}

		OneRun d = new OneRun();

		List<List<List<List<Double>>>> PFs = new ArrayList<>();

//		PFs.add(d.test(0, "Fit Worst", works, rng, isSpeedUp));
//		PFs.add(d.test(1, "Fit Best", works, rng, isSpeedUp));
//		PFs.add(d.test(2, "Fit First", works, rng, isSpeedUp));
//		PFs.add(d.test(3, "Fit Next", works, rng, isSpeedUp));
		PFs.add(d.test(4, "Optimisation Rule", works, rng, isSpeedUp));
//		PFs.add(d.test(6, "Optimisation Full", works, rng, isSpeedUp));
//		PFs.add(d.test(5, "Allocation Rule", works, rng, isSpeedUp));
//		PFs.add(d.test(7, "Optimisation Random", works, rng, isSpeedUp));

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
			String res = Indicators.compareForDisplay(PFsSorted.get(i), null) + "\n";
			values.add(Indicators.compare(PFsSorted.get(i), null));
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
		writeSystem("one run", res);

	}

	public List<List<List<Double>>> test(int allocation, String name, List<Workload> works, Random rng,
			boolean isSpeedUp) {
		List<Double> executionTimes = new ArrayList<>();
		List<List<List<Double>>> paretos = new ArrayList<>();

		for (int i = 0; i < works.size(); i++) {
			// TODO: we have a better way for deep copy now. Use that.
			Workload load = WorkloadConsumer.deepCopyWorkload(works.get(i));

			AllocationSolutions alloc = new AllocationSolutions();

			Pair<List<List<Double>>, Long> result = alloc.allocate(allocation, load, isSpeedUp, rng);

			List<List<Double>> tasksComplete = result.getLeft();
			long time = result.getRight();

			System.out.println("execute:" + i + " our time: " + (time / 1000.0) + " seconds");
			executionTimes.add(time / 1000.0);
			paretos.add(tasksComplete);

			for (int j = 0; j < tasksComplete.size(); j++) {
				System.out.println(tasksComplete.get(j));
			}
			System.out.println("\n\n");
			// System.out.println("Complete: " + tasksComplete);
		}

		String res = "Execution times in seconds \n";
		System.out.println("Summary: ");
		for (int i = 0; i < executionTimes.size(); i++) {
			System.out.println(executionTimes.get(i));
			res += executionTimes.get(i) + "\n";
		}

		res += "\n\n Best makespans \n";

		for (int i = 0; i < paretos.size(); i++) {
			System.out.println(paretos.get(i));
			List<List<Double>> onePF = paretos.get(i);
			res += onePF.get(0).get(0) + " " + onePF.get(0).get(1) + "\n";
		}

		res += "\n\n Execution Performance \n";

		for (int i = 0; i < paretos.size(); i++) {
			System.out.println(paretos.get(i));
			List<List<Double>> onePF = paretos.get(i);
			for (int j = 0; j < onePF.size(); j++) {
				res += onePF.get(j).get(0) + " " + onePF.get(j).get(1) + "\n";
			}
			res += "\n";
		}

		writeSystem(name + " execution", res);

		return paretos;
	}

	public void testRule() {

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