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

import factoryModel.OAS.Device;
import factoryModel.OAS.OASFactoryModel.DeviceType;
import factoryModel.OAS.OASXMLReader;
import factoryModel.OAS.ProductionLine;
import factoryModel.OAS.ProductionProcess;
import factoryModel.OAS.SubProcess;
import factoryModel.OAS.SubProcessRelation;
import uk.ac.york.safire.metrics.ConfigurationType;
import uk.ac.york.safire.metrics.ControlledMetricType;
import uk.ac.york.safire.metrics.KeyObjectiveType;
import uk.ac.york.safire.metrics.ObservableMetricType;
import uk.ac.york.safire.metrics.SampleRate;
import uk.ac.york.safire.metrics.SearchDirection;
import uk.ac.york.safire.metrics.ValueType;

///////////////////////////////////

public class OasProductionLineConfigurationType {

	public static OASXMLReader OASReader = null;

	public static List<Device> devices;
	public static List<ProductionLine> lines;

	public static List<ProductionProcess> processes;

	public static List<SubProcessRelation> relations;
	public static List<SequenceDependentTaskInfo> setups;

	public static List<String> objectives;

	public static List<String> realResourceNames = null;

	// List<List<String>> unAvailableTimes = new ArrayList<>();
	// List<String> unAvailableTimesProcessed = new ArrayList<>();

	public static Map<String, List<String>> unAvailableTimes = new HashMap<>();

