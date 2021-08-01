package factoryModel.OAS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;

import mitm.atb.SequenceDependentTaskInfo;

public class OASFactoryModel {

	/********************************************************************************************************************/

	public static Random ran = new Random();

	public static int MAX_PRIORITY = 100;
	public static int percentAvailability = 100;

	public static enum AllenOperator {
		M, UnDefined/* , LT, S, F, EQ, O, D */
	}

	public static enum DeviceType {
		Silo, Mixer, Tank
	}

	public static enum ModeType {
		Ecomony, Standard, Performance
	}

	public static int[] NUMBER_OF_DEVICES = { 5, 10, 2 };

	public static ModeType[][] MODE_TYPES = { { ModeType.Standard, ModeType.Ecomony, ModeType.Performance },
			{ ModeType.Standard, ModeType.Ecomony, ModeType.Performance }, { ModeType.Standard } };

	public static final double[] modeCost = { 0.75, 1.0, 1.25 };
	public static final int[][] standardEnergyAndMoneyCost = { { 3, 30 }, { 10, 50 }, { 1, 10 } };

	public static final double[] modeComputationTime = { 1.5, 1.0, 0.5 };
	public static final int[][] SILO_COMPUTATION_TIME = { { 10, 5 }, { 10, 5 }, { 10, 5 }, { 10, 5 } };
	public static final int[][] MIXER_COMPUTATION_TIME = { { 80, 60, 45 }, { 80, 60, 45 }, { 120, 90, 60 },
			{ 60, 45, 30 } };
	public static final int[][] TANK_COMPUTATION_TIME = { { 10 }, { 10 }, { 10 }, { 10 } };

	public static String[] PRODUCTIONS = { "Std Weiss", "Weiss Matt", "Super Glanz", "Weiss Basis" };
	public static int[] Urgency = { 1, 2, 3, 4 };
	public static int[] AMOUNT_REQUIRED = { 45, 40, 30, 20 };
	public static String ProductionUnit = "t";

	public static final double[][] DeviceTypeConfiguration = { { 0.4, 0.6 }, { 0.5, 0.2, 0.2 }, { 1.0 } };

	public static int setupProcessingTimeSwitchToOthersProcess = 10;
	public static int setupEnergyConsumptionSwitchToOthersProcess = 10;
	public static int setupMonetaryCostSwitchToOthersProcess = 1000;

	public static String[] objectives = { "makespan"/* , "energy", "montary" */, "discrepancy"/* , "urgency" */ };

	public static String getDefaultNotAvailableTime() {
		return "";
	}

	public static int getCost(int deviceTypeIndex, int modeTypeIndex, int costIndex) {
		double cost = (double) standardEnergyAndMoneyCost[deviceTypeIndex][costIndex]
				* (double) modeCost[modeTypeIndex];
		if (cost > 1) {
			return (int) Math.floor(cost);
		} else {
			return (int) Math.ceil(cost);
		}

	}

	private static List<List<List<Integer>>> getComputationTimes() {

		List<List<Integer>> siloTimes = new ArrayList<>();
		for (int i = 0; i < SILO_COMPUTATION_TIME[0].length; i++) {
			List<Integer> times = new ArrayList<>();
			for (int j = 0; j < SILO_COMPUTATION_TIME.length; j++) {

				times.add(SILO_COMPUTATION_TIME[j][i]);
			}
			siloTimes.add(times);
		}

		List<List<Integer>> mixerTimes = new ArrayList<>();
		for (int i = 0; i < MIXER_COMPUTATION_TIME[0].length; i++) {
			List<Integer> times = new ArrayList<>();
			for (int j = 0; j < MIXER_COMPUTATION_TIME.length; j++) {

				times.add(MIXER_COMPUTATION_TIME[j][i]);
			}
			mixerTimes.add(times);
		}

		List<List<Integer>> tankTimes = new ArrayList<>();
		for (int i = 0; i < TANK_COMPUTATION_TIME[0].length; i++) {
			List<Integer> times = new ArrayList<>();
			for (int j = 0; j < TANK_COMPUTATION_TIME.length; j++) {

				times.add(TANK_COMPUTATION_TIME[j][i]);
			}
			tankTimes.add(times);
		}

		List<List<List<Integer>>> results = new ArrayList<List<List<Integer>>>();
		results.add(siloTimes);
		results.add(mixerTimes);
		results.add(tankTimes);

		return results;

	}

