package allocation;

import java.util.List;

public class Task {
	public int id;
	public String name;
	public int priority;
	public List<Integer> computations;
	public List<Integer> energys;
	public List<Integer> costs;
	public List<Double> util;
	public List<String> affinity;
	public int allocation;

	public int type = -1;

	public double maxUtil;

	public Task(int id, String name, int priority, List<Integer> taskComputes, List<Integer> taskEnergy,
			List<Integer> taskCost, List<Double> taskUtil, List<String> affinity, int allocation) {
		this.id = id;
		this.name = name;
		this.priority = priority;
		this.computations = taskComputes;
		this.energys = taskEnergy;
		this.costs = taskCost;
		this.util = taskUtil;
		this.affinity = affinity;
		this.allocation = allocation;

		this.maxUtil = -1;

		assert (taskComputes.size() == taskEnergy.size());
		assert (taskEnergy.size() == taskCost.size());
		assert (taskCost.size() == taskUtil.size());
		assert (taskUtil.size() == affinity.size());
	}

	@Override
	public String toString() {
		return "ID: " + this.id + " Name: " + this.name + " Computataion: " + this.computations + " Energy: "
				+ this.energys + " Cost: " + this.costs + " Util: " + this.util + " Allocations: " + this.affinity
				+ " Current Allocation: " + this.allocation;
	}

}