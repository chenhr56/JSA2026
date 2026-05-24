import numpy as np
import utilities as util

class Cluster:
    def __init__(self, mean, problem):
        self.mean = mean
        self.problem = problem
        self.population = []
        self.changed = True
        self.linkageModel = []

    def append(self, solution):
        """Adds a solution to the population of this cluster."""
        self.population.append(solution)

    def clear(self):
        """Clears the population of this cluster."""
        self.population.clear()

    def computeMean(self):
        """Compute the mean of the cluster and determine if it has changed."""
        mean = np.zeros(self.problem.numberOfObjectives).tolist()

        # Only compute the mean if the cluster has solutions in its population.
        if len(self.population) > 0:
            for objective in range(self.problem.numberOfObjectives):
                for solution in self.population:
                    mean[objective] += solution.fitness[objective]
                mean[objective] /= len(self.population)

        if mean != self.mean:
            self.mean = mean
            self.changed = True
        else:
            self.changed = False

    def learnLinkageModel(self, selection):
        """Learns the linkage model using the Unweighted Pair Grouping Method with Arithmetic-mean (UPGMA) procedure."""

        # Compute the mutual information N-by-N matrix, where N is the problem size (amount of variables).
        mutualInformationMatrix = np.zeros((self.problem.problemSize, self.problem.problemSize))
        for x in range(self.problem.problemSize):
            for y in range(x):
                mutualInformationMatrix[x][y] = util.computeMutualInformation(x, y, selection)
                mutualInformationMatrix[y][x] = mutualInformationMatrix[x][y]

        # Initialize the subsets and linkage model with all univariate subsets.
        subsets = []
        linkageModel = []
        for x in range(self.problem.problemSize):
            subsets.append([x])
            linkageModel.append([x])

        # Consecutively combine two closest subsets until the subset containing all variable indices is created.
        while len(subsets) > 2:
            X = subsets[0]
            Y = subsets[1]
            closestPair = (X, Y)
            closestPairSimilarity = util.computeMutualInformationUPGMA(X, Y, mutualInformationMatrix)
            for X in subsets:
                for Y in subsets:
                    if X != Y:
                        similarity = util.computeMutualInformationUPGMA(X, Y, mutualInformationMatrix)
                        if similarity > closestPairSimilarity:
                            closestPair = (X, Y)

            # Combine the closest pair.
            X = closestPair[0]
            Y = closestPair[1]
            subsets.remove(X)
            subsets.remove(Y)
            combinedPair = X + Y
            subsets.append(combinedPair)

            # Add the combined pair to the linkage model.
            linkageModel.append(combinedPair)

        self.linkageModel = linkageModel