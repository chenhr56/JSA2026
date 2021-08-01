package factoryModel.Electrolux;

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

public class ElectroluxFactoryModel {

	/********************************************************************************************************************/

	public static Random ran = new Random();

	public static int MAX_PRIORITY = 100;
	public static int percentAvailability = 100;

	public static enum AllenOperator {
		M, UnDefined
	}

	public static enum DeviceType {
		Hob, Pot
	}

	public static int[] NUMBER_OF_DEVICES = { 4, 3 };
	public static int DEVICE_ONE_ZONE = 4;

	public static String[] PRODUCTIONS = { "Boiled Water", "Pasta", "Rice", "Beef", "Potato", "Mushroom" };

	public static int[] Urgency = { 1, 2, 3, 4, 5, 6 };

	public static int setupProcessingTimeSwitch = 10;
	public static int setupEnergyConsumptionSwitch = 10;
	public static int setupMonetaryCostSwitch = 1000;

	public static String[] objectives = { "makespan", /* "energy", "montary", "discrepancy", "urgency", */ "quality" };

	public static String getDefaultNotAvailableTime() {
		return "";
	}

	public static void main(String args[]) {
		ElectroluxFactoryModel eModel = new ElectroluxFactoryModel();
		eModel.getElectroluxConfiguration();
		System.out.println();
	}

	/********************************************************************************************************************/

	public List<List<Device>> devices;
	public List<CookingZone> cookingZone;
	public List<ProductionProcess> processes;
	public List<SequenceDependentTaskInfo> setups;

	public void getElectroluxConfiguration() {

		/**
		 * Devices
		 */
		devices = new ArrayList<List<Device>>();
		int deviceCount = 0;
		for (DeviceType deviceType : DeviceType.values()) {
			List<Device> devicesForOneType = new ArrayList<Device>();

			for (int i = 0; i < NUMBER_OF_DEVICES[deviceCount]; i++) {
				int id = i + 1;
				Device device = new Device(id, deviceType, 1, getDefaultNotAvailableTime());
				devicesForOneType.add(device);
			}

			devices.add(devicesForOneType);
			deviceCount++;
		}

		List<Device> hobs = devices.get(0);
		int NoL = devices.get(0).size() / DEVICE_ONE_ZONE;

		List<List<Device>> processedDevices = new ArrayList<>();
		for (int i = 0; i < NoL; i++) {
			processedDevices.add(new ArrayList<>());
		}

		for (int i = 0; i < hobs.size(); i++) {
			int indexTo = i / DEVICE_ONE_ZONE;
			processedDevices.get(indexTo).add(hobs.get(i));
		}

		devices.remove(0);
		devices.addAll(0, processedDevices);

		System.out.println("----------------------------- Devices -----------------------------");
		for (List<Device> oneType : devices)
			System.out.println(oneType.toString());

		System.out.println("--------------------------------------------------------------------");

		/**
		 * Cooking Zones
		 */
		cookingZone = new ArrayList<CookingZone>();

		for (int i = 0; i < devices.size() - 1; i++) {
			cookingZone.addAll(getCookingZones(devices.get(i), cookingZone.size() + 1));
		}

		cookingZone.sort((c1, c2) -> c1.devices.size() - c2.devices.size());

		for (int i = 0; i < cookingZone.size(); i++) {
			cookingZone.get(i).id = i + 1;
			cookingZone.get(i).name = "CookingZone" + " " + (i + 1);
		}

		System.out.println("\n----------------------------- Cooking Zones -----------------------------");
		for (int i = 0; i < cookingZone.size(); i++) {
			System.out.println(cookingZone.get(i).toString());
		}
		System.out.println("-----------------------------------------------------------------------------");

		/**
		 * Production Processes
		 */
		processes = new ArrayList<>();

		for (int productionNumber = 0; productionNumber < PRODUCTIONS.length; productionNumber++) {
			String productionProcessName = PRODUCTIONS[productionNumber];
			int productionAmount = AMOUNT_REQUIRED[productionNumber];

			ProductionProcess process = new ProductionProcess(productionNumber + 1, productionProcessName,
					productionProcessName, productionAmount + ProductionUnit, Urgency[productionNumber],
					getPredecessor(productionProcessName), getAmountProduced(productionProcessName),
					getComptiableZones(productionProcessName), getEnergy(productionProcessName),
					getComputationTime(productionProcessName), getMontary(productionProcessName),
					getQuality(productionProcessName));
			processes.add(process);
		}

		System.out.println("\n----------------------------- Production Processes -----------------------------");
		for (ProductionProcess process : processes) {
			System.out.println(process.toString());
		}
		System.out.println("----------------------------------------------------------------------------------");

		/**
		 * Dependent Setup
		 */
		List<String> subPorcesseTypes = new ArrayList<String>();
		Map<String, List<CookingZone>> resourcesUsedBySubProcesses = new HashMap<>();
		for (int i = 0; i < processes.size(); i++) {
			ProductionProcess process = processes.get(i);

			List<Map.Entry<String, Integer>> linear = process.getAmountProduced().entrySet().stream()
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
				setups.add(new SequenceDependentTaskInfo(setupProcessingTimeSwitch, setupEnergyConsumptionSwitch,
						setupMonetaryCostSwitch, resource, source, target, taskId));
			}
		}

