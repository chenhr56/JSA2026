# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build System

This is an **Eclipse Java project** (Java 11, no Maven/Gradle). Dependencies are JARs in `lib/`. Source in `src/`, compiled output in `bin/`.

**Compile everything:**
```bash
javac -cp "lib/*" -d bin $(find src -name "*.java")
```

**Compile and run a single class:**
```bash
javac -cp "lib/*" -d bin src/<path-to-package>/ClassName.java
java -cp "bin;lib/*" <fully.qualified.ClassName>
```

Windows: use `;` as classpath separator. On Unix: use `:`.

## Project Architecture

This is a **multi-objective evolutionary optimization** research project evaluating factory scheduling algorithms. Three factory models (ONA wire-cutting, OAS silo/mixer, Electrolux cooking appliances) are optimized by NSGA-II, MOEA/D, and MOGOMEA.

### Core packages

- **`metrics/`** — Core domain model: `Configuration` (immutable genome), `Value`/`ValueType` (typed gene values with visitor pattern), `OptimisationArguments`, `OptimisationResult`, search directions.
- **`optimisation/`** — Engine interface: `OptimisationEngine3` → `AuraLocalOptimisationEngine3` dispatches to NSGA-II, MOEA/D, or single-objective EA based on objective count. `ObjectiveFunction` is the evaluate-and-predict abstraction.
- **`aura/`** — Algorithm implementations: `NSGA2` (non-dominated sorting, crowding distance, tournament selection), `MOEADIslandMulti` (decomposition with weight vectors + neighborhoods), `EAIsland` (single-objective GA), `Operators` (crossover/mutation).
- **`factoryModel/`** — Three factory domain models (`ONA/`, `OAS/`, `Electrolux/`), each with Device, ProductionProcess, SubProcess, XMLReader/Writer classes. XML input files live in `input/`.
- **`mitm/atb/`** — Business case layer bridging factories to optimizers. `BusinessCase` enum (ONA/OAS/Electrolux), `*ConfigurationType` classes that build search spaces from XML.
- **`restCloud/`** — Island-model distributed optimization: `ManagerPPLocal` runs multiple islands in Java threads exchanging Pareto fronts. `ConnectManager` handles REST cloud communication. `ParetoFrontsKeeper` maintains non-dominated archives. `LinkageFactory` has its own `main()` for linkage learning benchmarks.
- **`allocation/`** — Independent task-allocation subsystem (bin-packing with WorstFit/BestFit/FirstFit/RuleOptimization heuristics). `OneRun` and `StaticSystemSimulation` are entry points.
- **`indicator/`** — Multi-objective quality indicators (GD, DCI, IGD, HyperVolume) for comparing Pareto fronts. `Indicators` orchestrates ranked comparisons.
- **`TC_experiments/`** — Experiment harnesses. `RunAllExperiment` is the master entry point running factory-scale + island-scale sweeps.
- **`util/`** — General utilities (`Utils`).

### Key architectural patterns

- **Engine dispatch**: `AuraLocalOptimisationEngine3` selects the algorithm at runtime — MOEA/D when engine=0, NSGA-II otherwise, single-objective EA for 1-objective problems.
- **XML-driven configuration**: Factory models are fully defined in `input/*.xml` files, parsed by JDOM-based `*XMLReader` classes.
- **Immutable value objects**: `Configuration`, `OptimisationArguments`, etc. use Apache Commons Lang builders for equals/hashCode/toString.
- **Island model**: Multiple populations evolve independently, periodically exchanging best solutions via push/pull/replace. This is not a demo — it's the production experiment runner.

## Testing and Experiments

There are **no unit tests**. Testing is done through `main()` methods and experiment harnesses:

- `TC_experiments.RunAllExperiment` — master orchestrator (factory + island scale experiments)
- `TC_experiments.Test_FactoryScale` — sweeps 40 configuration groups across factory sizes
- `TC_experiments.Test_SameCPUTime` — CPU-time-normalized algorithm comparisons
- Each `factoryModel/*/*XMLReader.java` — smoke-tests XML parsing by printing parsed model to stdout
- `indicator.Indicators` — quick HyperVolume sanity check
- MATLAB scripts in `TC_matlab/` for statistical analysis of output files

Experiment results are written to `result_factory/`, `result_island/`, and similarly named directories.
