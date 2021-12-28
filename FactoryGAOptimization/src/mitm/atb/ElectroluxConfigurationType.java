package mitm.atb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import factoryModel.Electrolux.CookingZone;
import factoryModel.Electrolux.Device;
import factoryModel.Electrolux.ElectroluxXMLReader;
import factoryModel.Electrolux.ProductionProcess;
import metrics.ConfigurationType;
import metrics.ControlledMetricType;
import metrics.KeyObjectiveType;
import metrics.ObservableMetricType;
import metrics.SampleRate;
import metrics.SearchDirection;
import metrics.ValueType;

///////////////////////////////////

public class ElectroluxConfigurationType {

	public static ElectroluxXMLReader ElectroluxReader = null;
	public static List<Device> devices;
	public static List<CookingZone> lines;
	public static List<ProductionProcess> processes;
	public static List<SequenceDependentTaskInfo> setups;
	public static List<String> objectives;
	public static List<String> realResourceNames = null;
	public static Map<String, List<String>> unAvailableTimes = new HashMap<>();

	public static void presetup() {
		if (ElectroluxReader == null) {
//			ElectroluxXMLWriter.write();

			ElectroluxReader = new ElectroluxXMLReader();
			ElectroluxReader.readElectroluxInput();

			devices = ElectroluxReader.getResources();
			lines = ElectroluxReader.getLines();
			processes = ElectroluxReader.getProcesses();
			setups = null;// ElectroluxReader.getSetups();
			objectives = ElectroluxReader.getObjectivesList();
		}
	}

	public static List<SequenceDependentTaskInfo> getSetUps() {
		presetup();
		return setups;
	}

	public static List<String> getObjectives() {
		presetup();
		return objectives;
	}

	public static List<String> getProducts() {
		presetup();
		List<String> commdities = new ArrayList<>();
		for (int i = 0; i < ElectroluxReader.getProcesses().size(); i++) {
			String nameArray = ElectroluxReader.getProcesses().get(i).getName();
			commdities.add(nameArray);
		}

		return commdities;
	}

	public static Set<String> getCommdities() {
		presetup();
		Set<String> commdities = new HashSet<>();
		for (int i = 0; i < ElectroluxConfigurationType.ElectroluxReader.getProcesses().size(); i++) {
			String nameArray = ElectroluxConfigurationType.ElectroluxReader.getProcesses().get(i).getName();
			commdities.add(nameArray);
		}

		return commdities;
	}

	private static List<List<Device>> generateLines(List<List<Device>> lines, List<Device> devicesForOneLine) {

		List<List<Device>> newLines = new ArrayList<>();
		if (lines.size() == 0) {

			for (int i = 0; i < devicesForOneLine.size(); i++) {
				ArrayList<Device> line = new ArrayList<>();
				line.add(devicesForOneLine.get(i));
				newLines.add(line);
			}
		} else {

			List<List<Device>> devicesForOneLineTemp = new ArrayList<List<Device>>();
			for (Device d : devicesForOneLine) {
				ArrayList<Device> deviceTemp = new ArrayList<Device>();
				deviceTemp.add(d);
				devicesForOneLineTemp.add(deviceTemp);
			}

			for (int i = 0; i < lines.size(); i++) {
				List<Device> deviceInLine = lines.get(i);

				for (int j = 0; j < devicesForOneLineTemp.size(); j++) {
					List<Device> deviceToAdd = devicesForOneLineTemp.get(j);

					ArrayList<Device> newLine = new ArrayList<>();
					newLine.addAll(deviceInLine);
					newLine.addAll(deviceToAdd);
					newLines.add(newLine);
				}
			}

		}

		return newLines;

	}