	private static Map<String, List<Integer>> getComputationTimeMap(List<List<List<Device>>> devices) {
		List<List<List<Integer>>> times = getComputationTimes();

		Map<String, List<Integer>> computationTime = new HashMap<>();

		for (List<List<Device>> oneType : devices) {
			switch (oneType.get(0).get(0).getType()) {
			case Silo:
				int index = (int) (oneType.size() * DeviceTypeConfiguration[0][0]);

				for (int i = 0; i < oneType.size(); i++) {
					for (Device device : oneType.get(i)) {
						switch (device.getMode()) {
						case Ecomony:

							computationTime.put(device.getKey(),
									times.get(0).get(i < index ? 0 : 1).stream()
											.map(x -> (int) Math.ceil(x * modeComputationTime[0]))
											.collect(Collectors.toList()));
							break;
						case Standard:
							computationTime.put(device.getKey(), times.get(0).get(i < index ? 0 : 1));
						default:
							break;
						}

					}
				}

				break;

			case Mixer:
				int index1 = (int) (oneType.size() * DeviceTypeConfiguration[1][0]); // 5
				int index2 = index1 + (int) (oneType.size() * DeviceTypeConfiguration[1][1]); // 7

				for (int i = 0; i < oneType.size(); i++) {
					for (Device device : oneType.get(i)) {
						int computationTimeindex = -1;
						if (i < index1)
							computationTimeindex = 0;
						else if (i >= index1 && i < index2)
							computationTimeindex = 1;
						else
							computationTimeindex = 2;
						switch (device.getMode()) {
						case Ecomony:

							computationTime.put(device.getKey(),
									times.get(1).get(computationTimeindex).stream()
											.map(x -> (int) Math.ceil(x * modeComputationTime[0]))
											.collect(Collectors.toList()));
							break;
						case Standard:

							computationTime.put(device.getKey(), times.get(1).get(computationTimeindex));
							break;
						case Performance:
							computationTime.put(device.getKey(),
									times.get(1).get(computationTimeindex).stream()
											.map(x -> (int) Math.ceil(x * modeComputationTime[2]))
											.collect(Collectors.toList()));
						default:
							break;
						}

					}
				}
				break;

			case Tank:
				for (int i = 0; i < oneType.size(); i++) {
					for (Device device : oneType.get(i)) {
						computationTime.put(device.getKey(), times.get(2).get(0));
					}
				}
				break;

			default:
				break;
			}
		}

		return computationTime;
	}

	public static List<DeviceType[]> SubProcessDevicePattern() {
		List<DeviceType[]> types = new ArrayList<DeviceType[]>();
		types.add(new DeviceType[] { DeviceType.Silo, DeviceType.Mixer });
		types.add(new DeviceType[] { DeviceType.Mixer });
		types.add(new DeviceType[] { DeviceType.Mixer, DeviceType.Tank });
		return types;
	}

	public static List<SubProcessRelationPattern> relationPattern() {
		List<DeviceType[]> subProcessTypes = SubProcessDevicePattern();

		List<SubProcessRelationPattern> relations = new ArrayList<SubProcessRelationPattern>();
		relations.add(new SubProcessRelationPattern(subProcessTypes.get(0), subProcessTypes.get(1), AllenOperator.M));
		// relations.add(new SubProcessRelationPattern(subProcessTypes.get(0),
		// subProcessTypes.get(2), AllenOperator.LT));
		relations.add(new SubProcessRelationPattern(subProcessTypes.get(1), subProcessTypes.get(2), AllenOperator.M));

		// relations.add(new SubProcessRelationPattern(subProcessTypes.get(0),
		// subProcessTypes.get(4), AllenOperator.LT));
		return relations;
	}

