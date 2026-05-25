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

	// [多线程修复] 使用 ThreadLocal 为每个线程保存独立的 processes/devices 快照。
	// 原代码直接读取 OnaConfigurationType.processes/devices 静态字段，
	// 多线程下不同 scale 的 presetup() 会替换这些静态 List，导致评估时使用的
	// processes/devices 与 Configuration 创建时不一致，抛出 ArrayIndexOutOfBounds 等异常。
	// ThreadLocal 保证每个线程在 startIsland() 中调用 captureForCurrentThread() 后，
	// 本线程内所有评估（包括 getFitness 和引擎的 of.evaluate）都使用同一份数据。
	private static ThreadLocal<List<ProductionProcess>> tlProcesses = new ThreadLocal<>();
	private static ThreadLocal<List<Device>> tlDevices = new ThreadLocal<>();

	/**
	 * [多线程修复] 当前线程在 presetup() 完成后调用此方法，将本线程所用 scale 的
	 * processes/devices 保存到 ThreadLocal。此后本线程内所有 predictKeyObjectivesImpl
	 * 调用都使用这份数据，不受其他线程 presetup() 的影响。
	 */
	public static void captureForCurrentThread() {
		tlProcesses.set(OnaConfigurationType.processes);
		tlDevices.set(OnaConfigurationType.devices);
	}

	public static List<Double> getFitness(Configuration config) {
		return new ONAFitnessFunction().predictKeyObjectivesImpl(config);
	}

	public List<Double> predictKeyObjectivesImpl(Configuration current) {
		// [多线程修复] 从 ThreadLocal 获取本线程专属的 processes/devices，
		// 而非直接读取 OnaConfigurationType 静态字段。
		List<ProductionProcess> processes = tlProcesses.get();
		List<Device> devices = tlDevices.get();

			// [多线程修复] Fallback：单线程场景（如 LinkageFactory.main()）不会调用
			// captureForCurrentThread()，ThreadLocal 为 null，此时直接读静态字段。
			if (processes == null) {
				processes = OnaConfigurationType.processes;
			}
			if (devices == null) {
				devices = OnaConfigurationType.devices;
			}

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

			// [多线程修复] 防御性检查：如果 index==-1，说明 Configuration 与当前 processes 不匹配
			if (index < 0) {
				throw new IllegalStateException(
						"[多线程] part='" + name + "' 不在 processes 的 parts 列表中。"
								+ "Configuration 可能使用了不同 scale 的 processes。");
			}

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

			// [多线程修复] 防御性检查：allocation 为 null 说明 Configuration 与 processes 不匹配
			if (allocation == null) {
				throw new IllegalStateException(
						"[多线程] allocation 为 null, part='" + name + "'. Configuration-processes 不匹配。");
			}

			String shortName = name.split(" ")[0];
			int number = Integer.parseInt(shortName.substring(1, shortName.length())) - 1;
			int index_allocation = processes.get(number).compitableResourceName
					.indexOf(allocation);

			// [多线程修复] 防御性检查
			if (index_allocation < 0) {
				throw new IllegalStateException(
						"[多线程] allocation='" + allocation + "' 未在 processes[" + number
								+ "].compitableResourceName 中找到, part='" + name + "'。");
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
