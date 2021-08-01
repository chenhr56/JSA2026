package factoryModel.OAS;

import java.util.Arrays;
import java.util.List;

import factoryModel.OAS.OASFactoryModel.DeviceType;

public class DependentSetupPattern {

	int processingTime = 0;
	int energyConsumption = 0;
	int monetaryCost = 0;

	DeviceType[] source;
	DeviceType[] destination;

	public DependentSetupPattern(DeviceType[] source, DeviceType[] destination, List<Integer> parameters) {
		this.source = source;
		this.destination = destination;

		switch (parameters.size()) {
		case 1:
			this.processingTime = parameters.get(0);
			break;
		case 2:
			this.processingTime = parameters.get(0);
			this.energyConsumption = parameters.get(1);
			break;
		case 3:
			this.processingTime = parameters.get(0);
			this.energyConsumption = parameters.get(1);
			this.monetaryCost = parameters.get(2);
			break;

		default:
			break;
		}
	}

	public DependentSetupPattern(DeviceType[] source, DeviceType[] destination, int processingTime,
			int energyConsumption, int monetaryCost) {
		this.source = source;
		this.destination = destination;

		this.processingTime = processingTime;
		this.energyConsumption = energyConsumption;
		this.monetaryCost = monetaryCost;

	}

	public DeviceType[] getSource() {
		return source;
	}

	public DeviceType[] getDestination() {
		return destination;
	}

	public int getProcessingTime() {
		return processingTime;
	}

	public int getEnergyConsumption() {
		return energyConsumption;
	}

	public int getMonetaryCost() {
		return monetaryCost;
	}

	@Override
	public String toString() {
		return "Dependent Setup:   Source: " + Arrays.asList(source).toString() + " Destination: "
				+ Arrays.asList(destination).toString() + "  processingTime: " + processingTime + " energyConsumption: "
				+ energyConsumption + " monetaryCost: " + monetaryCost;
	}

}
