package factoryModel.ONA;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import factoryModel.ONA.ONAFactoryModel.AllenOperator;
import factoryModel.ONA.ONAFactoryModel.DeviceType;
import mitm.atb.SequenceDependentTaskInfo;

public class ONAXMLReader {
	List<String> objectivesList;
	List<Device> resources;
	List<ProductionProcess> processes;
	List<SubProcessRelation> relations;
	List<SequenceDependentTaskInfo> setups;

	public void readOASInput() {
		try {

			File fXmlFile = new File("input/ONAConfiguration" + ONAFactoryModel.scale + ".xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document ONAConfiguration = dBuilder.parse(fXmlFile);

			ONAConfiguration.getDocumentElement().normalize();

			System.out.println("Root element :" + ONAConfiguration.getDocumentElement().getNodeName());

			objectivesList = getObjectives(ONAConfiguration);
			if (objectivesList.size() == 0) {
				System.err.println("Objective size must be bigger than 0");
				System.exit(-1);
			}
			resources = getResources(ONAConfiguration);
			processes = getProduction(ONAConfiguration, resources);
			relations = getSubProcessRelation(ONAConfiguration, processes);
			setups = getDependentSetup(ONAConfiguration, processes, resources);

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

				Device d = new Device(Integer.parseInt(deviceName.split(" ")[1]),
						stringToDeviceType(deviceName.split(" ")[0]), Integer.parseInt(availability),
						unAvailableTimesToString);
				resources.add(d);

			}
		}

		// System.out.println("\n-------------------------- Devices
		// --------------------------");
		// String out = "Devices: ";
		// String nameFlag = "";
		// for (int i = 0; i < resources.size(); i++) {
		// if (!nameFlag.equals(resources.get(i).getName())) {
		// out += "\n";
		// nameFlag = resources.get(i).getName();
		// }
		// out += resources.get(i).toString() + " ";
		// }
		//
		// System.out.println(out);
		//
		// System.out.println("-------------------------------------------------------------");
		return resources;
	}

	private List<ProductionProcess> getProduction(Document doc, List<Device> resources) {
		List<ProductionProcess> processes = new ArrayList<ProductionProcess>();

		NodeList productionProcessNodeList = doc.getElementsByTagName("productionProcess");

		for (int i = 0; i < productionProcessNodeList.getLength(); i++) {
			Node productionProcessNode = productionProcessNodeList.item(i);
			if (productionProcessNode.getNodeType() == Node.ELEMENT_NODE) {
				Element productionProcessElement = (Element) productionProcessNode;
				String productionProcessName = productionProcessElement.getAttribute("name");
				int urgency = Integer.parseInt(productionProcessElement.getAttribute("priority"));
				int instances = Integer.parseInt(productionProcessElement.getAttribute("instances"));
				int preemptionPoints = Integer.parseInt(productionProcessElement.getAttribute("cuts"));

				List<Device> compitables = new ArrayList<>();
				List<Integer> processingTimes = new ArrayList<>();
				List<Integer> energys = new ArrayList<>();
				List<Integer> montarys = new ArrayList<>();
				NodeList comptiableDeviceNodeList = productionProcessElement.getElementsByTagName("comptiableDevice");

				for (int j = 0; j < comptiableDeviceNodeList.getLength(); j++) {
					Node comptiableNode = comptiableDeviceNodeList.item(j);
					if (comptiableNode.getNodeType() == Node.ELEMENT_NODE) {
						Element comptiableElement = (Element) comptiableNode;
						String deviceName = comptiableElement.getAttribute("name");
						Device d = getDeviceByName(deviceName, resources);
						compitables.add(d);

						int processingTime = Integer.parseInt(comptiableElement.getAttribute("processingTime"));
						processingTimes.add(processingTime);

						int energy = Integer.parseInt(comptiableElement.getAttribute("energy"));
						energys.add(energy);

						int montary = Integer.parseInt(comptiableElement.getAttribute("montary"));
						montarys.add(montary);
					}
				}

				NodeList subProcessNodeList = productionProcessElement.getElementsByTagName("subProcess");

				ProductionProcess process = new ProductionProcess(productionProcessName, compitables, processingTimes,
						energys, montarys, preemptionPoints, urgency, instances);

				if (subProcessNodeList.getLength() > 0) {
					List<SubProcess> subProcesses = new ArrayList<>();

					for (int k = 0; k < subProcessNodeList.getLength(); k++) {
						Node subProcessNode = subProcessNodeList.item(k);
						if (subProcessNode.getNodeType() == Node.ELEMENT_NODE) {
							Element subProcessElement = (Element) subProcessNode;
							String[] subProcessName = subProcessElement.getAttribute("name").split(" ");

							List<Device> subComptiables = new ArrayList<>();
							List<String> subComptiablesNames = new ArrayList<>();
							List<Integer> subProcessingTimes = new ArrayList<>();
							List<Integer> subEnergys = new ArrayList<>();
							List<Integer> subMontarys = new ArrayList<>();

							NodeList subProcessProcessingDeviceNodeList = subProcessElement
									.getElementsByTagName("subProcessProcessingDevice");
							for (int l = 0; l < subProcessProcessingDeviceNodeList.getLength(); l++) {
								Node subProcessProcessingDeviceNode = subProcessProcessingDeviceNodeList.item(l);
								if (subProcessProcessingDeviceNode.getNodeType() == Node.ELEMENT_NODE) {
									Element subProcessProcessingDeviceElement = (Element) subProcessProcessingDeviceNode;
									String deviceName = subProcessProcessingDeviceElement.getAttribute("name");
									int processingTime = Integer
											.parseInt(subProcessProcessingDeviceElement.getAttribute("processingTime"));
									int energy = Integer
											.parseInt(subProcessProcessingDeviceElement.getAttribute("energy"));
									int montary = Integer
											.parseInt(subProcessProcessingDeviceElement.getAttribute("montary"));

									subComptiables.add(getDeviceByName(deviceName, resources));
									subComptiablesNames.add(deviceName);
									subProcessingTimes.add(processingTime);
									subEnergys.add(energy);
									subMontarys.add(montary);
								}
							}

							SubProcess subProcess = new SubProcess(Integer.parseInt(subProcessName[1]),
									subProcessName[0] + " " + subProcessName[1], subProcessingTimes, subEnergys,
									subMontarys, subComptiables, subComptiablesNames, process);
							subProcesses.add(subProcess);
						}
					}
					process.subProcesses = subProcesses;
					processes.add(process);
				} else {
					processes.add(process);
				}

			}

		}

		// System.out.println("\n-------------------------- Production Process
		// --------------------------");
		// for (ProductionProcess process : processes) {
		// System.out.println(process.toString());
		// }
		// System.out.println("-----------------------------------------------------------------------");

		return processes;
	}

