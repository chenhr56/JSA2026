package factoryModel.ONA;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import mitm.atb.SequenceDependentTaskInfo;

public class ONAXMLWriter {

	public static void main(String args[]) {

		for (int i = 0; i < 10; i++) {
			ONAFactoryModel.scale = i + 1;
			write();
		}
	}

	public static void write() {
		ONAFactoryModel ona = new ONAFactoryModel();
		ona.getONAConfiguration();

		List<String> objectives = Arrays.asList(ONAFactoryModel.objectives);
		List<List<Device>> devices = ona.devices;
		List<ProductionProcess> processes = ona.processes;
//		List<SubProcessRelation> relations = ona.relations;
		List<SequenceDependentTaskInfo> setups = ona.setups;

		try {
			Document doc = new Document();
			Element company = new Element("ONAFactoryModel");
			doc.setRootElement(company);
			doc.getRootElement().addContent(addObjective(objectives));
			doc.getRootElement().addContent(addProcessingDevices(devices));
			doc.getRootElement().addContent(addProductionProcesses(processes));
//			doc.getRootElement().addContent(addSubProcessRelation(relations));

			doc.getRootElement().addContent(addsequenceDependentSetup(setups));

			// new XMLOutputter().output(doc, System.out);
			XMLOutputter xmlOutput = new XMLOutputter();

			// display nice nice
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter("input/ONAConfiguration" + ONAFactoryModel.scale + ".xml"));

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

	private static Element addProcessingDevices(List<List<Device>> devices) {
		Element processingDevicesElement = new Element("processingDevices");

		for (List<Device> deviceInOneType : devices) {
			for (Device device : deviceInOneType) {
				Element processingDeviceElement = new Element("processingDevice");
				processingDeviceElement.setAttribute(new Attribute("name", device.getName()));
				processingDeviceElement.setAttribute(new Attribute("availability", device.getAvailability() + ""));

				String notAvailables = device.getNotavailbaileTime();

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

		return processingDevicesElement;

	}

	private static Element addProductionProcesses(List<ProductionProcess> processes) {
		Element productionProcessesElement = new Element("productionProcesses");

		for (ProductionProcess process : processes) {
			Element productionProcessElement = new Element("productionProcess");
			productionProcessElement.setAttribute(new Attribute("name", process.getName()));
			productionProcessElement.setAttribute(new Attribute("instances", process.getInstanceNumber() + ""));
			productionProcessElement.setAttribute(new Attribute("priority", process.getUrgency() + ""));

			productionProcessElement.setAttribute(new Attribute("cuts", process.numberOfCuts + ""));

			Element compitables = new Element("comptiableDevices");
			assert (process.compitableResource.size() == process.getprocessingTime().size());
			assert (process.compitableResource.size() == process.getMontarys().size());
			assert (process.compitableResource.size() == process.getEnergys().size());

			for (int i = 0; i < process.compitableResource.size(); i++) {
				Element compitable = new Element("comptiableDevice");
				compitable.setAttribute(new Attribute("name", process.compitableResource.get(i).getName()));
				compitable.setAttribute(new Attribute("processingTime", process.processingTime.get(i) + ""));
				compitable.setAttribute(new Attribute("energy", process.energys.get(i) + ""));
				compitable.setAttribute(new Attribute("montary", process.montarys.get(i) + ""));
				compitables.addContent(compitable);
			}
			productionProcessElement.addContent(compitables);

			Element subProcesses = new Element("subProcesses");
			for (int i = 0; i < process.subProcesses.size(); i++) {
				Element subProcess = new Element("subProcess");
				subProcess.setAttribute(new Attribute("name", process.subProcesses.get(i).getName()));

				for (int j = 0; j < process.getSubProcesses().get(i).comptiables.size(); j++) {
					Element processingDevice = new Element("subProcessProcessingDevice");
					processingDevice.setAttribute(
							new Attribute("name", process.getSubProcesses().get(i).comptiables.get(j).name + ""));
					processingDevice.setAttribute(new Attribute("processingTime",
							process.getSubProcesses().get(i).processingTime.get(j) + ""));
					processingDevice.setAttribute(
							new Attribute("energy", process.getSubProcesses().get(i).energyCost.get(j) + ""));
					processingDevice.setAttribute(
							new Attribute("montary", process.getSubProcesses().get(i).montaryCost.get(j) + ""));

					subProcess.addContent(processingDevice);
				}

				subProcesses.addContent(subProcess);
			}
			productionProcessElement.addContent(subProcesses);

			// productionProcessElement.addContent(subProcessesElement);
			productionProcessesElement.addContent(productionProcessElement);
		}
		return productionProcessesElement;
	}

//	private static Element addSubProcessRelation(List<SubProcessRelation> relations) {
//		Element subprocessRelationsElement = new Element("subprocessRelations");
//
//		for (SubProcessRelation relation : relations) {
//			if (relation.getRelation() != AllenOperator.UnDefined) {
//				Element subprocessRelationElement = new Element("subprocessRelation");
//				subprocessRelationElement.setAttribute(new Attribute("source", relation.getSource().getName()));
//				subprocessRelationElement
//						.setAttribute(new Attribute("destination", relation.getDestination().getName()));
//				subprocessRelationElement
//						.setAttribute(new Attribute("allensOperator", relation.getRelation().toString()));
//
//				subprocessRelationsElement.addContent(subprocessRelationElement);
//			}
//
//		}
//		return subprocessRelationsElement;
//	}

	private static Element addsequenceDependentSetup(List<SequenceDependentTaskInfo> setups) {
		Element sequenceDependentSetupsElement = new Element("sequenceDependentSetups");

		for (SequenceDependentTaskInfo setup : setups) {

			Element setupElement = new Element("sequenceDependentSetup");
			setupElement.setAttribute(new Attribute("source", setup.lastTaskPrefix));
			setupElement.setAttribute(new Attribute("destination", setup.nextTaskPrefix));

			setupElement.setAttribute(new Attribute("processingDevice", setup.resource));
			setupElement.setAttribute(new Attribute("extraProcessingTime", setup.duration + ""));
			setupElement.setAttribute(new Attribute("extraEnergyConsumption", setup.energyCost + ""));
			setupElement.setAttribute(new Attribute("extraMonetaryCost", setup.montaryCost + ""));

			sequenceDependentSetupsElement.addContent(setupElement);

		}
		return sequenceDependentSetupsElement;
	}

}
