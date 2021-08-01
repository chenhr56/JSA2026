package restCloud;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TIIAnalyser_suppressed {

	public static void main(String args[]) throws Exception {

		File f = new File("1.txt");
		String out = "";

		List<String> res = new ArrayList<>();

		InputStream file = new FileInputStream(f);
		InputStreamReader read = new InputStreamReader(file, "GBK");
		BufferedReader bufferedReader = new BufferedReader(read);
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			if (line.length() > 1)
				out += line + "\n";
			else {
				res.add(out);
				out = "";
			}
		}

		res.add(out);
		bufferedReader.close();

		for (int i = 0; i < res.size(); i++) {
			if (res.get(i).length() < 1) {
				res.remove(i);
				i--;
			}
		}

		List<List<String>> res_per_group = new ArrayList<>();
		for (int i = 0; i < res.size(); i++) {
			List<String> per_group = new ArrayList<>();
			String[] r = res.get(i).split("\n");
			for (int j = 0; j < r.length; j++) {
				per_group.add(r[j]);
			}
			res_per_group.add(per_group);
		}

		List<List<List<Double>>> value_per_group = new ArrayList<>();
		for (int i = 0; i < res_per_group.size(); i++) {
			List<List<Double>> per_group = new ArrayList<>();
			for (int j = 1; j < res_per_group.get(i).size(); j++) {
				String valueString = res_per_group.get(i).get(j);
				String[] valueStringSplit = valueString.split(" ");

				List<Double> value_per_line = new ArrayList<>();
				for (int k = 0; k < valueStringSplit.length; k++) {
					String v = valueStringSplit[k];
					Double value = Double.parseDouble(v);
					value_per_line.add(value);
				}
				per_group.add(value_per_line);
			}
			value_per_group.add(per_group);
		}

		List<List<Integer>> DCIrankings = getRankingbyQI(value_per_group, 0,false);
		List<List<Integer>> GDrankings = getRankingbyQI(value_per_group, 1,false);
		List<List<Integer>> D1Rrankings = getRankingbyQI(value_per_group, 2,false);

		printRanks(DCIrankings);
		printRanks(GDrankings);
		printRanks(D1Rrankings);

		System.out.println();
		
		List<List<Double>> push = new ArrayList<>();
		List<List<Double>> pull = new ArrayList<>();
		
		for (int i = 0; i < value_per_group.size(); i++) {
			push.add(value_per_group.get(i).get(3));
			pull.add(value_per_group.get(i).get(4));
		}
		
		System.out.println("\n\npush:");
		for (int i = 0; i < push.size(); i++) {
			List<Double> p = push.get(i);
			for (int j = 0; j < p.size(); j++) {
				System.out.print(p.get(j).intValue()+" ");
			}
			System.out.println();
		}
		
		System.out.println("\n\npush:");
		for (int i = 0; i < pull.size(); i++) {
			List<Double> p = pull.get(i);
			for (int j = 0; j < p.size(); j++) {
				System.out.print(p.get(j).intValue()+" ");
			}
			System.out.println();
		}
		
		System.out.println("\n\n");

	}

	public static void printRanks(List<List<Integer>> ranks) {

		for (int i = 0; i < ranks.size(); i++) {
			List<Integer> r = ranks.get(i);
			for (int j = 0; j < r.size(); j++) {
				System.out.print(r.get(j)+" ");
			}
			System.out.println();
		}
		
		System.out.println("\n\n");
	}

	public static List<List<Integer>> getRankingbyQI(List<List<List<Double>>> value_per_group, int QI, boolean containlinkage) {
		List<List<Integer>> rankings = new ArrayList<>();

		List<List<Double>> QIvalues = new ArrayList<>();

		for (int i = 0; i < value_per_group.size(); i++) {
			QIvalues.add(value_per_group.get(i).get(QI));
		}

		for (int i = 0; i < QIvalues.size(); i++) {
			List<Double> v = QIvalues.get(i);

			List<Double> v_copy = new ArrayList<>(v);
			v_copy.sort((d1, d2) -> Double.compare(d2, d1));

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
