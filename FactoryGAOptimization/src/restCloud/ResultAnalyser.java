package restCloud;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ResultAnalyser {

	public static void main(String[] args) {
		String folderName = "result";
		List<List<List<Double>>> allFiles = readResultAsList(folderName);
		List<List<List<Integer>>> ranks = ranking(allFiles);

		String out = "";

		for (int i = 0; i < allFiles.size(); i++) {
			for (int j = 0; j < allFiles.get(i).size(); j++) {
				String outOneQual = "";
				for (int k = 0; k < allFiles.get(i).get(j).size(); k++) {
					outOneQual += allFiles.get(i).get(j).get(k) + " ";
				}
				out += outOneQual + " , ";
			}
			out += "\n";
		}

		out += "\n";

		for (int i = 0; i < ranks.size(); i++) {
			for (int j = 0; j < ranks.get(i).size(); j++) {
				String outOneRank = "";
				for (int k = 0; k < ranks.get(i).get(j).size(); k++) {
					outOneRank += ranks.get(i).get(j).get(k) + " ";
				}
				out += outOneRank + " , ";
			}
			out += "\n";
		}

		System.out.println(out);

		writeResult(folderName + "/" + "all", out);
	}

	public static List<List<List<Integer>>> ranking(List<List<List<Double>>> allFiles) {

		List<List<List<Integer>>> ranksAllFiles = new ArrayList<>();
		for (int fileIndex = 0; fileIndex < allFiles.size(); fileIndex++) {

			List<List<Double>> oneFile = allFiles.get(fileIndex);

			List<List<Integer>> ranksOneFile = new ArrayList<>();
			for (int indicatorIndex = 0; indicatorIndex < oneFile.size(); indicatorIndex++) {
				List<Double> oneIndicator = oneFile.get(indicatorIndex);

				List<Double> oneIndicatorCopy = new ArrayList<>(oneIndicator);
				oneIndicatorCopy.sort((d1, d2) -> Double.compare(d1, d2));

				List<Integer> ranking = new ArrayList<>();

				int highestRanking = oneIndicatorCopy.size();
				int index = 0;
				while (index < oneIndicatorCopy.size()) {
					double quality = oneIndicatorCopy.get(index);
					ranking.add(highestRanking);

					int numberOfValueInRank = 1;
					index++;

					for (; index < oneIndicatorCopy.size(); index++) {
						if (quality == oneIndicatorCopy.get(index)) {
							numberOfValueInRank++;
							ranking.add(highestRanking);
						} else {
							break;
						}
					}
					highestRanking -= numberOfValueInRank;
				}

				List<Integer> ranks = new ArrayList<>();
				for (int i = 0; i < oneIndicator.size(); i++) {
					int indexInCopy = oneIndicatorCopy.indexOf(oneIndicator.get(i));
					ranks.add(ranking.get(indexInCopy));
				}

				ranksOneFile.add(ranks);
			}
			ranksAllFiles.add(ranksOneFile);
		}

		System.out.println("\n\n\n");
		for (int i = 0; i < ranksAllFiles.size(); i++) {
			System.out.println(ranksAllFiles.get(i));
		}

		return ranksAllFiles;
	}

	public static List<List<List<Double>>> readResultAsList(String folderName) {
		File f = new File(folderName);
		DecimalFormat fd = new DecimalFormat("0.000");

		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".txt");
			}
		};

		File[] files = f.listFiles(textFilter);

		List<List<List<Double>>> allFiles = new ArrayList<>();
		for (int i = 0; i < files.length; i++) {
			String file = folderName + "/" + files[i].getName();

			List<String> lines = null;
			try {
				lines = Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8);
			} catch (IOException e) {
			}

			List<List<Double>> oneFile = new ArrayList<>();
			for (int j = 0; j < lines.size(); j++) {
				String[] lineToArray = lines.get(j).split(" ");

				if (lineToArray.length > 1) {
					List<Double> oneLine = new ArrayList<Double>();
					for (int k = 0; k < lineToArray.length; k++) {
						double d = j == 0 ? 1 - Double.parseDouble(lineToArray[k]) : Double.parseDouble(lineToArray[k]);

						oneLine.add(Double.parseDouble(fd.format(d)));
					}
					oneFile.add(oneLine);
				}

			}
			allFiles.add(oneFile);
		}

		for (int i = 0; i < allFiles.size(); i++) {
			System.out.println(allFiles.get(i));
		}

		return allFiles;
	}

	public static void readResult(String folderName) {
		File f = new File(folderName);

		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".txt");
			}
		};

		File[] files = f.listFiles(textFilter);

		String result = "Resource For All \n\n";
		for (int i = 0; i < files.length; i++) {
			String file = folderName + "/" + files[i].getName();

			List<String> lines = null;
			try {
				lines = Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8);
			} catch (IOException e) {
			}

			for (int j = 0; j < lines.size(); j++) {
				result += lines.get(j) + "\n";
			}
			result += "\n";
		}

		System.out.println(result);

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(folderName + "/" + folderName + " all.txt"), false));
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

	public static void writeResult(String filename, String result) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(filename), false));
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
