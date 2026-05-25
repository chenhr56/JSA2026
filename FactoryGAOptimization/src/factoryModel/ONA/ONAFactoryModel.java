package factoryModel.ONA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;

import mitm.atb.SequenceDependentTaskInfo;

public class ONAFactoryModel {

	/********************************************************************************************************************/

	public static boolean runWith14 = true;

	public static Random ran = new Random();

	public static enum DeviceType {
		Small, Medium, Large
	}

	public static enum AllenOperator {
		M, UnDefined/* , LT, S, F, EQ, O, D */
	}

	// [多线程修复] 原为 public static int scale = 1; 多线程下各线程各自调用
	// startIsland() 设置不同的 scale，但静态字段会被互相覆盖。改为 ThreadLocal，
	// 每个线程独立持有自己的 scale 值，XML 读取和 presetup() 都不会读到错误 scale。
	// 使用方式: ONAFactoryModel.scale.get() 读取, ONAFactoryModel.scale.set(x) 设置。
	public static final ThreadLocal<Integer> scale = ThreadLocal.withInitial(() -> 1);

	public static String[] PRODUCTIONS = null;

	public static int numberOfCuts = 1;

	public static int setupProcessingTime = 10;
	public static int setupEnergy = 10;
	public static int setupMonetary = 1000;

	public static String[] objectives = { "makespan", /* "energy" , */ "montary" /* , "urgency" */ };

	public static String getDefaultNotAvailableTime() {
		return "";
	}

	public static void main(String args[]) {
		ONAFactoryModel ona = new ONAFactoryModel();
		ONAFactoryModel.scale.set(3);
		ona.getONAConfiguration();
		System.out.println("done");
	}

	/********************************************************************************************************************/

	public List<List<Device>> devices;
	public List<ProductionProcess> processes;
	// public List<SubProcessRelation> relations;
	public List<SequenceDependentTaskInfo> setups;

