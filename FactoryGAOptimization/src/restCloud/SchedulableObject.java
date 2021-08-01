package restCloud;

public class SchedulableObject {
	public String name;
	public int priority;
	public String allocation;

	public int time;
	public int cost;

	public SchedulableObject(String name, int priority, String allocation, int time, int cost) {
		this.name = name;
		this.priority = priority;
		this.allocation = allocation;

		this.time = time;
		this.cost = cost;
	}

	@Override
	public String toString() {
		return "name: " + name + ", prio: " + priority + ", alloc: " + allocation + ", time: " + time + ", cost: "
				+ cost;

	}
}