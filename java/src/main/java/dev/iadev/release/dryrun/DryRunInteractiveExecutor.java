package dev.iadev.release.dryrun;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Application use case that orchestrates an interactive
 * dry-run of the {@code /x-release} flow
 * (story-0039-0013).
 *
 * <p>Flow:
 * <ol>
 *   <li>Write a dummy state file via
 *       {@link DryRunStatePort#create(String)}.</li>
 *   <li>For each phase in {@link PhaseCatalogPort}:
 *     prompt the operator; record outcome; loop.</li>
 *   <li>Delete the dummy state file in a {@code finally}
 *       block (also runs on abort).</li>
 *   <li>Return an aggregate {@link DryRunSummary}.</li>
 * </ol>
 *
 * <p>Zero side effects: by contract this class NEVER
 * invokes real {@code git}, {@code mvn}, or {@code gh}.
 * All external interactions happen through ports.
 */
public final class DryRunInteractiveExecutor {

    private final PhaseCatalogPort catalogPort;
    private final PromptPort promptPort;
    private final DryRunStatePort statePort;

    public DryRunInteractiveExecutor(
            PhaseCatalogPort catalogPort,
            PromptPort promptPort,
            DryRunStatePort statePort) {
        this.catalogPort = Objects.requireNonNull(
                catalogPort, "catalogPort");
        this.promptPort = Objects.requireNonNull(
                promptPort, "promptPort");
        this.statePort = Objects.requireNonNull(
                statePort, "statePort");
    }

    /**
     * Runs the interactive dry-run for the given version.
     *
     * @param version simulated release version
     * @return aggregate summary (never null)
     */
    public DryRunSummary execute(String version) {
        Objects.requireNonNull(version, "version");
        List<PhaseDescriptor> phases = catalogPort.phases();
        Path statePath = null;
        try {
            statePath = statePort.create(version);
            List<DryRunPhaseResult> results =
                    runPhases(phases);
            return buildSummary(version, phases, results);
        } finally {
            statePort.delete(statePath);
        }
    }

    private List<DryRunPhaseResult> runPhases(
            List<PhaseDescriptor> phases) {
        List<DryRunPhaseResult> results = new ArrayList<>();
        boolean aborted = false;
        int total = phases.size();
        for (int i = 0; i < total; i++) {
            PhaseDescriptor phase = phases.get(i);
            if (aborted) {
                results.add(notReached(phase));
                continue;
            }
            DryRunPromptChoice choice = Objects.requireNonNull(
                    promptPort.promptForPhase(phase, i + 1, total),
                    "promptPort.promptForPhase(...) returned null");
            DryRunPhaseOutcome outcome = mapChoice(choice);
            results.add(new DryRunPhaseResult(
                    phase.name(), outcome, phase.commands()));
            if (outcome == DryRunPhaseOutcome.ABORTED) {
                aborted = true;
            }
        }
        return results;
    }

    private static DryRunPhaseResult notReached(
            PhaseDescriptor phase) {
        return new DryRunPhaseResult(phase.name(),
                DryRunPhaseOutcome.NOT_REACHED,
                phase.commands());
    }

    private static DryRunPhaseOutcome mapChoice(
            DryRunPromptChoice choice) {
        return switch (choice) {
            case CONTINUE -> DryRunPhaseOutcome.SIMULATED;
            case SKIP -> DryRunPhaseOutcome.SKIPPED;
            case ABORT -> DryRunPhaseOutcome.ABORTED;
        };
    }

    private static DryRunSummary buildSummary(
            String version,
            List<PhaseDescriptor> phases,
            List<DryRunPhaseResult> results) {
        int simulated = countByOutcome(results,
                DryRunPhaseOutcome.SIMULATED);
        int skipped = countByOutcome(results,
                DryRunPhaseOutcome.SKIPPED);
        int aborted = countByOutcome(results,
                DryRunPhaseOutcome.ABORTED);
        int notReached = countByOutcome(results,
                DryRunPhaseOutcome.NOT_REACHED);
        int predicted = countPredictedCommands(
                phases, results);
        return new DryRunSummary(version, phases.size(),
                simulated, skipped, aborted, notReached,
                predicted, results);
    }

    private static int countByOutcome(
            List<DryRunPhaseResult> results,
            DryRunPhaseOutcome target) {
        int count = 0;
        for (DryRunPhaseResult r : results) {
            if (r.outcome() == target) {
                count++;
            }
        }
        return count;
    }

    private static int countPredictedCommands(
            List<PhaseDescriptor> phases,
            List<DryRunPhaseResult> results) {
        int sum = 0;
        for (int i = 0; i < results.size(); i++) {
            DryRunPhaseResult r = results.get(i);
            if (r.outcome() == DryRunPhaseOutcome.SIMULATED
                    || r.outcome() == DryRunPhaseOutcome.SKIPPED
                    || r.outcome() == DryRunPhaseOutcome.ABORTED) {
                sum += phases.get(i).commands().size();
            }
        }
        return sum;
    }
}
