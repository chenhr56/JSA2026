package indicator;

import java.util.ArrayList;
import java.util.List;

public class DCICaculator {

	int DIVISION = 10;
	double[] idealPoints;
	double[] nadirPoints;

	void clean(List<List<Double>> PFSets, List<Double> lowerBound, List<Double> UpperBound) {
		for (int j = 0; j < PFSets.size(); j++) {
			for (int i = 0; i < PFSets.get(j).size(); i++) {
				if (PFSets.get(j).get(i) > UpperBound.get(i) || PFSets.get(j).get(i) < lowerBound.get(i)) {
					PFSets.remove(j);
					j--;
					break;
				}
			}
		}
	}

	public double[] apply(List<List<List<Double>>> solutionSets) {
		List<Double> upperBound = new ArrayList<Double>();
		List<Double> lowerBound = new ArrayList<Double>();
		List<Double> hyperBoxSize = new ArrayList<Double>();

		idealPoints = new double[solutionSets.get(0).get(0).size()];
		for (int i = 0; i < idealPoints.length; i++)
			idealPoints[i] = 999999999;
		nadirPoints = new double[solutionSets.get(0).get(0).size()];

		for (List<List<Double>> oneSet : solutionSets) {

			for (int j = 0; j < oneSet.get(0).size(); j++) {
				for (int i = 0; i < oneSet.size(); i++) {
					if (oneSet.get(i).get(j) < idealPoints[j])
						idealPoints[j] = oneSet.get(i).get(j);
					if (oneSet.get(i).get(j) > nadirPoints[j])
						nadirPoints[j] = oneSet.get(i).get(j);
				}
			}
		}

		for (int i = 0; i < idealPoints.length; i++) {
			lowerBound.add(idealPoints[i]);
			upperBound.add(nadirPoints[i] + (nadirPoints[i] - idealPoints[i]) / (DIVISION * 2));
			hyperBoxSize
					.add(Double.parseDouble(Indicators.df.format((upperBound.get(i) - lowerBound.get(i)) / DIVISION)));
		}

		List<List<Double>> mixedPFs = new ArrayList<>();
		List<List<List<Double>>> Ps = new ArrayList<>();
		for (List<List<Double>> set : solutionSets) {
			clean(set, lowerBound, upperBound);
			mixedPFs.addAll(set);
			Ps.add(set);
		}

		List<List<Double>> nonDominatedSolutions = new ArrayList<List<Double>>();
		for (List<Double> solution : mixedPFs) {
			updateNondominateSet(nonDominatedSolutions, solution);
		}

		// System.out.println(nonDominatedSolutions.toString());

		List<List<Double>> contributions = new ArrayList<List<Double>>();

		for (int i = 0; i < Ps.size(); i++) {
			contributions.add(new ArrayList<>());
		}

		assert (contributions.size() == Ps.size());

		for (List<Double> h : nonDominatedSolutions) {
			for (int i = 0; i < Ps.size(); i++) {
				List<List<Double>> tem = Ps.get(i);

				contributions.get(i).add(getContributionDegree(tem, h));
			}
		}

		double[] DCI = new double[Ps.size()];
		int S = nonDominatedSolutions.size();

		assert (contributions.get(0).size() == S);
		assert (contributions.get(1).size() == S);
		assert (contributions.get(2).size() == S);

		for (int i = 0; i < Ps.size(); i++) {
			double sum = 0;
			for (int j = 0; j < S; j++) {
				sum += contributions.get(i).get(j);
			}

			DCI[i] = Double.parseDouble(Indicators.df.format(((double) 1 / (double) S) * ((double) sum)));
		}

//		System.out.println("DCI Indicator, the higher the better.");
//		for (Double res : DCI) {
//			System.out.println(res);
//		}

		return DCI;

	}

	boolean dominate(List<Double> individual1, List<Double> individual2) {
		boolean isDominate = false;

		for (int i = 0; i < individual1.size(); i++) {
			if (individual1.get(i) > individual2.get(i))
				return false;
			if (individual1.get(i) <= individual2.get(i))
				isDominate = true;
		}

		return isDominate;
	}

	void updateNondominateSet(List<List<Double>> nonDominatedSet, List<Double> candidate) {

		if (nonDominatedSet.size() == 0)
			nonDominatedSet.add(candidate);
		else {
			boolean eligibleToJoin = true;

			for (int i = 0; i < nonDominatedSet.size(); i++) {
				List<Double> member = nonDominatedSet.get(i);
				if (dominate(candidate, member)) {
					/* the candidate dominates a member */
					nonDominatedSet.remove(i);
					i--;
				} else if (dominate(member, candidate))
					/* the candidate is dominated by a member */
					eligibleToJoin = false;
			}

			if (eligibleToJoin) {
				nonDominatedSet.add(candidate);
			}
		}

	}

	List<Double> GetHyperBoxCoordinate(List<Double> solution, List<Double> lowerBound, List<Double> hyperBoxSize) {
		List<Double> coordinate = new ArrayList<Double>();

		for (int i = 0; i < idealPoints.length; i++) {
			coordinate.add(Double.parseDouble(
					Indicators.df.format(Math.floor((solution.get(i) - lowerBound.get(i)) / hyperBoxSize.get(i)))));
		}

		return coordinate;
	}

	double getGridDistance(List<Double> coordinate1, List<Double> coordinate2) {
		double sum = 0;

		for (int i = 0; i < idealPoints.length; i++) {
			sum += (coordinate1.get(i) - coordinate2.get(i)) * (coordinate1.get(i) - coordinate2.get(i));
		}

		return Double.parseDouble(Indicators.df.format(Math.sqrt(sum)));

	}

	double getApproximationtoHyperBoxDistance(List<List<Double>> arrayList, List<Double> box) {

		double distance = Double.POSITIVE_INFINITY;

		for (List<Double> sol : arrayList) {
			distance = Double.min(distance, getGridDistance(box, sol));
		}

		return distance;

	}

	double getContributionDegree(List<List<Double>> arrayList, List<Double> box) {
		double condition = Double.parseDouble(Indicators.df.format(Math.sqrt(idealPoints.length + 1)));
		double apprxToBox = getApproximationtoHyperBoxDistance(arrayList, box);

		if (apprxToBox < condition)
			return Double.parseDouble(Indicators.df.format(1 - apprxToBox * apprxToBox / (idealPoints.length + 1)));
		else
			return 0;

	}

}
