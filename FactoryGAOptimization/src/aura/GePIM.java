package aura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import metrics.Configuration;
import metrics.ConfigurationType;
import metrics.SearchDirection;
import metrics.Value;
import metrics.ValueTypeVisitor;
import optimisation.ObjectiveFunction;
import optimisation.ObjectiveFunctionArguments;
import optimisation.ObjectiveFunctionResult;
import optimisation.OptimisationIslandResult;

/**
 * GePIM: Gene Pattern Island Model.
 *
 * Multi-island GA where each island evolves independently using uniform
 * crossover and bit-flip-style mutation. Periodically, elite individuals from
 * all islands are compared to extract gene patterns (sets of linked gene
 * positions), which are stored in a global pool. These patterns then drive
 * LGGM (Linked Gene Groups Migration) — instead of migrating whole
 * individuals, only the genes at linked positions are copied.
 */
public class GePIM {

    // ── Algorithm parameters ────────────────────────────────────────────

    private final int numIslands;
    private final int popPerIsland;
    private final int migrationFreq;
    private final int migrationNum;
    private final int retrievalFreq;
    private final int poolSize;
    private final double crossoverProb;
    private final double mutationProb;
    private final int maxEvaluations;

    // ── Runtime state ───────────────────────────────────────────────────

    private final ObjectiveFunction.LocalObjectiveFunction of;
    private final SearchDirection dir;
    private final Random rng;
    private final ConfigurationType configurationType;
    private final List<String> variableKeys;
    private final int numVars;
    private final int numObjectives;

    private int evaluations;
    private List<List<PopulationEntry>> islands;
    private List<PopulationEntry> bestSoFarPerIsland;
    private List<List<Integer>> genePatternPool;
    private List<PopulationEntry> globalArchive;

    // ── Constructor ─────────────────────────────────────────────────────

    public GePIM(List<Configuration> initialPopulation,
                 ObjectiveFunction.LocalObjectiveFunction of,
                 SearchDirection dir,
                 Random rng,
                 int maxEvaluations) {
        this(initialPopulation, of, dir, rng, maxEvaluations,
                30,   // numIslands
                200,  // popPerIsland (scaled down if initial pop is smaller)
                50,   // migrationFreq
                40,   // migrationNum
                500,  // retrievalFreq
                300,  // poolSize
                0.3,  // crossoverProb
                1.0 / (3 * Math.max(1, initialPopulation.get(0)
                        .getControlledMetrics().size()))  // mutationProb
        );
    }

    public GePIM(List<Configuration> initialPopulation,
                 ObjectiveFunction.LocalObjectiveFunction of,
                 SearchDirection dir,
                 Random rng,
                 int maxEvaluations,
                 int numIslands,
                 int popPerIsland,
                 int migrationFreq,
                 int migrationNum,
                 int retrievalFreq,
                 int poolSize,
                 double crossoverProb,
                 double mutationProb) {
        if (initialPopulation.isEmpty())
            throw new IllegalArgumentException();

        this.of = of;
        this.dir = dir;
        this.rng = rng;
        this.maxEvaluations = maxEvaluations;
        this.numIslands = numIslands;
        this.popPerIsland = popPerIsland;
        this.migrationFreq = migrationFreq;
        this.migrationNum = Math.min(migrationNum, popPerIsland);
        this.retrievalFreq = retrievalFreq;
        this.poolSize = poolSize;
        this.crossoverProb = crossoverProb;
        this.mutationProb = mutationProb;

        this.configurationType = initialPopulation.get(0).getConfigurationType();
        this.numObjectives = of.numObjectives();

        this.variableKeys = new ArrayList<>(
                initialPopulation.get(0).getControlledMetrics().keySet());
        Collections.sort(this.variableKeys);
        this.numVars = variableKeys.size();

        this.genePatternPool = new ArrayList<>();
        this.globalArchive = new ArrayList<>();
        this.bestSoFarPerIsland = new ArrayList<>(
                Collections.nCopies(numIslands, null));

        // Initialize islands
        this.islands = new ArrayList<>();
        for (int i = 0; i < numIslands; i++) {
            List<PopulationEntry> island = new ArrayList<>();
            for (int j = 0; j < popPerIsland; j++) {
                Configuration config;
                if (j < initialPopulation.size()) {
                    config = initialPopulation.get(j);
                } else {
                    config = randomConfiguration();
                }
                PopulationEntry entry = evaluate(config);
                island.add(entry);
                updateGlobalArchive(entry);
            }
            islands.add(island);
            updateBestSoFar(i);
        }
    }

