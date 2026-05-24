package JSA_experiments;

import java.util.List;

public class ResultBundle {

	public String name = "";
	
	public long time = -1;
	
	public int push = -1;
	
	public int pull = -1;
	
	public List<List<Double>> objectives = null;
	
	public String out = "";
	
	public ResultBundle(List<List<Double>> objectives, int push, int pull, long time, String out, String name) {
		this.objectives = objectives;
		this.push = push;
		this.pull = pull;
		this.time = time;
		
		this.out = out;
		this.name = name;
	}
	
	public ResultBundle(List<List<Double>> objectives, int push, int pull, long time) {
		this(objectives, push, pull, time, "","");
	}
	
	public ResultBundle(List<List<Double>> objectives, int push, int pull, long time, String name) {
		this(objectives, push, pull, time, "", name);
	}
}
