package factoryModel.OAS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import factoryModel.OAS.OASFactoryModel.DeviceType;

public class ProductionProcess {

	int id;
	String name;
	String product;
	String amount;
	int urgency;

	Map<String, List<ProductionLine>> compitableResource;
	Map<String, Integer> AmountProduced;
	Map<String, List<SubProcess>> subProcesses;

	public ProductionProcess(int id, String name, String product, String amount, int urgency, List<DeviceType[]> types,
			Map<String, List<ProductionLine>> compitableResource, Map<String, Integer> AmountProduced,
			List<List<List<Device>>> devices, List<List<List<Integer>>> processingTimes) {
		this.id = id;
		this.name = name;
		this.product = product;
		this.urgency = urgency;

		this.amount = amount;
		this.compitableResource = compitableResource;
		this.AmountProduced = AmountProduced;

		assert (compitableResource.size() == AmountProduced.size());
		assert (devices.size() == processingTimes.size());

		List<SubProcess> subProcessesInAll = new ArrayList<SubProcess>();
		for (int i = 0; i < devices.size(); i++) {
			subProcessesInAll
					.add(new SubProcess(i, name + " Task", types.get(i), devices.get(i), processingTimes.get(i), this));
		}

		generateSubProcesses(compitableResource, subProcessesInAll);
	}

	public ProductionProcess(int id, String name, String product, String amount, int urgency,
			Map<String, List<ProductionLine>> compitableResource, Map<String, Integer> AmountProduced,
			Map<String, List<SubProcess>> rawSubProcess) {
		this.id = id;
		this.name = name;
		this.product = product;
		this.urgency = urgency;
		this.amount = amount;
		this.compitableResource = compitableResource;
		this.AmountProduced = AmountProduced;

		assert (compitableResource.size() == AmountProduced.size());

		List<Map.Entry<String, List<SubProcess>>> linear = rawSubProcess.entrySet().stream()
				.collect(Collectors.toList());

		for (Map.Entry<String, List<SubProcess>> entry : linear) {
			List<SubProcess> spList = entry.getValue();
			for (int i = 0; i < spList.size(); i++) {
				spList.get(i).process = this;
			}
		}

		this.subProcesses = rawSubProcess;
	}

	private void generateSubProcesses(Map<String, List<ProductionLine>> compitableResource,
			List<SubProcess> subProcessesInAll) {
		subProcesses = new HashMap<>();

		List<Map.Entry<String, List<ProductionLine>>> linear = compitableResource.entrySet().stream()
				.collect(Collectors.toList());

		linear.sort((p1, p2) -> p1.getKey().compareTo(p2.getKey()));

		for (Map.Entry<String, List<ProductionLine>> entry : linear) {
			String processType = entry.getKey();
			List<ProductionLine> lines = entry.getValue();
			this.subProcesses.put(processType, getSubProcessByType(subProcessesInAll, lines, processType));
		}

	}

	private List<SubProcess> getSubProcessByType(List<SubProcess> subProcessesInAll, List<ProductionLine> lines,
			String name) {
		List<SubProcess> subProcesses = new ArrayList<SubProcess>();
		for (SubProcess sp : subProcessesInAll) {
			subProcesses.add(new SubProcess(sp, name));
		}

		List<Device> compitableDevices = new ArrayList<>();

		for (ProductionLine line : lines) {
			for (Device device : line.devices) {
				if (!compitableDevices.contains(device))
					compitableDevices.add(device);
			}
		}

		for (SubProcess sp : subProcesses) {
			List<List<Device>> spDevices = sp.getSubProcessGroup();

			for (int i = 0; i < spDevices.size(); i++) {
				List<Device> deviceInOneSubGroup = spDevices.get(i);
				boolean isCompitable = true;

				for (Device pDevice : deviceInOneSubGroup) {
					if (!isContain(compitableDevices, pDevice)) {
						isCompitable = false;
						break;
					}
				}

				if (!isCompitable) {
					spDevices.remove(i);
					i--;
				}
			}

		}

		return subProcesses;
	}

	boolean isContain(List<Device> compitableDevices, Device device) {

		for (Device comptiableDevice : compitableDevices) {
			if (comptiableDevice.getName().equals(device.getName()))
				return true;
		}

		return false;

	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getProduct() {
		return product;
	}

	public String getAmount() {
		return amount;
	}

	public Map<String, List<ProductionLine>> getCompitableResource() {
		return compitableResource;
	}

	public Map<String, Integer> getAmountProduced() {
		return AmountProduced;
	}

	public Map<String, List<SubProcess>> getSubProcesses() {
		return subProcesses;
	}

	public String getKey() {
		return name + " " + amount;
	}

	public int getUrgency() {
		return urgency;
	}

	@Override
	public String toString() {
		String out = "Process: " + name + "\n";
		List<Map.Entry<String, List<SubProcess>>> linear = subProcesses.entrySet().stream()
				.collect(Collectors.toList());

		for (Map.Entry<String, List<SubProcess>> entry : linear) {
			out += entry.getKey() + "\n";
			List<SubProcess> spList = entry.getValue();
			for (SubProcess sp : spList) {
				out += sp.toString() + "\n";
			}

		}

		return out;
	}

}