	public void getONAConfiguration() {
		int[] NUMBER_OF_DEVICES = { 4 * scale.get(), 4 * scale.get(), 4 * scale.get() };

		int NoP = 14 * scale.get();

		PRODUCTIONS = new String[NoP];
		for (int i = 0; i < NoP; i++) {
			PRODUCTIONS[i] = "P" + (i + 1);
		}

		/**
		 * Devices
		 */
		devices = new ArrayList<List<Device>>();

		for (int deviceType = 0; deviceType < DeviceType.values().length; deviceType++) {
			List<Device> devicesForOneType = new ArrayList<Device>();

			for (int i = 0; i < NUMBER_OF_DEVICES[deviceType]; i++) {
				int id = i + 1;

				devicesForOneType.add(new Device(id, DeviceType.values()[deviceType], 1, getDefaultNotAvailableTime()));
			}

			devices.add(devicesForOneType);

		}

		System.out.println("----------------------------- Devices -----------------------------");
		for (List<Device> oneType : devices) {
			for (Device d : oneType) {
				System.out.println(d.toString());
			}
			System.out.println();
		}
		System.out.println("--------------------------------------------------------------------");

		/**
		 * Production Processes
		 */
		processes = new ArrayList<>();

		Map<Pair<String, String>, Integer> processingTimes = generateProcessingTimes();
		Map<Pair<String, String>, Double> montarys = generateMontarys();

		for (int productionNumber = 0; productionNumber < PRODUCTIONS.length; productionNumber++) {
			String productionProcessName = PRODUCTIONS[productionNumber];

			List<Device> compitables = generateCompitables(productionProcessName);
			List<Integer> processingTime = new ArrayList<>();
			List<Integer> montary = new ArrayList<>();
			List<Integer> energy = new ArrayList<>();

			for (Device d : compitables) {
				Pair<String, String> key = Pair.create(productionProcessName, d.getName());
				int processTimeGet = 0;
				try {
					processTimeGet = processingTimes.get(key);
				} catch (Exception e) {
					System.out.println();
				}

				int energyGet = 0;
				int montaryGet = 0;
				try {
					montaryGet = (int) montarys.get(key).doubleValue();
				} catch (Exception e) {
					System.out.println();
				}

				switch (d.getName().split(" ")[0]) {
				case "Small":
					energyGet = 60;
					break;
				case "Medium":
					energyGet = 90;
					break;
				case "Large":

					energyGet = 120;
					break;

				default:
					break;
				}

				processingTime.add(processTimeGet);
				montary.add(montaryGet);
				energy.add(energyGet);
			}

			// if (compitables.size() == 12) {
			// ProductionProcess process = new ProductionProcess(productionProcessName,
			// compitables, processingTime,
			// energy, montary, numberOfCuts, productionNumber + 1, scale);
			// processes.add(process);
			// }
			// if (compitables.size() == 8) {
			// ProductionProcess process = new ProductionProcess(productionProcessName,
			// compitables, processingTime,
			// energy, montary, numberOfCuts, productionNumber + 1, scale);
			// processes.add(process);
			// }
			// if (compitables.size() == 4) {
			ProductionProcess process = new ProductionProcess(productionProcessName, compitables, processingTime,
					energy, montary, 1, productionNumber + 1, scale.get());
			processes.add(process);
			// }

		}

		System.out.println("\n----------------------------- Production Processes-----------------------------");
		for (ProductionProcess process : processes) {
			System.out.println(process.toString());
		}
		System.out.println("----------------------------------------------------------------------------------");

		// /**
		// * Subprocess Relations
		// */
		//
		// relations = new ArrayList<SubProcessRelation>();
		//
		// for (ProductionProcess production : processes) {
		//
		// for (int i = 0; i < production.getSubProcesses().size() - 1; i++) {
		// SubProcess source = production.getSubProcesses().get(i);
		// SubProcess target = production.getSubProcesses().get(i + 1);
		//
		// SubProcessRelation relation = new SubProcessRelation(source, target,
		// AllenOperator.M);
		// relations.add(relation);
		// }
		// }
		//
		// System.out.println("\n----------------------------- Subprocess Relation
		// -----------------------------");
		// for (SubProcessRelation relation : relations) {
		// System.out.println(relation.toString());
		// }
		// System.out.println("----------------------------------------------------------------------------------");

		/**
		 * Dependent Setup
		 */

		Map<String, List<Device>> resourcesUsedBySubProcesses = new HashMap<>();
		for (int i = 0; i < processes.size(); i++) {
			ProductionProcess process = processes.get(i);
			resourcesUsedBySubProcesses.put(process.getName(), process.compitableResource);
		}

		final List<Triple<String, String, Set<String>>> sharedResources = new ArrayList<>();

		for (int i = 0; i < processes.size(); i++) {
			for (int j = 0; j < processes.size(); j++) {

				String source = processes.get(i).getName();
				String target = processes.get(j).getName();

				if (j == i)
					continue;

				final List<String> sourcecompatNestedList = resourcesUsedBySubProcesses.get(source).stream()
						.map(c -> c.getName()).collect(Collectors.toList());
				final Set<String> sourcecompat = sourcecompatNestedList.stream().collect(Collectors.toSet());

				final List<String> targetcompatNestedList = resourcesUsedBySubProcesses.get(target).stream()
						.map(c -> c.getName()).collect(Collectors.toList());
				final Set<String> targetcompat = targetcompatNestedList.stream().collect(Collectors.toSet());

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
				setups.add(new SequenceDependentTaskInfo(setupProcessingTime, setupEnergy, setupMonetary, resource,
						source, target, taskId));
			}
		}

		System.out.println("\n----------------------------- Dependent SetUp -----------------------------");
		for (SequenceDependentTaskInfo setUp : setups) {

			System.out.println(setUp.getFullInfo());
		}
		System.out.println("----------------------------------------------------------------------------------");

	}

