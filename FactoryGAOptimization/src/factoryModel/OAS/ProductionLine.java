package factoryModel.OAS;

import java.util.ArrayList;
import java.util.List;

public class ProductionLine {

	int id;
	String name;

	List<Integer> deviceIDs;

	List<String> deviceNames;

	List<Device> devices;

	public ProductionLine(int id, List<Device> productionLine) {
		this.id = id;
		this.name = "ProductionLine" + " " + this.id;
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
		return "Silo(" + deviceIDs.get(0) + ") Mixer(" + deviceIDs.get(1) + ") Tank(" + deviceIDs.get(2) + ")";
	}

}
