package factoryModel.OAS;

public class ProcessingDevice {

	int processingTime = 0;
	Device device;

	public ProcessingDevice(Device device, int processingTime) {
		this.device = device;
		this.processingTime = processingTime;
	}

	public int getProcessingTime() {
		return processingTime;
	}

	public Device getDevice() {
		return device;
	}

	@Override
	public String toString() {
		return device.toString();
	}

}
