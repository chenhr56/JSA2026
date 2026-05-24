from MOGOMEA import MOGOMEA
from problems.knapsack.knapsack import Knapsack
import matplotlib.pyplot as plt

# Import a knapsack instance.
instance = []
file = open('../problems/knapsack/instances/dense/80.txt')
for line in file:
    line = line.split()
    line = [int(i) for i in line]
    instance.append(line)

numberOfObjectives = 2
problemSize = instance[0][0] # Number of items in the search space.
capacity = instance[0][1] # Knapsack capacity.
searchSpace = instance[1:] # Problem search space.
problem = Knapsack(numberOfObjectives, problemSize, capacity, searchSpace)

populationSize = 100
amountOfClusters = 3
maxEvaluations = 10**5 # Maximum amount of fitness evaluations.

# Initialize and run the algorithm.
algorithm = MOGOMEA(populationSize, amountOfClusters, problem, maxEvaluations)
algorithm.evolve()

# Plot the initial and final pareto fronts.
for generation in [0, algorithm.currentGeneration]:
    firstObjective = []
    secondObjective = []
    for elitist in algorithm.elitistArchive[generation]:
        firstObjective.append(elitist.fitness[0])
        secondObjective.append(elitist.fitness[1])
    plt.scatter(firstObjective, secondObjective, label=("Generation " + str(generation)))
plt.legend()
plt.show()