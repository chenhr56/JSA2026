package factoryModel.Electrolux;

import java.util.ArrayList;
import java.util.List;

public class CookingZone {

	int id;
	String name;

	List<Integer> deviceIDs;

	List<String> deviceNames;

	List<Device> devices;

	public CookingZone(int id, List<Device> productionLine) {
		this.id = id;
		this.name = "CookingZone" + " " + this.id;
		this.devices = productionLine;

		this.deviceIDs = new ArrayList<Integer>();
		this.deviceNames = new ArrayList<String>();

		for (Device device : productionLine) {
			deviceIDs.add(device.getId());
			deviceNames.add(device.getName());
		}

	}

	public String getName() {
		return this.name;
	}

	public int getId() {
		return id;
	}

	public List<Integer> getDeviceIDs() {
		return deviceIDs;
	}

	public List<String> getDeviceNames() {
		return deviceNames;
	}

	public List<Device> getDevices() {
		return devices;
	}

	@Override
	public String toString() {
		return name + " " + deviceNames.toString();

	}

	public String resourceName() {
		String name = "";

		for (int i = 0; i < devices.size() - 1; i++) {
			name += devices.get(i).getType().toString() + "(" + devices.get(i).getId() + ") ";
		}

		name += devices.get(devices.size() - 1).getType().toString() + "(" + devices.get(devices.size() - 1).getId()
				+ ")";

		return name;
	}

}
