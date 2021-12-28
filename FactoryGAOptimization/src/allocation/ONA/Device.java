package allocation.ONA;

import allocation.ONA.ONAFactoryModel.DeviceType;

public class Device {

	int id;
	String name;
	DeviceType type;

	int availability = 0;
	String notavailbaileTime;

	public Device(int id, DeviceType type, int availability, String notavailbaileTime) {
		this.id = id;
		this.name = type.toString() + " " + this.id;
		this.type = type;
		this.availability = availability;
		this.notavailbaileTime = notavailbaileTime;
	}

	@Override
	public String toString() {
		return this.name + " ";
	}

	public int getId() {
		return id;
	}

	public DeviceType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public int getAvailability() {
		return availability;
	}

	public String getNotavailbaileTime() {
		return notavailbaileTime;
	}

}
