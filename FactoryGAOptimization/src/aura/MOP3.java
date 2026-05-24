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
 * MO-P3: Multi-Objective Parameter-less Population Pyramid.
 *
 * Simplified implementation based on the MO-P3 paper (Przewozniczek et al.,
 * Swarm and Evolutionary Computation 60, 2021). Uses random weight vectors to
 * scalarize the multi-objective problem, first-improvement hill climbing
 * (FIHC), and an elitist archive. Complex parts (linkage learning, pyramid
 * structure) are replaced with simpler alternatives.
 */
public class MOP3 {

    private final ObjectiveFunction.LocalObjectiveFunction objectiveFunction;
    private final SearchDirection searchDirection;
    private final Random rng;
    private final ConfigurationType configurationType;
    private final int numberOfObjectives;
    private final int maxEvaluations;

    private int evaluations;
    private List<PopulationEntry> population;
    private List<PopulationEntry> archive;
    private List<Double> idealPoint;
    private List<String> variableKeys;

    /** Tchebycheff scalarization weight per iteration. */
    private double[] weightVector;

    // ── Constructor ─────────────────────────────────────────────────────

    public MOP3(List<Configuration> initialPopulation,
                ObjectiveFunction.LocalObjectiveFunction objectiveFunction,
                SearchDirection searchDirection,
                Random rng,
                int maxEvaluations) {
        if (initialPopulation.isEmpty())
            throw new IllegalArgumentException();

        this.objectiveFunction = objectiveFunction;
        this.searchDirection = searchDirection;
        this.rng = rng;
        this.maxEvaluations = maxEvaluations;
        this.numberOfObjectives = objectiveFunction.numObjectives();
        this.configurationType = initialPopulation.get(0).getConfigurationType();

        this.variableKeys = new ArrayList<>(
                initialPopulation.get(0).getControlledMetrics().keySet());
        Collections.sort(this.variableKeys);

        this.population = new ArrayList<>();
        this.archive = new ArrayList<>();
        this.idealPoint = new ArrayList<>(Collections.nCopies(numberOfObjectives, Double.POSITIVE_INFINITY));

        for (Configuration config : initialPopulation) {
            PopulationEntry entry = evaluate(config);
            this.population.add(entry);
            updateIdealPoint(entry);
            updateArchive(entry);
        }
    }

    // ── Static factory ──────────────────────────────────────────────────

    public static OptimisationIslandResult apply(
            List<Configuration> initialPopulation,
            ObjectiveFunction.LocalObjectiveFunction of,
            SearchDirection dir,
            Random rng,
            int maxEvaluations) {

        MOP3 mop3 = new MOP3(initialPopulation, of, dir, rng, maxEvaluations);
        return mop3.evolve();
    }

    // ── Main evolution loop ─────────────────────────────────────────────

    public OptimisationIslandResult evolve() {
        while (evaluations < maxEvaluations) {
            // 1. Choose a random normalized weight vector
            weightVector = randomWeightVector();

            // 2. Create a new random individual
            Configuration randomConfig = randomConfiguration();
            PopulationEntry offspring = evaluate(randomConfig);

            // 3. Apply FIHC to improve the new individual
            offspring = firstImprovementHillClimber(offspring);

            // 4. Mix with population (OM-like)
            offspring = mixWithPopulation(offspring);

            // 5. Update archive and ideal point
            updateIdealPoint(offspring);
            updateArchive(offspring);

            // 6. Add to population
            population.add(offspring);
        }

        return new OptimisationIslandResult(new ArrayList<>(archive), population, null);
    }

    // ── Weight vector ───────────────────────────────────────────────────

    private double[] randomWeightVector() {
        double[] w = new double[numberOfObjectives];
        double sum = 0;
        for (int i = 0; i < numberOfObjectives; i++) {
            w[i] = rng.nextDouble();
            sum += w[i];
        }
        for (int i = 0; i < numberOfObjectives; i++) {
            w[i] /= sum;
        }
        return w;
    }

    /**
     * Smart-ish weight vector: pick a random archive solution, normalize its
     * objective values into a weight vector. This biases search toward
     * under-represented regions because archive solutions with extreme values
     * produce skewed weights.
     */
    @SuppressWarnings("unused")
    private double[] smartWeightVector() {
        if (archive.isEmpty())
            return randomWeightVector();

        PopulationEntry ref = archive.get(rng.nextInt(archive.size()));
        double[] w = new double[numberOfObjectives];
        double sum = 0;
        for (int i = 0; i < numberOfObjectives; i++) {
            double val = ref.getObjectives().get(i);
            w[i] = val == 0 ? 1e-6 : Math.abs(1.0 / val);
            sum += w[i];
        }
        for (int i = 0; i < numberOfObjectives; i++) {
            w[i] /= sum;
        }
        return w;
    }

    // ── FIHC: First Improvement Hill Climber ────────────────────────────

    private PopulationEntry firstImprovementHillClimber(PopulationEntry start) {
        PopulationEntry current = start;
        boolean improved = true;

        while (improved && evaluations < maxEvaluations) {
            improved = false;
            List<String> shuffledKeys = new ArrayList<>(variableKeys);
            Collections.shuffle(shuffledKeys, rng);

            for (String key : shuffledKeys) {
                ValueTypeVisitor.RandomValueVisitor randomV = new ValueTypeVisitor.RandomValueVisitor(rng);
                Value newVal = randomV.visit(current.getConfiguration()
                        .getControlledMetrics().get(key).getType());
                if (newVal.equals(current.getConfiguration().getControlledMetrics().get(key)))
                    continue;

                Map<String, Value> trialControls = new HashMap<>(
                        current.getConfiguration().getControlledMetrics());
                trialControls.put(key, newVal);

                Configuration trialConfig = new Configuration(configurationType, trialControls,
                        Collections.emptyMap());
                PopulationEntry trial = evaluate(trialConfig);

                if (scalarizedCompare(trial, current) > 0) {
                    current = trial;
                    improved = true;
                    updateIdealPoint(current);
                    updateArchive(current);
                    break; // First improvement: accept and restart
                }
            }
        }
        return current;
    }