	/********************************************************************************************************************/

	public List<List<List<Device>>> devices;
	public List<ProductionLine> productionLines;
	public List<ProductionProcess> processes;
	public List<SubProcessRelation> relations;
	public List<SequenceDependentTaskInfo> setups;

	public void getOASConfiguration() {
		/**
		 * Devices
		 */
		devices = new ArrayList<List<List<Device>>>();

		for (DeviceType deviceType : DeviceType.values()) {
			List<List<Device>> devicesForOneType = new ArrayList<List<Device>>();

			switch (deviceType) {
			case Silo:
				for (int i = 0; i < NUMBER_OF_DEVICES[0]; i++) {
					List<Device> deviceList = new ArrayList<Device>();

					for (ModeType modeType : ModeType.values()) {
						int deviceTypeIndex = Arrays.asList(DeviceType.values()).indexOf(deviceType);
						int modeTypeIndex = Arrays.asList(ModeType.values()).indexOf(modeType);
						int id = i + 1;
						Device device = null;
						switch (modeType) {
						case Ecomony:
							device = new Device(id, deviceType, modeType, getCost(deviceTypeIndex, modeTypeIndex, 0),
									getCost(deviceTypeIndex, modeTypeIndex, 1), 1, getDefaultNotAvailableTime());
							deviceList.add(device);
							break;
						case Standard:
							device = new Device(id, deviceType, modeType, getCost(deviceTypeIndex, modeTypeIndex, 0),
									getCost(deviceTypeIndex, modeTypeIndex, 1), 1, getDefaultNotAvailableTime());
							deviceList.add(device);
							break;
						// case Performance:
						// device = new Device(id, deviceType, modeType, EnergyConsumptionPerformance,
						// MonetaryCostPerformance);
						// break;
						default:
							break;
						}

					}
					devicesForOneType.add(deviceList);
				}
				break;

			case Mixer:
				for (int i = 0; i < NUMBER_OF_DEVICES[1]; i++) {
					List<Device> deviceList = new ArrayList<Device>();

					for (ModeType modeType : ModeType.values()) {
						int deviceTypeIndex = Arrays.asList(DeviceType.values()).indexOf(deviceType);
						int modeTypeIndex = Arrays.asList(ModeType.values()).indexOf(modeType);
						int id = i + 1;
						Device device = null;
						switch (modeType) {
						case Standard:
							device = new Device(id, deviceType, modeType, getCost(deviceTypeIndex, modeTypeIndex, 0),
									getCost(deviceTypeIndex, modeTypeIndex, 1), 1, getDefaultNotAvailableTime());
							deviceList.add(device);
							break;
						case Ecomony:
							device = new Device(id, deviceType, modeType, getCost(deviceTypeIndex, modeTypeIndex, 0),
									getCost(deviceTypeIndex, modeTypeIndex, 1), 1, getDefaultNotAvailableTime());
							deviceList.add(device);
							break;
						case Performance:
							device = new Device(id, deviceType, modeType, getCost(deviceTypeIndex, modeTypeIndex, 0),
									getCost(deviceTypeIndex, modeTypeIndex, 1), 1, getDefaultNotAvailableTime());
							deviceList.add(device);
							break;
						default:
							break;
						}

					}
					devicesForOneType.add(deviceList);
				}
				break;

			case Tank:
				for (int i = 0; i < NUMBER_OF_DEVICES[2]; i++) {
					List<Device> deviceList = new ArrayList<Device>();

					for (ModeType modeType : ModeType.values()) {
						int deviceTypeIndex = Arrays.asList(DeviceType.values()).indexOf(deviceType);
						int modeTypeIndex = Arrays.asList(ModeType.values()).indexOf(modeType);
						int id = i + 1;
						Device device = null;
						switch (modeType) {
						case Standard:
							device = new Device(id, deviceType, modeType, getCost(deviceTypeIndex, modeTypeIndex, 0),
									getCost(deviceTypeIndex, modeTypeIndex, 1), 1, getDefaultNotAvailableTime());
							deviceList.add(device);
							break;
						// case Ecomony:
						// device = new Device(id, deviceType, modeType, EnergyConsumptionEco,
						// MonetaryCostEco);
						// deviceList.add(device);
						// break;
						// case Performance:
						// device = new Device(id, deviceType, modeType, EnergyConsumptionPerformance,
						// MonetaryCostPerformance);
						// deviceList.add(device);
						// break;
						default:
							break;
						}

					}
					devicesForOneType.add(deviceList);
				}
				break;

			default:
				break;
			}

			devices.add(devicesForOneType);

		}

		System.out.println("----------------------------- Devices -----------------------------");
		for (List<List<Device>> oneType : devices) {
			for (List<Device> oneMode : oneType) {
				System.out.println(oneMode.toString());
			}
			System.out.println();
		}
		System.out.println("--------------------------------------------------------------------");

		Map<String, List<Integer>> processingTimeMap = getComputationTimeMap(devices);

		/**
		 * Production Lines
		 */
		productionLines = new ArrayList<ProductionLine>();

		List<List<Device>> devicesForAllLines = new ArrayList<List<Device>>();

		int SilotoMixer = devices.get(1).size() / devices.get(0).size(); // 2
		int TanktoMixer = devices.get(1).size() / devices.get(2).size(); // 5

		for (int i = 0; i < devices.get(1).size(); i++) {
			List<Device> deviceOneLine = new ArrayList<Device>();
			int siloNo = (int) Math.ceil((double) (i + 1) / (double) SilotoMixer);
			int mixerNo = i + 1;
			int tankno = (int) Math.ceil((double) (i + 1) / (double) TanktoMixer);

			deviceOneLine.add(devices.get(0).get(siloNo - 1).get(0));
			deviceOneLine.add(devices.get(1).get(mixerNo - 1).get(0));
			deviceOneLine.add(devices.get(2).get(tankno - 1).get(0));

			devicesForAllLines.add(deviceOneLine);
		}

		// for (int i = 0; i < devices.size(); i++) {
		// for (int j = 0; j < devices.get(i).size(); j++) {
		// devicesForAllLines.get(j).add(devices.get(i).get(j).get(0));
		// }
		// }

		System.out.println("\n----------------------------- Production Lines -----------------------------");
		for (int i = 0; i < devicesForAllLines.size(); i++) {
			ProductionLine productionLine = new ProductionLine(i + 1, devicesForAllLines.get(i));
			productionLines.add(productionLine);
			System.out.println(productionLine.toString());
		}
		System.out.println("-----------------------------------------------------------------------------");

		/**
		 * Production Processes
		 */
		processes = new ArrayList<>();

		for (int productionNumber = 0; productionNumber < PRODUCTIONS.length; productionNumber++) {
			String productionProcessName = PRODUCTIONS[productionNumber];
			int productionAmount = AMOUNT_REQUIRED[productionNumber];
			List<DeviceType[]> subProcessTypes = SubProcessDevicePattern();

			List<List<List<Device>>> subProcessDevices = new ArrayList<List<List<Device>>>();
			List<List<List<Integer>>> subProcessProcessingTime = new ArrayList<List<List<Integer>>>();

			for (int subProcessNumber = 0; subProcessNumber < subProcessTypes.size(); subProcessNumber++) {
				DeviceType[] subProcessType = subProcessTypes.get(subProcessNumber);

				List<List<Device>> oneSubGroup = getSubGroups(devices, subProcessType, productionLines);
				subProcessDevices.add(oneSubGroup);
				subProcessProcessingTime.add(getComputationTimes(oneSubGroup, processingTimeMap, productionNumber));
			}

			ProductionProcess process = new ProductionProcess(productionNumber + 1, productionProcessName,
					productionProcessName, productionAmount + ProductionUnit, Urgency[productionNumber],
					subProcessTypes, getCompitableResources(productionProcessName, productionLines),
					getAmountProduced(productionProcessName), subProcessDevices, subProcessProcessingTime);
			processes.add(process);
		}

		System.out.println("\n----------------------------- Production Processes -----------------------------");
		for (ProductionProcess process : processes) {
			System.out.println(process.toString());
		}
		System.out.println("----------------------------------------------------------------------------------");

		/**
		 * SubProcess Relations
		 */
		List<SubProcessRelationPattern> relationPattern = relationPattern();

		System.out.println("\n----------------------------- SubProcess Relation Pattern-----------------------------");
		for (SubProcessRelationPattern relation : relationPattern) {
			if (relation.getRelation() != AllenOperator.UnDefined)
				System.out.println(relation.toString());
		}
		System.out.println("----------------------------------------------------------------------------------");

		relations = new ArrayList<SubProcessRelation>();

		for (ProductionProcess process : processes) {
			List<Map.Entry<String, List<SubProcess>>> linear = process.getSubProcesses().entrySet().stream()
					.collect(Collectors.toList());
			linear.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

			for (Map.Entry<String, List<SubProcess>> entry : linear) {
				List<SubProcess> subporcess = new ArrayList<>();
				subporcess.addAll(entry.getValue());

				for (int i = 0; i < subporcess.size() - 1; i++) {
					for (int j = i + 1; j < subporcess.size(); j++) {
						SubProcess source = subporcess.get(i);
						SubProcess target = subporcess.get(j);

						AllenOperator relation = AllenOperator.UnDefined;

						for (int z = 0; z < relationPattern.size(); z++) {
							SubProcessRelationPattern relationType = relationPattern.get(z);
							if (compareSubProcessTypes(source.getType(), relationType.getSource())
									&& compareSubProcessTypes(target.getType(), relationType.getDestination())) {
								relation = relationType.getRelation();
							}
						}

						relations.add(new SubProcessRelation(source, target, relation));

					}
				}

			}

		}

		System.out.println("\n----------------------------- SubProcess Relation -----------------------------");
		for (SubProcessRelation relation : relations) {
			System.out.println(relation.toString());
		}
		System.out.println("----------------------------------------------------------------------------------");

		/**
		 * Dependent Setup
		 */

		List<String> subPorcesseTypes = new ArrayList<String>();
		Map<String, List<ProductionLine>> resourcesUsedBySubProcesses = new HashMap<>();
		for (int i = 0; i < processes.size(); i++) {
			ProductionProcess process = processes.get(i);

			List<Map.Entry<String, List<SubProcess>>> linear = process.getSubProcesses().entrySet().stream()
					.collect(Collectors.toList());
			linear.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

			for (int j = 0; j < linear.size(); j++) {
				subPorcesseTypes.add(linear.get(j).getKey());
			}

			resourcesUsedBySubProcesses.putAll(process.getCompitableResource());
		}

		final List<Triple<String, String, Set<String>>> sharedResources = new ArrayList<>();

		for (int i = 0; i < subPorcesseTypes.size(); i++) {
			for (int j = 0; j < subPorcesseTypes.size(); j++) {

				String source = subPorcesseTypes.get(i);
				String target = subPorcesseTypes.get(j);

				if (j == i || (source.split("")[0].equals(target.split("")[0])
						&& source.split("")[1].equals(target.split("")[1])))
					continue;

				final List<List<String>> sourcecompatNestedList = resourcesUsedBySubProcesses.get(source).stream()
						.map(c -> Arrays.asList(c.resourceName().split(" "))).collect(Collectors.toList());
				final Set<String> sourcecompat = sourcecompatNestedList.stream().flatMap(List::stream)
						.filter(r -> r.contains("(")).collect(Collectors.toSet());

				final List<List<String>> targetcompatNestedList = resourcesUsedBySubProcesses.get(target).stream()
						.map(c -> Arrays.asList(c.resourceName().split(" "))).collect(Collectors.toList());
				final Set<String> targetcompat = targetcompatNestedList.stream().flatMap(List::stream)
						.filter(r -> r.contains("(")).collect(Collectors.toSet());

				sourcecompat.retainAll(targetcompat);

				if (!sourcecompat.isEmpty())
					sharedResources.add(Triple.of(source, target, sourcecompat));
			}
		}

		setups = new ArrayList<SequenceDependentTaskInfo>();
		for (int i = 0; i < sharedResources.size(); ++i) {
			final String source = sharedResources.get(i).getLeft();
			final String target = sharedResources.get(i).getMiddle();
			final Set<String> compatibleResources = sharedResources.get(i).getRight();

			for (String resource : compatibleResources) {
				final String taskId = "DependentSetUp from " + source + " to " + target;
				setups.add(new SequenceDependentTaskInfo(setupProcessingTimeSwitchToOthersProcess,
						setupEnergyConsumptionSwitchToOthersProcess, setupMonetaryCostSwitchToOthersProcess, resource,
						source, target, taskId));
			}
		}

		System.out.println("\n----------------------------- Dependent SetUp -----------------------------");
		for (SequenceDependentTaskInfo setUp : setups) {

			System.out.println(setUp.getFullInfo());
		}
		System.out.println("----------------------------------------------------------------------------------");

	}

