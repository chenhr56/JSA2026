package allocation.ONA;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductionProcess {

	String name;
	int urgency;
	int numberOfCuts;
	int instanceNumber;

	List<Integer> processingTime;
	List<Integer> energys;
	List<Integer> montarys;
	List<Device> compitableResource;

	List<SubProcess> subProcesses = new ArrayList<SubProcess>();

	public ProductionProcess(String name, List<Device> compitableResource, List<Integer> processingTime,
			List<Integer> energys, List<Integer> montarys, int numberOfCuts, int urgency, int instanceNumber) {

		assert (numberOfCuts > 0);

		this.name = name;
		this.compitableResource = compitableResource;
		this.numberOfCuts = numberOfCuts;
		this.processingTime = processingTime;
		this.energys = energys;
		this.montarys = montarys;
		this.urgency = urgency;
		this.instanceNumber = instanceNumber;
		generateSubProcesses();
	}

	private void generateSubProcesses() {
		for (int i = 0; i < numberOfCuts; i++) {
			List<Integer> processingTimeForSubprocess = processingTime.stream().map(t -> t / numberOfCuts)
					.collect(Collectors.toList());
			List<Integer> energysForSubprocess = energys.stream().map(t -> t / numberOfCuts)
					.collect(Collectors.toList());
			List<Integer> montarysForSubprocess = montarys.stream().map(t -> t / numberOfCuts)
					.collect(Collectors.toList());

			SubProcess sub = new SubProcess(i, this.name, processingTimeForSubprocess, energysForSubprocess,
					montarysForSubprocess, compitableResource, this);
			subProcesses.add(sub);
		}

	}

	public String getName() {
		return name;
	}

	public List<Integer> getprocessingTime() {
		return processingTime;
	}

	public List<Integer> getMontarys() {
		return montarys;
	}

	public List<Integer> getEnergys() {
		return energys;
	}

	public List<Device> getCompitableResource() {
		return compitableResource;
	}

	public int getUrgency() {
		return urgency;
	}

	public int getNumberOfCuts() {
		return numberOfCuts;
	}

	public int getInstanceNumber() {
		return instanceNumber;
	}

	public List<SubProcess> getSubProcesses() {
		return subProcesses;
	}

	@Override
	public String toString() {
		String out = "Process: " + name + " preemptions: " + numberOfCuts + " compitables: "
				+ compitableResource.toString() + " processingTimes: " + processingTime.toString() + " montarys: "
				+ montarys.toString() + "\n";
		for (SubProcess sp : subProcesses) {
			out += sp.toString() + "\n";
		}
		return out;
	}

}
