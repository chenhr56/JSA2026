package restCloud;

import java.util.List;

class ObjectiveCapsule {

	List<List<Double>> list;

	public ObjectiveCapsule(List<List<Double>> list) {
		this.list = list;
	}

	public void set(List<List<Double>> list) {
		this.list = list;
	}

	public List<List<Double>> get() {
		return list;
	}

}