	/*****************************************************************************************************************/

	private Map<String, List<ProductionLine>> getCompitableResources(String ProductName, List<ProductionLine> lines) {
		Map<String, List<ProductionLine>> compitableResource = new HashMap<>();
		compitableResource.put(ProductName + " A", lines.subList(0, 5));
		compitableResource.put(ProductName + " B", lines.subList(5, 7));
		compitableResource.put(ProductName + " C", lines.subList(7, lines.size()));
		return compitableResource;
	}

	private Map<String, Integer> getAmountProduced(String ProductName) {
		Map<String, Integer> amounts = new HashMap<>();
		switch (ProductName) {
		case "Std Weiss":
			amounts.put("Std Weiss A", 5);
			amounts.put("Std Weiss B", 10);
			amounts.put("Std Weiss C", 10);
			amounts.put("Std Weiss D", 10);
			break;
		case "Weiss Matt":
			amounts.put("Weiss Matt A", 5);
			amounts.put("Weiss Matt B", 10);
			amounts.put("Weiss Matt C", 10);
			amounts.put("Weiss Matt D", 10);
			break;
		case "Super Glanz":
			amounts.put("Super Glanz A", 4);
			amounts.put("Super Glanz B", 8);
			amounts.put("Super Glanz C", 8);
			amounts.put("Super Glanz D", 8);
			break;
		case "Weiss Basis":
			amounts.put("Weiss Basis A", 6);
			amounts.put("Weiss Basis B", 12);
			amounts.put("Weiss Basis C", 12);
			amounts.put("Weiss Basis D", 12);
			break;

		default:
			break;
		}
		return amounts;
	}