    // ── Population mixing (simplified OM) ───────────────────────────────

    private PopulationEntry mixWithPopulation(PopulationEntry offspring) {
        if (population.isEmpty())
            return offspring;

        PopulationEntry current = offspring;
        int trials = Math.min(population.size(), 10);

        for (int t = 0; t < trials && evaluations < maxEvaluations; t++) {
            PopulationEntry donor = population.get(rng.nextInt(population.size()));
            Configuration crossed = onePointCrossover(
                    current.getConfiguration(), donor.getConfiguration());
            Configuration mutated = hyperMutate(crossed);

            PopulationEntry trial = evaluate(mutated);
            updateIdealPoint(trial);

            if (scalarizedCompare(trial, current) > 0) {
                current = trial;
                updateArchive(current);
            }
        }
        return current;
    }

    // ── Scalarized fitness comparison ───────────────────────────────────

    private double scalarizedFitness(PopulationEntry entry) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < numberOfObjectives; i++) {
            double w = Math.max(weightVector[i], 1e-6);
            double diff = Math.abs(entry.getObjectives().get(i) - idealPoint.get(i));
            double val = w * diff;
            if (val > max)
                max = val;
        }
        return max;
    }

    /** Returns positive if a is better than b (MINIMIZING direction). */
    private int scalarizedCompare(PopulationEntry a, PopulationEntry b) {
        double fa = scalarizedFitness(a);
        double fb = scalarizedFitness(b);
        return searchDirection == SearchDirection.MINIMIZING
                ? -Double.compare(fa, fb)
                : Double.compare(fa, fb);
    }

    // ── Genetic operators (simplified) ──────────────────────────────────

    private Configuration onePointCrossover(Configuration c1, Configuration c2) {
        List<Map.Entry<String, Value>> linear1 = new ArrayList<>(
                c1.getControlledMetrics().entrySet());
        List<Map.Entry<String, Value>> linear2 = new ArrayList<>(
                c2.getControlledMetrics().entrySet());

        linear1.sort(Map.Entry.comparingByKey());
        linear2.sort(Map.Entry.comparingByKey());

        int index = rng.nextInt(linear1.size());
        Map<String, Value> crossed = new HashMap<>();
        for (int i = 0; i < index; i++) {
            crossed.put(linear1.get(i).getKey(), linear1.get(i).getValue());
        }
        for (int i = index; i < linear2.size(); i++) {
            crossed.put(linear2.get(i).getKey(), linear2.get(i).getValue());
        }
        return new Configuration(configurationType, crossed, Collections.emptyMap());
    }

    private Configuration hyperMutate(Configuration c) {
        Map<String, Value> controls = new HashMap<>(c.getControlledMetrics());
        List<String> keys = new ArrayList<>(controls.keySet());
        double pm = 1.0 / keys.size();

        for (String key : keys) {
            if (rng.nextDouble() < pm) {
                Value oldVal = controls.get(key);
                Value newVal = new ValueTypeVisitor.RandomValueVisitor(rng).visit(oldVal.getType());
                controls.put(key, newVal);
            }
        }
        return new Configuration(configurationType, controls, Collections.emptyMap());
    }

    private Configuration randomConfiguration() {
        Map<String, Value> controlledMetrics = new HashMap<>();
        for (String key : variableKeys) {
            Configuration first = population.get(0).getConfiguration();
            Value oldVal = first.getControlledMetrics().get(key);
            Value newVal = new ValueTypeVisitor.RandomValueVisitor(rng).visit(oldVal.getType());
            controlledMetrics.put(key, newVal);
        }
        return new Configuration(configurationType, controlledMetrics, Collections.emptyMap());
    }

    // ── Elitist archive ─────────────────────────────────────────────────

    private void updateArchive(PopulationEntry candidate) {
        for (int i = 0; i < archive.size(); i++) {
            PopulationEntry e = archive.get(i);
            if (NSGA2.paretoDominates(candidate.getObjectives(), e.getObjectives(), searchDirection)) {
                archive.remove(i);
                i--;
            } else if (NSGA2.paretoDominates(e.getObjectives(), candidate.getObjectives(), searchDirection)) {
                return;
            }
        }
        // Diversity check: skip if same objectives already present
        for (PopulationEntry e : archive) {
            if (e.getObjectives().equals(candidate.getObjectives()))
                return;
        }
        archive.add(candidate);
    }

    // ── Ideal point ─────────────────────────────────────────────────────

    private void updateIdealPoint(PopulationEntry entry) {
        for (int i = 0; i < numberOfObjectives; i++) {
            double obj = entry.getObjectives().get(i);
            if (obj < idealPoint.get(i)) {
                idealPoint.set(i, obj);
            }
        }
    }

    // ── Evaluation ──────────────────────────────────────────────────────

    private PopulationEntry evaluate(Configuration config) {
        evaluations++;
        ObjectiveFunctionResult result = objectiveFunction.evaluate(
                new ObjectiveFunctionArguments(config, config.getControlledMetrics()));
        return new PopulationEntry(result.getConfiguration(), result.getObjectiveValues());
    }
}
