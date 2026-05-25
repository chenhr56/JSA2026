package optimisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutablePair;

import factoryModel.ONA.Device;
import factoryModel.ONA.ProductionProcess;
import metrics.Configuration;
import metrics.Value;
import metrics.ValueType;
import metrics.Value.Int;
import metrics.Value.Nominal;
import mitm.atb.OnaConfigurationType;
import restCloud.SchedulableObject;

public class ONAFitnessFunction extends ObjectiveFunction.LocalObjectiveFunction {

	public static List<Double> getFitness(Configuration config) {
		return new ONAFitnessFunction().predictKeyObjectivesImpl(config);
	}

	public List<Double> predictKeyObjectivesImpl(Configuration current) {
		// [多线程修复] 快照 OnaConfigurationType 的静态字段到局部变量。
		// 原代码在方法中部多次读取 OnaConfigurationType.processes / .devices，
		// 多线程下另一个线程的 presetup() 可能中途替换这些 List 为不同 scale 的数据，
		// 导致 IndexOutOfBoundsException（line 74/108）。
		// 捕获引用后，即使静态字段被替换，本方法仍持有本次调用对应的 processes/devices。
		List<ProductionProcess> processes = OnaConfigurationType.processes;
		List<Device> devices = OnaConfigurationType.devices;

		Map<String, Value> controlMetric = current.getControlledMetrics();

		List<MutablePair<String, Value>> controlsList = controlMetric.entrySet().stream()
				.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList());

		/**
		 * Organize parts information
		 */
		List<String> parts = new ArrayList<>();
		for (ProductionProcess process : processes) {
			String name = process.getName();

			for (int i = 0; i < process.getInstanceNumber(); i++) {
				parts.add(name + " " + i);
			}
		}

		int[] prios = new int[parts.size()];
		String[] allocs = new String[parts.size()];

		for (MutablePair<String, Value> p : controlsList) {
			String[] name_split = p.getKey().split(" ");
			String name = name_split[0] + " " + name_split[1];

			int index = parts.indexOf(name);

			if (p.getKey().contains("priority")) {
				Int prio = (Value.Int) p.right;
				int pri = prio.value;
				prios[index] = pri;
			} else {
				Nominal alloc = (Value.Nominal) p.right;
				String allo = alloc.value;
				allocs[index] = allo;
			}
		}

		List<SchedulableObject> scheds = new ArrayList<>();
		for (int i = 0; i < parts.size(); i++) {
			String name = parts.get(i);
			String allocation = allocs[i];

			// [多线程修复] 增加防御性检查：如果 allocation 为 null 或 indexOf 返回 -1，
			// 说明 Configuration 与当前 processes 不匹配（属于不同 scale），给出明确报错。
			// 原代码直接使用返回值导致 IndexOutOfBoundsException。
			if (allocation == null) {
				throw new IllegalStateException(
						"[多线程] allocation 为 null, part='" + name + "'. Configuration-processes 不匹配。");
			}

			String shortName = name.split(" ")[0];
			int number = Integer.parseInt(shortName.substring(1, shortName.length())) - 1;
			int index_allocation = processes.get(number).compitableResourceName
					.indexOf(allocation);

			if (index_allocation < 0) {
				throw new IllegalStateException(
						"[多线程] allocation='" + allocation + "' 未在 processes[" + number
								+ "].compitableResourceName 中找到, part='" + name
								+ "'. 当前 processes 可能属于不同 scale。");
			}

			int cost = processes.get(number).montarys.get(index_allocation);
			int time = processes.get(number).processingTime.get(index_allocation);

			SchedulableObject sch = new SchedulableObject(parts.get(i), prios[i], allocs[i], time, cost);
			scheds.add(sch);
		}

		scheds.sort((c1, c2) -> Integer.compare(c1.priority, c2.priority));

		/**
		 * Allocate parts now
		 */
		long[] makespan = new long[devices.size()];

		double finalTime = 0;
		double finalCost = 0;

		for (SchedulableObject so : scheds) {
			String machine_size = so.allocation.split(" ")[0];
			int wireNum = Integer.parseInt(so.allocation.split(" ")[1]) - 1;
			int baseIndex = -1;

			if (machine_size.equals("Small")) {
				baseIndex = 0;
			}
			if (machine_size.equals("Medium")) {
				baseIndex = devices.size() / 3;
			}
			if (machine_size.equals("Large")) {
				baseIndex = devices.size() / 3 * 2;
			}

			int index = baseIndex + wireNum;

			makespan[index] = makespan[index] + so.time;

			finalCost += so.cost;
		}

		long[] makespanWithMutex = new long[devices.size() / 3];

		for (int i = 0; i < makespanWithMutex.length; i++) {
			int smallIndex = i;
			int mediumIndex = i + devices.size() / 3;
			int largeIndex = i + devices.size() / 3 * 2;

			makespanWithMutex[i] += makespan[smallIndex];
			makespanWithMutex[i] += makespan[mediumIndex];
			makespanWithMutex[i] += makespan[largeIndex];

			if (finalTime < makespanWithMutex[i]) {
				finalTime = makespanWithMutex[i];
			}
		}

		List<Double> objectives = new ArrayList<>();
		objectives.add(finalTime);
		objectives.add(finalCost);

		return objectives;
	}

	@Override
	public Result predictKeyObjectives(Configuration current, Map<String, Value> proposedControlMetrics) {

		List<Double> objectives = predictKeyObjectivesImpl(current);

		Map<String, Value> objectiveMap = new HashMap<String, Value>();

		objectiveMap.put("makespan", Value.realValue(objectives.get(0), ValueType.realType(0.0, Double.MAX_VALUE)));
		objectiveMap.put("montary", Value.realValue(objectives.get(1), ValueType.realType(0.0, Double.MAX_VALUE)));

		Result res = new Result(objectiveMap, "", objectives);

		return res;
	}

	@Override
	public int numObjectives() {

		return 2;
	}

}