	private static List<List<Integer>> getComputationTimes(List<List<Device>> devices,
			Map<String, List<Integer>> computationTimeMap, int productionIndex) {

		List<List<Integer>> computationTimes = new ArrayList<>();
		for (List<Device> oneGroup : devices) {
			List<Integer> computaionTime = new ArrayList<>();
			for (Device device : oneGroup) {
				computaionTime.add(computationTimeMap.get(device.getKey()).get(productionIndex));
			}
			computationTimes.add(computaionTime);
		}

		return computationTimes;
	}

	private static List<List<Device>> getSubGroups(List<List<List<Device>>> devices, DeviceType[] subProcessType,
			List<ProductionLine> lines) {
		List<Device> allDevices = new ArrayList<Device>();
		for (List<List<Device>> oneType : devices) {
			oneType.forEach(allDevices::addAll);
		}

		List<List<Device>> deviceForProcess = new ArrayList<List<Device>>();
		for (int i = 0; i < subProcessType.length; i++) {
			DeviceType type = subProcessType[i];
			List<Device> deviceForType = new ArrayList<Device>();

			for (Device d : allDevices) {
				if (d.getType() == type) {
					deviceForType.add(d);
				}
			}
			deviceForProcess.add(deviceForType);
		}

		if (deviceForProcess.size() < 2) {
			List<List<Device>> results = new ArrayList<List<Device>>();
			for (Device dev : deviceForProcess.get(0)) {
				List<Device> devs = new ArrayList<Device>();
				devs.add(dev);
				results.add(devs);
			}
			return results;
		}

		List<List<Device>> results = getCombine(deviceForProcess.get(0), deviceForProcess.get(1), lines);

		return results;
	}

