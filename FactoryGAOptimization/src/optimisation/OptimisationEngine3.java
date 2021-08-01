package optimisation;

import uk.ac.york.safire.metrics.OptimisationArguments;

///////////////////////////////////

public interface OptimisationEngine3 {

	public OptimisationIslandResult optimise(OptimisationArguments args);

	///////////////////////////////

	public static class DoNothingOptimisationEngine implements OptimisationEngine3 {

		@Override
		public OptimisationIslandResult optimise(OptimisationArguments args) {
			return OptimisationIslandResult.constructFromConfigurations(args.getConfigurations(),
					args.getConfigurations());
		}
	}

	///////////////////////////////

	public static abstract class LocalOptimisationEngine implements OptimisationEngine3 {

		private final ObjectiveFunction objectiveFunction;

		///////////////////////////

		public LocalOptimisationEngine(ObjectiveFunction objectiveFunction) {
			this.objectiveFunction = objectiveFunction;
		}

		///////////////////////////

		public ObjectiveFunction getObjectiveFunction() {
			return objectiveFunction;
		}
	}

	///////////////////////////////

}

// End ///////////////////////////////////////////////////////////////
