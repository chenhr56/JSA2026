package factoryModel.Electrolux;

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

import mitm.atb.SequenceDependentTaskInfo;

public class ElectroluxXMLWriter {

	public static void main(String args[]) {
		ElectroluxXMLWriter.write();
	}

	public static void write() {
		ElectroluxFactoryModel electrolux = new ElectroluxFactoryModel();
		electrolux.getElectroluxConfiguration();

		List<String> objectives = Arrays.asList(ElectroluxFactoryModel.objectives);
		List<List<Device>> devices = electrolux.devices;
		List<CookingZone> cookingZones = electrolux.cookingZone;
		List<ProductionProcess> processes = electrolux.processes;
		List<SequenceDependentTaskInfo> setups = electrolux.setups;

		try {
			Document doc = new Document();
			Element company = new Element("ElectroluxFactoryModel");
			doc.setRootElement(company);
			doc.getRootElement().addContent(addObjective(objectives));
			doc.getRootElement().addContent(addProcessingDevices(devices));
			doc.getRootElement().addContent(addCookingZones(cookingZones));
			doc.getRootElement().addContent(addProductionProcesses(processes));
			doc.getRootElement().addContent(addsequenceDependentSetup(setups));

			XMLOutputter xmlOutput = new XMLOutputter();

			// display nice nice
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter("input/ElectroluxConfiguration.xml"));

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

	private static Element addCookingZones(List<CookingZone> zones) {
		Element productionLinesElement = new Element("cookingZones");

		for (CookingZone line : zones) {
			Element productionLineElement = new Element("cookingZone");
			productionLineElement.setAttribute(new Attribute("name", line.getName()));

			Element productionLineProcessingDevicesElement = new Element("cookingZoneProcessingDevices");

			for (String device : line.getDeviceNames()) {
				Element productionLineProcessingDeviceElement = new Element("cookingZoneProcessingDevice");
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

			List<Map.Entry<String, List<CookingZone>>> linear = process.getCompitableResource().entrySet().stream()
					.collect(Collectors.toList());
			linear.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

			Element processTypesElement = new Element("processTypes");
			for (int i = 0; i < linear.size(); i++) {
				Element typeElement = new Element("processType");
				typeElement.setAttribute(new Attribute("name", linear.get(i).getKey()));
				typeElement.setAttribute(
						new Attribute("amountProduced", process.getAmountProduced().get(linear.get(i).getKey()) + "t"));
				typeElement.setAttribute(
						new Attribute("predecessor", process.getPredecessor().get(linear.get(i).getKey())));
				typeElement.setAttribute(new Attribute("energy", process.getEnergy().get(linear.get(i).getKey()) + ""));
				typeElement.setAttribute(
						new Attribute("processingTime", process.getProcessingTime().get(linear.get(i).getKey()) + ""));
				typeElement
						.setAttribute(new Attribute("montary", process.getMontary().get(linear.get(i).getKey()) + ""));
				typeElement
						.setAttribute(new Attribute("quality", process.getQuality().get(linear.get(i).getKey()) + ""));

				Element comptiableProductionLinesElement = new Element("comptiableCookingZones");
				for (CookingZone line : linear.get(i).getValue()) {
					Element lineElement = new Element("comptiableCookingZone");
					lineElement.setText(line.getName());
					comptiableProductionLinesElement.addContent(lineElement);
				}
				typeElement.addContent(comptiableProductionLinesElement);
				processTypesElement.addContent(typeElement);
			}

			productionProcessElement.addContent(processTypesElement);
			productionProcessesElement.addContent(productionProcessElement);
		}
		return productionProcessesElement;
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