	static List<List<Device>> getCombine(List<Device> s1, List<Device> s2, List<ProductionLine> lines) {
		List<List<Device>> result = new ArrayList<List<Device>>();

		for (int i = 0; i < s1.size(); i++) {
			for (int j = 0; j < s2.size(); j++) {
				List<Device> com = new ArrayList<Device>();
				com.add(s1.get(i));
				com.add(s2.get(j));

				List<String> comNames = new ArrayList<String>();
				comNames.add(s1.get(i).getName());
				comNames.add(s2.get(j).getName());

				for (ProductionLine line : lines) {
					if (line.getDeviceNames().containsAll(comNames)) {
						result.add(com);
						break;
					}
				}

			}
		}

		return result;
	}

	static boolean compareSubProcessTypes(DeviceType[] dt1, DeviceType[] dt2) {
		if (dt1.length != dt2.length)
			return false;

		for (int i = 0; i < dt1.length; i++) {
			if (dt1[i].toString() != dt2[i].toString())
				return false;
		}
		return true;

	}

	// static List<Device> getSameDeviceTypes(List<ProductionLine> source,
	// List<ProductionLine> target) {
	//
	// final List<List<String>> sourcecompatNestedList = source.stream().map(c ->
	// Arrays.asList(c.resourceName().split(" "))).collect(Collectors.toList());
	// final Set<String> sourcecompat =
	// sourcecompatNestedList.stream().flatMap(List::stream)
	// .filter(r -> r.contains("(")).collect(Collectors.toSet());
	//
	// final List<List<String>> targetcompatNestedList = target.stream().map(c ->
	// Arrays.asList(c.resourceName().split(" "))).collect(Collectors.toList());
	// final Set<String> targetcompat =
	// targetcompatNestedList.stream().flatMap(List::stream)
	// .filter(r -> r.contains("(")).collect(Collectors.toSet());
	//
	// sourcecompat.retainAll(targetcompat);
	//
	//
	//
	//
	// List<Device> sourceDevices =
	// source.getSubProcessGroup().stream().flatMap(List::stream).map(c -> c)
	// .collect(Collectors.toList());
	//
	// List<Device> targetDevices =
	// target.getSubProcessGroup().stream().flatMap(List::stream).map(c -> c)
	// .collect(Collectors.toList());
	//
	// List<Device> sameDevices = new ArrayList<Device>();
	//
	// for (Device sd : sourceDevices) {
	// for (Device td : targetDevices) {
	// if (sd.getKey().equals(td.getKey())) {
	// sameDevices.add(sd);
	// }
	// }
	// }
	//
	// List<Device> devices = new ArrayList<>();
	// for (Device rd : sameDevices) {
	// boolean canInsert = true;
	// for (Device d : devices) {
	// if (d.getName().equals(rd.getName())) {
	// canInsert = false;
	// }
	// }
	//
	// if (canInsert)
	// devices.add(rd);
	// }
	//
	// devices.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));
	//
	// return devices;
	// }

}
