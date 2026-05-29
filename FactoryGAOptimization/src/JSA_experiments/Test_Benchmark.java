package JSA_experiments;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import indicator.Indicators;
import mitm.atb.OnaConfigurationType;
import restCloud.ManagerPP;
import restCloud.ManagerPPLocal;
import restCloud.ResultAnalyser;

public class Test_Benchmark {
/**
 * 对应图5 6的实验
 */
	public static void main(String args[]) {
		start();
	}

	public static void start() {

		ManagerPP.testSameCPUTime = false;
		ManagerPP.testStageScale = false;

//		int factorySize = Integer.parseInt(args[0]);
//		int runGroup = Integer.parseInt(args[1]);
//		int NoC = Integer.parseInt(args[2]);
//		int factorySize = 6;
//		int runGroup = 2;

		int numberOfIslands = 5;
		int NoC = 5;
		String folder = "benchmark_Brandimarte_result/";

//		runGAforOneSize(factorySize, runGroup, NoC);

		for (int j = 1; j < 2; j++) {

			for (int i = 1; i < 41; i++) {
				// [多线程修复] 原代码: OnaConfigurationType.ONAReader = null;
				// 多线程下外部直接置 null 可在 presetup() 执行中途导致 NPE。
				// presetup() 现已通过 currentScale 跟踪 scale 变化并自动重新初始化，不再需要此行。
				// OnaConfigurationType.ONAReader = null;  // <-- 原代码，已移除
				runGAforOneSize(j, i, NoC, folder, numberOfIslands);
			}

			System.out.println("*******************************************************");
			TCADResultsReaderAndAnalyzer.runAnalysisFactory(folder, numberOfIslands);
			System.out.println("*******************************************************");
			System.out.println("--------------------------------------------------------------------");
			System.out.println("\n\n");

		}

	}

	public static void runGAforOneSize(int factoryScale, int runGroup, int NoC, String folder, int numberOfIslands) {

		int numberOfReplace = 1;
		int NotImprovedInRow = 3;

//		if (NoC != 50 && NoC != 100) { // NoC 50 , 100
//			System.out.println("Number of processors not supported!  NoC: " + NoC);
//			System.exit(-1);
//			;
//		}

		int controlledVariabile = 1; // TODO: controlledVariabile 4 , 2

		if (runGroup < 1 || runGroup > 40 / controlledVariabile) { // runGroup = [1,10] , [1,20]
			System.out.println("Wrong run group index !");
			System.exit(-1);
		}

		int Startingseed = 1000 + controlledVariabile * (runGroup - 1);

		List<List<List<Double>>> results = new ArrayList<>();

		for (int i = 0; i < controlledVariabile; i++) {
			List<List<Double>> res = new ArrayList<>();
			results.add(res);
		}

		for (int i = 0; i < controlledVariabile; i++) {
			System.out.println("seed: " + (Startingseed + i) + " factoryScale: " + (factoryScale) + " numberOfIslands: "
					+ numberOfIslands + " numberOfReplace: " + numberOfReplace + " notImprovedInRow: "
					+ NotImprovedInRow);

			List<List<Double>> res = new ManagerPPLocal().startPPLocal(Startingseed + i, factoryScale, numberOfIslands,
					numberOfReplace, NotImprovedInRow, folder);

			results.set(i, res);
		}

		finalAnalyserAndSummarizer(results, factoryScale, folder, numberOfIslands);
	}

