package factoryModel.OAS;

import java.util.Arrays;

import factoryModel.OAS.OASFactoryModel.AllenOperator;

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
		return "SubProcessRelation:  Source: " + source.name + " " + Arrays.asList(source.type).toString()
				+ "  Destination: " + destination.name + " " + Arrays.asList(destination.type).toString()
				+ " relation: " + relation.toString();
	}
}
