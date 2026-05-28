package allocation.ONA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

public class ONAFactoryModel {

	/********************************************************************************************************************/

	public static Random ran = new Random();

	public static enum DeviceType {
		Small, Medium, Large
	}

	public static enum AllenOperator {
		M, UnDefined
	}

	public static int productScale = 3;
	public static int machineScale = 1;
	public static int NoPoduction = 20;

	public String[] PRODUCTIONS = null;

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
		ona.getONAConfiguration();
		System.out.println("done");
	}

	/********************************************************************************************************************/

	public List<List<Device>> devices;
	public List<ProductionProcess> processes;

	public void getONAConfiguration() {
		int[] NUMBER_OF_DEVICES = { 4 * machineScale, 4 * machineScale, 4 * machineScale };

		int NoP = NoPoduction;

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

			ProductionProcess process = new ProductionProcess(productionProcessName, compitables, processingTime,
					energy, montary, 1, productionNumber + 1, productScale);
			processes.add(process);

		}

		System.out.println("\n----------------------------- Production Processes-----------------------------");
		for (ProductionProcess process : processes) {
			System.out.println(process.toString());
		}
		System.out.println("----------------------------------------------------------------------------------");
	}

	public void getRandomConfiguration(Random ran) {
		int[] NUMBER_OF_DEVICES = { 4 * machineScale, 4 * machineScale, 4 * machineScale };

		int NoP = NoPoduction;

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

		/**
		 * Production Processes
		 */
		processes = new ArrayList<>();

		Map<Pair<String, String>, Integer> processingTimesTemplate = randomProcessingTimeTemplate();
		Map<Pair<String, String>, Double> montarysTemplate = randomMontarysTemplate();

		int prodcutionCount = 1;
		for (int productionNumber = 0; productionNumber < PRODUCTIONS.length; productionNumber++) {
			String productionProcessName = PRODUCTIONS[productionNumber];

			List<Device> compitables = generateCompitables(productionProcessName);
			List<Integer> processingTime = new ArrayList<>();
			List<Integer> montary = new ArrayList<>();
			List<Integer> energy = new ArrayList<>();

			String type = "";
			if (prodcutionCount <= 4)
				type = "S";
			else if (prodcutionCount <= 11)
				type = "M";
			else
				type = "L";

			for (Device d : compitables) {
				Pair<String, String> key = Pair.create(type, d.getName());
				int processTimeGet = 0;
				try {
					processTimeGet = processingTimesTemplate.get(key);
					switch (type) {
					case "S":
						processTimeGet += ran.nextInt(500);
						break;
					case "M":
						processTimeGet += ran.nextInt(1000);
						break;
					case "L":
						processTimeGet += ran.nextInt(2000);
						break;

					default:
						System.err.println("Production Type Error!");
						break;
					}
				} catch (Exception e) {
					System.out.println();
				}

				int energyGet = 0;
				int montaryGet = 0;
				try {
					montaryGet = (int) montarysTemplate.get(key).doubleValue();
					switch (type) {
					case "S":
						processTimeGet += ran.nextInt(200);
						break;
					case "M":
						processTimeGet += ran.nextInt(500);
						break;
					case "L":
						processTimeGet += ran.nextInt(1000);
						break;

					default:
						System.err.println("Production Type Error!");
						break;
					}
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

			ProductionProcess process = new ProductionProcess(productionProcessName, compitables, processingTime,
					energy, montary, 1, productionNumber + 1, productScale);
			processes.add(process);
			prodcutionCount++;
		}

	}

	private List<Device> generateCompitables(String productionName) {

		List<Device> comptiables = new ArrayList<>();

		comptiables.addAll(devices.get(0));
		comptiables.addAll(devices.get(1));
		comptiables.addAll(devices.get(2));

		return comptiables;
	}

	private Map<Pair<String, String>, Double> generateMontarys() {
		Map<Pair<String, String>, Double> recipeAndResourceNameToCost = new HashMap<>();

		int startIndex = 0;

		for (int i = 0; i < productScale; i++) {

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

			/////////////////////////////

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

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P3", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P3", "Medium " + (1 + startIndex)), 1096.3);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Medium " + (2 + startIndex)), 903.8);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Medium " + (3 + startIndex)), 902.8);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Medium " + (4 + startIndex)), 867.0);

			recipeAndResourceNameToCost.put(Pair.create("P3", "Large " + (1 + startIndex)), 2396.2);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Large " + (2 + startIndex)), 1967.2);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Large " + (3 + startIndex)), 1833.9);
			recipeAndResourceNameToCost.put(Pair.create("P3", "Large " + (4 + startIndex)), 1817.1);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P4", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P4", "Medium " + (1 + startIndex)), 1224.4);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Medium " + (2 + startIndex)), 1004.3);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Medium " + (3 + startIndex)), 915.4);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Medium " + (4 + startIndex)), 856.4);

			recipeAndResourceNameToCost.put(Pair.create("P4", "Large " + (1 + startIndex)), 2676.2);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Large " + (2 + startIndex)), 2185.7);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Large " + (3 + startIndex)), 2062.6);
			recipeAndResourceNameToCost.put(Pair.create("P4", "Large " + (4 + startIndex)), 1904.6);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P5", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P5", "Medium " + (1 + startIndex)), 1737.7);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Medium " + (2 + startIndex)), 1599.2);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Medium " + (3 + startIndex)), 1370.1);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Medium " + (4 + startIndex)), 1244.3);

			recipeAndResourceNameToCost.put(Pair.create("P5", "Large " + (1 + startIndex)), 3798.3);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Large " + (2 + startIndex)), 3045.3);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Large " + (3 + startIndex)), 2783.1);
			recipeAndResourceNameToCost.put(Pair.create("P5", "Large " + (4 + startIndex)), 2617.6);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P6", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P6", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P6", "Large " + (1 + startIndex)), 4865.4);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Large " + (2 + startIndex)), 3854.2);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Large " + (3 + startIndex)), 3453.0);
			recipeAndResourceNameToCost.put(Pair.create("P6", "Large " + (4 + startIndex)), 3269.4);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P7", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P7", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P7", "Large " + (1 + startIndex)), 4560.0);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Large " + (2 + startIndex)), 3826.5);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Large " + (3 + startIndex)), 3614.3);
			recipeAndResourceNameToCost.put(Pair.create("P7", "Large " + (4 + startIndex)), 3532.6);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P8", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P8", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P8", "Large " + (1 + startIndex)), 5175.0);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Large " + (2 + startIndex)), 4325.6);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Large " + (3 + startIndex)), 4274.2);
			recipeAndResourceNameToCost.put(Pair.create("P8", "Large " + (4 + startIndex)), 3982.4);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P9", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P9", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P9", "Large " + (1 + startIndex)), 7138.4);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Large " + (2 + startIndex)), 7347.4);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Large " + (3 + startIndex)), 8039.5);
			recipeAndResourceNameToCost.put(Pair.create("P9", "Large " + (4 + startIndex)), 10027.4);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P10", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P10", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P10", "Large " + (1 + startIndex)), 8823.1);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Large " + (2 + startIndex)), 9116.0);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Large " + (3 + startIndex)), 10175.0);
			recipeAndResourceNameToCost.put(Pair.create("P10", "Large " + (4 + startIndex)), 12844.6);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P11", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P11", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P11", "Large " + (1 + startIndex)), 2481.0);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Large " + (2 + startIndex)), 1755.7);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Large " + (3 + startIndex)), 1452.9);
			recipeAndResourceNameToCost.put(Pair.create("P11", "Large " + (4 + startIndex)), 1206.2);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P12", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P12", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P12", "Large " + (1 + startIndex)), 2908.8);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Large " + (2 + startIndex)), 2058.4);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Large " + (3 + startIndex)), 1816.1);
			recipeAndResourceNameToCost.put(Pair.create("P12", "Large " + (4 + startIndex)), 1583.1);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P13", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P13", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P13", "Large " + (1 + startIndex)), 6573.1);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Large " + (2 + startIndex)), 4651.4);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Large " + (3 + startIndex)), 3566.2);
			recipeAndResourceNameToCost.put(Pair.create("P13", "Large " + (4 + startIndex)), 3255.4);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P14", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P14", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P14", "Large " + (1 + startIndex)), 8764.1);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Large " + (2 + startIndex)), 6201.9);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Large " + (3 + startIndex)), 5673.8);
			recipeAndResourceNameToCost.put(Pair.create("P14", "Large " + (4 + startIndex)), 4754.9);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P15", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P15", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P15", "Large " + (1 + startIndex)), 6573.1);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Large " + (2 + startIndex)), 4651.4);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Large " + (3 + startIndex)), 4255.4);
			recipeAndResourceNameToCost.put(Pair.create("P15", "Large " + (4 + startIndex)), 3566.2);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P16", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P16", "Medium " + (1 + startIndex)), 2225.9);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Medium " + (2 + startIndex)), 1770.8);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Medium " + (3 + startIndex)), 1587.2);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Medium " + (4 + startIndex)), 1303.0);

			recipeAndResourceNameToCost.put(Pair.create("P16", "Large " + (1 + startIndex)), 8764.1);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Large " + (2 + startIndex)), 6201.9);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Large " + (3 + startIndex)), 5673.8);
			recipeAndResourceNameToCost.put(Pair.create("P16", "Large " + (4 + startIndex)), 4754.9);

			/////////////////////////////

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

			/////////////////////////////

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

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P19", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (1 + startIndex)), 1096.3);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (2 + startIndex)), 903.8);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (3 + startIndex)), 902.8);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Medium " + (4 + startIndex)), 867.0);

			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (1 + startIndex)), 2396.2);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (2 + startIndex)), 1967.2);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (3 + startIndex)), 1833.9);
			recipeAndResourceNameToCost.put(Pair.create("P19", "Large " + (4 + startIndex)), 1817.1);

			/////////////////////////////

			recipeAndResourceNameToCost.put(Pair.create("P20", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("P20", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("P20", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("P20", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("P20", "Medium " + (1 + startIndex)), 1224.4);
			recipeAndResourceNameToCost.put(Pair.create("P20", "Medium " + (2 + startIndex)), 1004.3);
			recipeAndResourceNameToCost.put(Pair.create("P20", "Medium " + (3 + startIndex)), 915.4);
			recipeAndResourceNameToCost.put(Pair.create("P20", "Medium " + (4 + startIndex)), 856.4);

			recipeAndResourceNameToCost.put(Pair.create("P20", "Large " + (1 + startIndex)), 2676.2);
			recipeAndResourceNameToCost.put(Pair.create("P20", "Large " + (2 + startIndex)), 2185.7);
			recipeAndResourceNameToCost.put(Pair.create("P20", "Large " + (3 + startIndex)), 2062.6);
			recipeAndResourceNameToCost.put(Pair.create("P20", "Large " + (4 + startIndex)), 1904.6);

			startIndex += 4;
		}

		return recipeAndResourceNameToCost;
	}

	private Map<Pair<String, String>, Integer> randomProcessingTimeTemplate() {
		final Map<Pair<String, String>, Integer> recipeAndResourceNameToCuttingTime = new HashMap<>();

		// 4 small 7 medium 9 large

		int startIndex = 0;

		for (int i = 0; i < machineScale; i++) {

			///////////////////////////// small 1

			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Small " + (1 + startIndex)), (int) (2833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Small " + (2 + startIndex)), (int) (2956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Small " + (3 + startIndex)), (int) (3042.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Small " + (4 + startIndex)), (int) (3174.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Medium " + (1 + startIndex)), (int) (2033.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Medium " + (2 + startIndex)), (int) (2156.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Medium " + (3 + startIndex)), (int) (2242.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Medium " + (4 + startIndex)), (int) (2674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Large " + (1 + startIndex)), (int) (1256.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Large " + (2 + startIndex)), (int) (1633.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Large " + (3 + startIndex)), (int) (1842.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("S", "Large " + (4 + startIndex)), (int) (1974.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Small " + (1 + startIndex)), (int) (3833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Small " + (2 + startIndex)), (int) (3956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Small " + (3 + startIndex)), (int) (3542.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Small " + (4 + startIndex)), (int) (3674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("M", "Large " + (4 + startIndex)), (int) (2650.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Small " + (1 + startIndex)), (int) (5341.3 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Small " + (2 + startIndex)), (int) (5505.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Small " + (3 + startIndex)), (int) (6205.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Small " + (4 + startIndex)), (int) (7421.3 * 3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Medium " + (1 + startIndex)), (int) (5341.3 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Medium " + (2 + startIndex)), (int) (5505.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Medium " + (3 + startIndex)), (int) (6205.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Medium " + (4 + startIndex)), (int) (7421.3 * 2));

			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("L", "Large " + (4 + startIndex)), (int) (7421.3));

			startIndex += 4;
		}
		return recipeAndResourceNameToCuttingTime;
	}

	private Map<Pair<String, String>, Double> randomMontarysTemplate() {
		Map<Pair<String, String>, Double> recipeAndResourceNameToCost = new HashMap<>();

		int startIndex = 0;

		for (int i = 0; i < productScale; i++) {
			recipeAndResourceNameToCost.put(Pair.create("S", "Small " + (1 + startIndex)), 192.1);
			recipeAndResourceNameToCost.put(Pair.create("S", "Small " + (2 + startIndex)), 168.4);
			recipeAndResourceNameToCost.put(Pair.create("S", "Small " + (3 + startIndex)), 175.9);
			recipeAndResourceNameToCost.put(Pair.create("S", "Small " + (4 + startIndex)), 167.0);

			recipeAndResourceNameToCost.put(Pair.create("S", "Medium " + (1 + startIndex)), 273.1);
			recipeAndResourceNameToCost.put(Pair.create("S", "Medium " + (2 + startIndex)), 238.6);
			recipeAndResourceNameToCost.put(Pair.create("S", "Medium " + (3 + startIndex)), 237.1);
			recipeAndResourceNameToCost.put(Pair.create("S", "Medium " + (4 + startIndex)), 230.0);

			recipeAndResourceNameToCost.put(Pair.create("S", "Large " + (1 + startIndex)), 596.9);
			recipeAndResourceNameToCost.put(Pair.create("S", "Large " + (2 + startIndex)), 519.3);
			recipeAndResourceNameToCost.put(Pair.create("S", "Large " + (3 + startIndex)), 481.6);
			recipeAndResourceNameToCost.put(Pair.create("S", "Large " + (4 + startIndex)), 462.1);

			recipeAndResourceNameToCost.put(Pair.create("M", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("M", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("M", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("M", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("M", "Medium " + (1 + startIndex)), 434.2);
			recipeAndResourceNameToCost.put(Pair.create("M", "Medium " + (2 + startIndex)), 376.3);
			recipeAndResourceNameToCost.put(Pair.create("M", "Medium " + (3 + startIndex)), 381.5);
			recipeAndResourceNameToCost.put(Pair.create("M", "Medium " + (4 + startIndex)), 361.1);

			recipeAndResourceNameToCost.put(Pair.create("M", "Large " + (1 + startIndex)), 949.1);
			recipeAndResourceNameToCost.put(Pair.create("M", "Large " + (2 + startIndex)), 819.0);
			recipeAndResourceNameToCost.put(Pair.create("M", "Large " + (3 + startIndex)), 775.0);
			recipeAndResourceNameToCost.put(Pair.create("M", "Large " + (4 + startIndex)), 756.9);

			recipeAndResourceNameToCost.put(Pair.create("L", "Small " + (1 + startIndex)), 305.5);
			recipeAndResourceNameToCost.put(Pair.create("L", "Small " + (2 + startIndex)), 265.6);
			recipeAndResourceNameToCost.put(Pair.create("L", "Small " + (3 + startIndex)), 283.1);
			recipeAndResourceNameToCost.put(Pair.create("L", "Small " + (4 + startIndex)), 262.2);

			recipeAndResourceNameToCost.put(Pair.create("L", "Medium " + (1 + startIndex)), 1096.3);
			recipeAndResourceNameToCost.put(Pair.create("L", "Medium " + (2 + startIndex)), 903.8);
			recipeAndResourceNameToCost.put(Pair.create("L", "Medium " + (3 + startIndex)), 902.8);
			recipeAndResourceNameToCost.put(Pair.create("L", "Medium " + (4 + startIndex)), 867.0);

			recipeAndResourceNameToCost.put(Pair.create("L", "Large " + (1 + startIndex)), 2396.2);
			recipeAndResourceNameToCost.put(Pair.create("L", "Large " + (2 + startIndex)), 1967.2);
			recipeAndResourceNameToCost.put(Pair.create("L", "Large " + (3 + startIndex)), 1833.9);
			recipeAndResourceNameToCost.put(Pair.create("L", "Large " + (4 + startIndex)), 1817.1);

			startIndex += 4;
		}

		return recipeAndResourceNameToCost;
	}

	private Map<Pair<String, String>, Integer> generateProcessingTimes() {
		final Map<Pair<String, String>, Integer> recipeAndResourceNameToCuttingTime = new HashMap<>();

		int startIndex = 0;

		for (int i = 0; i < machineScale; i++) {

			///////////////////////////// small 1

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

			///////////////////////////// small 2

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

			///////////////////////////// small 3

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

			///////////////////////////// small 4

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

			///////////////////////////// medium 1

			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Small " + (1 + startIndex)), (int) (3833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Small " + (2 + startIndex)), (int) (3956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Small " + (3 + startIndex)), (int) (3542.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Small " + (4 + startIndex)), (int) (3674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P3", "Large " + (4 + startIndex)), (int) (2650.4));

			///////////////////////////// medium 2

			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Small " + (1 + startIndex)), (int) (3833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Small " + (2 + startIndex)), (int) (3956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Small " + (3 + startIndex)), (int) (3542.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Small " + (4 + startIndex)), (int) (3674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P4", "Large " + (4 + startIndex)), (int) (2650.4));

			///////////////////////////// medium 3

			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Small " + (1 + startIndex)), (int) (3833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Small " + (2 + startIndex)), (int) (3956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Small " + (3 + startIndex)), (int) (3542.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Small " + (4 + startIndex)), (int) (3674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P5", "Large " + (4 + startIndex)), (int) (2650.4));

			///////////////////////////// medium 4

			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Small " + (1 + startIndex)), (int) (3833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Small " + (2 + startIndex)), (int) (3956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Small " + (3 + startIndex)), (int) (3542.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Small " + (4 + startIndex)), (int) (3674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P6", "Large " + (4 + startIndex)), (int) (2650.4));

			///////////////////////////// medium 5

			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Small " + (1 + startIndex)), (int) (3833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Small " + (2 + startIndex)), (int) (3956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Small " + (3 + startIndex)), (int) (3642.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Small " + (4 + startIndex)), (int) (3774.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P19", "Large " + (4 + startIndex)), (int) (2650.4));

			///////////////////////////// medium 6

			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Small " + (1 + startIndex)), (int) (3833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Small " + (2 + startIndex)), (int) (3956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Small " + (3 + startIndex)), (int) (3642.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Small " + (4 + startIndex)), (int) (3774.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P20", "Large " + (4 + startIndex)), (int) (2650.4));

			///////////////////////////// medium 7

			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Small " + (1 + startIndex)), (int) (3833.5));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Small " + (2 + startIndex)), (int) (3956.2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Small " + (3 + startIndex)), (int) (3542.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Small " + (4 + startIndex)), (int) (3674.1));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Medium " + (1 + startIndex)), (int) (2899.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Medium " + (2 + startIndex)), (int) (2990.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Medium " + (3 + startIndex)), (int) (3093.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Medium " + (4 + startIndex)), (int) (3250.4));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large " + (1 + startIndex)), (int) (2099.7));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large " + (2 + startIndex)), (int) (2290.0));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large " + (3 + startIndex)), (int) (2493.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P7", "Large " + (4 + startIndex)), (int) (2650.4));

			///////////////////////////// Large 1

			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Small " + (1 + startIndex)), (int) (5341.3 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Small " + (2 + startIndex)), (int) (5505.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Small " + (3 + startIndex)), (int) (6205.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Small " + (4 + startIndex)), (int) (7421.3 * 3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Medium " + (1 + startIndex)), (int) (5341.3 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Medium " + (2 + startIndex)), (int) (5505.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Medium " + (3 + startIndex)), (int) (6205.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Medium " + (4 + startIndex)), (int) (7421.3 * 2));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P8", "Large " + (4 + startIndex)), (int) (7421.3));

			///////////////////////////// Large 2

			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Small " + (1 + startIndex)), (int) (5341.3 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Small " + (2 + startIndex)), (int) (5505.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Small " + (3 + startIndex)), (int) (6205.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Small " + (4 + startIndex)), (int) (7421.3 * 3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Medium " + (1 + startIndex)), (int) (5341.3 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Medium " + (2 + startIndex)), (int) (5505.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Medium " + (3 + startIndex)), (int) (6205.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Medium " + (4 + startIndex)), (int) (7421.3 * 2));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P9", "Large " + (4 + startIndex)), (int) (7421.3));

			///////////////////////////// Large 3

			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Small " + (1 + startIndex)), (int) (5341.3 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Small " + (2 + startIndex)), (int) (5505.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Small " + (3 + startIndex)), (int) (6205.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Small " + (4 + startIndex)), (int) (7421.3 * 3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Medium " + (1 + startIndex)),
					(int) (5341.3 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Medium " + (2 + startIndex)),
					(int) (5505.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Medium " + (3 + startIndex)),
					(int) (6205.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Medium " + (4 + startIndex)),
					(int) (7421.3 * 2));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P10", "Large " + (4 + startIndex)), (int) (7421.3));

			///////////////////////////// Large 4

			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Small " + (1 + startIndex)), (int) (5341.3 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Small " + (2 + startIndex)), (int) (5505.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Small " + (3 + startIndex)), (int) (6205.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Small " + (4 + startIndex)), (int) (7421.3 * 3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Medium " + (1 + startIndex)),
					(int) (5341.3 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Medium " + (2 + startIndex)),
					(int) (5505.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Medium " + (3 + startIndex)),
					(int) (6205.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Medium " + (4 + startIndex)),
					(int) (7421.3 * 2));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P11", "Large " + (4 + startIndex)), (int) (7421.3));

			///////////////////////////// Large 5

			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Small " + (1 + startIndex)), (int) (5341.3 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Small " + (2 + startIndex)), (int) (5505.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Small " + (3 + startIndex)), (int) (6205.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Small " + (4 + startIndex)), (int) (7421.3 * 3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Medium " + (1 + startIndex)),
					(int) (5341.3 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Medium " + (2 + startIndex)),
					(int) (5505.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Medium " + (3 + startIndex)),
					(int) (6205.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Medium " + (4 + startIndex)),
					(int) (7421.3 * 2));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P12", "Large " + (4 + startIndex)), (int) (7421.3));

			///////////////////////////// Large 6

			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Small " + (1 + startIndex)), (int) (5341.3 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Small " + (2 + startIndex)), (int) (5505.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Small " + (3 + startIndex)), (int) (6205.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Small " + (4 + startIndex)), (int) (7421.3 * 3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Medium " + (1 + startIndex)),
					(int) (5341.3 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Medium " + (2 + startIndex)),
					(int) (5505.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Medium " + (3 + startIndex)),
					(int) (6205.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Medium " + (4 + startIndex)),
					(int) (7421.3 * 2));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P13", "Large " + (4 + startIndex)), (int) (7421.3));

			///////////////////////////// Large 7

			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Small " + (1 + startIndex)), (int) (5341.3 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Small " + (2 + startIndex)), (int) (5505.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Small " + (3 + startIndex)), (int) (6205.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Small " + (4 + startIndex)), (int) (7421.3 * 3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Medium " + (1 + startIndex)),
					(int) (5341.3 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Medium " + (2 + startIndex)),
					(int) (5505.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Medium " + (3 + startIndex)),
					(int) (6205.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Medium " + (4 + startIndex)),
					(int) (7421.3 * 2));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P14", "Large " + (4 + startIndex)), (int) (7421.3));

			///////////////////////////// Large 8

			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Small " + (1 + startIndex)), (int) (5341.3 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Small " + (2 + startIndex)), (int) (5505.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Small " + (3 + startIndex)), (int) (6205.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Small " + (4 + startIndex)), (int) (7421.3 * 3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Medium " + (1 + startIndex)),
					(int) (5341.3 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Medium " + (2 + startIndex)),
					(int) (5505.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Medium " + (3 + startIndex)),
					(int) (6205.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Medium " + (4 + startIndex)),
					(int) (7421.3 * 2));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P15", "Large " + (4 + startIndex)), (int) (7421.3));

			///////////////////////////// Large 9

			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Small " + (1 + startIndex)), (int) (5341.3 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Small " + (2 + startIndex)), (int) (5505.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Small " + (3 + startIndex)), (int) (6205.1 * 3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Small " + (4 + startIndex)), (int) (7421.3 * 3));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Medium " + (1 + startIndex)),
					(int) (5341.3 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Medium " + (2 + startIndex)),
					(int) (5505.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Medium " + (3 + startIndex)),
					(int) (6205.1 * 2));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Medium " + (4 + startIndex)),
					(int) (7421.3 * 2));

			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Large " + (1 + startIndex)), (int) (5341.3));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Large " + (2 + startIndex)), (int) (5505.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Large " + (3 + startIndex)), (int) (6205.1));
			recipeAndResourceNameToCuttingTime.put(Pair.create("P16", "Large " + (4 + startIndex)), (int) (7421.3));

			startIndex += 4;
		}
		return recipeAndResourceNameToCuttingTime;
	}

}