	public static ConfigurationType hardwiredConfigurationType(Random random, int availablePercentage) {
		presetup();

		List<List<List<Device>>> resourceNames1 = new ArrayList<>();

		for (CookingZone line : lines) {
			List<String> resourceName = line.getDeviceNames();
			List<List<Device>> devicesForOneLine = new ArrayList<>();
			for (String name : resourceName) {
				List<Device> deviceByName = new ArrayList<Device>();
				for (Device d : devices) {
					if (d.getName().equals(name))
						deviceByName.add(d);
				}
				devicesForOneLine.add(deviceByName);
			}
			resourceNames1.add(devicesForOneLine);
		}

		List<List<List<Device>>> resourceNames2 = new ArrayList<>();

		for (int i = 0; i < resourceNames1.size(); i++) {
			List<List<Device>> resourcesInOneLine = new ArrayList<>();

			for (List<Device> oneDevice : resourceNames1.get(i)) {
				List<List<Device>> generatedLine = generateLines(resourcesInOneLine, oneDevice);
				resourcesInOneLine = generatedLine;
			}

			resourceNames2.add(resourcesInOneLine);
		}

		List<List<String>> processedResources = new ArrayList<>();
		for (List<List<Device>> lines : resourceNames2) {
			List<String> processedResource = new ArrayList<String>();
			for (List<Device> line : lines) {
				String lineName = "";
				for (Device d : line) {
					lineName += d.getType() + "(" + d.getId() + ") ";
				}
				processedResource.add(lineName.substring(0, lineName.length() - 1));
			}
			processedResources.add(processedResource);
		}

		for (Device d : devices) {
			String[] deviceIDToArray = d.getName().split(" ");
			String deviceID = deviceIDToArray[0] + "(" + deviceIDToArray[1] + ")";
			if (!unAvailableTimes.containsKey(deviceID) && d.getNotavailbaileTime().length() > 1) {
				unAvailableTimes.put(deviceID, Arrays.asList(d.getNotavailbaileTime().split(" ")));
			}
		}

		List<String> noAllocation = new ArrayList<String>();
		noAllocation.add("No allocation");
		processedResources.add(noAllocation);

		List<String> resourceNames = processedResources.stream().flatMap(List::stream).collect(Collectors.toList());

		realResourceNames = new ArrayList<>(resourceNames);

		final Map<String, List<RecipeInfo>> recipeInfo = new HashMap<>();
		final Map<String, Integer> mapRecipeNameToAmountRequired = new HashMap<>();
		final Map<String, Integer> mapRecipeInfoNameToAmountProduced = new HashMap<>();

		for (int i = 0; i < ElectroluxReader.getProcesses().size(); i++) {
			String productName = ElectroluxReader.getProcesses().get(i).getName();
			int processAmount = Integer.parseInt(ElectroluxReader.getProcesses().get(i).getAmount());

			List<Map.Entry<String, Integer>> keys = ElectroluxReader.getProcesses().get(i).getAmountProduced()
					.entrySet().stream().collect(Collectors.toList());
			keys.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

			List<Integer> numberOfInstance = keys.stream()
					.map(r -> (int) Math.ceil((double) processAmount / (double) r.getValue()))
					.collect(Collectors.toList());

			List<RecipeInfo> recipes = new ArrayList<RecipeInfo>();
			for (int j = 0; j < keys.size(); j++) {
				List<CookingZone> comptiableResources = ElectroluxReader.getProcesses().get(i).getCompitableResource()
						.get(keys.get(j).getKey());
				List<String> comptiableResourcesNames = comptiableResources.stream().map(p -> p.resourceName())
						.collect(Collectors.toList());
				comptiableResourcesNames.add("No allocation");
				List<Integer> comptiables = new ArrayList<>();

				for (String name : comptiableResourcesNames) {
					int nameCount = 0;
					for (String nameWithMode : resourceNames) {
						String processedName = null;
						if (nameWithMode != "No allocation") {
							processedName = nameWithMode;
						} else
							processedName = "No allocation";

						if (name.equals(processedName)) {
							comptiables.add(nameCount);
						}
						nameCount++;
					}
				}
				recipes.add(new RecipeInfo(keys.get(j).getKey(), numberOfInstance.get(j), comptiables));
			}

			recipeInfo.put(productName, recipes);
			mapRecipeNameToAmountRequired.put(productName, processAmount);
			mapRecipeInfoNameToAmountProduced.putAll(ElectroluxReader.getProcesses().get(i).getAmountProduced());
		}

		return makeConfigurationType(recipeInfo, resourceNames, mapRecipeNameToAmountRequired,
				mapRecipeInfoNameToAmountProduced, random, availablePercentage);
	}

	private static ConfigurationType makeConfigurationType(Map<String, List<RecipeInfo>> recipeInfo,
			List<String> resourceNames, Map<String, Integer> mapRecipeNameToAmountRequired,
			Map<String, Integer> mapRecipeInfoNameToAmountProduced, Random random, int availablePercentage) {

		final List<KeyObjectiveType> keyObjectiveTypes = new ArrayList<>();

		for (int i = 0; i < objectives.size(); i++) {
			if (!objectives.get(i).equals("discrepancy")) {

				if (objectives.get(i).equals("quality")) {
					keyObjectiveTypes.add(new KeyObjectiveType(objectives.get(i),
							ValueType.realType(0, Double.MAX_VALUE), "n/a", SearchDirection.MAXIMIZING));
				} else {
					keyObjectiveTypes.add(new KeyObjectiveType(objectives.get(i),
							ValueType.realType(0, Double.MAX_VALUE), "n/a", SearchDirection.MINIMIZING));
				}

			} else {
				for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet())
					keyObjectiveTypes.add(new KeyObjectiveType(e.getKey() + " amount discrepancy score",
							ValueType.realType(0, Double.MAX_VALUE), "n/a", SearchDirection.MINIMIZING));
			}
		}

		final List<ControlledMetricType> controlledMetricTypes = new ArrayList<>();
		final List<ObservableMetricType> observableMetricTypes = new ArrayList<>();

		for (String resourceName : resourceNames) {
			int result = 1;

			String[] singleDevices = resourceName.split(" ");
			for (String singleDevice : singleDevices) {
				for (Device d : devices) {
					if (singleDevice.equals(d.getFormattedName()) && d.getAvailability() == 0) {
						result = 0;
					}
				}
			}

			observableMetricTypes.add(new ObservableMetricType(resourceName + " availability",
					ValueType.intType(result, result), "n/a", SampleRate.eventDriven));
		}