		System.out.println("\n----------------------------- Dependent SetUp -----------------------------");
		for (SequenceDependentTaskInfo setUp : setups) {

			System.out.println(setUp.getFullInfo());
		}
		System.out.println("----------------------------------------------------------------------------------");

	}

	/*****************************************************************************************************************/

	private List<CookingZone> getCookingZones(List<Device> devicesToBeProcessed, int zoneCounter) {
		assert (devicesToBeProcessed.size() == 4);
		List<CookingZone> zones = new ArrayList<>();

		for (int i = 0; i < devicesToBeProcessed.size(); i++) {
			List<Device> deviceInOneZone = new ArrayList<>();
			deviceInOneZone.add(devicesToBeProcessed.get(i));
			deviceInOneZone.add(devices.get(devices.size() - 1).get(0));
			CookingZone zone = new CookingZone(zoneCounter, deviceInOneZone);
			zones.add(zone);
			zoneCounter++;
		}

		for (int i = 0; i < devicesToBeProcessed.size(); i++) {
			if ((i + 1) % 2 == 0) {
				List<Device> deviceInOneZone = new ArrayList<>();
				deviceInOneZone.add(devicesToBeProcessed.get(i - 1));
				deviceInOneZone.add(devicesToBeProcessed.get(i));
				deviceInOneZone.add(devices.get(devices.size() - 1).get(1));
				CookingZone zone = new CookingZone(zoneCounter, deviceInOneZone);
				zones.add(zone);
				zoneCounter++;
			}
		}

		for (int i = 0; i < devicesToBeProcessed.size(); i++) {
			if ((i + 1) % 3 == 0) {
				List<Device> deviceInOneZone = new ArrayList<>();
				deviceInOneZone.add(devicesToBeProcessed.get(i - 2));
				deviceInOneZone.add(devicesToBeProcessed.get(i - 1));
				deviceInOneZone.add(devicesToBeProcessed.get(i));
				deviceInOneZone.add(devices.get(devices.size() - 1).get(2));
				CookingZone zone = new CookingZone(zoneCounter, deviceInOneZone);
				zones.add(zone);
				zoneCounter++;
			}
		}

		return zones;
	}

	private Map<String, String> getPredecessor(String ProductName) {

		Map<String, String> predecessors = new HashMap<>();
		switch (ProductName) {
		case "Boiled Water":
			predecessors.put("Boiled Water A", "");
			predecessors.put("Boiled Water B", "");
			predecessors.put("Boiled Water C", "");
			break;
		case "Pasta":
			predecessors.put("Pasta A", "Boiled Water A");
			predecessors.put("Pasta B", "Boiled Water A");
			predecessors.put("Pasta C", "Boiled Water B");
			predecessors.put("Pasta D", "Boiled Water B");
			predecessors.put("Pasta E", "Boiled Water C");
			predecessors.put("Pasta F", "Boiled Water C");
			break;
		case "Rice":
			predecessors.put("Rice A", "Boiled Water A");
			predecessors.put("Rice B", "Boiled Water A");
			predecessors.put("Rice C", "Boiled Water B");
			predecessors.put("Rice D", "Boiled Water B");
			predecessors.put("Rice E", "Boiled Water C");
			predecessors.put("Rice F", "Boiled Water C");
			break;
		case "Beef":
			predecessors.put("Beef A", "Boiled Water A");
			predecessors.put("Beef B", "Boiled Water A");
			predecessors.put("Beef C", "Boiled Water B");
			predecessors.put("Beef D", "Boiled Water B");
			predecessors.put("Beef E", "Boiled Water C");
			predecessors.put("Beef F", "Boiled Water C");
			break;
		case "Potato":
			predecessors.put("Potato A", "Boiled Water A");
			predecessors.put("Potato B", "Boiled Water A");
			predecessors.put("Potato C", "Boiled Water B");
			predecessors.put("Potato D", "Boiled Water B");
			predecessors.put("Potato E", "Boiled Water C");
			predecessors.put("Potato F", "Boiled Water C");
			break;
		case "Mushroom":
			predecessors.put("Mushroom A", "");
			predecessors.put("Mushroom B", "");
			predecessors.put("Mushroom C", "");
			predecessors.put("Mushroom D", "");
			predecessors.put("Mushroom E", "");
			predecessors.put("Mushroom F", "");
			break;

		default:
			break;
		}
		return predecessors;
	}

	private Map<String, List<CookingZone>> getComptiableZones(String ProductName) {
		List<List<CookingZone>> types = new ArrayList<>();
		types.add(cookingZone.stream().filter(zone -> zone.devices.size() == 2).collect(Collectors.toList()));
		types.add(cookingZone.stream().filter(zone -> zone.devices.size() == 3).collect(Collectors.toList()));
		types.add(cookingZone.stream().filter(zone -> zone.devices.size() == 4).collect(Collectors.toList()));

		Map<String, List<CookingZone>> comptiableZones = new HashMap<>();
		switch (ProductName) {
		case "Boiled Water":
			comptiableZones.put("Boiled Water A", types.get(0));
			comptiableZones.put("Boiled Water B", types.get(1));
			comptiableZones.put("Boiled Water C", types.get(2));
			break;
		case "Pasta":
			comptiableZones.put("Pasta A", types.get(0));
			comptiableZones.put("Pasta B", types.get(0));
			comptiableZones.put("Pasta C", types.get(1));
			comptiableZones.put("Pasta D", types.get(1));
			comptiableZones.put("Pasta E", types.get(2));
			comptiableZones.put("Pasta F", types.get(2));
			break;
		case "Rice":
			comptiableZones.put("Rice A", types.get(0));
			comptiableZones.put("Rice B", types.get(0));
			comptiableZones.put("Rice C", types.get(1));
			comptiableZones.put("Rice D", types.get(1));
			comptiableZones.put("Rice E", types.get(2));
			comptiableZones.put("Rice F", types.get(2));
			break;
		case "Beef":
			comptiableZones.put("Beef A", types.get(0));
			comptiableZones.put("Beef B", types.get(0));
			comptiableZones.put("Beef C", types.get(1));
			comptiableZones.put("Beef D", types.get(1));
			comptiableZones.put("Beef E", types.get(2));
			comptiableZones.put("Beef F", types.get(2));
			break;
		case "Potato":
			comptiableZones.put("Potato A", types.get(0));
			comptiableZones.put("Potato B", types.get(0));
			comptiableZones.put("Potato C", types.get(1));
			comptiableZones.put("Potato D", types.get(1));
			comptiableZones.put("Potato E", types.get(2));
			comptiableZones.put("Potato F", types.get(2));
			break;
		case "Mushroom":
			comptiableZones.put("Mushroom A", types.get(0));
			comptiableZones.put("Mushroom B", types.get(0));
			comptiableZones.put("Mushroom C", types.get(1));
			comptiableZones.put("Mushroom D", types.get(1));
			comptiableZones.put("Mushroom E", types.get(2));
			comptiableZones.put("Mushroom F", types.get(2));
			break;

		default:
			break;
		}
		return comptiableZones;
	}

	private Map<String, Integer> getMontary(String ProductName) {
		Map<String, Integer> amounts = new HashMap<>();
		switch (ProductName) {
		case "Boiled Water":
			amounts.put("Boiled Water A", 3);
			amounts.put("Boiled Water B", 6);
			amounts.put("Boiled Water C", 9);
			break;
		case "Pasta":
			amounts.put("Pasta A", 21);
			amounts.put("Pasta B", 18);
			amounts.put("Pasta C", 21);
			amounts.put("Pasta D", 18);
			amounts.put("Pasta E", 21);
			amounts.put("Pasta F", 18);
			break;
		case "Rice":
			amounts.put("Rice A", 45);
			amounts.put("Rice B", 39);
			amounts.put("Rice C", 45);
			amounts.put("Rice D", 39);
			amounts.put("Rice E", 45);
			amounts.put("Rice F", 39);
			break;
		case "Beef":
			amounts.put("Beef A", 27);
			amounts.put("Beef B", 18);
			amounts.put("Beef C", 27);
			amounts.put("Beef D", 18);
			amounts.put("Beef E", 27);
			amounts.put("Beef F", 18);
			break;
		case "Potato":
			amounts.put("Potato A", 66);
			amounts.put("Potato B", 60);
			amounts.put("Potato C", 66);
			amounts.put("Potato D", 60);
			amounts.put("Potato E", 66);
			amounts.put("Potato F", 60);
			break;
		case "Mushroom":
			amounts.put("Mushroom A", 72);
			amounts.put("Mushroom B", 60);
			amounts.put("Mushroom C", 90);
			amounts.put("Mushroom D", 78);
			amounts.put("Mushroom E", 108);
			amounts.put("Mushroom F", 96);
			break;

		default:
			break;
		}
		return amounts;
	}

	private Map<String, Integer> getEnergy(String ProductName) {
		Map<String, Integer> amounts = new HashMap<>();
		switch (ProductName) {
		case "Boiled Water":
			amounts.put("Boiled Water A", 350);
			amounts.put("Boiled Water B", 1400);
			amounts.put("Boiled Water C", 3150);
			break;
		case "Pasta":
			amounts.put("Pasta A", 840);
			amounts.put("Pasta B", 770);
			amounts.put("Pasta C", 1120);
			amounts.put("Pasta D", 1190);
			amounts.put("Pasta E", 1520);
			amounts.put("Pasta F", 1590);
			break;
		case "Rice":
			amounts.put("Rice A", 1260);
			amounts.put("Rice B", 1400);
			amounts.put("Rice C", 1610);
			amounts.put("Rice D", 1750);
			amounts.put("Rice E", 1960);
			amounts.put("Rice F", 2100);
			break;
		case "Beef":
			amounts.put("Beef A", 4550);
			amounts.put("Beef B", 6650);
			amounts.put("Beef C", 6900);
			amounts.put("Beef D", 7000);
			amounts.put("Beef E", 7350);
			amounts.put("Beef F", 7550);
			break;
		case "Potato":
			amounts.put("Potato A", 1750);
			amounts.put("Potato B", 1890);
			amounts.put("Potato C", 2100);
			amounts.put("Potato D", 2240);
			amounts.put("Potato E", 2450);
			amounts.put("Potato F", 2590);
			break;
		case "Mushroom":
			amounts.put("Mushroom A", 700);
			amounts.put("Mushroom B", 840);
			amounts.put("Mushroom C", 910);
			amounts.put("Mushroom D", 1050);
			amounts.put("Mushroom E", 1120);
			amounts.put("Mushroom F", 1260);
			break;

		default:
			break;
		}
		return amounts;
	}

	private Map<String, Integer> getQuality(String ProductName) {
		Map<String, Integer> amounts = new HashMap<>();
		switch (ProductName) {
		case "Boiled Water":
			amounts.put("Boiled Water A", 5);
			amounts.put("Boiled Water B", 8);
			amounts.put("Boiled Water C", 11);
			break;
		case "Pasta":
			amounts.put("Pasta A", 2);
			amounts.put("Pasta B", 9);
			amounts.put("Pasta C", 14);
			amounts.put("Pasta D", 19);
			amounts.put("Pasta E", 22);
			amounts.put("Pasta F", 25);
			break;
		case "Rice":
			amounts.put("Rice A", 7);
			amounts.put("Rice B", 15);
			amounts.put("Rice C", 19);
			amounts.put("Rice D", 22);
			amounts.put("Rice E", 28);
			amounts.put("Rice F", 33);
			break;
		case "Beef":
			amounts.put("Beef A", 5);
			amounts.put("Beef B", 9);
			amounts.put("Beef C", 12);
			amounts.put("Beef D", 16);
			amounts.put("Beef E", 21);
			amounts.put("Beef F", 27);
			break;
		case "Potato":
			amounts.put("Potato A", 3);
			amounts.put("Potato B", 11);
			amounts.put("Potato C", 19);
			amounts.put("Potato D", 23);
			amounts.put("Potato E", 26);
			amounts.put("Potato F", 31);
			break;
		case "Mushroom":
			amounts.put("Mushroom A", 11);
			amounts.put("Mushroom B", 16);
			amounts.put("Mushroom C", 19);
			amounts.put("Mushroom D", 20);
			amounts.put("Mushroom E", 26);
			amounts.put("Mushroom F", 29);
			break;

		default:
			break;
		}
		return amounts;
	}

	private Map<String, Integer> getComputationTime(String ProductName) {
		Map<String, Integer> amounts = new HashMap<>();
		switch (ProductName) {
		case "Boiled Water":
			amounts.put("Boiled Water A", 15);
			amounts.put("Boiled Water B", 10);
			amounts.put("Boiled Water C", 5);
			break;
		case "Pasta":
			amounts.put("Pasta A", 30);
			amounts.put("Pasta B", 25);
			amounts.put("Pasta C", 20);
			amounts.put("Pasta D", 15);
			amounts.put("Pasta E", 10);
			amounts.put("Pasta F", 5);
			break;
		case "Rice":
			amounts.put("Rice A", 50);
			amounts.put("Rice B", 45);
			amounts.put("Rice C", 40);
			amounts.put("Rice D", 35);
			amounts.put("Rice E", 15);
			amounts.put("Rice F", 13);
			break;
		case "Beef":
			amounts.put("Beef A", 120);
			amounts.put("Beef B", 115);
			amounts.put("Beef C", 90);
			amounts.put("Beef D", 85);
			amounts.put("Beef E", 60);
			amounts.put("Beef F", 55);
			break;
		case "Potato":
			amounts.put("Potato A", 42);
			amounts.put("Potato B", 40);
			amounts.put("Potato C", 32);
			amounts.put("Potato D", 30);
			amounts.put("Potato E", 22);
			amounts.put("Potato F", 20);
			break;
		case "Mushroom":
			amounts.put("Mushroom A", 38);
			amounts.put("Mushroom B", 36);
			amounts.put("Mushroom C", 25);
			amounts.put("Mushroom D", 23);
			amounts.put("Mushroom E", 12);
			amounts.put("Mushroom F", 10);
			break;

		default:
			break;
		}
		return amounts;
	}

	public static int[] AMOUNT_REQUIRED = { 3000, 300, 600, 750, 600, 400 };
	public static String ProductionUnit = "g";

	private Map<String, Integer> getAmountProduced(String ProductName) {
		Map<String, Integer> amounts = new HashMap<>();
		switch (ProductName) {
		case "Boiled Water":
			amounts.put("Boiled Water A", 1000);
			amounts.put("Boiled Water B", 2000);
			amounts.put("Boiled Water C", 3000);
			break;
		case "Pasta":
			amounts.put("Pasta A", 100);
			amounts.put("Pasta B", 100);
			amounts.put("Pasta C", 200);
			amounts.put("Pasta D", 200);
			amounts.put("Pasta E", 300);
			amounts.put("Pasta F", 300);
			break;
		case "Rice":
			amounts.put("Rice A", 200);
			amounts.put("Rice B", 200);
			amounts.put("Rice C", 400);
			amounts.put("Rice D", 400);
			amounts.put("Rice E", 600);
			amounts.put("Rice F", 600);
			break;
		case "Beef":
			amounts.put("Beef A", 250);
			amounts.put("Beef B", 250);
			amounts.put("Beef C", 500);
			amounts.put("Beef D", 500);
			amounts.put("Beef E", 750);
			amounts.put("Beef F", 750);
			break;
		case "Potato":
			amounts.put("Potato A", 200);
			amounts.put("Potato B", 200);
			amounts.put("Potato C", 400);
			amounts.put("Potato D", 400);
			amounts.put("Potato E", 600);
			amounts.put("Potato F", 600);
			break;
		case "Mushroom":
			amounts.put("Mushroom A", 200);
			amounts.put("Mushroom B", 200);
			amounts.put("Mushroom C", 300);
			amounts.put("Mushroom D", 300);
			amounts.put("Mushroom E", 400);
			amounts.put("Mushroom F", 400);
			break;

		default:
			break;
		}
		return amounts;
	}
}
