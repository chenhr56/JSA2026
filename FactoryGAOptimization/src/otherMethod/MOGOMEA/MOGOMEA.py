from solution import Solution
from cluster import Cluster
import utilities as util
import random
import copy
import math

class MOGOMEA:
    def __init__(self, populationSize, amountOfClusters, problem, maxEvaluations):
        self.populationSize = populationSize
        self.amountOfClusters = amountOfClusters
        self.problem = problem
        self.evaluations = 0 # Number of evaluations
        self.maxEvaluations = maxEvaluations
        self.currentGeneration = 0 # Generation number
        self.noImprovementStretch = 0 # No-improvement stretch
        self.population = []
        self.elitistArchive = [[]] # TODO: POSSIBLE CHANGE = REDUCE MEMORY BY ONLY STORING TWO ARCHIVES (t - 1 AND t)
        self.clusters = []
        self.extremeClusters = {}

    def evolve(self):
        """Runs the algorithm until an optimum is found or until the maximum amount of evaluations is reached."""
        for _ in range(self.populationSize):
            solution = self.createRandomSolution()
            self.population.append(solution)
            self.evaluateFitness(solution)
            self.updateElitistArchive(self.elitistArchive[self.currentGeneration], solution)

        while self.evaluations < self.maxEvaluations and self.noImprovementStretch < 1 + 2 * math.floor(math.log10(self.populationSize)):
            print(str(self.evaluations / self.maxEvaluations * 100) + "%")

            self.currentGeneration += 1
            self.elitistArchive.append(copy.deepcopy(self.elitistArchive[self.currentGeneration - 1]))
            self.clusterPopulation()
            self.determineExtremeClusters()

            for cluster in self.clusters:
                selection = self.tournamentSelection(cluster)
                cluster.learnLinkageModel(selection)

            offspring = []
            for solution in self.population:
                cluster = self.determineCluster(solution)
                if cluster in self.extremeClusters:
                    objective = random.choice(self.extremeClusters[cluster])
                    offspring.append(self.singleObjectiveOptimalMixing(objective, solution, cluster, self.elitistArchive[self.currentGeneration]))
                else:
                    offspring.append(self.multiObjectiveOptimalMixing(solution, cluster, self.elitistArchive[self.currentGeneration]))
            self.population = offspring

            if self.evaluateFitnessElitistArchive(self.elitistArchive[self.currentGeneration]) == self.evaluateFitnessElitistArchive(self.elitistArchive[self.currentGeneration - 1]):
               self.noImprovementStretch += 1
            else:
               self.noImprovementStretch = 0

    def createRandomSolution(self):
        """Creates a random solution."""
        # TODO: HOW DO YOU CREATE A RANDOM SOLUTION?
        randomSolution = Solution(util.randomGenotype(self.problem.problemSize))

        while not self.problem.isValidSolution(randomSolution):
            randomSolution = Solution(util.randomGenotype(self.problem.problemSize))
        return randomSolution

    def evaluateFitness(self, solution):
        """Evaluates the fitness of the given solution."""
        self.evaluations += 1
        self.problem.evaluateFitness(solution)

    def clusterPopulation(self):
        """Clusters the given population into k clusters using balanced k-leader-means clustering."""
        # TODO: POSSIBLE CHANGE = CALCULATE OPTIMAL k VALUE

        # The first leader is the solution with maximum value in an arbitrary objective.
        leaders = [self.population[0]]
        for solution in self.population:
            if solution.fitness[0] > leaders[0].fitness[0]:
                leaders[0] = solution

        # The solution with the largest nearest-leader distance is chosen as the next leader,
        # repeated k - 1 times to obtain k leaders.
        for _ in range(self.amountOfClusters - 1):
            nearestLeaderDistance = {}
            for solution in self.population:
                if solution not in leaders:
                    nearestLeaderDistance[solution] = util.euclidianDistance(solution.fitness, leaders[0].fitness)
                    for leader in leaders:
                        leaderDistance = util.euclidianDistance(solution.fitness, leader.fitness)
                        if leaderDistance < nearestLeaderDistance[solution]:
                            nearestLeaderDistance[solution] = leaderDistance
            leader = max(nearestLeaderDistance, key=nearestLeaderDistance.get)
            leaders.append(leader)

        # k-means clustering is performed with k leaders as the initial cluster means.
        clusters = []
        for leader in leaders:
            mean = leader.fitness
            cluster = Cluster(mean, self.problem)
            clusters.append(cluster)

        # Perform k-means clustering until all clusters are unchanged.
        while True in [cluster.changed for cluster in clusters]:
            for solution in self.population:
                nearestCluster = clusters[0]
                nearestClusterDistance = util.euclidianDistance(solution.fitness, nearestCluster.mean)
                for cluster in clusters:
                    clusterDistance = util.euclidianDistance(solution.fitness, cluster.mean)
                    if clusterDistance < nearestClusterDistance:
                        nearestCluster = cluster
                        nearestClusterDistance = clusterDistance
                nearestCluster.append(solution)

            for cluster in clusters:
                cluster.computeMean()
                cluster.clear()

        # Expand the clusters with the closest c solutions.
        c = int(2 / self.amountOfClusters * self.populationSize)
        for cluster in clusters:
            distance = {}
            for solution in self.population:
                distance[solution] = util.euclidianDistance(solution.fitness, cluster.mean)
            for _ in range(c):
                if len(distance) > 0:
                    solution = min(distance, key=distance.get)
                    del distance[solution]
                    cluster.append(solution)
        self.clusters = clusters

    def tournamentSelection(self, cluster):
        """Performs tournament selection in the given cluster."""
        selection = []
        for _ in range(len(cluster.population)):
            one = random.choice(cluster.population)
            other = random.choice(cluster.population)
            if one.dominates(other):
                selection.append(one)
            else:
                selection.append(other)
        return selection

    def determineCluster(self, solution):
        """Determines the cluster index of the given solution from the list of clusters."""

        # Determine the clusters that are assigned to the solution.
        assignedClusters = []
        for cluster in self.clusters:
            if solution in cluster.population:
                assignedClusters.append(cluster)

        if len(assignedClusters) > 0:
            # One or more clusters are assigned to the solution, return a random one.
            return random.choice(self.clusters)
        else:
            # No clusters are assigned to the solution, return the nearest one.
            nearestCluster = self.clusters[0]
            nearestClusterDistance = util.euclidianDistance(solution.fitness, nearestCluster.mean)
            for cluster in self.clusters:
                clusterDistance = util.euclidianDistance(solution.fitness, cluster.mean)
                if clusterDistance < nearestClusterDistance:
                    nearestCluster = cluster
                    nearestClusterDistance = clusterDistance
            return nearestCluster

    def determineExtremeClusters(self):
        """Determines which clusters are extreme clusters."""
        extremeClusters = {}

        for one in self.clusters:
            for objective in range(self.problem.numberOfObjectives):
                isExtreme = True
                for other in self.clusters:
                    if other != one:
                        if other.mean[objective] > one.mean[objective]:
                            isExtreme = False
                if isExtreme:
                    if one in extremeClusters:
                        extremeClusters[one].append(objective)
                    else:
                        extremeClusters[one] = [objective]

        self.extremeClusters = extremeClusters

    def multiObjectiveOptimalMixing(self, parent, cluster, elitistArchive):
        """Generates an offspring solution using multi-objective optimal mixing."""

        # Clone the parent solution and create a backup solution.
        offspring = copy.deepcopy(parent)
        backup = copy.deepcopy(parent)
        changed = False

        # Traverse the cluster's linkage groups and choose a random donor and apply its genotype to the offspring if
        # the mixing results in an improved, equally good or side-stepped solution.
        for linkageGroup in cluster.linkageModel:
            donor = random.choice(cluster.population)
            unchanged = True
            for index in linkageGroup:
                offspring.genotype[index] = donor.genotype[index]
                if offspring.genotype[index] != backup.genotype[index]:
                    unchanged = False
            if not unchanged:
                self.evaluateFitness(offspring)
                if offspring.dominates(backup) or offspring.fitness == backup.fitness or not self.dominatedByElitistArchive(elitistArchive, offspring):
                    for index in linkageGroup:
                        backup.genotype[index] = offspring.genotype[index]
                    backup.fitness = copy.deepcopy(offspring.fitness)
                    changed = True
                else:
                    for index in linkageGroup:
                        offspring.genotype[index] = backup.genotype[index]
                    offspring.fitness = copy.deepcopy(backup.fitness)

        # If the previous mixing step did not change the offspring, repeat the same step, but now pick a random elitist
        # from the elitist archive as a donor. Apply the donor's genotype to the offspring if the mixing results in a
        # direct domination or a pareto front improvement.
        if not changed or self.noImprovementStretch > 1 + math.floor(math.log10(self.populationSize)):
            changed = False
            for linkageGroup in cluster.linkageModel:
                donor = random.choice(elitistArchive)
                unchanged = True
                for index in linkageGroup:
                    offspring.genotype[index] = donor.genotype[index]
                    if offspring.genotype[index] != backup.genotype[index]:
                        unchanged = False
                if not unchanged:
                    self.evaluateFitness(offspring)
                    if offspring.dominates(backup) or (not self.dominatedByElitistArchive(elitistArchive, offspring) and not self.fitnessContainedInElitistArchive(elitistArchive, offspring)):
                        for index in linkageGroup:
                            backup.genotype[index] = offspring.genotype[index]
                        backup.fitness = copy.deepcopy(offspring.fitness)
                        changed = True
                    else:
                        for index in linkageGroup:
                            offspring.genotype[index] = backup.genotype[index]
                        offspring.fitness = copy.deepcopy(backup.fitness)
                if changed: break

        # If both previous mixing steps still did not change the offspring, pick a random elitist from the elitist
        # archive as a donor and apply the full donor's genotype to the offspring.
        if not changed:
            donor = random.choice(elitistArchive)
            offspring.genotype = copy.deepcopy(donor.genotype)
            offspring.fitness = copy.deepcopy(donor.fitness)

        self.updateElitistArchive(elitistArchive, offspring)
        return offspring

    def singleObjectiveOptimalMixing(self, objective, parent, cluster, elitistArchive):
        """Generates an offspring solution using single-objective optimal mixing with the given objective."""

        # Clone the parent solution and create a backup solution.
        best = copy.deepcopy(parent)
        offspring = copy.deepcopy(parent)
        backup = copy.deepcopy(parent)
        changed = False

        for linkageGroup in cluster.linkageModel:
            donor = random.choice(cluster.population)
            unchanged = True
            for index in linkageGroup:
                offspring.genotype[index] = donor.genotype[index]
                if offspring.genotype[index] != backup.genotype[index]:
                    unchanged = False
            if not unchanged:
                self.evaluateFitness(offspring)
                if offspring.fitness[objective] >= backup.fitness[objective]:
                    for index in linkageGroup:
                        backup.genotype[index] = offspring.genotype[index]
                    backup.fitness = copy.deepcopy(offspring.fitness)
                    changed = True
                else:
                    for index in linkageGroup:
                        offspring.genotype[index] = backup.genotype[index]
                    offspring.fitness = copy.deepcopy(backup.fitness)
                if donor.fitness[objective] > best.fitness[objective]:
                    best = donor

        if not changed or self.noImprovementStretch > 1 + math.floor(math.log10(self.populationSize)):
            changed = False
            for linkageGroup in cluster.linkageModel:
                donor = best
                unchanged = True
                for index in linkageGroup:
                    offspring.genotype[index] = donor.genotype[index]
                    if offspring.genotype[index] != backup.genotype[index]:
                        unchanged = False
                if not unchanged:
                    self.evaluateFitness(offspring)
                    if offspring.fitness[objective] >= backup.fitness[objective]:
                        for index in linkageGroup:
                            backup.genotype[index] = offspring.genotype[index]
                        backup.fitness = copy.deepcopy(offspring.fitness)
                        changed = True
                    else:
                        for index in linkageGroup:
                            offspring.genotype[index] = backup.genotype[index]
                        offspring.fitness = copy.deepcopy(backup.fitness)
                if changed: break

        if not changed:
            donor = best
            offspring.genotype = copy.deepcopy(donor.genotype)
            offspring.fitness = copy.deepcopy(donor.fitness)

        self.updateElitistArchive(elitistArchive, offspring)
        return offspring

    def updateElitistArchive(self, elitistArchive, solution):
        """Updates the given elitist archive using the given solution."""

        # Discard the solution if it is already in the elitist archive.
        for elitist in elitistArchive:
            if elitist.genotype == solution.genotype:
                return

        # Replace elitist by solution if the solution is further away from the nearest archive neighbor of the
        # elitist, based on the Hamming distance.
        # TODO: POSSIBLE CHANGE = CHOOSE A DIFFERENT METRIC TO ENSURE DIVERSITY IN THE ARCHIVE
        for i, elitist in enumerate(elitistArchive):
            if elitist.fitness == solution.fitness:
                if i == 0:
                    nearestElitist = elitistArchive[i + 1]
                else:
                    nearestElitist = elitistArchive[i - 1]
                nearestNeighborDistance = util.hammingDistance(elitist.genotype, nearestElitist.genotype)
                for otherElitist in elitistArchive:
                    if elitist != otherElitist:
                        distance = util.euclidianDistance(elitist.genotype, otherElitist.genotype)
                        if distance < nearestNeighborDistance:
                            nearestElitist = otherElitist
                            nearestNeighborDistance = distance

                solutionDistance = util.hammingDistance(solution.genotype, nearestElitist.genotype)
                if solutionDistance < nearestNeighborDistance:
                    elitistArchive[i] = solution
                    return

        # Determine if the solution is dominated by elitists, and determine which elitists are dominated by
        # the solution.
        dominatedElitists = []
        for elitist in elitistArchive:
            if elitist.dominates(solution):
                return
            if solution.dominates(elitist):
                dominatedElitists.append(elitist)

        # Remove elitists that are dominated by the solution.
        elitistArchive.append(solution)
        for dominatedElitist in dominatedElitists:
            elitistArchive.remove(dominatedElitist)

    def dominatedByElitistArchive(self, elitistArchive, solution):
        """Determines whether the given solution is dominated by any solution in the given elitist archive."""
        for elitist in elitistArchive:
            if elitist.dominates(solution):
                return True
        return False

    def fitnessContainedInElitistArchive(self, elitistArchive, solution):
        """Determines whether a solution in the elitists archive has the same fitness as the given solution."""
        for elitist in elitistArchive:
            if elitist.fitness == solution.fitness:
                return True
        return False

    def evaluateFitnessElitistArchive(self, elitistArchive):
        """Evaluates the fitness of the given elitist archive."""
        fitness = [0, 0]
        for solution in elitistArchive:
            fitness = [x + y for x, y in zip(fitness, solution.fitness)]
        return fitness