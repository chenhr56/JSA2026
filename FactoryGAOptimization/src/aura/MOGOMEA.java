package aura;

import java.util.ArrayList;
// import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import metrics.Configuration;
import metrics.ConfigurationType;
import metrics.SearchDirection;
import metrics.Value;
import optimisation.ObjectiveFunction;
import optimisation.ObjectiveFunctionArguments;
import optimisation.ObjectiveFunctionResult;
import optimisation.OptimisationIslandResult;

/**
 * MO-GOMEA: Multi-Objective Gene-pool Optimal Mixing Evolutionary Algorithm.
 *
 * Ported from the Python reference implementation. Combines overlapping k-means
 * clustering in objective space, UPGMA linkage learning on mutual information,
 * and Gene-pool Optimal Mixing (GOM) as the variation operator.
 */
public class MOGOMEA {

    private final int populationSize;
    private final int amountOfClusters;
    private final ObjectiveFunction.LocalObjectiveFunction objectiveFunction;
    private final int maxEvaluations;
    private final SearchDirection searchDirection;
    private final Random rng;
    private final ConfigurationType configurationType;

    private int evaluations;
    private int noImprovementStretch;
    private List<PopulationEntry> population;
    private List<List<PopulationEntry>> elitistArchive;
    private List<Cluster> clusters;
    private Map<Cluster, List<Integer>> extremeClusters;
    private List<String> variableKeys;
    private int numberOfObjectives;

    // ── Cluster (inner class) ───────────────────────────────────────────

    private static final class Cluster {
        List<Double> mean;
        List<PopulationEntry> population = new ArrayList<>();
        boolean changed = true;
        List<List<Integer>> linkageModel = new ArrayList<>();

        Cluster(List<Double> mean) {
            this.mean = mean;
        }

        void append(PopulationEntry solution) {
            population.add(solution);
        }

        void clear() {
            population.clear();
        }

        void computeMean(int numberOfObjectives) {
            List<Double> newMean = new ArrayList<>(Collections.nCopies(numberOfObjectives, 0.0));
            if (!population.isEmpty()) {
                for (int o = 0; o < numberOfObjectives; o++) {
                    double sum = 0;
                    for (PopulationEntry pe : population) {
                        sum += pe.getObjectives().get(o);
                    }
                    newMean.set(o, sum / population.size());
                }
            }
            if (!newMean.equals(mean)) {
                mean = newMean;
                changed = true;
            } else {
                changed = false;
            }
        }

        void learnLinkageModel(List<PopulationEntry> selection, List<String> variableKeys, int numVars) {
            double[][] miMatrix = new double[numVars][numVars];
            for (int x = 0; x < numVars; x++) {
                for (int y = 0; y < x; y++) {
                    double mi = computeMutualInformation(x, y, selection, variableKeys);
                    miMatrix[x][y] = mi;
                    miMatrix[y][x] = mi;
                }
            }

            List<List<Integer>> subsets = new ArrayList<>();
            linkageModel = new ArrayList<>();
            for (int i = 0; i < numVars; i++) {
                List<Integer> singleton = new ArrayList<>();
                singleton.add(i);
                subsets.add(singleton);
                linkageModel.add(singleton);
            }

            while (subsets.size() > 2) {
                List<Integer> closestX = subsets.get(0);
                List<Integer> closestY = subsets.get(1);
                double bestSim = computeMIUPGMA(closestX, closestY, miMatrix);

                for (List<Integer> s1 : subsets) {
                    for (List<Integer> s2 : subsets) {
                        if (s1 != s2) {
                            double sim = computeMIUPGMA(s1, s2, miMatrix);
                            if (sim > bestSim) {
                                bestSim = sim;
                                closestX = s1;
                                closestY = s2;
                            }
                        }
                    }
                }

                subsets.remove(closestX);
                subsets.remove(closestY);
                List<Integer> combined = new ArrayList<>(closestX);
                combined.addAll(closestY);
                subsets.add(combined);
                linkageModel.add(combined);
            }
        }
    }

    // ── Constructor ─────────────────────────────────────────────────────

