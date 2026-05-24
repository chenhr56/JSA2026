class Solution:
    def __init__(self, genotype):
        self.genotype = genotype
        self.fitness = []

    def dominates(self, other):
        """Determines whether this solution dominates the given other solution."""
        if self.fitness == other.fitness:
            return False

        for selfFitness, otherFitness in zip(self.fitness, other.fitness):
            if selfFitness < otherFitness:
                return False

        return True