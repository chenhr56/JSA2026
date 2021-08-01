package factoryModel.OAS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import factoryModel.OAS.OASFactoryModel.DeviceType;

public class SubProcess {
	int id;
	String name;
//	int priority;
	ProductionProcess process;

	DeviceType[] type;
	List<List<Device>> subProcessGroup;

	List<Integer> processingTimes;
	List<Integer> energyCosts;
	List<Integer> montaryCosts;

	public SubProcess(int id, String name, DeviceType[] type, List<List<Device>> devices,
			List<List<Integer>> processingTimes, ProductionProcess process) {
		this.id = id + 1;
		this.name = name + " " + this.id;
//		this.priority = OASFactoryModel.ran.nextInt(OASFactoryModel.MAX_PRIORITY) + 1;
		this.type = type;
		this.subProcessGroup = new ArrayList<List<Device>>();
		this.process = process;

		this.processingTimes = new ArrayList<>();
		this.energyCosts = new ArrayList<>();
		this.montaryCosts = new ArrayList<>();

		initCost(devices, processingTimes);
	}

	public SubProcess(int id, String name, DeviceType[] type, List<List<Device>> devices,
			List<Integer> processingTimes) {
		this.id = id + 1;
		this.name = name + " " + this.id;
//		this.priority = OASFactoryModel.ran.nextInt(OASFactoryModel.MAX_PRIORITY) + 1;
		this.type = type;
		this.process = null;

		this.subProcessGroup = new ArrayList<List<Device>>();

		this.processingTimes = new ArrayList<>(processingTimes);
		this.energyCosts = new ArrayList<>();
		this.montaryCosts = new ArrayList<>();

		initCostForRead(devices, processingTimes);
	}

	public SubProcess(SubProcess subProcess, String name) {
		this.id = subProcess.id;
		this.name = name + " Task " + id;
		this.type = subProcess.type.clone();
//		this.priority = subProcess.priority;
		this.process = subProcess.process;

		this.subProcessGroup = new ArrayList<List<Device>>();
		for (List<Device> subGourp : subProcess.subProcessGroup) {
			subProcessGroup.add(new ArrayList<Device>(subGourp));
		}
		this.processingTimes = new ArrayList<Integer>(subProcess.processingTimes);
		this.energyCosts = new ArrayList<Integer>(subProcess.energyCosts);
		this.montaryCosts = new ArrayList<Integer>(subProcess.montaryCosts);

	}

	public void initCost(List<List<Device>> devices, List<List<Integer>> processingTimes) {
		assert (devices.size() == processingTimes.size());

		for (int i = 0; i < devices.size(); i++) {
			assert (devices.get(i).size() == processingTimes.get(i).size());

			subProcessGroup.add(devices.get(i));
			this.processingTimes.add(Collections.min(processingTimes.get(i)));
		}

		for (int i = 0; i < subProcessGroup.size(); i++) {
			List<Device> oneGroup = subProcessGroup.get(i);

			int energyCost = 0;
			int montaryCost = 0;

			for (Device d : oneGroup) {
				energyCost += this.processingTimes.get(i) * d.getEnergyConsumptionPerTimeUnit();
				montaryCost += this.processingTimes.get(i) * d.getMonetaryCostPerTimeUnit();
			}

			this.energyCosts.add(energyCost);
			this.montaryCosts.add(montaryCost);
		}
	}

	public void initCostForRead(List<List<Device>> devices, List<Integer> processingTimes) {
		assert (devices.size() == processingTimes.size());

		for (int i = 0; i < devices.size(); i++) {
			subProcessGroup.add(devices.get(i));
		}

		for (int i = 0; i < subProcessGroup.size(); i++) {
			List<Device> oneGroup = subProcessGroup.get(i);

			int energyCost = 0;
			int montaryCost = 0;

			for (Device d : oneGroup) {
				energyCost += this.processingTimes.get(i) * d.getEnergyConsumptionPerTimeUnit();
				montaryCost += this.processingTimes.get(i) * d.getMonetaryCostPerTimeUnit();
			}

			this.energyCosts.add(energyCost);
			this.montaryCosts.add(montaryCost);
		}
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<List<Device>> getSubProcessGroup() {
		return subProcessGroup;
	}

	public DeviceType[] getType() {
		return type;
	}

//	public int getPriority() {
//		return priority;
//	}

	public ProductionProcess getProcess() {
		return process;
	}

	public List<Integer> getProcessingTimes() {
		return processingTimes;
	}

	public List<Integer> getEnergyCosts() {
		return energyCosts;
	}

	public List<Integer> getMontaryCosts() {
		return montaryCosts;
	}

	@Override
	public String toString() {
		String out = "SubProcess: " + name + "\n";
		for (int i = 0; i < subProcessGroup.size(); i++) {
			out += "SubProcessingGoup: " + subProcessGroup.get(i).toString() + " Cost: " + this.processingTimes.get(i)
					+ " " + this.energyCosts.get(i) + " " + this.montaryCosts.get(i) + " \n";
		}
		return out;
	}

}