	public static void presetup() {
		if (OASReader == null) {

			OASReader = new OASXMLReader();
			OASReader.readOASInput();

			devices = OASReader.getResources();
			lines = OASReader.getLines();

			processes = OASReader.getProcesses();
			relations = OASReader.getRelations();
			setups = OASReader.getSetups();

			objectives = OASReader.getObjectivesList();
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
		for (int i = 0; i < OASReader.getProcesses().size(); i++) {
			String[] nameArray = OASReader.getProcesses().get(i).getName().split(" ");
			commdities.add(nameArray[0] + " " + nameArray[1]);
		}

		return commdities;
	}

	public static Set<String> getCommdities() {
		presetup();

		Set<String> commdities = new HashSet<>();
		for (int i = 0; i < OasProductionLineConfigurationType.OASReader.getProcesses().size(); i++) {
			String[] nameArray = OasProductionLineConfigurationType.OASReader.getProcesses().get(i).getName()
					.split(" ");
			commdities.add(nameArray[0] + " " + nameArray[1]);
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

	public static ConfigurationType configurationType(Random random, int availablePercentage) {
		presetup();
		// List<String> productionLines = lines.stream().map(x ->
		// x.resourceName()).collect(Collectors.toList());

		List<List<List<Device>>> resourceNames1 = new ArrayList<>();

		for (ProductionLine line : lines) {
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
				String modeName = "";
				for (Device d : line) {
					lineName += d.getType() + "(" + d.getId() + ") ";
					modeName += d.getMode() + "-";
				}
				lineName += modeName;
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

		for (int i = 0; i < OASReader.getProcesses().size(); i++) {
			String[] processNameArray = OASReader.getProcesses().get(i).getName().split(" ");
			int processAmount = Integer.parseInt(OASReader.getProcesses().get(i).getAmount());

			List<Map.Entry<String, Integer>> keys = OASReader.getProcesses().get(i).getAmountProduced().entrySet()
					.stream().collect(Collectors.toList());
			keys.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

			List<Integer> numberOfInstance = keys.stream()
					.map(r -> (int) Math.ceil((double) processAmount / (double) r.getValue()))
					.collect(Collectors.toList());

			List<RecipeInfo> recipes = new ArrayList<RecipeInfo>();
			for (int j = 0; j < keys.size(); j++) {
				List<ProductionLine> comptiableResources = OASReader.getProcesses().get(i).getCompitableResource()
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
							processedName = nameWithMode.substring(0, nameWithMode.lastIndexOf(" "));
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

			recipeInfo.put(processNameArray[0] + " " + processNameArray[1], recipes);
			mapRecipeNameToAmountRequired.put(processNameArray[0] + " " + processNameArray[1], processAmount);
			mapRecipeInfoNameToAmountProduced.putAll(OASReader.getProcesses().get(i).getAmountProduced());
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
				keyObjectiveTypes.add(new KeyObjectiveType(objectives.get(i), ValueType.realType(0, Double.MAX_VALUE),
						"n/a", SearchDirection.MINIMIZING));
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

		List<String> processedNames = new ArrayList<>();
		for (String resourceName : resourceNames) {
			if (!resourceName.equals("No allocation")) {
				List<String> resourceNameList = new ArrayList<String>(Arrays.asList(resourceName.split(" ")));
				resourceNameList.remove(resourceNameList.size() - 1);
				String processedName = String.join(" ", resourceNameList);
				if (!processedNames.contains(processedName))
					processedNames.add(processedName);
			}

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
				for (int i = 0; i < resourceArray.length - 1; i++) {
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
		// Collections.shuffle(priorities, random);

		int instanceCount = 0;
//		int productsCount = 0;

		for (Map.Entry<String, List<RecipeInfo>> e : recipeInfo.entrySet()) {

			for (RecipeInfo r : e.getValue()) {
				int productsCount = getProductIndex(r.name);
				List<SubProcess> subProcesses = processes.get(productsCount).getSubProcesses().get(r.name);

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

					for (int j = 0; j < comptiableResources.length - 1; j++) {

						List<String> deviceNamePattern = Arrays.asList(comptiableResources[j]
								.substring(0, comptiableResources[j].lastIndexOf(" ")).split(" "));
						List<String> deviceModePattern = Arrays.asList(comptiableResources[j]
								.substring(comptiableResources[j].lastIndexOf(" ") + 1, comptiableResources[j].length())
								.split("-"));

						for (SubProcess sp : subProcesses) {
							List<List<Device>> spDeviceGroups = sp.getSubProcessGroup();

							for (List<Device> oneGroup : spDeviceGroups) {
								List<String> deviceToNames = oneGroup.stream()
										.map(d -> d.getType() + "(" + d.getId() + ")").collect(Collectors.toList());
								List<String> deviceToMode = (oneGroup.stream().map(d -> d.getMode().toString())
										.collect(Collectors.toList()));

								List<Integer> matchingIndex = new ArrayList<>();
								for (String name : deviceToNames) {
									if (deviceNamePattern.contains(name))
										matchingIndex.add(deviceNamePattern.indexOf(name));
								}

								if (matchingIndex.size() == deviceToNames.size()) {
									boolean isMatched = true;

									for (int modeIndex = 0; modeIndex < deviceToMode.size(); modeIndex++) {
										String mode = deviceToMode.get(modeIndex);
										int patternIndex = matchingIndex.get(modeIndex);
										String pattern = deviceModePattern.get(patternIndex);
										if (!mode.equals(pattern)) {
											isMatched = false;
										}
									}

									if (isMatched) {
										int subGroupIndex = sp.getSubProcessGroup().indexOf(oneGroup);
										final int start = 0;
										final int end = sp.getProcessingTimes().get(subGroupIndex);

										final int energyCost = sp.getEnergyCosts().get(subGroupIndex);
										final int montaryCost = sp.getMontaryCosts().get(subGroupIndex);

										String deviceNamePreFix = oneGroup.stream()
												.map(d -> d.getType().toString().toLowerCase())
												.collect(Collectors.joining("_"));

										String recipeAndResourceNamePrefix = instanceName + " " + comptiableResources[j]
												+ " " + deviceNamePreFix;

										// System.out.println(recipeAndResourceNamePrefix + " " + end + " " + energyCost
										// + " " + montaryCost);

										SubProcess spRelation = null;
										for (SubProcessRelation relation : relations) {
											if (relation.getDestination() == sp) {
												spRelation = relation.getSource();
											}
										}

										if (spRelation != null) {
											List<DeviceType> taskExecutedBeforeDeviceType = Arrays
													.asList(spRelation.getType());
											List<String> taskExecutedBeforePrefix = taskExecutedBeforeDeviceType
													.stream().map(dt -> dt.toString().toLowerCase())
													.collect(Collectors.toList());

											String taskBefore = String.join("_", taskExecutedBeforePrefix);

											observableMetricTypes.add(new ObservableMetricType(
													recipeAndResourceNamePrefix + " executedAfter " + taskBefore,
													ValueType.intType(1, 1), "n/a", SampleRate.eventDriven));
										}

//										String obs = recipeAndResourceNamePrefix + "_start end energy montary" + "_"
//												+ start + " " + end + " " + energyCost + " " + montaryCost;
//										observableMetricTypes.add(new ObservableMetricType(obs, ValueType.intType(1, 1),
//												"n/a", SampleRate.eventDriven));

										observableMetricTypes.add(new ObservableMetricType(
												recipeAndResourceNamePrefix + " start", ValueType.intType(start, start),
												"n/a", SampleRate.eventDriven));
										observableMetricTypes
												.add(new ObservableMetricType(recipeAndResourceNamePrefix + " end",
														ValueType.intType(end, end), "n/a", SampleRate.eventDriven));

										observableMetricTypes
												.add(new ObservableMetricType(recipeAndResourceNamePrefix + " energy",
														ValueType.intType(energyCost, energyCost), "n/a",
														SampleRate.eventDriven));
										observableMetricTypes
												.add(new ObservableMetricType(recipeAndResourceNamePrefix + " montary",
														ValueType.intType(montaryCost, montaryCost), "n/a",
														SampleRate.eventDriven));

									}
								}

							}

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

	private static int getProductIndex(String name) {
		String[] name_array = name.split(" ");
		String key = name_array[0] + " " + name_array[1];
		for (int i = 0; i < processes.size(); i++) {
			if (processes.get(i).getName().contains(key)) {
				return i;
			}
		}

		System.err.print("cannot get product via given name.");
		System.exit(-1);
		return -1;
	}

}