    // ── Static factory ──────────────────────────────────────────────────

    public static OptimisationIslandResult apply(
            List<Configuration> initialPopulation,
            ObjectiveFunction.LocalObjectiveFunction of,
            SearchDirection dir,
            Random rng,
            int maxEvaluations) {

        GePIM gepim = new GePIM(initialPopulation, of, dir, rng, maxEvaluations);
        return gepim.evolve();
    }

    public static OptimisationIslandResult apply(
            List<Configuration> initialPopulation,
            ObjectiveFunction.LocalObjectiveFunction of,
            SearchDirection dir,
            Random rng,
            int maxEvaluations,
            int numIslands, int popPerIsland,
            int migrationFreq, int migrationNum, int retrievalFreq, int poolSize,
            double crossoverProb, double mutationProb) {

        GePIM gepim = new GePIM(initialPopulation, of, dir, rng, maxEvaluations,
                numIslands, popPerIsland, migrationFreq, migrationNum,
                retrievalFreq, poolSize, crossoverProb, mutationProb);
        return gepim.evolve();
    }

    // ── Main evolution loop ─────────────────────────────────────────────

    public OptimisationIslandResult evolve() {
        int iteration = 1;

        while (evaluations < maxEvaluations) {
            // Evolve each island independently
            for (int i = 0; i < numIslands; i++) {
                evolveIsland(i);
            }

            // Periodic linkage retrieval
            if (iteration % retrievalFreq == 0) {
                retrieveLinkage();
            }

            // Periodic LGGM migration
            if (iteration % migrationFreq == 0) {
                migrateLinkedGeneGroups();
            }

            iteration++;
        }

        // Collect all individuals from all islands for the final result
        List<PopulationEntry> allIndividuals = new ArrayList<>();
        for (List<PopulationEntry> island : islands) {
            allIndividuals.addAll(island);
        }

        return new OptimisationIslandResult(new ArrayList<>(globalArchive), allIndividuals, null);
    }

    // ── Island GA evolution ─────────────────────────────────────────────

    private void evolveIsland(int islandIdx) {
        List<PopulationEntry> pop = islands.get(islandIdx);
        List<PopulationEntry> offspring = new ArrayList<>();

        for (int j = 0; j < pop.size() / 2; j++) {
            // Tournament selection
            PopulationEntry p1 = tournamentSelect(pop);
            PopulationEntry p2 = tournamentSelect(pop);

            // Uniform crossover
            Configuration c1 = uniformCrossover(p1.getConfiguration(), p2.getConfiguration());
            Configuration c2 = uniformCrossover(p1.getConfiguration(), p2.getConfiguration());

            // Mutation
            c1 = mutate(c1);
            c2 = mutate(c2);

            // Evaluate
            PopulationEntry e1 = evaluate(c1);
            PopulationEntry e2 = evaluate(c2);
            offspring.add(e1);
            offspring.add(e2);

            updateGlobalArchive(e1);
            updateGlobalArchive(e2);

            if (evaluations >= maxEvaluations)
                break;
        }

        // Elitism: keep best from old population
        PopulationEntry oldBest = getCurrentBest(islandIdx);
        if (oldBest != null && offspring.size() < popPerIsland) {
            offspring.add(oldBest);
        }

        // Replace population
        while (offspring.size() > popPerIsland) {
            offspring.remove(offspring.size() - 1);
        }
        while (offspring.size() < popPerIsland) {
            offspring.add(evaluate(randomConfiguration()));
        }
        islands.set(islandIdx, offspring);
        updateBestSoFar(islandIdx);
    }

    // ── Tournament selection ────────────────────────────────────────────

