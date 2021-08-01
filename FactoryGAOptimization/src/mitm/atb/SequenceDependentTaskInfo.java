package mitm.atb;

import org.apache.commons.lang3.builder.ToStringStyle;

///////////////////////////////////

public final class SequenceDependentTaskInfo {

	public final int duration;
	public final int energyCost;
	public final int montaryCost;
	public final String resource;
	public final String lastTaskPrefix;
	public final String nextTaskPrefix;
	public final String id;

	///////////////////////////

	public SequenceDependentTaskInfo(int duration, int energyCost, int montaryCost, String resource,
			String lastTaskPrefix, String nextTaskPrefix, String id) {
		this.duration = duration;
		this.energyCost = energyCost;
		this.montaryCost = montaryCost;

		this.resource = resource;
		this.lastTaskPrefix = lastTaskPrefix;
		this.nextTaskPrefix = nextTaskPrefix;
		this.id = id;
	}

	@Override
	public boolean equals(Object rhs) {
		return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, rhs);
	}

	@Override
	public int hashCode() {
		return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public String toString() {
		return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public String getInfo() {
		return id + " " + lastTaskPrefix + " " + nextTaskPrefix + " " + resource + " " + energyCost + " " + duration;
	}

	public String getFullInfo() {
		return "ID: " + id + " resource: " + resource + " duration: " + duration + " energy: " + energyCost
				+ " montary: " + montaryCost;
	}
};

// End ///////////////////////////////////////////////////////////////
