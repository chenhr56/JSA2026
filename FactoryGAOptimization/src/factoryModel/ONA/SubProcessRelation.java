package factoryModel.ONA;

import factoryModel.ONA.ONAFactoryModel.AllenOperator;

public class SubProcessRelation {

	SubProcess source;
	SubProcess destination;
	AllenOperator relation;

	public SubProcessRelation(SubProcess source, SubProcess destination, AllenOperator relation) {
		this.source = source;
		this.destination = destination;
		this.relation = relation;
	}

	public SubProcess getSource() {
		return source;
	}

	public SubProcess getDestination() {
		return destination;
	}

	public AllenOperator getRelation() {
		return relation;
	}

	@Override
	public String toString() {
		return "SubProcessRelation:  Source: " + source.getName() + "  Destination: " + destination.getName()
				+ " relation: " + relation.toString();
	}
}