    private PopulationEntry tournamentSelect(List<PopulationEntry> pop) {
        PopulationEntry a = pop.get(rng.nextInt(pop.size()));
        PopulationEntry b = pop.get(rng.nextInt(pop.size()));
        if (NSGA2.paretoDominates(a.getObjectives(), b.getObjectives(), dir)) {
            return a;
        }
        return b;
    }

    // ── Uniform crossover ───────────────────────────────────────────────

    private Configuration uniformCrossover(Configuration c1, Configuration c2) {
        Map<String, Value> result = new HashMap<>();
        Map<String, Value> m1 = c1.getControlledMetrics();
        Map<String, Value> m2 = c2.getControlledMetrics();

        for (String key : variableKeys) {
            if (rng.nextDouble() < crossoverProb) {
                result.put(key, m2.get(key));
            } else {
                result.put(key, m1.get(key));
            }
        }
        return new Configuration(configurationType, result, Collections.emptyMap());
    }

    // ── Mutation (bit-flip equivalent for arbitrary Value types) ─────────

    private Configuration mutate(Configuration c) {
        Map<String, Value> controls = new HashMap<>(c.getControlledMetrics());

        for (String key : variableKeys) {
            if (rng.nextDouble() < mutationProb) {
                Value oldVal = controls.get(key);
                Value newVal = new ValueTypeVisitor.RandomValueVisitor(rng)
                        .visit(oldVal.getType());
                controls.put(key, newVal);
            }
        }
        return new Configuration(configurationType, controls, Collections.emptyMap());
    }

    // ── Random configuration ────────────────────────────────────────────

    private Configuration randomConfiguration() {
        Map<String, Value> controls = new HashMap<>();
        for (String key : variableKeys) {
            // Use the first island's first individual as reference for value types
            Value refVal = islands.get(0).get(0).getConfiguration()
                    .getControlledMetrics().get(key);
            Value newVal = new ValueTypeVisitor.RandomValueVisitor(rng)
                    .visit(refVal.getType());
            controls.put(key, newVal);
        }
        return new Configuration(configurationType, controls, Collections.emptyMap());
    }

    // ── Current best per island ─────────────────────────────────────────

    private PopulationEntry getCurrentBest(int islandIdx) {
        List<PopulationEntry> pop = islands.get(islandIdx);
        if (pop.isEmpty())
            return null;
        PopulationEntry best = pop.get(0);
        for (PopulationEntry e : pop) {
            if (NSGA2.paretoDominates(e.getObjectives(), best.getObjectives(), dir)) {
                best = e;
            }
        }
        return best;
    }

    private void updateBestSoFar(int islandIdx) {
        PopulationEntry current = getCurrentBest(islandIdx);
        if (current == null)
            return;
        PopulationEntry best = bestSoFarPerIsland.get(islandIdx);
        if (best == null
                || NSGA2.paretoDominates(current.getObjectives(), best.getObjectives(), dir)) {
            bestSoFarPerIsland.set(islandIdx, current);
        }
    }

    // ── retrieveLinkage() ───────────────────────────────────────────────

    private void retrieveLinkage() {
        // Collect elites: currentBest + bestSoFar from each island
        List<PopulationEntry> elites = new ArrayList<>();
        for (int i = 0; i < numIslands; i++) {
            PopulationEntry cb = getCurrentBest(i);
            if (cb != null)
                elites.add(cb);
            PopulationEntry bs = bestSoFarPerIsland.get(i);
            if (bs != null)
                elites.add(bs);
        }

        // Pairwise comparison: generate gene patterns from differing positions
        for (int a = 0; a < elites.size(); a++) {
            for (int b = a + 1; b < elites.size(); b++) {
                List<Integer> pattern = differingPositions(
                        elites.get(a).getConfiguration(),
                        elites.get(b).getConfiguration());
                if (!pattern.isEmpty()) {
                    insertIntoPool(pattern);
                }
            }
        }
    }

    /**
     * Returns the indices of controlled metrics that differ between two
     * configurations. These form a gene pattern (potential linkage group).
     */
    private List<Integer> differingPositions(Configuration c1, Configuration c2) {
        List<Integer> positions = new ArrayList<>();
        Map<String, Value> m1 = c1.getControlledMetrics();
        Map<String, Value> m2 = c2.getControlledMetrics();

        for (int i = 0; i < numVars; i++) {
            String key = variableKeys.get(i);
            if (!m1.get(key).equals(m2.get(key))) {
                positions.add(i);
            }
        }
        return positions;
    }

