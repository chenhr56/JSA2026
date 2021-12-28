package optimisation;

import java.util.ArrayList;
import java.util.List;

import aura.PopulationEntry;
import metrics.Configuration;

public class OptimisationIslandResult {
	private final List<PopulationEntry> front;
	private final List<PopulationEntry> finalPopulation;
	private double[][] caps;

	public double optimisationTime = -1;

	public OptimisationIslandResult(List<PopulationEntry> front, List<PopulationEntry> finalPopulation,
			double[][] caps) {
		this.front = front;
		this.finalPopulation = finalPopulation;
		this.caps = caps;
	}

	public static OptimisationIslandResult constructFromConfigurations(List<Configuration> front,
			List<Configuration> finalPopulation) {
		List<PopulationEntry> populationEntriesFront = new ArrayList<PopulationEntry>();
		for (Configuration config : front) {
			populationEntriesFront.add(new PopulationEntry(config, new ArrayList<Double>()));
		}

		List<PopulationEntry> populationEntriesFinalPopulation = new ArrayList<PopulationEntry>();
		for (Configuration config : finalPopulation) {
			populationEntriesFinalPopulation.add(new PopulationEntry(config, new ArrayList<Double>()));
		}

		double[][] caps = new double[6][2];

		return new OptimisationIslandResult(populationEntriesFront, populationEntriesFinalPopulation, caps);
	}

//	public static OptimisationIslandResult constructFromPopulationEntryAndConfiguration(List<PopulationEntry> front, List<Configuration> finalPopulation) {
//
//		List<PopulationEntry> populationEntriesFinalPopulation = new ArrayList<PopulationEntry>(); 
//		for(Configuration config: finalPopulation) {
//			populationEntriesFinalPopulation.add(new PopulationEntry(config,new ArrayList<Double>()));
//		}
//		
//		return new OptimisationIslandResult(front,populationEntriesFinalPopulation);
//	}

//	public static OptimisationIslandResult constructFromPopulationEntries(List<PopulationEntry> front, List<PopulationEntry> finalPopulation) {
//		return new OptimisationIslandResult(front,finalPopulation);
//	}

	public List<PopulationEntry> getFront() {
		return front;
	}

	public List<PopulationEntry> getFinalPopulation() {
		return finalPopulation;
	}

	public double[][] getCaps() {
		return caps;
	}

}
