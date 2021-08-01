package indicator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Indicators {

	public static final int NoQI = 5;
	public static final String[] nameQI = { "GD", "DIR", "DCI", "IGD", "HV" };
	public static final boolean[] dirQI = { false, false, true, false, true };
	public static final DecimalFormat df = new DecimalFormat("0.000");

	public static void main(String args[]) {
		List<List<List<Double>>> sets = new ArrayList<>();

		List<List<Double>> set = new ArrayList<>();
		for (int j = 0; j < 5; j++) {
			List<Double> one = new ArrayList<>();
			one.add((double) (j + 1));
			one.add((double) (5 - j));
			set.add(one);
		}
		sets.add(set);

		List<List<Double>> set1 = new ArrayList<>();
		for (int j = 0; j < 5; j++) {
			List<Double> one = new ArrayList<>();
			one.add((double) (j + 0.5));
			one.add((double) (5.5 - j));
			set1.add(one);
		}
		sets.add(set1);

		System.out.println(sets);

		double[] out = new HyperVolume().applyHV(sets);
		for (double d : out) {
			System.out.print(d + " ");
		}

	}

	public static String compareForDisplay(List<List<List<Double>>> solutionSets, List<List<Double>> globalPF) {
		String out = "\n\n GD: \n";
		double[] result = new GDCalculator().applyGD(solutionSets);
		for (double d : result) {
			out += d + "\n";
		}

		out += "\n\n D1R: \n";
		result = new GDCalculator().applyD1R(solutionSets);
		for (double d : result) {
			out += d + "\n";
		}

		out += "DCI: \n";
		result = new DCICaculator().apply(solutionSets);
		for (double d : result) {
			out += d + "\n";
		}

		out += "\n\n IGD: \n";
		result = new IGDCalculator().applyIGD(solutionSets, globalPF);
		for (double d : result) {
			out += d + "\n";
		}

		out += "\n\n HV: \n";
		result = new HyperVolume().applyHV(solutionSets);
		for (double d : result) {
			out += d + "\n";
		}

		return out;
	}

	public static List<List<Double>> compare(List<List<List<Double>>> solutionSets, List<List<Double>> globalPF) {
		List<List<Double>> res = new ArrayList<List<Double>>();

		double[] result = new GDCalculator().applyGD(solutionSets);
		List<Double> gd = new ArrayList<>();
		for (int i = 0; i < result.length; i++)
			gd.add(result[i]);
		res.add(gd);

		result = new GDCalculator().applyD1R(solutionSets);
		List<Double> d1r = new ArrayList<>();
		for (int i = 0; i < result.length; i++)
			d1r.add(result[i]);
		res.add(d1r);

		result = new DCICaculator().apply(solutionSets);
		List<Double> dci = new ArrayList<>();
		for (int i = 0; i < result.length; i++)
			dci.add(result[i]);
		res.add(dci);

		result = new IGDCalculator().applyIGD(solutionSets, globalPF);
		List<Double> igd = new ArrayList<>();
		for (int i = 0; i < result.length; i++)
			igd.add(result[i]);
		res.add(igd);

		result = new HyperVolume().applyHV(solutionSets);
		List<Double> hv = new ArrayList<>();
		for (int i = 0; i < result.length; i++)
			hv.add(result[i]);
		res.add(hv);

		return res;
	}

}
