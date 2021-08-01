package mitm.atb;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import optimisation.ONAFitnessFunction;
import optimisation.ObjectiveFunction;
import uk.ac.york.safire.metrics.ConfigurationType;

///////////////////////////////////

public enum BusinessCase {

	OAS {

		@Override
		public ConfigurationType getConfigurationType(int percentAvailability, Random random) {
			return OasHardwiredConfigurationType.hardwiredConfigurationType(percentAvailability, random);
		}

		@Override
		public ObjectiveFunction.LocalObjectiveFunction getObjectiveFunction() {
			return null;
		}

		@Override
		public double[] getWeight() {
			return new double[] { 0.5, 0.125, 0.125, 0.125, 0.125 };
		}

		@Override
		public int getNumberOfObjectives() {
			return -1;
		}
	},
	ONA {
		@Override
		public ConfigurationType getConfigurationType(int percentAvailability, Random random) {
			final boolean isMultiobjective = true;
			final Pair<Map<String, List<RecipeInfo>>, String[]> recipesAndResources = OnaConfigurationType
					.recipesAndResourceNames();
			final Map<String, List<RecipeInfo>> recipeInfo = recipesAndResources.getLeft();
			final String[] resourceNames = recipesAndResources.getRight();

			return OnaConfigurationType.configurationType(recipeInfo, resourceNames, isMultiobjective, random);
		}

		@Override
		public ObjectiveFunction.LocalObjectiveFunction getObjectiveFunction() {
			// return new PreemptionIAObjectiveFunction(OnaConfigurationType.getSetUps());
			return new ONAFitnessFunction();
		}

		@Override
		public double[] getWeight() {
			return new double[] { 0.8, 0.3 };
		}

		@Override
		public int getNumberOfObjectives() {
			return OnaConfigurationType.getObjectives().size();
		}

		// @Override
		// public List<ObservableMetricType> getObservableMetricTypes() {
		// // TODO Auto-generated method stub
		// return null;
		// }
		//
		// @Override
		// public Map<String, Value> getObservableMetrics() {
		// // TODO Auto-generated method stub
		// return null;
		// }

	},
	Electrolux {
		@Override
		public ConfigurationType getConfigurationType(int percentAvailability, Random random) {
			return ElectroluxConfigurationType.hardwiredConfigurationType(random, percentAvailability);
		}

		@Override
		public ObjectiveFunction.LocalObjectiveFunction getObjectiveFunction() {
			return null;
		}

		@Override
		public double[] getWeight() {
			return new double[] { 0.6, 0.4 };
		}

		@Override
		public int getNumberOfObjectives() {
			return ElectroluxConfigurationType.getObjectives().size();
		}

		// @Override
		// public List<ObservableMetricType> getObservableMetricTypes() {
		// // TODO Auto-generated method stub
		// return null;
		// }
		//
		// @Override
		// public Map<String, Value> getObservableMetrics() {
		// // TODO Auto-generated method stub
		// return null;
		// }

	};

	///////////////////////////////

	public abstract ObjectiveFunction.LocalObjectiveFunction getObjectiveFunction();

	public abstract ConfigurationType getConfigurationType(int percentAvailability, Random random);

	public abstract double[] getWeight();

	public abstract int getNumberOfObjectives();

	// public static List<ObservableMetricType> observableMetricTypes;
	// public static Map<String, Value> observableMetrics;

	// public abstract List<ObservableMetricType> getObservableMetricTypes();
	// public abstract Map<String, Value> getObservableMetrics();

}

// End ///////////////////////////////////////////////////////////////