    public MOGOMEA(List<Configuration> initialPopulation,
                   ObjectiveFunction.LocalObjectiveFunction objectiveFunction,
                   SearchDirection searchDirection,
                   Random rng,
                   int maxEvaluations,
                   int amountOfClusters) {
        if (initialPopulation.isEmpty())
            throw new IllegalArgumentException("Initial population must not be empty");

        this.populationSize = initialPopulation.size();
        this.objectiveFunction = objectiveFunction;
        this.searchDirection = searchDirection;
        this.rng = rng;
        this.maxEvaluations = maxEvaluations;
        this.amountOfClusters = amountOfClusters;
        this.numberOfObjectives = objectiveFunction.numObjectives();
        this.configurationType = initialPopulation.get(0).getConfigurationType();

        this.variableKeys = new ArrayList<>(
                initialPopulation.get(0).getControlledMetrics().keySet());
        Collections.sort(this.variableKeys);

        this.population = new ArrayList<>();
        this.elitistArchive = new ArrayList<>();
        this.elitistArchive.add(new ArrayList<>());

        for (Configuration config : initialPopulation) {
            PopulationEntry entry = evaluate(config);
            this.population.add(entry);
            updateElitistArchive(this.elitistArchive.get(0), entry);
        }
    }

    // ── Static factory ──────────────────────────────────────────────────

    public static OptimisationIslandResult apply(
            List<Configuration> initialPopulation,
            ObjectiveFunction.LocalObjectiveFunction of,
            SearchDirection dir,
            Random rng,
            int maxEvaluations,
            int amountOfClusters) {

        MOGOMEA algo = new MOGOMEA(initialPopulation, of, dir, rng, maxEvaluations, amountOfClusters);
        return algo.evolve();
    }

    // ── Main evolution loop ─────────────────────────────────────────────

    public OptimisationIslandResult evolve() {
        int noImprovementLimit = 1 + 2 * (int) Math.floor(Math.log10(populationSize));
        int currentGeneration = 0;

        while (evaluations < maxEvaluations && noImprovementStretch < noImprovementLimit) {
            currentGeneration++;
            elitistArchive.add(new ArrayList<>(elitistArchive.get(currentGeneration - 1)));

            clusterPopulation();
            determineExtremeClusters();

            for (Cluster cluster : clusters) {
                List<PopulationEntry> selection = tournamentSelection(cluster);
                cluster.learnLinkageModel(selection, variableKeys, variableKeys.size());
            }

            List<PopulationEntry> offspring = new ArrayList<>();
            for (PopulationEntry solution : population) {
                Cluster cluster = determineCluster(solution);
                if (extremeClusters.containsKey(cluster)) {
                    List<Integer> objs = extremeClusters.get(cluster);
                    int objective = objs.get(rng.nextInt(objs.size()));
                    offspring.add(singleObjectiveOptimalMixing(objective, solution, cluster,
                            elitistArchive.get(currentGeneration)));
                } else {
                    offspring.add(multiObjectiveOptimalMixing(solution, cluster,
                            elitistArchive.get(currentGeneration)));
                }
            }
            population = offspring;

            if (evaluateFitnessArchive(elitistArchive.get(currentGeneration))
                    .equals(evaluateFitnessArchive(elitistArchive.get(currentGeneration - 1)))) {
                noImprovementStretch++;
            } else {
                noImprovementStretch = 0;
            }
        }

        return new OptimisationIslandResult(
                new ArrayList<>(elitistArchive.get(elitistArchive.size() - 1)),
                population, null);
    }

    // ── Clustering ──────────────────────────────────────────────────────

