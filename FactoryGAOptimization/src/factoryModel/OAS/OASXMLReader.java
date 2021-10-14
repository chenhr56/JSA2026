package factoryModel.OAS;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import factoryModel.OAS.OASFactoryModel.AllenOperator;
import factoryModel.OAS.OASFactoryModel.DeviceType;
import factoryModel.OAS.OASFactoryModel.ModeType;
import mitm.atb.SequenceDependentTaskInfo;

public class OASXMLReader {
	List<String> objectivesList;
	List<Device> resources;
	List<ProductionLine> lines;
	List<ProductionProcess> processes;
	List<SubProcessRelation> relations;
	List<SequenceDependentTaskInfo> setups;

	public void readOASInput() {
		try {

			File fXmlFile = new File("input/OASConfiguration.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document OASConfiguration = dBuilder.parse(fXmlFile);

			OASConfiguration.getDocumentElement().normalize();

//			System.out.println("Root element :" + OASConfiguration.getDocumentElement().getNodeName());

			objectivesList = getObjectives(OASConfiguration);
			if (objectivesList.size() == 0) {
				System.err.println("Objective size must be bigger than 0");
				System.exit(-1);
			}
			resources = getResources(OASConfiguration);
			lines = getProductionLines(OASConfiguration, resources);
			processes = getProduction(OASConfiguration, resources, lines);
			relations = getSubProcessRelation(OASConfiguration, processes);
			setups = getDependentSetup(OASConfiguration, processes, resources);

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

				NodeList modesNodeList = deviceElement.getElementsByTagName("mode");

				for (int j = 0; j < modesNodeList.getLength(); j++) {
					Node modeNode = modesNodeList.item(j);

					if (modeNode.getNodeType() == Node.ELEMENT_NODE) {
						Element modeElement = (Element) modeNode;
						String modeType = modeElement.getAttribute("name");

						String energyConsumptionPerTimeUnit = modeElement.getAttribute("energyConsumptionPerTimeUnit");
						String monetaryCostPerTimeUnit = modeElement.getAttribute("monetaryCostPerTimeUnit");

						Device device = new Device(Integer.parseInt(deviceName.split(" ")[1]),
								stringToDeviceType(deviceName.split(" ")[0]), stringToModeType(modeType),
								Integer.parseInt(energyConsumptionPerTimeUnit),
								Integer.parseInt(monetaryCostPerTimeUnit), Integer.parseInt(availability),
								unAvailableTimesToString);
						resources.add(device);
					}
				}

			}
		}

//		System.out.println("\n-------------------------- Devices --------------------------");
//		String out = "Devices: ";
//		String nameFlag = "";
//		for (int i = 0; i < resources.size(); i++) {
//			if (!nameFlag.equals(resources.get(i).getName())) {
//				out += "\n";
//				nameFlag = resources.get(i).getName();
//			}
//			out += resources.get(i).toString() + "    ";
//		}
//
//		System.out.println(out);
//
//		System.out.println("-------------------------------------------------------------");
		return resources;
	}

	// private int getAvailablility(Document doc) {
	//
	// int availability = Integer.parseInt(
	// doc.getElementsByTagName("percentAvailability").item(0).getChildNodes().item(0).getNodeValue());
	//
	// System.out.println("percentAvailability: " + availability);
	// return availability;
	// }

	private List<ProductionLine> getProductionLines(Document doc, List<Device> devices) {
		List<ProductionLine> line = new ArrayList<ProductionLine>();

		NodeList productionLineNodeList = doc.getElementsByTagName("productionLine");

		for (int i = 0; i < productionLineNodeList.getLength(); i++) {
			Node productionLineNode = productionLineNodeList.item(i);

			if (productionLineNode.getNodeType() == Node.ELEMENT_NODE) {
				Element productionLineElement = (Element) productionLineNode;
				String productionLineName = productionLineElement.getAttribute("name");

				NodeList productionLineProcessingDeviceNodeList = productionLineElement
						.getElementsByTagName("productionLineProcessingDevice");
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

				line.add(new ProductionLine(Integer.parseInt(productionLineName.split(" ")[1]), devicesForOneLine));
			}
		}

//		System.out.println("\n-------------------------- Production Line --------------------------");
//		String out = "Production Line: \n";
//		for (ProductionLine oneLine : line) {
//			out += oneLine.toString() + "\n";
//		}
//		System.out.println(out);
//		System.out.println("-----------------------------------------------------------------------");
		return line;
	}

