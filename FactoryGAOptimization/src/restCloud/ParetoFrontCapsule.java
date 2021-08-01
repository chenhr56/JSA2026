package restCloud;

import java.util.List;

import aura.PopulationEntry;

class ParetoFrontCapsule {

	List<PopulationEntry> list;

	LinkageFactory linkageFactory;

	public ParetoFrontCapsule(List<PopulationEntry> list, LinkageFactory linkageFactory) {
		this.list = list;
		this.linkageFactory = linkageFactory;
	}

	public void set(List<PopulationEntry> list) {
		this.list = list;
	}

	public List<PopulationEntry> get() {
		return list;
	}

}
