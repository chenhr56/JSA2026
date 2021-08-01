package restCloud;

import java.util.List;

import aura.PopulationEntry;

public class ParetoFrontsKeeper {

	int SIZE_OF_EP;

	public ParetoFrontsKeeper(int SIZE_OF_EP) {
		this.SIZE_OF_EP = SIZE_OF_EP;
	}

	private boolean dominate(PopulationEntry individual1, PopulationEntry individual2) {
		boolean isDominate = false;

		if (individual1.getObjectives().size() != individual2.getObjectives().size()) {
			System.out.println("error");
		}

		for (int i = 0; i < individual1.getObjectives().size(); i++) {
			if (individual1.getObjectives().get(i) > individual2.getObjectives().get(i))
				return false;
			if (individual1.getObjectives().get(i) <= individual2.getObjectives().get(i))
				isDominate = true;
		}

		return isDominate;
	}

	public boolean updateExternalPopulation(List<PopulationEntry> externalPopulation, PopulationEntry candidate) {
		boolean result = false;

		if (externalPopulation.size() == 0) {
			externalPopulation.add(candidate);
			result = true;
		}

		else {
			boolean eligibleToJoin = true;

			/*
			 * remove the members that are dominated by the candidate and check the
			 * eligibility of the candidate.
			 */
			for (int i = 0; i < externalPopulation.size(); i++) {
				PopulationEntry member = externalPopulation.get(i);

				if (ObjectivesEquals(candidate, member)) {
					eligibleToJoin = false;
					break;
				}

				if (dominate(candidate, member)) {
					/* the candidate dominates a member */
					externalPopulation.remove(i);
					i--;
				} else if (dominate(member, candidate))
					/* the candidate is dominated by a member */
					eligibleToJoin = false;
			}

			if (eligibleToJoin) {
				externalPopulation.add(candidate);
				result = true;
			}
		}

		return result;

	}

	private boolean ObjectivesEquals(PopulationEntry candidate, PopulationEntry member) {

		assert (candidate.getObjectives().size() == member.getObjectives().size());

		for (int i = 0; i < candidate.getObjectives().size(); i++) {
			double candidateV = candidate.getObjectives().get(i);
			double memberV = member.getObjectives().get(i);
			if (candidateV != memberV) {
				return false;
			}
		}
		return true;
	}

}