	private List<Device> generateCompitables(String productionName) {

		List<Device> comptiables = new ArrayList<>();

		switch (productionName) {
		case "P1":
			comptiables.addAll(devices.get(0));
			comptiables.addAll(devices.get(1));
			comptiables.addAll(devices.get(2));
			break;
		case "P2":
			comptiables.addAll(devices.get(0));
			comptiables.addAll(devices.get(1));
			comptiables.addAll(devices.get(2));
			break;
		case "P3":
			comptiables.addAll(devices.get(1));
			comptiables.addAll(devices.get(2));
			break;
		case "P4":
			comptiables.addAll(devices.get(1));
			comptiables.addAll(devices.get(2));
			break;
		case "P5":
			comptiables.addAll(devices.get(1));
			comptiables.addAll(devices.get(2));
			break;
		case "P6":
			comptiables.addAll(devices.get(1));
			comptiables.addAll(devices.get(2));
			break;
		case "P7":
			comptiables.addAll(devices.get(2));
			break;
		case "P8":
			comptiables.addAll(devices.get(2));
			break;
		case "P9":
			comptiables.addAll(devices.get(2));
			break;
		case "P10":
			comptiables.addAll(devices.get(2));
			break;
		case "P11":
			comptiables.addAll(devices.get(2));
			break;
		case "P12":
			comptiables.addAll(devices.get(2));
			break;
		case "P13":
			comptiables.addAll(devices.get(2));
			break;
		case "P14":
			comptiables.addAll(devices.get(2));
			break;
		case "P15":
			comptiables.addAll(devices.get(2));
			break;
		case "P16":
			comptiables.addAll(devices.get(2));
			break;

		case "P17":
			comptiables.addAll(devices.get(0));
			comptiables.addAll(devices.get(1));
			comptiables.addAll(devices.get(2));
			break;
		case "P18":
			comptiables.addAll(devices.get(0));
			comptiables.addAll(devices.get(1));
			comptiables.addAll(devices.get(2));
			break;
		case "P19":
			comptiables.addAll(devices.get(1));
			comptiables.addAll(devices.get(2));
			break;
		case "P20":
			comptiables.addAll(devices.get(1));
			comptiables.addAll(devices.get(2));
			break;
		default:
			break;
		}

		return comptiables;
	}

	private Map<Pair<String, String>, Double> generateMontarys() {
		Map<Pair<String, String>, Double> recipeAndResourceNameToCost = new HashMap<>();

		int startIndex = 0;

		for (int i = 0; i < scale.get(); i++) {

			recipeAndResourceNameToCost.put(Pair.create("P1", "Small " + (1 + startIndex)), 192.1);
			recipeAndResourceNameToCost.put(Pair.create("P1", "Small " + (2 + startIndex)), 168.4);
			recipeAndResourceNameToCost.put(Pair.create("P1", "Small " + (3 + startIndex)), 175.9);
			recipeAndResourceNameToCost.put(Pair.create("P1", "Small " + (4 + startIndex)), 167.0);

			recipeAndResourceNameToCost.put(Pair.create("P1", "Medium " + (1 + startIndex)), 273.1);
			recipeAndResourceNameToCost.put(Pair.create("P1", "Medium " + (2 + startIndex)), 238.6);
			recipeAndResourceNameToCost.put(Pair.create("P1", "Medium " + (3 + startIndex)), 237.1);
			recipeAndResourceNameToCost.put(Pair.create("P1", "Medium " + (4 + startIndex)), 230.0);

			recipeAndResourceNameToCost.put(Pair.create("P1", "Large " + (1 + startIndex)), 596.9);
			recipeAndResourceNameToCost.put(Pair.create("P1", "Large " + (2 + startIndex)), 519.3);
			recipeAndResourceNameToCost.put(Pair.create("P1", "Large " + (3 + startIndex)), 481.6);
			recipeAndResourceNameToCost.put(Pair.create("P1", "Large " + (4 + startIndex)), 462.1);

			recipeAndResourceNameToCost.put(Pair.create("P2", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P2", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P2", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P2", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P2", "Medium " + (1 + startIndex)), 434.2);
			recipeAndResourceNameToCost.put(Pair.create("P2", "Medium " + (2 + startIndex)), 376.3);
			recipeAndResourceNameToCost.put(Pair.create("P2", "Medium " + (3 + startIndex)), 381.5);
			recipeAndResourceNameToCost.put(Pair.create("P2", "Medium " + (4 + startIndex)), 361.1);

			recipeAndResourceNameToCost.put(Pair.create("P2", "Large " + (1 + startIndex)), 949.1);
			recipeAndResourceNameToCost.put(Pair.create("P2", "Large " + (2 + startIndex)), 819.0);
			recipeAndResourceNameToCost.put(Pair.create("P2", "Large " + (3 + startIndex)), 775.0);
			recipeAndResourceNameToCost.put(Pair.create("P2", "Large " + (4 + startIndex)), 756.9);

			recipeAndResourceNameToCost.put(Pair.create("P3", "Medium " + (1 + startIndex)), 1096.3);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Medium " + (2 + startIndex)), 903.8);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Medium " + (3 + startIndex)), 902.8);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Medium " + (4 + startIndex)), 867.0);

