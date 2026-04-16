package dev.iadev.release.dryrun;

import java.util.List;
import java.util.Objects;

/**
 * Aggregate result of a full interactive dry-run.
 *
 * @param version            release version simulated
 * @param totalPhases        total catalog phase count
 * @param simulatedCount     phases with outcome SIMULATED
 * @param skippedCount       phases with outcome SKIPPED
 * @param abortedCount       phases with outcome ABORTED (0 or 1)
 * @param notReachedCount    phases with outcome NOT_REACHED
 * @param predictedCommands  total commands previewed
 * @param phaseResults       per-phase records (ordered)
 */
public record DryRunSummary(
        String version,
        int totalPhases,
        int simulatedCount,
        int skippedCount,
        int abortedCount,
        int notReachedCount,
        int predictedCommands,
        List<DryRunPhaseResult> phaseResults) {

    public DryRunSummary {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(phaseResults, "phaseResults");
        phaseResults = List.copyOf(phaseResults);
    }

    /**
     * @return true when the simulation was aborted mid-flight
     */
    public boolean aborted() {
        return abortedCount > 0;
    }
}
