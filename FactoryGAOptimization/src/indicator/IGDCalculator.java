package indicator;

import java.util.ArrayList;
import java.util.List;

public class IGDCalculator {

	public double[] applyIGD(List<List<List<Double>>> solutionSets, List<List<Double>> globalPF) {

		for (List<List<Double>> s : solutionSets) {
			s.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));
		}

		List<List<Double>> reference = globalPF == null ? getReferenceFP(solutionSets) : new ArrayList<>(globalPF);

		reference.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));

		List<Double> results = new ArrayList<>();

		for (List<List<Double>> set : solutionSets) {
			results.add(getIGD(set, reference));
		}

		// System.out.println("\nGD Indicator, the lower the better.");

		double[] out = new double[results.size()];
		for (int i = 0; i < results.size(); i++) {
			// System.out.println(Indicators.df.format(results.get(i)));
			out[i] = Double.parseDouble(Indicators.df.format(results.get(i)));
		}

		return out;
	}

	private List<List<Double>> getReferenceFP(List<List<List<Double>>> solutionSets) {
		List<List<Double>> reference = new ArrayList<>();
		List<List<Double>> allSets = new ArrayList<>();

		for (int i = 0; i < solutionSets.size(); i++) {
			allSets.addAll(solutionSets.get(i));
		}

		for (int i = 0; i < allSets.size() - 1; i++) {
			updateExternalPopulation(reference, allSets.get(i));
		}

		return reference;
	}

	void updateExternalPopulation(List<List<Double>> reference, List<Double> candidate) {

		if (reference.size() == 0)
			reference.add(candidate);
		else {
			boolean eligibleToJoin = true;

			/*
			 * remove the members that are dominated by the candidate and check the
			 * eligibility of the candidate.
			 */
			for (int i = 0; i < reference.size(); i++) {
				List<Double> member = reference.get(i);
				if (dominate(candidate, member)) {
					/* the candidate dominates a member */
					reference.remove(i);
					i--;
				} else if (dominate(member, candidate))
					/* the candidate is dominated by a member */
					eligibleToJoin = false;
			}

			if (eligibleToJoin) {
				reference.add(candidate);
			}
		}

	}

	boolean dominate(List<Double> individual1, List<Double> individual2) {
		boolean isDominate = false;

		if (individual1.size() != individual2.size()) {
			System.out.println("error");
		}

		for (int i = 0; i < individual1.size(); i++) {
			if (individual1.get(i) > individual2.get(i))
				return false;
			if (individual1.get(i) <= individual2.get(i))
				isDominate = true;
		}

		return isDominate;
	}

	private double getIGD(List<List<Double>> solutionSet, List<List<Double>> reference) {

		double sumOfD = 0;
		// for (List<Double> solution : solutionSet) {
		// sumOfD += getDxy(solution, reference);
		// }

		for (List<Double> z : reference) {
			sumOfD += getDxy(solutionSet, z);
		}

		return sumOfD / (double) reference.size();
	}

	private double getDxy(List<List<Double>> solution, List<Double> z) {

		List<Double> distance = new ArrayList<>();

		for (List<Double> s : solution) {

			if (s.size() != z.size()) {
				System.err.println("Error in IGD");
				System.exit(-1);
			}

			double dis = 0;
			for (int i = 0; i < s.size(); i++) {
				dis += (z.get(i) - s.get(i)) * (z.get(i) - s.get(i));
			}

			dis = Math.sqrt(dis);
			distance.add(dis);
		}

		double forsqrt = distance.stream().mapToDouble(c -> c).min().getAsDouble();
		return forsqrt;
	}

	// private double getDxy(List<Double> solution, List<List<Double>> reference) {
	//
	// double forsqrt = 0;
	//
	// for(List<Double> ref: reference) {
	// if(ref.size() != solution.size()) {
	// System.err.println("Error in IGD");
	// System.exit(-1);
	// }
	//
	// double oneRef = 0;
	// for(int i=0; i<ref.size();i++) {
	// oneRef += (ref.get(i) - solution.get(i)) * (ref.get(i) - solution.get(i));
	//
	// }
	//
	// forsqrt+= Math.sqrt(oneRef);
	// }
	//
	// return forsqrt / (double) reference.size();
	// }

}