			recipeAndResourceNameToCost.put(Pair.create("P3", "Large " + (1 + startIndex)), 2396.2);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Large " + (2 + startIndex)), 1967.2);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Large " + (3 + startIndex)), 1833.9);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Large " + (4 + startIndex)), 1817.1);

			recipeAndResourceNameToCost.put(Pair.create("P4", "Medium " + (1 + startIndex)), 1224.4);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Medium " + (2 + startIndex)), 1004.3);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Medium " + (3 + startIndex)), 915.4);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Medium " + (4 + startIndex)), 856.4);

			recipeAndResourceNameToCost.put(Pair.create("P4", "Large " + (1 + startIndex)), 2676.2);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Large " + (2 + startIndex)), 2185.7);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Large " + (3 + startIndex)), 2062.6);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Large " + (4 + startIndex)), 1904.6);

			recipeAndResourceNameToCost.put(Pair.create("P5", "Medium " + (1 + startIndex)), 1737.7);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Medium " + (2 + startIndex)), 1599.2);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Medium " + (3 + startIndex)), 1370.1);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Medium " + (4 + startIndex)), 1244.3);

			recipeAndResourceNameToCost.put(Pair.create("P5", "Large " + (1 + startIndex)), 3798.3);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Large " + (2 + startIndex)), 3045.3);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Large " + (3 + startIndex)), 2783.1);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Large " + (4 + startIndex)), 2617.6);

			recipeAndResourceNameToCost.put(Pair.create("P6", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P6", "Large " + (1 + startIndex)), 4865.4);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Large " + (2 + startIndex)), 3854.2);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Large " + (3 + startIndex)), 3453.0);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Large " + (4 + startIndex)), 3269.4);

			recipeAndResourceNameToCost.put(Pair.create("P7", "Large " + (1 + startIndex)), 4560.0);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Large " + (2 + startIndex)), 3826.5);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Large " + (3 + startIndex)), 3614.3);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Large " + (4 + startIndex)), 3532.6);

			recipeAndResourceNameToCost.put(Pair.create("P8", "Large " + (1 + startIndex)), 5175.0);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Large " + (2 + startIndex)), 4325.6);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Large " + (3 + startIndex)), 4274.2);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Large " + (4 + startIndex)), 3982.4);

			recipeAndResourceNameToCost.put(Pair.create("P9", "Large " + (1 + startIndex)), 7138.4);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Large " + (2 + startIndex)), 7347.4);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Large " + (3 + startIndex)), 8039.5);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Large " + (4 + startIndex)), 10027.4);

			recipeAndResourceNameToCost.put(Pair.create("P10", "Large " + (1 + startIndex)), 8823.1);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Large " + (2 + startIndex)), 9116.0);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Large " + (3 + startIndex)), 10175.0);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Large " + (4 + startIndex)), 12844.6);

			recipeAndResourceNameToCost.put(Pair.create("P11", "Large " + (1 + startIndex)), 2481.0);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Large " + (2 + startIndex)), 1755.7);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Large " + (3 + startIndex)), 1452.9);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Large " + (4 + startIndex)), 1206.2);

			recipeAndResourceNameToCost.put(Pair.create("P12", "Large " + (1 + startIndex)), 2908.8);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Large " + (2 + startIndex)), 2058.4);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Large " + (3 + startIndex)), 1816.1);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Large " + (4 + startIndex)), 1583.1);

			recipeAndResourceNameToCost.put(Pair.create("P13", "Large " + (1 + startIndex)), 6573.1);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Large " + (2 + startIndex)), 4651.4);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Large " + (3 + startIndex)), 3566.2);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Large " + (4 + startIndex)), 3255.4);

			recipeAndResourceNameToCost.put(Pair.create("P14", "Large " + (1 + startIndex)), 8764.1);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Large " + (2 + startIndex)), 6201.9);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Large " + (3 + startIndex)), 5673.8);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Large " + (4 + startIndex)), 4754.9);

			recipeAndResourceNameToCost.put(Pair.create("P15", "Large " + (1 + startIndex)), 6573.1);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Large " + (2 + startIndex)), 4651.4);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Large " + (3 + startIndex)), 4255.4);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Large " + (4 + startIndex)), 3566.2);

			recipeAndResourceNameToCost.put(Pair.create("P16", "Large " + (1 + startIndex)), 8764.1);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Large " + (2 + startIndex)), 6201.9);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Large " + (3 + startIndex)), 5673.8);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Large " + (4 + startIndex)), 4754.9);

			recipeAndResourceNameToCost.put(Pair.create("P17", "Small " + (1 + startIndex)), 192.1);
			recipeAndResourceNameToCost.put(Pair.create("P17", "Small " + (2 + startIndex)), 168.4);
			recipeAndResourceNameToCost.put(Pair.create("P17", "Small " + (3 + startIndex)), 175.9);
			recipeAndResourceNameToCost.put(Pair.create("P17", "Small " + (4 + startIndex)), 167.0);

			recipeAndResourceNameToCost.put(Pair.create("P17", "Medium " + (1 + startIndex)), 273.1);
			recipeAndResourceNameToCost.put(Pair.create("P17", "Medium " + (2 + startIndex)), 238.6);
			recipeAndResourceNameToCost.put(Pair.create("P17", "Medium " + (3 + startIndex)), 237.1);
			recipeAndResourceNameToCost.put(Pair.create("P17", "Medium " + (4 + startIndex)), 230.0);

			recipeAndResourceNameToCost.put(Pair.create("P17", "Large " + (1 + startIndex)), 596.9);
			recipeAndResourceNameToCost.put(Pair.create("P17", "Large " + (2 + startIndex)), 519.3);
			recipeAndResourceNameToCost.put(Pair.create("P17", "Large " + (3 + startIndex)), 481.6);
			recipeAndResourceNameToCost.put(Pair.create("P17", "Large " + (4 + startIndex)), 462.1);

			recipeAndResourceNameToCost.put(Pair.create("P18", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P18", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P18", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P18", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P18", "Medium " + (1 + startIndex)), 434.2);
			recipeAndResourceNameToCost.put(Pair.create("P18", "Medium " + (2 + startIndex)), 376.3);
			recipeAndResourceNameToCost.put(Pair.create("P18", "Medium " + (3 + startIndex)), 381.5);
			recipeAndResourceNameToCost.put(Pair.create("P18", "Medium " + (4 + startIndex)), 361.1);

			recipeAndResourceNameToCost.put(Pair.create("P18", "Large " + (1 + startIndex)), 949.1);
			recipeAndResourceNameToCost.put(Pair.create("P18", "Large " + (2 + startIndex)), 819.0);
			recipeAndResourceNameToCost.put(Pair.create("P18", "Large " + (3 + startIndex)), 775.0);
			recipeAndResourceNameToCost.put(Pair.create("P18", "Large " + (4 + startIndex)), 756.9);

			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (1 + startIndex)), 1096.3);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (2 + startIndex)), 903.8);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (3 + startIndex)), 902.8);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (4 + startIndex)), 867.0);

			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (1 + startIndex)), 2396.2);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (2 + startIndex)), 1967.2);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (3 + startIndex)), 1833.9);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (4 + startIndex)), 1817.1);

			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (1 + startIndex)), 1224.4);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (2 + startIndex)), 1004.3);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (3 + startIndex)), 915.4);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (4 + startIndex)), 856.4);

			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (1 + startIndex)), 2676.2);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (2 + startIndex)), 2185.7);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (3 + startIndex)), 2062.6);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (4 + startIndex)), 1904.6);

			startIndex += 4;
		}

		return recipeAndResourceNameToCost;
	}

	private Map<Pair<String, String>, Integer> generateProcessingTimes() {
		final Map<Pair<String, String>, Integer> recipeAndResourceNameToCuttingTime = new HashMap<>();

		int startIndex = 0;

		for (int i = 0; i < scale.get(); i++) {
			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small " + (1 + startIndex)), (int) (2833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small " + (2 + startIndex)), (int) (2956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small " + (3 + startIndex)), (int) (3042.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Small " + (4 + startIndex)), (int) (3174.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium " + (1 + startIndex)), (int) (2033.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium " + (2 + startIndex)), (int) (2156.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium " + (3 + startIndex)), (int) (2242.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Medium " + (4 + startIndex)), (int) (2674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large " + (1 + startIndex)), (int) (1256.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large " + (2 + startIndex)), (int) (1633.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large " + (3 + startIndex)), (int) (1842.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P1", "Large " + (4 + startIndex)), (int) (1974.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small " + (1 + startIndex)), (int) (2833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small " + (2 + startIndex)), (int) (2956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small " + (3 + startIndex)), (int) (3042.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Small " + (4 + startIndex)), (int) (3174.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium " + (1 + startIndex)), (int) (2033.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium " + (2 + startIndex)), (int) (2156.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium " + (3 + startIndex)), (int) (2242.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Medium " + (4 + startIndex)), (int) (2674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large " + (1 + startIndex)), (int) (1256.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large " + (2 + startIndex)), (int) (1633.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large " + (3 + startIndex)), (int) (1842.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P2", "Large " + (4 + startIndex)), (int) (1974.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large " + (4 + startIndex)), (int) (2650.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large " + (4 + startIndex)), (int) (2650.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large " + (4 + startIndex)), (int) (2650.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large " + (4 + startIndex)), (int) (2650.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large " + (4 + startIndex)), (int) (7421.3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large " + (4 + startIndex)), (int) (7421.3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large " + (4 + startIndex)), (int) (7421.3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large " + (4 + startIndex)), (int) (7421.3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large " + (4 + startIndex)), (int) (7421.3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large " + (4 + startIndex)), (int) (7421.3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large " + (4 + startIndex)), (int) (7421.3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large " + (4 + startIndex)), (int) (7421.3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Large " + (4 + startIndex)), (int) (7421.3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Large " + (4 + startIndex)), (int) (7421.3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Small " + (1 + startIndex)), (int) (2833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Small " + (2 + startIndex)), (int) (2956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Small " + (3 + startIndex)), (int) (3042.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Small " + (4 + startIndex)), (int) (3174.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Medium " + (1 + startIndex)), (int) (2033.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Medium " + (2 + startIndex)), (int) (2156.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Medium " + (3 + startIndex)), (int) (2242.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Medium " + (4 + startIndex)), (int) (2674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Large " + (1 + startIndex)), (int) (1256.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Large " + (2 + startIndex)), (int) (1633.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Large " + (3 + startIndex)), (int) (1842.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P17", "Large " + (4 + startIndex)), (int) (1974.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Small " + (1 + startIndex)), (int) (2833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Small " + (2 + startIndex)), (int) (2956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Small " + (3 + startIndex)), (int) (3042.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Small " + (4 + startIndex)), (int) (3174.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Medium " + (1 + startIndex)), (int) (2033.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Medium " + (2 + startIndex)), (int) (2156.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Medium " + (3 + startIndex)), (int) (2242.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Medium " + (4 + startIndex)), (int) (2674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Large " + (1 + startIndex)), (int) (1256.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Large " + (2 + startIndex)), (int) (1633.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Large " + (3 + startIndex)), (int) (1842.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P18", "Large " + (4 + startIndex)), (int) (1974.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Large " + (4 + startIndex)), (int) (2650.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Large " + (4 + startIndex)), (int) (2650.4));

			startIndex += 4;
		}
		return recipeAndResourceNameToCuttingTime;
	}

}
