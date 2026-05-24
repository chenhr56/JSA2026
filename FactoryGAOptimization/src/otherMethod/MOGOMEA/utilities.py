import numpy as np
import math

def euclidianDistance(a, b):
    """Calculates the Euclidian distance between points a and b."""
    return np.linalg.norm(np.array(a) - np.array(b))

def hammingDistance(a, b):
    """Calculates the Hamming distance between points a and b."""
    return np.count_nonzero(np.array(a) != np.array(b))

def randomGenotype(n):
    """Generates a random genotype of length n."""
    return np.random.randint(2, size=n).tolist()

def computeProbability(x, v, population):
    """Compute the probability of variable x occuring with the given value v in the given population."""
    occurrences = 0
    for solution in population:
        if solution.genotype[x] == v:
            occurrences += 1
    return occurrences / len(population)

def computeJointProbability(x, y, v1, v2, population):
    """Compute the joint probability of variable x and y occuring with the given values in the given population."""
    occurrences = 0
    for solution in population:
        if solution.genotype[x] == v1 and solution.genotype[y] == v2:
            occurrences += 1
    return occurrences / len(population)

def computeEntropy(x, population):
    """Compute the entropy of variable x in the given population."""
    entropy = 0
    possibleValues = [0, 1]
    for value in possibleValues:
        probability = computeProbability(x, value, population)
        if probability != 0:
            entropy -= probability * math.log2(probability)
    return entropy

def computeJointEntropy(x, y, population):
    """Compute the joint entropy of variable x and y in the given population."""
    jointEntropy = 0
    possibleValues = [0, 1]
    for v1 in possibleValues:
        for v2 in possibleValues:
            jointProbability = computeJointProbability(x, y, v1, v2, population)
            if jointProbability != 0:
                jointEntropy -= jointProbability * math.log2(jointProbability)
    return  jointEntropy

def computeMutualInformation(x, y, population):
    """Compute the mutual information of variable x and y in the given population."""
    return computeEntropy(x, population) + computeEntropy(y, population) - computeJointEntropy(x, y, population)

def computeMutualInformationUPGMA(X, Y, mutualInformationMatrix):
    """Compute the UPGMA mutual information of variable X and Y using the given mutual information matrix."""
    mutualInformationUPGMA = 0
    for x in X:
        for y in Y:
            mutualInformationUPGMA += mutualInformationMatrix[x][y]
    return mutualInformationUPGMA / (len(X) * len(Y))