package allocation;

import java.util.ArrayList;
import java.util.List;

public class AllocationResult {
	
	int workID;
	int allocationID;
	String allocationName;
	
	long comptuationTime;
	
	List<List<Double>> objectives;
	
	Workload work;
	
	List<List<Integer>> ls = new ArrayList<>();
	
	public AllocationResult(int workID, int allocationID, String allocationName, long time, List<List<Double>> objectives, Workload work,List<List<Integer>> ls) {
		this.workID = workID;
		this.allocationID = allocationID;
		this.allocationName = allocationName;
		this.comptuationTime = time;
		this.objectives = objectives;
		this.work = work;
		this.ls = ls;
	}

}