	public static void finalAnalyserAndSummarizer(List<List<List<Double>>> results, int factoryScale, String folder,
			int numberOfIslands) {

		List<List<List<Double>>> qis = new ArrayList<>();

		List<List<Integer>> push = new ArrayList<>();
		List<List<Integer>> pull = new ArrayList<>();
		List<List<Integer>> time = new ArrayList<>();

		for (int i = 0; i < results.size(); i++) {

			List<List<Double>> qi = new ArrayList<>();

			for (int j = 0; j < results.get(i).size(); j++) {
				if (j < Indicators.NoQI) {
					qi.add(results.get(i).get(j));
				}

				else {
					switch (j) {
					case Indicators.NoQI:
						push.add(results.get(i).get(j).stream().map(c -> c.intValue()).collect(Collectors.toList()));
						break;

					case Indicators.NoQI + 1:
						pull.add(results.get(i).get(j).stream().map(c -> c.intValue()).collect(Collectors.toList()));
						break;

					case Indicators.NoQI + 2:
						time.add(results.get(i).get(j).stream().map(c -> c.intValue()).collect(Collectors.toList()));
						break;

					default:
						break;
					}
				}
			}

			qis.add(qi);
		}

		List<List<List<Integer>>> rankings = new ArrayList<>();

		for (int i = 0; i < Indicators.NoQI; i++) {
			rankings.add(getRankingbyQI(qis, i, Indicators.dirQI[i]));
		}

		DecimalFormat fd = new DecimalFormat("0.000");
		List<List<Double>> averageRanking = new ArrayList<>();

		for (int i = 0; i < rankings.size(); i++) {
			List<Double> averageRankForOneMethod = new ArrayList<>();

			List<List<Integer>> ranks = rankings.get(i);
			for (int j = 0; j < ranks.get(0).size(); j++) {
				int ranksForOneMethod = 0;
				for (int k = 0; k < ranks.size(); k++) {
					ranksForOneMethod += ranks.get(k).get(j);
				}
				double averageRank = (double) ranksForOneMethod / (double) ranks.size();

				averageRankForOneMethod.add(Double.parseDouble(fd.format(averageRank)));
			}

			averageRanking.add(averageRankForOneMethod);
		}

		List<Double> averagePush = getAverage(push);
		List<Double> averagePull = getAverage(pull);
		List<Double> averagetime = getAverage(time);

		String out = "";

//		System.out.println("\n\n---------------------------------------\n\n");
//		printRanks(rankings);
//		System.out.println("\n\n---------------------------------------\n\n");

//		out += printAverageRanks(averageRanking) + "\n";
		out += printAverage(averagePush, "Push:") + "\n";
		out += printAverage(averagePull, "Pull:") + "\n";
		out += printAverage(averagetime, "Time:");

		System.out.println("Time: " + Arrays.toString(averagetime.toArray()));

		try {
			File theDir = new File(folder);
			if (!theDir.exists()) {
				theDir.mkdirs();
			}

			File theDir1 = new File(folder + "FGCS");
			if (!theDir1.exists()) {
				theDir1.mkdirs();
			}
		} catch (Exception e) {
		}

		ResultAnalyser.writeResult(folder + "FGCS/all " + factoryScale + " " + numberOfIslands + ".txt", out);
	}

	public static String printAverage(List<Double> value, String info) {
		String out = "";
//		System.out.println(info);

		for (int k = 0; k < value.size(); k++) {
			out += value.get(k) + " ";
//			System.out.print(value.get(k) + " ");
		}
//		System.out.println();
		out += "\n";

		return out;
	}

	public static String printAverageRanks(List<List<Double>> ranks) {

		String out = "";

		System.out.print("QI: ");

		for (int i = 0; i < Indicators.nameQI.length; i++) {
			System.out.print(Indicators.nameQI[i] + "    ");
		}
		System.out.println();

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

	public static List<Double> getAverage(List<List<Integer>> values) {
		DecimalFormat fd = new DecimalFormat("0.000");

		List<Double> averageV = new ArrayList<>();

		for (int j = 0; j < values.get(0).size(); j++) {
			int valueForOneMethod = 0;
			for (int k = 0; k < values.size(); k++) {
				valueForOneMethod += values.get(k).get(j);
			}
			double averageRank = (double) valueForOneMethod / (double) values.size();

			averageV.add(Double.parseDouble(fd.format(averageRank)));
		}

		return averageV;
	}

	public static List<List<Integer>> getRankingbyQI(List<List<List<Double>>> value_per_group, int QI,
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

}