    private void clusterPopulation() {
        // Select k leaders: first = best on objective 0, rest by farthest-point
        List<PopulationEntry> leaders = new ArrayList<>();
        PopulationEntry firstLeader = population.get(0);
        for (PopulationEntry pe : population) {
            if (compareObjective(pe.getObjectives().get(0), firstLeader.getObjectives().get(0)) > 0) {
                firstLeader = pe;
            }
        }
        leaders.add(firstLeader);

        for (int k = 1; k < amountOfClusters; k++) {
            PopulationEntry bestLeader = null;
            double bestDist = -1;
            for (PopulationEntry pe : population) {
                if (leaders.contains(pe))
                    continue;
                double nearestDist = euclideanDistance(pe.getObjectives(), leaders.get(0).getObjectives());
                for (int i = 0; i < leaders.size(); i++) {
                    double d = euclideanDistance(pe.getObjectives(), leaders.get(i).getObjectives());
                    if (d < nearestDist)
                        nearestDist = d;
                }
                if (nearestDist > bestDist) {
                    bestDist = nearestDist;
                    bestLeader = pe;
                }
            }
            if (bestLeader != null)
                leaders.add(bestLeader);
        }

        // Initialize clusters with leader means
        clusters = new ArrayList<>();
        for (PopulationEntry leader : leaders) {
            clusters.add(new Cluster(new ArrayList<>(leader.getObjectives())));
        }

        // k-means iterations
        boolean anyChanged = true;
        while (anyChanged) {
            for (PopulationEntry pe : population) {
                Cluster nearest = clusters.get(0);
                double nearestDist = euclideanDistance(pe.getObjectives(), nearest.mean);
                for (Cluster c : clusters) {
                    double d = euclideanDistance(pe.getObjectives(), c.mean);
                    if (d < nearestDist) {
                        nearest = c;
                        nearestDist = d;
                    }
                }
                nearest.append(pe);
            }

            anyChanged = false;
            for (Cluster c : clusters) {
                c.computeMean(numberOfObjectives);
                if (c.changed)
                    anyChanged = true;
                c.clear();
            }
        }

        // Expand clusters to size c = 2/k * populationSize
        int cSize = Math.max(1, (int) (2.0 / amountOfClusters * populationSize));
        for (Cluster cluster : clusters) {
            List<PopulationEntry> sorted = new ArrayList<>(population);
            final List<Double> cm = cluster.mean;
            sorted.sort((a, b) -> Double.compare(
                    euclideanDistance(a.getObjectives(), cm),
                    euclideanDistance(b.getObjectives(), cm)));
            for (int i = 0; i < Math.min(cSize, sorted.size()); i++) {
                cluster.append(sorted.get(i));
            }
        }
    }

    // ── Tournament selection ────────────────────────────────────────────

    private List<PopulationEntry> tournamentSelection(Cluster cluster) {
        List<PopulationEntry> selection = new ArrayList<>();
        List<PopulationEntry> pop = cluster.population;
        if (pop.isEmpty())
            return selection;

        for (int i = 0; i < pop.size(); i++) {
            PopulationEntry one = pop.get(rng.nextInt(pop.size()));
            PopulationEntry other = pop.get(rng.nextInt(pop.size()));
            if (paretoDominates(one.getObjectives(), other.getObjectives())) {
                selection.add(one);
            } else {
                selection.add(other);
            }
        }
        return selection;
    }

    // ── Cluster assignment ──────────────────────────────────────────────

    private Cluster determineCluster(PopulationEntry solution) {
        List<Cluster> assigned = new ArrayList<>();
        for (Cluster c : clusters) {
            if (c.population.contains(solution)) {
                assigned.add(c);
            }
        }
        if (!assigned.isEmpty()) {
            return assigned.get(rng.nextInt(assigned.size()));
        }
        // Nearest by centroid distance
        Cluster nearest = clusters.get(0);
        double nearestDist = euclideanDistance(solution.getObjectives(), nearest.mean);
        for (Cluster c : clusters) {
            double d = euclideanDistance(solution.getObjectives(), c.mean);
            if (d < nearestDist) {
                nearest = c;
                nearestDist = d;
            }
        }
        return nearest;
    }

    // ── Extreme clusters ────────────────────────────────────────────────

    private void determineExtremeClusters() {
        extremeClusters = new HashMap<>();
        for (int obj = 0; obj < numberOfObjectives; obj++) {
            // Best = minimum mean (MINIMIZING direction)
            Cluster best = clusters.get(0);
            for (Cluster c : clusters) {
                if (c.mean.get(obj) < best.mean.get(obj)) {
                    best = c;
                }
            }
            extremeClusters.computeIfAbsent(best, k -> new ArrayList<>()).add(obj);
        }
    }

    // ── Multi-objective optimal mixing ──────────────────────────────────

