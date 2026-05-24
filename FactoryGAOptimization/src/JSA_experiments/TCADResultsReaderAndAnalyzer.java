package JSA_experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Median;

import indicator.Indicators;
import restCloud.ResultAnalyser;

public class TCADResultsReaderAndAnalyzer {
	
	public static int island_No = 5;
	public static int factory_Size = 1;
	public static  int startSeed = 1000;
	public static  int caseNum = 40;

	public static void main(String args[]) {

		runAnalysisFactory("result_factory/", island_No);
	}

	public static void runAnalysisFactory(String folder, int island) {
		int startSeed = 1000;
		int caseNum = 40;

		try {
			File theDir = new File(folder + "FGCS");
			if (!theDir.exists()) {
				theDir.mkdirs();
			}

			File theDir1 = new File(folder);
			if (!theDir1.exists()) {
				theDir1.mkdirs();
			}

		} catch (Exception e) {
		}

		for (int i = 1; i <= factory_Size; i++) {
			analyseOneFactory(i, startSeed, caseNum, folder, island);
		}

	}
	
	public static void runAnalysIsland(String folder, int factory) {
		try {
			File theDir = new File(folder + "FGCS");
			if (!theDir.exists()) {
				theDir.mkdirs();
			}

			File theDir1 = new File(folder);
			if (!theDir1.exists()) {
				theDir1.mkdirs();
			}

		} catch (Exception e) {
		}

		for (int i = 1; i < 11; i++) {
			analyseOneFactory(factory, startSeed, caseNum, folder, i);
		}

	}

	public static void analyseOneFactory(int factoryScale, int startSeed, int caseNum, String folder, int island) {
		System.out.println("Factory size: " + factoryScale);

		List<List<Double>> globalPF = findGlobalOptimal(factoryScale, startSeed, caseNum, folder, island);

		List<List<List<Double>>> qisOneSize = new ArrayList<>();

		for (int i = 0; i < caseNum; i++) {
			String file = "PFs " + island + " " + factoryScale + " " + startSeed + ".txt";

			File f = new File(folder + "result_PF/" + file);
			if (f.exists() && !f.isDirectory()) {
				List<List<List<Double>>> res = readResults(folder + "result_PF", file);

				List<List<Double>> qis = Indicators.compare(res, globalPF);
				qisOneSize.add(qis);

			}

			startSeed++;
		}

		analyser(qisOneSize, factoryScale, globalPF, folder, island);
	}

