package factoryModel.OAS;

import factoryModel.OAS.OASFactoryModel.DeviceType;
import factoryModel.OAS.OASFactoryModel.ModeType;

public class Device {

	int id;
	String name;
	DeviceType type;
	ModeType mode;

	int availability = 0;
	int energyConsumptionPerTimeUnit = 0;
	int monetaryCostPerTimeUnit = 0;

	String notavailbaileTime;

	public Device(int id, DeviceType type, ModeType mode, int energyConsumptionPerTimeUnit, int monetaryCost,
			int availability, String notavailbaileTime) {
		this.id = id;
		this.name = type.toString() + " " + this.id;
		this.type = type;
		this.mode = mode;
		this.energyConsumptionPerTimeUnit = energyConsumptionPerTimeUnit;
		this.monetaryCostPerTimeUnit = monetaryCost;
		this.availability = availability;
		this.notavailbaileTime = notavailbaileTime;
	}

	@Override
	public String toString() {
		return this.name + " " + this.mode + " " + this.energyConsumptionPerTimeUnit + " " + monetaryCostPerTimeUnit;
	}

	public int getId() {
		return id;
	}

	public DeviceType getType() {
		return type;
	}

	public ModeType getMode() {
		return mode;
	}

	public String getName() {
		return name;
	}

	public int getEnergyConsumptionPerTimeUnit() {
		return energyConsumptionPerTimeUnit;
	}

	public int getMonetaryCostPerTimeUnit() {
		return monetaryCostPerTimeUnit;
	}

	public String getKey() {
		return this.name + " " + this.mode;
	}

	public int getAvailability() {
		return availability;
	}

	public String getNotavailbaileTime() {
		return notavailbaileTime;
	}

	public String getFormattedName() {
		return name.split(" ")[0] + "(" + name.split(" ")[1] + ")";
	}

}
