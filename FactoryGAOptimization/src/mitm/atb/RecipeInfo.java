package mitm.atb;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringStyle;

public class RecipeInfo {
	final String name;
	final List<Integer> compatibleResources;
	final int instances;
	// final int commodityProduced;
	// final int executionTime;

	///////////////////////////

	public RecipeInfo(String name, int instances, List<Integer> compatibleResources
	// , int commodityProduced, int executionTime
	) {
		this.name = name;
		this.instances = instances;
		this.compatibleResources = compatibleResources;
		// this.commodityProduced = commodityProduced;
		// this.executionTime = executionTime;
	}

	@Override
	public String toString() {
		return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}
}

// End ///////////////////////////////////////////////////////////////