	public static void analyser(List<List<List<Double>>> qis, int factoryScale, List<List<Double>> globalPF,
			String folder, int island) {

		List<List<List<Integer>>> rankings = new ArrayList<>();

		for (int i = 0; i < Indicators.NoQI; i++) {
			rankings.add(getRankingbyQI(qis, i, Indicators.dirQI[i]));
		}

		DecimalFormat fd = new DecimalFormat("0.000");
		List<List<Double>> averageRanking = new ArrayList<>();
		List<List<Double>> medianRanking = new ArrayList<>();

		for (int i = 0; i < rankings.size(); i++) {
			List<Double> averageRankForOneMethod = new ArrayList<>();
			List<Double> medianRankForOneMethod = new ArrayList<>();

			List<List<Integer>> ranks = rankings.get(i);

			for (int j = 0; j < ranks.get(0).size(); j++) {

				int ranksForOneMethod = 0;
				double[] ranksD = new double[ranks.size()];

				for (int k = 0; k < ranks.size(); k++) {
					ranksForOneMethod += ranks.get(k).get(j);
					ranksD[k] = ranks.get(k).get(j);
				}

				double averageRank = (double) ranksForOneMethod / (double) ranks.size();

				Median median = new Median();
				double med = median.evaluate(ranksD);

				averageRankForOneMethod.add(Double.parseDouble(fd.format(averageRank)));
				medianRankForOneMethod.add(Double.parseDouble(fd.format(med)));
			}

			averageRanking.add(averageRankForOneMethod);
			medianRanking.add(medianRankForOneMethod);
		}

		
		String out = "";
		out += "----------------- All ---------------\n\n";
		for (int i = 0; i < rankings.size(); i++) {
			String out_oneQI = "";
			out += Indicators.nameQI[i] + "\n";

			List<List<Integer>> ranksForOneQI = rankings.get(i);
			for (int j = 0; j < ranksForOneQI.size(); j++) {
				for (int k = 0; k < ranksForOneQI.get(j).size(); k++) {
					out += ranksForOneQI.get(j).get(k);
					out_oneQI += ranksForOneQI.get(j).get(k);
					if (k != ranksForOneQI.get(j).size() - 1) {
						out += " ";
						out_oneQI += " ";
					}
						
				}

				out += "\n";
				out_oneQI += "\n";
			}
			out += "\n";
			
			
			ResultAnalyser.writeResult("TC_matlab/data/" + Indicators.nameQI[i] + ".txt", out_oneQI);
		}

		String out_avg_med = "";
		out += "\n\n----------------- Avg --------------\n\n";
		out_avg_med += "\n\n----------------- Avg --------------\n\n";
		String avg = printAverageRanks(averageRanking);
		out += avg;
		out_avg_med += avg;

		out += "\n\n----------------- Med ---------------\n\n";
		out_avg_med += "\n\n----------------- Med --------------\n\n";
		String med = printAverageRanks(medianRanking);
		out += med;
		out_avg_med += med;

		ResultAnalyser.writeResult("TC_matlab/data/rank_avg_med.txt", out_avg_med);
		ResultAnalyser.writeResult(folder + "FGCS/rank " + factoryScale + " " + island + ".txt", out);

		System.out.println("\n");
	}

	private static String printAverageRanks(List<List<Double>> ranks) {

		String out = "";

		System.out.print("QI: ");
		out += "QI: ";
		for (int i = 0; i < Indicators.nameQI.length; i++) {
			System.out.print(Indicators.nameQI[i] + "    ");
			out += Indicators.nameQI[i] + "    ";
		}
		System.out.println();
		out += "\n";

		for (int k = 0; k < ranks.size(); k++) {

			List<Double> r = ranks.get(k);
			for (int j = 0; j < r.size(); j++) {
				System.out.print(r.get(j) + " ");
				out += r.get(j) + " ";
			}
			System.out.println();
			out += "\n";
		}

		return out;

	}

	public static String printRanks(List<List<List<Integer>>> ranks) {
		String out = "";
		System.out.print("QI: ");
		out += "QI: ";
		for (int i = 0; i < Indicators.nameQI.length; i++) {
			System.out.println(Indicators.nameQI[i] + "    ");
			out += Indicators.nameQI[i] + "    ";
		}
		System.out.println();
		out += "\n";

		for (int k = 0; k < ranks.size(); k++) {

			for (int i = 0; i < ranks.get(k).size(); i++) {
				List<Integer> r = ranks.get(k).get(i);
				for (int j = 0; j < r.size(); j++) {
					System.out.print(r.get(j) + " ");
					out += r.get(j) + " ";
				}
				System.out.println();
				out += "\n";
			}

			System.out.println();
			out += "\n";
		}

		return out;
	}

//	private static List<Double> getAverage(List<List<Integer>> values) {
//		DecimalFormat fd = new DecimalFormat("0.000");
//
//		List<Double> averageV = new ArrayList<>();
//
//		for (int j = 0; j < values.get(0).size(); j++) {
//			int valueForOneMethod = 0;
//			for (int k = 0; k < values.size(); k++) {
//				valueForOneMethod += values.get(k).get(j);
//			}
//			double averageRank = (double) valueForOneMethod / (double) values.size();
//
//			averageV.add(Double.parseDouble(fd.format(averageRank)));
//		}
//
//		return averageV;
//	}