    private PopulationEntry multiObjectiveOptimalMixing(PopulationEntry parent, Cluster cluster,
                                                        List<PopulationEntry> archive) {
        Map<String, Value> offControls = new LinkedHashMap<>(
                parent.getConfiguration().getControlledMetrics());
        // Map<String, Value> bakControls = new LinkedHashMap<>(offControls);
        List<Double> bakObjectives = new ArrayList<>(parent.getObjectives());
        boolean changed = false;

        int phase2Threshold = 1 + (int) Math.floor(Math.log10(populationSize));

        // Phase 1: cluster donors
        for (List<Integer> linkageGroup : cluster.linkageModel) {
            PopulationEntry donor = cluster.population.get(rng.nextInt(cluster.population.size()));
            Map<String, Value> donControls = donor.getConfiguration().getControlledMetrics();

            Map<String, Value> trial = new LinkedHashMap<>(offControls);
            boolean unchanged = copyGenes(trial, donControls, linkageGroup);

            if (!unchanged) {
                PopulationEntry trialEntry = evaluate(makeConfig(trial));
                if (paretoDominates(trialEntry.getObjectives(), bakObjectives)
                        || trialEntry.getObjectives().equals(bakObjectives)
                        || !dominatedByArchive(archive, trialEntry)) {
                    copyGenes(offControls, donControls, linkageGroup);
                    bakObjectives = trialEntry.getObjectives();
                    changed = true;
                }
            }
        }

        // Phase 2: archive donors
        if (!changed || noImprovementStretch > phase2Threshold) {
            changed = false;
            for (List<Integer> linkageGroup : cluster.linkageModel) {
                PopulationEntry donor = archive.get(rng.nextInt(archive.size()));
                Map<String, Value> donControls = donor.getConfiguration().getControlledMetrics();

                Map<String, Value> trial = new LinkedHashMap<>(offControls);
                boolean unchanged = copyGenes(trial, donControls, linkageGroup);

                if (!unchanged) {
                    PopulationEntry trialEntry = evaluate(makeConfig(trial));
                    if (paretoDominates(trialEntry.getObjectives(), bakObjectives)
                            || (!dominatedByArchive(archive, trialEntry)
                                    && !fitnessContainedInArchive(archive, trialEntry))) {
                        copyGenes(offControls, donControls, linkageGroup);
                        bakObjectives = trialEntry.getObjectives();
                        changed = true;
                    }
                }
                if (changed)
                    break;
            }
        }

        // Phase 3: full replacement from archive
        if (!changed) {
            PopulationEntry donor = archive.get(rng.nextInt(archive.size()));
            offControls = new LinkedHashMap<>(donor.getConfiguration().getControlledMetrics());
            bakObjectives = new ArrayList<>(donor.getObjectives());
        }

        Configuration resultConfig = makeConfig(offControls);
        PopulationEntry result = makeEntry(resultConfig, bakObjectives);
        updateElitistArchive(archive, result);
        return result;
    }

    // ── Single-objective optimal mixing ─────────────────────────────────

    private PopulationEntry singleObjectiveOptimalMixing(int objective, PopulationEntry parent,
                                                         Cluster cluster, List<PopulationEntry> archive) {
        Map<String, Value> offControls = new LinkedHashMap<>(
                parent.getConfiguration().getControlledMetrics());
        // Map<String, Value> bakControls = new LinkedHashMap<>(offControls);
        List<Double> bakObjectives = new ArrayList<>(parent.getObjectives());
        boolean changed = false;

        int phase2Threshold = 1 + (int) Math.floor(Math.log10(populationSize));

        // Phase 1: cluster donors, track best
        PopulationEntry bestDonor = parent;
        double bestDonorVal = bakObjectives.get(objective);

        for (List<Integer> linkageGroup : cluster.linkageModel) {
            for (PopulationEntry donor : cluster.population) {
                Map<String, Value> donControls = donor.getConfiguration().getControlledMetrics();
                Map<String, Value> trial = new LinkedHashMap<>(offControls);
                boolean unchanged = copyGenes(trial, donControls, linkageGroup);

                if (!unchanged) {
                    PopulationEntry trialEntry = evaluate(makeConfig(trial));
                    if (isBetterOrEqual(trialEntry.getObjectives().get(objective),
                            bakObjectives.get(objective))) {
                        copyGenes(offControls, donControls, linkageGroup);
                        bakObjectives = trialEntry.getObjectives();
                        changed = true;
                    }
                    if (isBetter(donor.getObjectives().get(objective), bestDonorVal)) {
                        bestDonorVal = donor.getObjectives().get(objective);
                        bestDonor = donor;
                    }
                }
            }
        }

        // Phase 2: best donor gene-by-gene
        if (!changed || noImprovementStretch > phase2Threshold) {
            changed = false;
            for (List<Integer> linkageGroup : cluster.linkageModel) {
                Map<String, Value> donControls = bestDonor.getConfiguration().getControlledMetrics();
                Map<String, Value> trial = new LinkedHashMap<>(offControls);
                boolean unchanged = copyGenes(trial, donControls, linkageGroup);

                if (!unchanged) {
                    PopulationEntry trialEntry = evaluate(makeConfig(trial));
                    if (isBetterOrEqual(trialEntry.getObjectives().get(objective),
                            bakObjectives.get(objective))) {
                        copyGenes(offControls, donControls, linkageGroup);
                        bakObjectives = trialEntry.getObjectives();
                        changed = true;
                    }
                }
                if (changed)
                    break;
            }
        }

        // Phase 3: full replacement from best donor
        if (!changed) {
            offControls = new LinkedHashMap<>(bestDonor.getConfiguration().getControlledMetrics());
            bakObjectives = new ArrayList<>(bestDonor.getObjectives());
        }

        Configuration resultConfig = makeConfig(offControls);
        PopulationEntry result = makeEntry(resultConfig, bakObjectives);
        updateElitistArchive(archive, result);
        return result;
    }

