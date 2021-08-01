package indicator;

import java.util.ArrayList;
import java.util.List;

public class HyperVolume {

	public double[] applyHV(List<List<List<Double>>> solutionSets) {

		for (List<List<Double>> s : solutionSets) {
			s.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));
		}

		List<List<Double>> all = new ArrayList<>();
		for (List<List<Double>> set : solutionSets) {
			for (List<Double> one : set) {
				all.add(one);
			}
		}

		List<List<Double>> transfered = new ArrayList<>();

		for (int i = 0; i < all.get(0).size(); i++) {
			List<Double> oneObj = new ArrayList<>();
			for (int j = 0; j < all.size(); j++) {
				double value = all.get(j).get(i);
				oneObj.add(value);
			}
			transfered.add(oneObj);
		}

		List<Double> reference = new ArrayList<>();
		for (int i = 0; i < transfered.size(); i++) {
			double max = transfered.get(i).stream().mapToDouble(c -> c).max().getAsDouble();
			reference.add(max + 1);
		}

		List<Double> resultsRaw = new ArrayList<>();

		for (List<List<Double>> set : solutionSets) {
			resultsRaw.add(volumeForOneSet(set, reference));
		}

		double max = resultsRaw.stream().mapToDouble(c -> c).max().getAsDouble();

		List<Double> results = new ArrayList<>();

		for (int i = 0; i < resultsRaw.size(); i++) {
			results.add(resultsRaw.get(i) / max);
		}

		double[] out = new double[resultsRaw.size()];
		for (int i = 0; i < results.size(); i++) {
			// System.out.println(Indicators.df.format(results.get(i)));
			out[i] = Double.parseDouble(Indicators.df.format(results.get(i)));
		}

		return out;
	}

	private double volumeForOneSet(List<List<Double>> set, List<Double> reference) {
		double out = (reference.get(0) - set.get(0).get(0)) * (reference.get(1) - set.get(0).get(1));

		for (int i = 1; i < set.size(); i++) {
			out += (reference.get(0) - set.get(i).get(0)) * (set.get(i - 1).get(1) - set.get(i).get(1));
		}

		return out;
	}

}