    private void insertIntoPool(List<Integer> pattern) {
        if (genePatternPool.size() < poolSize) {
            genePatternPool.add(pattern);
        } else {
            // Random replacement: frequently-generated patterns survive longer
            int idx = rng.nextInt(genePatternPool.size());
            genePatternPool.set(idx, pattern);
        }
    }

    // ── migrateLinkedGeneGroups() (LGGM) ────────────────────────────────

    private void migrateLinkedGeneGroups() {
        if (numIslands < 2 || genePatternPool.isEmpty())
            return;

        // Select source and receiving islands
        int srcIdx = rng.nextInt(numIslands);
        int recvIdx;
        do {
            recvIdx = rng.nextInt(numIslands);
        } while (recvIdx == srcIdx);

        List<PopulationEntry> srcPop = islands.get(srcIdx);
        List<PopulationEntry> recvPop = islands.get(recvIdx);

        // Get best individuals from each (up to migrationNum)
        List<PopulationEntry> srcBest = getSortedBest(srcPop, migrationNum);
        List<PopulationEntry> recvBest = getSortedBest(recvPop, migrationNum);

        // Pair by rank and migrate linked genes
        int pairs = Math.min(srcBest.size(), recvBest.size());
        for (int i = 0; i < pairs && evaluations < maxEvaluations; i++) {
            List<Integer> pattern = genePatternPool.get(
                    rng.nextInt(genePatternPool.size()));

            PopulationEntry src = srcBest.get(i);
            Map<String, Value> recvControls = new HashMap<>(
                    recvBest.get(i).getConfiguration().getControlledMetrics());
            Map<String, Value> srcControls = src.getConfiguration().getControlledMetrics();

            // Copy linked genes from source to receiving
            for (int pos : pattern) {
                String key = variableKeys.get(pos);
                recvControls.put(key, srcControls.get(key));
            }

            Configuration newConfig = new Configuration(configurationType,
                    recvControls, Collections.emptyMap());
            PopulationEntry modified = evaluate(newConfig);
            updateGlobalArchive(modified);

            // Replace the receiving individual
            int recvOrigIdx = recvPop.indexOf(recvBest.get(i));
            if (recvOrigIdx >= 0) {
                recvPop.set(recvOrigIdx, modified);
            }
        }
    }

    private List<PopulationEntry> getSortedBest(List<PopulationEntry> pop, int n) {
        List<PopulationEntry> sorted = new ArrayList<>(pop);
        // Sort by dominance rank: non-dominated first
        sorted.sort((a, b) -> {
            if (NSGA2.paretoDominates(a.getObjectives(), b.getObjectives(), dir))
                return -1;
            if (NSGA2.paretoDominates(b.getObjectives(), a.getObjectives(), dir))
                return 1;
            return 0;
        });
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    // ── Global Pareto archive ───────────────────────────────────────────

    private void updateGlobalArchive(PopulationEntry candidate) {
        for (int i = 0; i < globalArchive.size(); i++) {
            PopulationEntry e = globalArchive.get(i);
            if (NSGA2.paretoDominates(candidate.getObjectives(),
                    e.getObjectives(), dir)) {
                globalArchive.remove(i);
                i--;
            } else if (NSGA2.paretoDominates(e.getObjectives(),
                    candidate.getObjectives(), dir)) {
                return;
            }
        }
        for (PopulationEntry e : globalArchive) {
            if (e.getObjectives().equals(candidate.getObjectives()))
                return;
        }
        globalArchive.add(candidate);
    }

    // ── Evaluation ──────────────────────────────────────────────────────

    private PopulationEntry evaluate(Configuration config) {
        evaluations++;
        ObjectiveFunctionResult result = of.evaluate(
                new ObjectiveFunctionArguments(config, config.getControlledMetrics()));
        return new PopulationEntry(result.getConfiguration(),
                result.getObjectiveValues());
    }
}