    // ── Gene copy helper ────────────────────────────────────────────────

    /**
     * Copies genes at the given linkage group indices from donor to target map.
     * Returns true if no genes changed (all values were already equal).
     */
    private boolean copyGenes(Map<String, Value> target, Map<String, Value> donor,
                              List<Integer> linkageGroup) {
        boolean unchanged = true;
        for (int idx : linkageGroup) {
            String key = variableKeys.get(idx);
            Value donVal = donor.get(key);
            if (!donVal.equals(target.get(key))) {
                target.put(key, donVal);
                unchanged = false;
            }
        }
        return unchanged;
    }

    private Configuration makeConfig(Map<String, Value> controlledMetrics) {
        return new Configuration(configurationType, controlledMetrics,
                Collections.emptyMap());
    }

    private PopulationEntry makeEntry(Configuration config, List<Double> objectives) {
        // Build a Configuration with proper key objectives from the objectives list.
        // The key objectives are set by the evaluation, but if we have them already we
        // can use the stored values.
        return new PopulationEntry(config, objectives);
    }

    // ── Evaluation ──────────────────────────────────────────────────────

    private PopulationEntry evaluate(Configuration config) {
        evaluations++;
        ObjectiveFunctionResult result = objectiveFunction.evaluate(
                new ObjectiveFunctionArguments(config, config.getControlledMetrics()));
        return new PopulationEntry(result.getConfiguration(), result.getObjectiveValues());
    }

    // ── Elitist archive ─────────────────────────────────────────────────

    private void updateElitistArchive(List<PopulationEntry> archive, PopulationEntry solution) {
        // Discard duplicates by configuration
        for (PopulationEntry e : archive) {
            if (e.getConfiguration().equals(solution.getConfiguration()))
                return;
        }

        // Replace by diversity if same fitness
        for (int i = 0; i < archive.size(); i++) {
            PopulationEntry elitist = archive.get(i);
            if (elitist.getObjectives().equals(solution.getObjectives())) {
                PopulationEntry nearest = (i == 0) ? archive.get((i + 1) % archive.size())
                        : archive.get(i - 1);
                double nearestDist = configurationDistance(elitist.getConfiguration(),
                        nearest.getConfiguration());
                for (PopulationEntry other : archive) {
                    if (elitist != other) {
                        double d = configurationDistance(elitist.getConfiguration(),
                                other.getConfiguration());
                        if (d < nearestDist) {
                            nearest = other;
                            nearestDist = d;
                        }
                    }
                }
                double solutionDist = configurationDistance(solution.getConfiguration(),
                        nearest.getConfiguration());
                if (solutionDist < nearestDist) {
                    archive.set(i, solution);
                }
                return;
            }
        }

        // Dominance-based update
        List<PopulationEntry> dominated = new ArrayList<>();
        for (PopulationEntry e : archive) {
            if (paretoDominates(e.getObjectives(), solution.getObjectives()))
                return;
            if (paretoDominates(solution.getObjectives(), e.getObjectives()))
                dominated.add(e);
        }
        archive.add(solution);
        archive.removeAll(dominated);
    }

