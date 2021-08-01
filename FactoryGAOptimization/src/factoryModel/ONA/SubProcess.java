package factoryModel.ONA;

import java.util.List;

public class SubProcess {
	int id;
	String name;
	ProductionProcess process;

	List<Integer> processingTime;
	List<Integer> energyCost;
	List<Integer> montaryCost;

	List<Device> comptiables;
	List<String> comptiablesNames;

	public SubProcess(int id, String name, List<Integer> processingTime, List<Integer> energyCost,
			List<Integer> montaryCost, List<Device> comptiables, List<String> comptiablesNames,
			ProductionProcess process) {
		this.id = id;
		this.name = name + " " + this.id;
		this.process = process;

		this.processingTime = processingTime;
		this.energyCost = energyCost;
		this.montaryCost = montaryCost;

		this.comptiables = comptiables;
		this.comptiablesNames = comptiablesNames;

	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public ProductionProcess getProcess() {
		return process;
	}

	public List<Integer> getProcessingTime() {
		return processingTime;
	}

	public List<Integer> getEnergyCost() {
		return energyCost;
	}

	public List<Integer> getMontaryCost() {
		return montaryCost;
	}

	public List<Device> getComptiables() {
		return comptiables;
	}

	public List<String> getComtiableNames() {
		return comptiablesNames;
	}

	@Override
	public String toString() {
		String out = "SubProcess: " + name + " processingTime: " + processingTime + " energy: " + energyCost
				+ " montary: " + montaryCost + " comptiable Resources: " + process.compitableResource.toString();

		return out;
	}

}