		for (int i = 0; i < processes.size(); i++) {
			String productInfo = processes.get(i).getName();
			int urgency = processes.get(i).getUrgency();

			observableMetricTypes.add(new ObservableMetricType(productInfo + " urgency " + urgency,
					ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
		}

		for (Entry<String, List<String>> entry : unAvailableTimes.entrySet()) {
			String resourceKey = entry.getKey();
			if (entry.getValue().size() > 0) {
				observableMetricTypes.add(new ObservableMetricType(
						resourceKey + " unAvailabileTime " + String.join("_", entry.getValue()),
						ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
			}
		}

		for (Map.Entry<String, Integer> e : mapRecipeInfoNameToAmountProduced.entrySet()) {
			final Integer quantity = e.getValue();
			observableMetricTypes.add(new ObservableMetricType(e.getKey() + " amount produced",
					ValueType.intType(quantity, quantity), "n/a", SampleRate.eventDriven));
		}

		for (Map.Entry<String, Integer> e : mapRecipeNameToAmountRequired.entrySet()) {
			final Integer quantity = e.getValue();
			observableMetricTypes.add(new ObservableMetricType(e.getKey() + " amount required",
					ValueType.intType(quantity, quantity), "n/a", SampleRate.eventDriven));
		}

		for (String resourceName : resourceNames) {
			if (!resourceName.equals("No allocation")) {
				String[] resourceArray = resourceName.split(" ");
				for (int i = 0; i < resourceArray.length; i++) {
					observableMetricTypes.add(new ObservableMetricType(resourceName + " mutex " + resourceArray[i],
							ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
				}
			}

		}

		int totalInstances = 0;
		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet())
			for (RecipeInfo r : e.getValue())
				totalInstances += r.instances;

		final List<Integer> priorities = IntStream.rangeClosed(0, totalInstances).boxed().collect(Collectors.toList());

		int instanceCount = 0;
//		int productsCount = 0;

		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {
			for (RecipeInfo r : e.getValue()) {
				for (int i = 0; i < r.instances; ++i) {
					final String instanceName = r.name + " " + i;

					String[] comptiableResources = r.compatibleResources.stream().map(index -> resourceNames.get(index))
							.toArray(String[]::new);
					final ValueType allocationValueType = ValueType.nominalType(instanceName + " allocation type ",
							comptiableResources);

					controlledMetricTypes
							.add(new ControlledMetricType(instanceName + " allocation", allocationValueType, "n/a"));

					final int priorityValue = priorities.get(instanceCount);

					controlledMetricTypes.add(new ControlledMetricType(instanceName + " priority",
							ValueType.intType(priorityValue, priorityValue), "n/a"));

					int index = getIndex(r.name);
					for (int j = 0; j < comptiableResources.length; j++) {
						if (!comptiableResources[j].equals("No allocation")) {
							final String recipeAndResourceNamePrefix = instanceName + " " + comptiableResources[j];

							String predecessor = processes.get(index).getPredecessor().get(r.name);

							if (!predecessor.equals("")) {
								observableMetricTypes.add(new ObservableMetricType(
										recipeAndResourceNamePrefix + " executedAfter " + predecessor,
										ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
							}

							int start = 0;
							int end = processes.get(index).getProcessingTime().get(r.name);
							int energy = processes.get(index).getEnergy().get(r.name);
							int montary = processes.get(index).getMontary().get(r.name);
							int quality = processes.get(index).getQuality().get(r.name);

							observableMetricTypes.add(new ObservableMetricType(recipeAndResourceNamePrefix + " start",
									ValueType.intType(start, start), "n/a", SampleRate.eventDriven));
							observableMetricTypes.add(new ObservableMetricType(recipeAndResourceNamePrefix + " end",
									ValueType.intType(end, end), "n/a", SampleRate.eventDriven));

							observableMetricTypes.add(new ObservableMetricType(recipeAndResourceNamePrefix + " energy",
									ValueType.intType(energy, energy), "n/a", SampleRate.eventDriven));
							observableMetricTypes.add(new ObservableMetricType(recipeAndResourceNamePrefix + " montary",
									ValueType.intType(montary, montary), "n/a", SampleRate.eventDriven));
							observableMetricTypes.add(new ObservableMetricType(recipeAndResourceNamePrefix + " quality",
									ValueType.intType(quality, quality), "n/a", SampleRate.eventDriven));
						}

					}

					instanceCount += 1;
				}
			}
//			productsCount++;
		}

		UoYEarlyPrototypeDemo.observableMetricTypes = observableMetricTypes;

		return new ConfigurationType.Explicit(keyObjectiveTypes, controlledMetricTypes);
	}

	private static int getIndex(String name) {

		int index = -1;

		for (int i = 0; i < processes.size(); i++) {
			if (name.contains(processes.get(i).getName())) {
				return i;
			}
		}

		return index;
	}

}