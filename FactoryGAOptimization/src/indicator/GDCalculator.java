package indicator;

import java.util.ArrayList;
import java.util.List;

public class GDCalculator {

	public double[] applyGD(List<List<List<Double>>> solutionSets) {

		List<Double> results = new ArrayList<>();

		for (List<List<Double>> set : solutionSets) {
			results.add(getGD(set));
		}

//		System.out.println("\nGD Indicator, the lower the better.");

		double[] out = new double[results.size()];
		for (int i = 0; i < results.size(); i++) {
//			System.out.println(Indicators.df.format(results.get(i)));
			out[i] = Double.parseDouble(Indicators.df.format(results.get(i)));
		}

		return out;
	}

	public double[] applyD1R(List<List<List<Double>>> solutionSets) {
		List<Double> results = new ArrayList<>();

		for (List<List<Double>> set : solutionSets) {
			results.add(getD1R(set));
		}

//		System.out.println("\nD1R Indicator, the lower the better.");

		double[] out = new double[results.size()];
		for (int i = 0; i < results.size(); i++) {
//			System.out.println(Indicators.df.format(results.get(i)));
			out[i] = Double.parseDouble(Indicators.df.format(results.get(i)));
		}

		return out;
	}

	private double getD1R(List<List<Double>> solutionSet) {

		List<Double> sumOfD = new ArrayList<>();
		for (List<Double> solution : solutionSet) {
			sumOfD.add(getDxy(solution));
		}

		sumOfD.sort((c1, c2) -> Double.compare(c1, c2));
		return sumOfD.get(0);
	}

	private double getGD(List<List<Double>> solutionSet) {

		double sumOfD = 0;
		for (List<Double> solution : solutionSet) {
			sumOfD += getDxy(solution);
		}
		return (1.0 / (double) solutionSet.size()) * sumOfD;
	}

	private double getDxy(List<Double> solution) {
		double forsqrt = 0;
		for (Double d : solution) {
			forsqrt += d * d;
		}
		return Math.sqrt(forsqrt);
	}

}
