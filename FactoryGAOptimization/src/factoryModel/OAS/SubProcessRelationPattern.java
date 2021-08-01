package factoryModel.OAS;

import java.util.Arrays;

import factoryModel.OAS.OASFactoryModel.AllenOperator;
import factoryModel.OAS.OASFactoryModel.DeviceType;

public class SubProcessRelationPattern {

	DeviceType[] source;
	DeviceType[] destination;
	AllenOperator relation;

	public SubProcessRelationPattern(DeviceType[] source, DeviceType[] destination, AllenOperator relation) {
		this.source = source;
		this.destination = destination;
		this.relation = relation;
	}

	public DeviceType[] getSource() {
		return source;
	}

	public DeviceType[] getDestination() {
		return destination;
	}

	public AllenOperator getRelation() {
		return relation;
	}

	@Override
	public String toString() {
		return "SubProcessRelation:  Source: " + Arrays.asList(source).toString() + "  Destination: "
				+ Arrays.asList(destination).toString() + " relation: " + relation.toString();
	}

}