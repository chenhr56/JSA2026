package allocation;

import java.util.List;
import java.util.stream.Collectors;

import metrics.Configuration;
import optimisation.ONAFitnessFunction;
import optimisation.ObjectiveFunction.LocalObjectiveFunction;


public class Workload {
	private int id;
	private List<Task> tasks;
	private Configuration configuration;
	private Configuration rullConfiguration;
	private List<String> resources;
	private double maxUtilPerCore;

	public Workload(int id, List<Task> tasks, Configuration config, Configuration rullConfig, List<String> resources) {
		this.id = id;
		this.tasks = tasks;
		this.configuration = config;
		this.rullConfiguration = rullConfig;
		this.resources = resources;

		double totalUtil = tasks.stream().map(t -> t.maxUtil).collect(Collectors.toList()).stream().mapToDouble(a -> a)
				.sum();
		if (totalUtil / resources.size() < 0.5)
			maxUtilPerCore = 0.5;
		else if (totalUtil / resources.size() < 0.6)
			maxUtilPerCore = 0.6;
		else if (totalUtil / resources.size() < 0.65)
			maxUtilPerCore = 0.65;
		else
			maxUtilPerCore = totalUtil / resources.size() <= 0.9 ? (totalUtil / resources.size()) + 0.05 : 1;
	}

	public int getID() {
		return id;
	}

	public List<Task> getTasks() {
		return tasks;
	}

	public Configuration getConfig() {
		return configuration;
	}

	public Configuration getRullConfig() {
		return rullConfiguration;
	}

	public List<String> getResources() {
		return resources;
	}

	public double getMaxUtilPerCore() {
		return maxUtilPerCore;
	}

	public LocalObjectiveFunction getObjectiveFunction(boolean isRule, boolean isSpeedUp) {
		if (isRule) {
			return new RuleFitnessFunction(tasks, resources, isSpeedUp);
		} else {
			if (isSpeedUp) {
				return new FastFitnessFunction(tasks, resources);
			} else {
				return new ONAFitnessFunction();
			}
		}
	}
}