	private List<ProductionProcess> getProduction(Document doc, List<Device> resources, List<ProductionLine> lines) {
		List<ProductionProcess> processes = new ArrayList<ProductionProcess>();

		NodeList productionProcessNodeList = doc.getElementsByTagName("productionProcess");

		for (int i = 0; i < productionProcessNodeList.getLength(); i++) {
			Node productionProcessNode = productionProcessNodeList.item(i);
			if (productionProcessNode.getNodeType() == Node.ELEMENT_NODE) {
				Element productionProcessElement = (Element) productionProcessNode;
				String productionProcessName = productionProcessElement.getAttribute("name");
				String urgency = productionProcessElement.getAttribute("priority");

				Map<String, List<ProductionLine>> compitableResource = new java.util.HashMap<String, List<ProductionLine>>();
				Map<String, Integer> amounts = new java.util.HashMap<String, Integer>();
				Map<String, List<SubProcess>> rawSubProcesses = new java.util.HashMap<String, List<SubProcess>>();

				NodeList processTypesNodeList = productionProcessElement.getElementsByTagName("processType");

				for (int j = 0; j < processTypesNodeList.getLength(); j++) {
					Node processTypeNode = processTypesNodeList.item(j);
					if (processTypeNode.getNodeType() == Node.ELEMENT_NODE) {
						Element processTypeElement = (Element) processTypeNode;
						String processTypeName = processTypeElement.getAttribute("name");
						String processTypeAmount = processTypeElement.getAttribute("amountProduced");
						amounts.put(processTypeName,
								Integer.parseInt(processTypeAmount.substring(0, processTypeAmount.length() - 1)));

						List<ProductionLine> comptiableLines = new ArrayList<>();
						NodeList productionLineNodeList = processTypeElement
								.getElementsByTagName("comptiableProductionLine");
						for (int k = 0; k < productionLineNodeList.getLength(); k++) {
							Node productionLineNode = productionLineNodeList.item(k);
							if (productionLineNode.getNodeType() == Node.ELEMENT_NODE) {
								Element productionLineElement = (Element) productionLineNode;
								String lineName = productionLineElement.getTextContent();
								comptiableLines.add(getProductionLineByName(lineName, lines));
							}
						}
						compitableResource.put(processTypeName, comptiableLines);

						NodeList subProcessNodeList = processTypeElement.getElementsByTagName("subprocess");
						List<List<List<Device>>> devices = new ArrayList<List<List<Device>>>();
						List<List<Integer>> params = new ArrayList<List<Integer>>();
						List<DeviceType[]> types = new ArrayList<DeviceType[]>();
						List<String> names = new ArrayList<String>();

						for (int k = 0; k < subProcessNodeList.getLength(); k++) {
							Node subProcessNode = subProcessNodeList.item(k);
							if (subProcessNode.getNodeType() == Node.ELEMENT_NODE) {
								Element subProcessElement = (Element) subProcessNode;

								names.add(subProcessElement.getAttribute("name"));

								List<List<Device>> devicesInOneSubProcess = new ArrayList<List<Device>>();
								List<Integer> paramsInOneSubProcess = new ArrayList<>();

								NodeList subDeviceGroupsNodeList = subProcessElement
										.getElementsByTagName("subprocessProcessingDevicesGroup");

								for (int h = 0; h < subDeviceGroupsNodeList.getLength(); h++) {
									Node subGroupNode = subDeviceGroupsNodeList.item(h);
									if (subGroupNode.getNodeType() == Node.ELEMENT_NODE) {
										Element subGroupElement = (Element) subGroupNode;

										int subProcessDeviceProcessingTime = Integer
												.parseInt(subGroupElement.getAttribute("processingTime"));

										paramsInOneSubProcess.add(subProcessDeviceProcessingTime);

										List<Device> deviceInOneGroup = new ArrayList<>();

										NodeList subprocessDevicesNodeList = subGroupElement
												.getElementsByTagName("subprocessProcessingDevice");
										for (int z = 0; z < subprocessDevicesNodeList.getLength(); z++) {
											Node subProcessDeviceNode = subprocessDevicesNodeList.item(z);
											if (subProcessDeviceNode.getNodeType() == Node.ELEMENT_NODE) {
												Element subProcessDeviceElement = (Element) subProcessDeviceNode;
												String subProcessDeviceName = subProcessDeviceElement
														.getAttribute("name");
												String subProcessDeviceMode = subProcessDeviceElement
														.getAttribute("mode");

												String[] nameAsArray = subProcessDeviceName.split(" ");

												Device device = getDeviceByName(Integer.parseInt(nameAsArray[1]),
														stringToDeviceType(nameAsArray[0]),
														stringToModeType(subProcessDeviceMode), resources);
												deviceInOneGroup.add(device);
											}
										}

										devicesInOneSubProcess.add(deviceInOneGroup);
									}
								}

								devices.add(devicesInOneSubProcess);
								params.add(paramsInOneSubProcess);

								DeviceType[] type = new DeviceType[devicesInOneSubProcess.get(0).size()];
								for (int t = 0; t < devicesInOneSubProcess.get(0).size(); t++) {
									type[t] = devicesInOneSubProcess.get(0).get(t).getType();
								}
								types.add(type);
							}
						}

						assert (names.size() == devices.size());
						assert (devices.size() == params.size());
						assert (types.size() == params.size());

						List<SubProcess> rawSubProcessForOneType = new ArrayList<>();

						for (int index = 0; index < devices.size(); index++) {
							String[] nameAsArray = names.get(index).split(" ");
							String name = "";

							for (int nameIndex = 0; nameIndex < nameAsArray.length - 1; nameIndex++) {
								name += nameAsArray[nameIndex] + " ";
							}
							SubProcess rawSp = new SubProcess(index, name.substring(0, name.length() - 1),
									types.get(index), devices.get(index), params.get(index));
							rawSubProcessForOneType.add(rawSp);
						}

						rawSubProcesses.put(processTypeName, rawSubProcessForOneType);

					}
				}

				String[] nameToArray = productionProcessName.split(" ");
				String product = Arrays.copyOfRange(nameToArray, 0, nameToArray.length).toString();
				String amount = nameToArray[nameToArray.length - 1].substring(0,
						nameToArray[nameToArray.length - 1].length() - 1);

				ProductionProcess process = new ProductionProcess(i + 1, productionProcessName, product, amount,
						Integer.parseInt(urgency), compitableResource, amounts, rawSubProcesses);

				processes.add(process);
			}

		}

//		System.out.println("\n-------------------------- Production Process --------------------------");
//		System.out.println(processes.toString());
//		System.out.println("-----------------------------------------------------------------------");

		return processes;
	}