	private static List<List<Integer>> getRankingbyQI(List<List<List<Double>>> value_per_group, int QI,
			boolean higherForBetter) {
		List<List<Integer>> rankings = new ArrayList<>();

		List<List<Double>> QIvalues = new ArrayList<>();

		for (int i = 0; i < value_per_group.size(); i++) {
			QIvalues.add(value_per_group.get(i).get(QI));
		}

		for (int i = 0; i < QIvalues.size(); i++) {
			List<Double> v = QIvalues.get(i);

			List<Double> v_copy = new ArrayList<>(v);

			if (higherForBetter)
				v_copy.sort((d1, d2) -> Double.compare(d2, d1));
			else
				v_copy.sort((d1, d2) -> Double.compare(d1, d2));

			List<Integer> ranking = new ArrayList<>();

			int highestRanking = v_copy.size();
			int index = 0;
			while (index < v_copy.size()) {
				double quality = v_copy.get(index);
				ranking.add(highestRanking);

				int numberOfValueInRank = 1;
				index++;

				for (; index < v_copy.size(); index++) {
					if (quality == v_copy.get(index)) {
						numberOfValueInRank++;
						ranking.add(highestRanking);
					} else {
						break;
					}
				}
				highestRanking -= numberOfValueInRank;
			}

			List<Integer> ranks = new ArrayList<>();
			for (int j = 0; j < v.size(); j++) {
				int indexInCopy = v_copy.indexOf(v.get(j));
				ranks.add(ranking.get(indexInCopy));
			}

			rankings.add(ranks);
		}

		return rankings;
	}

	public static List<List<Double>> findGlobalOptimal(int size, int seed, int caseNum, String folder, int island) {

		List<List<Double>> gloablPF = new ArrayList<>();

		for (int i = 0; i < caseNum; i++) {
			String file = "PFs " + island + " " + size + " " + seed + ".txt";

			File f = new File(folder + "result_PF/" + file);
			if (f.exists() && !f.isDirectory()) {
				List<List<List<Double>>> localPFs = readResults(folder + "result_PF", file);

				for (List<List<Double>> oneMethod : localPFs) {
					for (List<Double> oneSolution : oneMethod) {
						updateExternalPopulation(gloablPF, oneSolution);
					}
				}
			}

			seed++;
		}

		gloablPF.sort((c1, c2) -> Double.compare(c1.get(0), c2.get(0)));

		return gloablPF;
	}

	private static void updateExternalPopulation(List<List<Double>> reference, List<Double> candidate) {

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

	private static boolean dominate(List<Double> individual1, List<Double> individual2) {
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

	private static List<List<List<Double>>> readResults(String dir, String inputFile) {

		List<List<List<Double>>> PFs = new ArrayList<>();

		File f = new File(dir + "/" + inputFile);
		String out = "";

		InputStream file;
		InputStreamReader read;
		BufferedReader bufferedReader;
		try {
			file = new FileInputStream(f);
			read = new InputStreamReader(file, "GBK");
			bufferedReader = new BufferedReader(read);
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
//				if (line.length() > 1)
				out += line + "\n";
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] s_split = out.split("\n\n");

		for (int i = 0; i < s_split.length; i++) {

			String oneMethod = s_split[i];
			String[] PF_split = oneMethod.split("\n");

			List<List<Double>> PF_one = new ArrayList<>();

			for (int j = 0; j < PF_split.length; j++) {
				if (PF_split[j].length() > 0 && Character.isDigit(PF_split[j].charAt(0))) {
					String[] oneSolution = PF_split[j].split(" ");
					List<Double> d = new ArrayList<>();
					d.add(Double.parseDouble(oneSolution[0]));
					d.add(Double.parseDouble(oneSolution[1]));
					PF_one.add(d);
				}
			}

			if (PF_one.size() > 0)
				PFs.add(PF_one);

		}

		// String s = s_split[0];
		//
		// List<String> byMethod = new ArrayList<>();
//		System.out.println(out);

		return PFs;

	}

}