	private List<SubProcessRelation> getSubProcessRelation(Document doc, List<ProductionProcess> processes) {
		List<SubProcessRelation> relations = new ArrayList<SubProcessRelation>();

		List<SubProcess> subProcesses = new ArrayList<SubProcess>();
		for (int i = 0; i < processes.size(); i++) {
			ProductionProcess process = processes.get(i);
			List<SubProcess> linear = process.getSubProcesses().stream().collect(Collectors.toList());

			for (int j = 0; j < linear.size(); j++) {
				subProcesses.add(linear.get(j));
			}
		}

		NodeList relationsNodeList = doc.getElementsByTagName("subprocessRelation");
		for (int i = 0; i < relationsNodeList.getLength(); i++) {
			Node relationNode = relationsNodeList.item(i);
			if (relationNode.getNodeType() == Node.ELEMENT_NODE) {
				Element relationElement = (Element) relationNode;

				String source = relationElement.getAttribute("source");
				String dest = relationElement.getAttribute("destination");
				String allenOperator = relationElement.getAttribute("allensOperator");

				SubProcessRelation relation = new SubProcessRelation(getSubProcessByName(source, subProcesses),
						getSubProcessByName(dest, subProcesses), getAllenOperatorByName(allenOperator));
				relations.add(relation);
			}
		}

		// System.out.println("\n----------------------------- SubProcess Relation
		// -----------------------------");
		// for (SubProcessRelation relation : relations) {
		// System.out.println(relation.toString());
		// }
		// System.out.println("----------------------------------------------------------------------------------");

		return relations;
	}

	private SubProcess getSubProcessByName(String name, List<SubProcess> subProcesses) {
		for (SubProcess sub : subProcesses) {
			if (sub.getName().equals(name))
				return sub;
		}

		return null;
	}

	private AllenOperator getAllenOperatorByName(String operator) {
		for (AllenOperator op : AllenOperator.values()) {
			if (op.toString().equals(operator)) {
				return op;
			}
		}
		return AllenOperator.UnDefined;
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

				SequenceDependentTaskInfo setup = new SequenceDependentTaskInfo(Integer.parseInt(extraProcessingTime),
						Integer.parseInt(extraEnergyConsumption), Integer.parseInt(extraMonetaryCost), deviceNamesRaw,
						source, dest, taskId);
				setups.add(setup);
			}
		}

		// System.out.println("\n----------------------------- Dependent SetUp
		// -----------------------------");
		// for (SequenceDependentTaskInfo setUp : setups) {
		//
		// System.out.println(setUp.getFullInfo());
		// }
		// System.out.println("----------------------------------------------------------------------------------");

		return setups;
	}

	private DeviceType stringToDeviceType(String typeName) {
		for (DeviceType type : DeviceType.values()) {
			if (type.toString().equals(typeName))
				return type;
		}

		return null;
	}

	private Device getDeviceByName(String name, List<Device> devices) {
		for (Device device : devices) {
			String temp = device.getName();
			if (temp.equals(name)) {
				return device;
			}
		}
		return null;

	}

	public List<Device> getResources() {
		return resources;
	}

	public List<ProductionProcess> getProcesses() {
		return processes;
	}

	public List<String> getObjectivesList() {
		return objectivesList;
	}

	public List<SequenceDependentTaskInfo> getSetUps() {
		return setups;
	}

	public List<SubProcessRelation> getRelations() {
		return relations;
	}

	public static void main(String args[]) {
		ONAXMLReader reader = new ONAXMLReader();
		reader.readOASInput();
		System.out.println("Done");
	}
}
