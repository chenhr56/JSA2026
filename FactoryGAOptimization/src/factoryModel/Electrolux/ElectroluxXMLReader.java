package factoryModel.Electrolux;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import factoryModel.Electrolux.ElectroluxFactoryModel.DeviceType;
import mitm.atb.SequenceDependentTaskInfo;



public class ElectroluxXMLReader {
	List<String> objectivesList;
	List<Device> resources;
	List<CookingZone> lines;
	List<ProductionProcess> processes;
	List<SequenceDependentTaskInfo> setups;

	public void readElectroluxInput() {
		try {

			File fXmlFile = new File("input/ElectroluxConfiguration.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document ElectroluxConfiguration = dBuilder.parse(fXmlFile);

			ElectroluxConfiguration.getDocumentElement().normalize();

//			System.out.println("Root element :" + ElectroluxConfiguration.getDocumentElement().getNodeName());

			objectivesList = getObjectives(ElectroluxConfiguration);
			if (objectivesList.size() == 0) {
				System.err.println("Objective size must be bigger than 0");
				System.exit(-1);
			}
			resources = getResources(ElectroluxConfiguration);
			lines = getProductionLines(ElectroluxConfiguration, resources);
			processes = getProduction(ElectroluxConfiguration, resources, lines);
			setups = getDependentSetup(ElectroluxConfiguration, processes, resources);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<String> getObjectives(Document doc) {
		List<String> objectives = new ArrayList<String>();

		NodeList objectivesNodeList = doc.getElementsByTagName("objective");

		for (int i = 0; i < objectivesNodeList.getLength(); i++) {
			Node objectiveNode = objectivesNodeList.item(i);

			if (objectiveNode.getNodeType() == Node.ELEMENT_NODE) {
				Element objectiveElement = (Element) objectiveNode;
				String objectiveName = objectiveElement.getAttribute("name");

				objectives.add(objectiveName);
			}
		}

		return objectives;
	}

	private List<Device> getResources(Document doc) {

		List<Device> resources = new ArrayList<Device>();

		NodeList processingDeviceNodeList = doc.getElementsByTagName("processingDevice");

		for (int i = 0; i < processingDeviceNodeList.getLength(); i++) {

			Node deviceNode = processingDeviceNodeList.item(i);

			if (deviceNode.getNodeType() == Node.ELEMENT_NODE) {
				Element deviceElement = (Element) deviceNode;
				String deviceName = deviceElement.getAttribute("name");
				String availability = deviceElement.getAttribute("availability");

				List<String> unavailableTimes = new ArrayList<>();
				NodeList unavailableTimeList = deviceElement.getElementsByTagName("unavailableTime");
				for (int j = 0; j < unavailableTimeList.getLength(); j++) {
					Node unavailableTimeNode = unavailableTimeList.item(j);

					if (unavailableTimeNode.getNodeType() == Node.ELEMENT_NODE) {
						Element unavailableTimeElement = (Element) unavailableTimeNode;
						String content = unavailableTimeElement.getTextContent();
						unavailableTimes.add(content);
					}
				}

				String unAvailableTimesToString = String.join(" ", unavailableTimes);

				Device device = new Device(Integer.parseInt(deviceName.split(" ")[1]),
						stringToDeviceType(deviceName.split(" ")[0]), Integer.parseInt(availability),
						unAvailableTimesToString);
				resources.add(device);
			}
		}

		System.out.println("\n-------------------------- Devices --------------------------");
		String out = "Devices: ";
		String nameFlag = "";
		for (int i = 0; i < resources.size(); i++) {
			if (!nameFlag.equals(resources.get(i).getName())) {
				out += "\n";
				nameFlag = resources.get(i).getName();
			}
			out += resources.get(i).toString() + "    ";
		}

		System.out.println(out);

		System.out.println("-------------------------------------------------------------");
		return resources;
	}

	private List<CookingZone> getProductionLines(Document doc, List<Device> devices) {
		List<CookingZone> line = new ArrayList<CookingZone>();

		NodeList productionLineNodeList = doc.getElementsByTagName("cookingZone");

		for (int i = 0; i < productionLineNodeList.getLength(); i++) {
			Node productionLineNode = productionLineNodeList.item(i);

			if (productionLineNode.getNodeType() == Node.ELEMENT_NODE) {
				Element productionLineElement = (Element) productionLineNode;
				String productionLineName = productionLineElement.getAttribute("name");

				NodeList productionLineProcessingDeviceNodeList = productionLineElement
						.getElementsByTagName("cookingZoneProcessingDevice");
				List<Device> devicesForOneLine = new ArrayList<Device>();

				for (int j = 0; j < productionLineProcessingDeviceNodeList.getLength(); j++) {
					Node productionLineProcessingDeviceNode = productionLineProcessingDeviceNodeList.item(j);
					if (productionLineProcessingDeviceNode.getNodeType() == Node.ELEMENT_NODE) {
						Element productionLineProcessingDeviceElement = (Element) productionLineProcessingDeviceNode;
						String productionLineProcessingDeviceName = productionLineProcessingDeviceElement
								.getAttribute("name");
						Device device = getDeviceByName(productionLineProcessingDeviceName, devices);
						devicesForOneLine.add(device);
					}
				}

				line.add(new CookingZone(Integer.parseInt(productionLineName.split(" ")[1]), devicesForOneLine));
			}
		}

		System.out.println("\n-------------------------- Production Line --------------------------");
		String out = "Production Line: \n";
		for (CookingZone oneLine : line) {
			out += oneLine.toString() + "\n";
		}
		System.out.println(out);
		System.out.println("-----------------------------------------------------------------------");
		return line;
	}

	private List<ProductionProcess> getProduction(Document doc, List<Device> resources, List<CookingZone> lines) {
		List<ProductionProcess> processes = new ArrayList<ProductionProcess>();

		NodeList productionProcessNodeList = doc.getElementsByTagName("productionProcess");

		for (int i = 0; i < productionProcessNodeList.getLength(); i++) {
			Node productionProcessNode = productionProcessNodeList.item(i);
			if (productionProcessNode.getNodeType() == Node.ELEMENT_NODE) {
				Element productionProcessElement = (Element) productionProcessNode;
				String productionProcessName = productionProcessElement.getAttribute("name");
				String urgency = productionProcessElement.getAttribute("priority");

				Map<String, List<CookingZone>> compitableResource = new java.util.HashMap<String, List<CookingZone>>();
				Map<String, Integer> amounts = new java.util.HashMap<String, Integer>();
				Map<String, String> predecessor = new java.util.HashMap<>();

				Map<String, Integer> energy = new java.util.HashMap<String, Integer>();
				Map<String, Integer> processingTime = new java.util.HashMap<String, Integer>();
				Map<String, Integer> montary = new java.util.HashMap<String, Integer>();
				Map<String, Integer> quality = new java.util.HashMap<String, Integer>();

				NodeList processTypesNodeList = productionProcessElement.getElementsByTagName("processType");

				for (int j = 0; j < processTypesNodeList.getLength(); j++) {
					Node processTypeNode = processTypesNodeList.item(j);
					if (processTypeNode.getNodeType() == Node.ELEMENT_NODE) {
						Element processTypeElement = (Element) processTypeNode;
						String processTypeName = processTypeElement.getAttribute("name");

						String processTypeAmount = processTypeElement.getAttribute("amountProduced");
						amounts.put(processTypeName,
								Integer.parseInt(processTypeAmount.substring(0, processTypeAmount.length() - 1)));

						String processTypePredecessor = processTypeElement.getAttribute("predecessor");
						predecessor.put(processTypeName, processTypePredecessor);

						String processTypeEnergy = processTypeElement.getAttribute("energy");
						energy.put(processTypeName, Integer.parseInt(processTypeEnergy));

						String processTypeProcessingTime = processTypeElement.getAttribute("processingTime");
						processingTime.put(processTypeName, Integer.parseInt(processTypeProcessingTime));

						String processTypeMontary = processTypeElement.getAttribute("montary");
						montary.put(processTypeName, Integer.parseInt(processTypeMontary));

						String processTypeQuality = processTypeElement.getAttribute("quality");
						quality.put(processTypeName, Integer.parseInt(processTypeQuality));

						List<CookingZone> comptiableLines = new ArrayList<>();
						NodeList productionLineNodeList = processTypeElement
								.getElementsByTagName("comptiableCookingZone");
						for (int k = 0; k < productionLineNodeList.getLength(); k++) {
							Node productionLineNode = productionLineNodeList.item(k);
							if (productionLineNode.getNodeType() == Node.ELEMENT_NODE) {
								Element productionLineElement = (Element) productionLineNode;
								String lineName = productionLineElement.getTextContent();
								comptiableLines.add(getProductionLineByName(lineName, lines));
							}
						}
						compitableResource.put(processTypeName, comptiableLines);
					}
				}

				String[] nameToArray = productionProcessName.split(" ");
				String nameRaw = "";
				for (int k = 0; k < nameToArray.length - 1; k++) {
					nameRaw += nameToArray[k] + " ";
				}
				String name = nameRaw.substring(0, nameRaw.length() - 1);

				String product = Arrays.copyOfRange(nameToArray, 0, nameToArray.length).toString();
				String amount = nameToArray[nameToArray.length - 1].substring(0,
						nameToArray[nameToArray.length - 1].length() - 1);

				ProductionProcess process = new ProductionProcess(i + 1, name, product, amount,
						Integer.parseInt(urgency), predecessor, amounts, compitableResource, energy, processingTime,
						montary, quality);
				processes.add(process);
			}

		}

		System.out.println("\n-------------------------- Production Process --------------------------");
		System.out.println(processes.toString());
		System.out.println("-----------------------------------------------------------------------");

		return processes;
	}

	private CookingZone getProductionLineByName(String lineName, List<CookingZone> lines) {
		for (CookingZone line : lines) {
			if (line.getName().equals(lineName))
				return line;
		}
		return null;
	}

	private List<SequenceDependentTaskInfo> getDependentSetup(Document doc, List<ProductionProcess> processes,
			List<Device> devices) {
		List<SequenceDependentTaskInfo> setups = new ArrayList<SequenceDependentTaskInfo>();

		NodeList setupsNodeList = doc.getElementsByTagName("sequenceDependentSetup");
		for (int i = 0; i < setupsNodeList.getLength(); i++) {
			Node setupNode = setupsNodeList.item(i);
			if (setupNode.getNodeType() == Node.ELEMENT_NODE) {
				Element setupElement = (Element) setupNode;

				String source = setupElement.getAttribute("source");
				String dest = setupElement.getAttribute("destination");
				String deviceNamesRaw = setupElement.getAttribute("processingDevice");
				String extraProcessingTime = setupElement.getAttribute("extraProcessingTime");
				String extraEnergyConsumption = setupElement.getAttribute("extraEnergyConsumption");
				String extraMonetaryCost = setupElement.getAttribute("extraMonetaryCost");

				final String taskId = "DependentSetUp from " + source + " to " + dest;
				String[] deviceNamesRawArray = deviceNamesRaw.split(" ");
				String deviceNames = deviceNamesRawArray[0] + "(" + deviceNamesRawArray[1] + ")";

				SequenceDependentTaskInfo setup = new SequenceDependentTaskInfo(Integer.parseInt(extraProcessingTime),
						Integer.parseInt(extraEnergyConsumption), Integer.parseInt(extraMonetaryCost), deviceNames,
						source, dest, taskId);
				setups.add(setup);
			}
		}

		System.out.println("\n----------------------------- Dependent SetUp  -----------------------------");
		for (SequenceDependentTaskInfo setUp : setups) {

			System.out.println(setUp.getFullInfo());
		}
		System.out.println("----------------------------------------------------------------------------------");

		return setups;
	}

	private DeviceType stringToDeviceType(String typeName) {
		for (DeviceType type : DeviceType.values()) {
			if (type.toString().equals(typeName))
				return type;
		}

		System.err.println("cannot find deviceType via name");
		System.exit(-1);
		return null;
	}

	private Device getDeviceByName(String name, List<Device> devices) {
		for (Device device : devices) {
			if (device.getName().equals(name)) {
				return device;
			}
		}
		System.err.println("cannot find device via name");
		System.exit(-1);
		return null;

	}

	public List<Device> getResources() {
		return resources;
	}

	public List<CookingZone> getLines() {
		return lines;
	}

	public List<ProductionProcess> getProcesses() {
		return processes;
	}

	public List<SequenceDependentTaskInfo> getSetups() {
		return setups;
	}

	public List<String> getObjectivesList() {
		return objectivesList;
	}

	public static void main(String args[]) {
		ElectroluxXMLReader reader = new ElectroluxXMLReader();
		reader.readElectroluxInput();
		System.out.println("");
	}
}