	private ProductionLine getProductionLineByName(String lineName, List<ProductionLine> lines) {
		for (ProductionLine line : lines) {
			if (line.getName().equals(lineName))
				return line;
		}
		return null;
	}

	private List<SubProcessRelation> getSubProcessRelation(Document doc, List<ProductionProcess> processes) {
		List<SubProcessRelation> relations = new ArrayList<SubProcessRelation>();

		List<SubProcess> subProcesses = new ArrayList<SubProcess>();
		for (int i = 0; i < processes.size(); i++) {
			ProductionProcess process = processes.get(i);
			List<Map.Entry<String, List<SubProcess>>> linear = process.getSubProcesses().entrySet().stream()
					.collect(Collectors.toList());

			for (int j = 0; j < linear.size(); j++) {
				subProcesses.addAll(linear.get(j).getValue());
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

//		System.out.println("\n----------------------------- SubProcess Relation -----------------------------");
//		for (SubProcessRelation relation : relations) {
//			System.out.println(relation.toString());
//		}
//		System.out.println("----------------------------------------------------------------------------------");

		return relations;
	}

	private List<SequenceDependentTaskInfo> getDependentSetup(Document doc, List<ProductionProcess> processes,
			List<Device> devices) {
		List<SequenceDependentTaskInfo> setups = new ArrayList<SequenceDependentTaskInfo>();

		List<SubProcess> subProcesses = new ArrayList<SubProcess>();
		for (int i = 0; i < processes.size(); i++) {
			ProductionProcess process = processes.get(i);
			List<Map.Entry<String, List<SubProcess>>> linear = process.getSubProcesses().entrySet().stream()
					.collect(Collectors.toList());

			for (int j = 0; j < linear.size(); j++) {
				subProcesses.addAll(linear.get(j).getValue());
			}
		}

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

				// List<Device> sameDevices = getDevices(deviceNames, resources);
				SequenceDependentTaskInfo setup = new SequenceDependentTaskInfo(Integer.parseInt(extraProcessingTime),
						Integer.parseInt(extraEnergyConsumption), Integer.parseInt(extraMonetaryCost), deviceNames,
						source, dest, taskId);
				setups.add(setup);
			}
		}

//		System.out.println("\n----------------------------- Dependent SetUp  -----------------------------");
//		for (SequenceDependentTaskInfo setUp : setups) {
//
//			System.out.println(setUp.getFullInfo());
//		}
//		System.out.println("----------------------------------------------------------------------------------");

		return setups;
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

	private DeviceType stringToDeviceType(String typeName) {
		for (DeviceType type : DeviceType.values()) {
			if (type.toString().equals(typeName))
				return type;
		}

		return null;
	}

	private ModeType stringToModeType(String modeType) {
		for (ModeType type : ModeType.values()) {
			if (type.toString().equals(modeType))
				return type;
		}

		return null;
	}

	private Device getDeviceByName(int DeviceID, DeviceType type, ModeType mode, List<Device> resources) {

		Device device = null;

		for (Device res : resources) {
			if (res.getId() == DeviceID && res.getType() == type && res.getMode() == mode) {
				device = res;
			}
		}

		return device;
	}

	private Device getDeviceByName(String name, List<Device> devices) {
		for (Device device : devices) {
			if (device.getName().equals(name)) {
				return device;
			}
		}
		return null;

	}

	public List<Device> getResources() {
		return resources;
	}

	// public int getPercentAvailability() {
	// return percentAvailability;
	// }

	public List<ProductionLine> getLines() {
		return lines;
	}

	public List<ProductionProcess> getProcesses() {
		return processes;
	}

	public List<SubProcessRelation> getRelations() {
		return relations;
	}

	public List<SequenceDependentTaskInfo> getSetups() {
		return setups;
	}

	public List<String> getObjectivesList() {
		return objectivesList;
	}

	public static void main(String args[]) {
		OASXMLReader reader = new OASXMLReader();
		reader.readOASInput();
		System.out.println();
	}
}
