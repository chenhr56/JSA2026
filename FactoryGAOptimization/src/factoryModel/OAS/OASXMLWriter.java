package factoryModel.OAS;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import factoryModel.OAS.OASFactoryModel.AllenOperator;
import mitm.atb.SequenceDependentTaskInfo;

public class OASXMLWriter {

	public static void main(String args[]) {
		OASFactoryModel oas = new OASFactoryModel();
		oas.getOASConfiguration();

		List<String> objectives = Arrays.asList(OASFactoryModel.objectives);
		List<List<List<Device>>> devices = oas.devices;
		List<ProductionLine> productionLines = oas.productionLines;
		List<ProductionProcess> processes = oas.processes;
		List<SubProcessRelation> relations = oas.relations;
		List<SequenceDependentTaskInfo> setups = oas.setups;

		try {
			Document doc = new Document();
			Element company = new Element("OASFactoryModel");
			doc.setRootElement(company);
			doc.getRootElement().addContent(addObjective(objectives));
			doc.getRootElement().addContent(addProcessingDevices(devices));
			doc.getRootElement().addContent(addProductionLines(productionLines));
			doc.getRootElement().addContent(addProductionProcesses(processes));
			doc.getRootElement().addContent(addSubProcessRelation(relations));
			doc.getRootElement().addContent(addsequenceDependentSetup(setups));

			// new XMLOutputter().output(doc, System.out);
			XMLOutputter xmlOutput = new XMLOutputter();

			// display nice nice
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter("input/OASConfiguration.xml"));

			System.out.println("File Saved!");
		} catch (IOException io) {
			System.out.println(io.getMessage());
		}
	}

	private static Element addObjective(List<String> objectives) {
		Element objectiveElement = new Element("objectives");

		for (String objective : objectives) {
			Element objElement = new Element("objective");
			objElement.setAttribute(new Attribute("name", objective));
			objectiveElement.addContent(objElement);
		}

		return objectiveElement;

	}

	private static Element addProcessingDevices(List<List<List<Device>>> devices) {
		Element processingDevicesElement = new Element("processingDevices");

		for (List<List<Device>> deviceInOneType : devices) {
			for (List<Device> device : deviceInOneType) {
				Element processingDeviceElement = new Element("processingDevice");
				processingDeviceElement.setAttribute(new Attribute("name", device.get(0).getName()));
				processingDeviceElement
						.setAttribute(new Attribute("availability", device.get(0).getAvailability() + ""));
				Element modesElement = new Element("modes");
				for (Device deviceInOneMode : device) {
					Element mode = new Element("mode");
					mode.setAttribute(new Attribute("name", deviceInOneMode.getMode().toString()));
					mode.setAttribute(new Attribute("energyConsumptionPerTimeUnit",
							deviceInOneMode.getEnergyConsumptionPerTimeUnit() + ""));
					mode.setAttribute(new Attribute("monetaryCostPerTimeUnit",
							deviceInOneMode.getMonetaryCostPerTimeUnit() + ""));
					modesElement.addContent(mode);
				}
				processingDeviceElement.addContent(modesElement);

				String notAvailables = device.get(0).getNotavailbaileTime();

				List<String> notAvailablesToString = Arrays.asList(notAvailables.split(" "));

				Element unavailableTimesElement = new Element("unavailableTimes");
				for (String aTime : notAvailablesToString) {
					Element unavailableTimeElement = new Element("unavailableTime");
					unavailableTimeElement.addContent(aTime);
					unavailableTimesElement.addContent(unavailableTimeElement);
				}
				processingDeviceElement.addContent(unavailableTimesElement);

				processingDevicesElement.addContent(processingDeviceElement);
			}
		}

		// processingDevicesElement.addContent(new
		// Element("percentAvailability").setText(100 + ""));
		return processingDevicesElement;

	}

	private static Element addProductionLines(List<ProductionLine> lines) {
		Element productionLinesElement = new Element("productionLines");

		for (ProductionLine line : lines) {
			Element productionLineElement = new Element("productionLine");
			productionLineElement.setAttribute(new Attribute("name", line.getName()));

			Element productionLineProcessingDevicesElement = new Element("productionLineProcessingDevices");

			for (String device : line.getDeviceNames()) {
				Element productionLineProcessingDeviceElement = new Element("productionLineProcessingDevice");
				productionLineProcessingDeviceElement
						.setAttribute(new Attribute("order", "" + line.getDeviceNames().indexOf(device)));
				productionLineProcessingDeviceElement.setAttribute(new Attribute("name", "" + device));

				productionLineProcessingDevicesElement.addContent(productionLineProcessingDeviceElement);
			}

			productionLineElement.addContent(productionLineProcessingDevicesElement);
			productionLinesElement.addContent(productionLineElement);

		}

		return productionLinesElement;

	}

	private static Element addProductionProcesses(List<ProductionProcess> processes) {
		Element productionProcessesElement = new Element("productionProcesses");

		for (ProductionProcess process : processes) {
			Element productionProcessElement = new Element("productionProcess");
			productionProcessElement.setAttribute(new Attribute("name", process.getKey()));
			productionProcessElement.setAttribute(new Attribute("priority", process.getUrgency() + ""));

			List<Map.Entry<String, List<ProductionLine>>> linear = process.getCompitableResource().entrySet().stream()
					.collect(Collectors.toList());
			linear.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

			Element processTypesElement = new Element("processTypes");
			for (int i = 0; i < linear.size(); i++) {
				Element typeElement = new Element("processType");
				typeElement.setAttribute(new Attribute("name", linear.get(i).getKey()));
				typeElement.setAttribute(
						new Attribute("amountProduced", process.getAmountProduced().get(linear.get(i).getKey()) + "t"));
				Element comptiableProductionLinesElement = new Element("comptiableProductionLines");

				for (ProductionLine line : linear.get(i).getValue()) {
					Element lineElement = new Element("comptiableProductionLine");
					lineElement.setText(line.getName());
					comptiableProductionLinesElement.addContent(lineElement);
				}
				typeElement.addContent(comptiableProductionLinesElement);

				Element subProcessesElement = new Element("subprocesses");
				for (SubProcess subProcess : process.getSubProcesses().get(linear.get(i).getKey())) {
					Element subProcessElement = new Element("subprocess");
					subProcessElement.setAttribute(new Attribute("name", subProcess.getName()));
//					subProcessElement.setAttribute(new Attribute("priority", subProcess.getPriority() + ""));

					int counter = 0;
					for (List<Device> subProcessGroup : subProcess.getSubProcessGroup()) {
						Element subprocessProcessingDevicesGroupElement = new Element(
								"subprocessProcessingDevicesGroup");
						subprocessProcessingDevicesGroupElement.setAttribute(new Attribute("processingTime",
								subProcess.getProcessingTimes().get(counter).toString()));
						// subprocessProcessingDevicesGroupElement.setAttribute(new
						// Attribute("energyCost",
						// subProcess.getEnergyCosts().get(counter).toString()));
						// subprocessProcessingDevicesGroupElement.setAttribute(new
						// Attribute("montaryCost",
						// subProcess.getMontaryCosts().get(counter).toString()));
						// Element subprocessProcessingDevicesElement = new
						// Element("subprocessProcessingDevices");

						for (Device device : subProcessGroup) {
							Element subprocessProcessingDeviceElement = new Element("subprocessProcessingDevice");
							subprocessProcessingDeviceElement.setAttribute(new Attribute("name", device.getName()));
							subprocessProcessingDeviceElement
									.setAttribute(new Attribute("mode", device.getMode().toString()));
							// subprocessProcessingDeviceElement
							// .setAttribute(new Attribute("processingTime", device.getProcessingTime() +
							// ""));
							// subprocessProcessingDeviceElement
							// .setAttribute(new Attribute("energyConsumption",
							// device.getEnergyConsumption() + ""));
							// subprocessProcessingDeviceElement.setAttribute(new Attribute("monetaryCost",
							// device.getMonetaryCost() + ""));
							subprocessProcessingDevicesGroupElement.addContent(subprocessProcessingDeviceElement);
						}

						// subprocessProcessingDevicesGroupElement.addContent(subprocessProcessingDevicesElement);
						subProcessElement.addContent(subprocessProcessingDevicesGroupElement);

						counter++;
					}

					subProcessesElement.addContent(subProcessElement);
				}

				typeElement.addContent(subProcessesElement);

				processTypesElement.addContent(typeElement);
			}

			productionProcessElement.addContent(processTypesElement);

			// productionProcessElement.addContent(subProcessesElement);
			productionProcessesElement.addContent(productionProcessElement);
		}
		return productionProcessesElement;
	}

	private static Element addSubProcessRelation(List<SubProcessRelation> relations) {
		Element subprocessRelationsElement = new Element("subprocessRelations");

		for (SubProcessRelation relation : relations) {
			if (relation.getRelation() != AllenOperator.UnDefined) {
				Element subprocessRelationElement = new Element("subprocessRelation");
				subprocessRelationElement.setAttribute(new Attribute("source", relation.getSource().getName()));
				subprocessRelationElement
						.setAttribute(new Attribute("destination", relation.getDestination().getName()));
				subprocessRelationElement
						.setAttribute(new Attribute("allensOperator", relation.getRelation().toString()));

				subprocessRelationsElement.addContent(subprocessRelationElement);
			}

		}
		return subprocessRelationsElement;
	}

	private static Element addsequenceDependentSetup(List<SequenceDependentTaskInfo> setups) {
		Element sequenceDependentSetupsElement = new Element("sequenceDependentSetups");

		for (SequenceDependentTaskInfo setup : setups) {

			Element setupElement = new Element("sequenceDependentSetup");
			setupElement.setAttribute(new Attribute("source", setup.lastTaskPrefix));
			setupElement.setAttribute(new Attribute("destination", setup.nextTaskPrefix));

			String[] deviceName = setup.resource.split("\\(");
			String[] deviceNumber = deviceName[1].split("\\)");

			setupElement.setAttribute(new Attribute("processingDevice", deviceName[0] + " " + deviceNumber[0]));
			setupElement.setAttribute(new Attribute("extraProcessingTime", setup.duration + ""));
			setupElement.setAttribute(new Attribute("extraEnergyConsumption", setup.energyCost + ""));
			setupElement.setAttribute(new Attribute("extraMonetaryCost", setup.montaryCost + ""));

			sequenceDependentSetupsElement.addContent(setupElement);

		}
		return sequenceDependentSetupsElement;
	}

}