    private boolean dominatedByArchive(List<PopulationEntry> archive, PopulationEntry solution) {
        for (PopulationEntry e : archive) {
            if (paretoDominates(e.getObjectives(), solution.getObjectives()))
                return true;
        }
        return false;
    }

    private boolean fitnessContainedInArchive(List<PopulationEntry> archive, PopulationEntry solution) {
        for (PopulationEntry e : archive) {
            if (e.getObjectives().equals(solution.getObjectives()))
                return true;
        }
        return false;
    }

    private List<Double> evaluateFitnessArchive(List<PopulationEntry> archive) {
        List<Double> sum = new ArrayList<>(Collections.nCopies(numberOfObjectives, 0.0));
        for (PopulationEntry e : archive) {
            for (int o = 0; o < numberOfObjectives; o++) {
                sum.set(o, sum.get(o) + e.getObjectives().get(o));
            }
        }
        return sum;
    }

    // ── Dominance helpers ───────────────────────────────────────────────

    private boolean paretoDominates(List<Double> o1, List<Double> o2) {
        return NSGA2.paretoDominates(o1, o2, searchDirection);
    }

    private int compareObjective(double o1, double o2) {
        return searchDirection == SearchDirection.MINIMIZING
                ? -Double.compare(o1, o2)
                : Double.compare(o1, o2);
    }

    private boolean isBetter(double a, double b) {
        return searchDirection == SearchDirection.MINIMIZING ? a < b : a > b;
    }

    private boolean isBetterOrEqual(double a, double b) {
        return searchDirection == SearchDirection.MINIMIZING ? a <= b : a >= b;
    }

    // ── Distance / diversity ────────────────────────────────────────────

    private static double euclideanDistance(List<Double> a, List<Double> b) {
        double sum = 0;
        for (int i = 0; i < a.size(); i++) {
            double diff = a.get(i) - b.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private int configurationDistance(Configuration a, Configuration b) {
        int dist = 0;
        Map<String, Value> ca = a.getControlledMetrics();
        Map<String, Value> cb = b.getControlledMetrics();
        for (Map.Entry<String, Value> e : ca.entrySet()) {
            Value vb = cb.get(e.getKey());
            if (vb != null && !e.getValue().equals(vb)) {
                dist++;
            }
        }
        return dist;
    }

    // ── Mutual information / entropy ────────────────────────────────────

    private static double computeMutualInformation(int x, int y,
                                                   List<PopulationEntry> selection,
                                                   List<String> keys) {
        return computeEntropy(x, selection, keys)
                + computeEntropy(y, selection, keys)
                - computeJointEntropy(x, y, selection, keys);
    }

    private static double computeEntropy(int varIdx, List<PopulationEntry> selection,
                                         List<String> keys) {
        Map<Value, Integer> counts = new HashMap<>();
        for (PopulationEntry pe : selection) {
            Value v = pe.getConfiguration().getControlledMetrics().get(keys.get(varIdx));
            counts.merge(v, 1, Integer::sum);
        }
        double entropy = 0;
        double n = selection.size();
        for (int count : counts.values()) {
            double p = count / n;
            if (p > 0) {
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    private static double computeJointEntropy(int varIdx1, int varIdx2,
                                              List<PopulationEntry> selection,
                                              List<String> keys) {
        Map<String, Integer> jointCounts = new HashMap<>();
        for (PopulationEntry pe : selection) {
            Map<String, Value> cm = pe.getConfiguration().getControlledMetrics();
            String pairKey = cm.get(keys.get(varIdx1)).toString()
                    + "|||" + cm.get(keys.get(varIdx2)).toString();
            jointCounts.merge(pairKey, 1, Integer::sum);
        }
        double entropy = 0;
        double n = selection.size();
        for (int count : jointCounts.values()) {
            double p = count / n;
            if (p > 0) {
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    private static double computeMIUPGMA(List<Integer> X, List<Integer> Y, double[][] miMatrix) {
        double sum = 0;
        for (int x : X) {
            for (int y : Y) {
                sum += miMatrix[x][y];
            }
        }
        return sum / (X.size() * Y.size());
    }
}
