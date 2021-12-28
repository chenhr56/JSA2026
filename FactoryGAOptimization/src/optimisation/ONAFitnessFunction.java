package optimisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutablePair;

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
		Map<String, Value> controlMetric = current.getControlledMetrics();

		List<MutablePair<String, Value>> controlsList = controlMetric.entrySet().stream()
				.map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList());

		/**
		 * Organize parts information
		 */
		List<String> parts = new ArrayList<>();
		for (ProductionProcess process : OnaConfigurationType.processes) {
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

			String shortName = name.split(" ")[0];
			int number = Integer.parseInt(shortName.substring(1, shortName.length())) - 1;
			int index_allocation = OnaConfigurationType.processes.get(number).compitableResourceName
					.indexOf(allocation);

			int cost = OnaConfigurationType.processes.get(number).montarys.get(index_allocation);
			int time = OnaConfigurationType.processes.get(number).processingTime.get(index_allocation);

			SchedulableObject sch = new SchedulableObject(parts.get(i), prios[i], allocs[i], time, cost);
			scheds.add(sch);
		}

		scheds.sort((c1, c2) -> Integer.compare(c1.priority, c2.priority));

		/**
		 * Allocate parts now
		 */
		long[] makespan = new long[OnaConfigurationType.devices.size()];

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
				baseIndex = OnaConfigurationType.devices.size() / 3;
			}
			if (machine_size.equals("Large")) {
				baseIndex = OnaConfigurationType.devices.size() / 3 * 2;
			}

			int index = baseIndex + wireNum;

			makespan[index] = makespan[index] + so.time;

			finalCost += so.cost;
		}

		long[] makespanWithMutex = new long[OnaConfigurationType.devices.size() / 3];

		for (int i = 0; i < makespanWithMutex.length; i++) {
			int smallIndex = i;
			int mediumIndex = i + OnaConfigurationType.devices.size() / 3;
			int largeIndex = i + OnaConfigurationType.devices.size() / 3 * 2;

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
