package dev.iadev.release.dryrun;

import java.util.Objects;

/**
 * Renders a human-readable summary of a
 * {@link DryRunSummary} matching story-0039-0013 §5.2.
 */
public final class DryRunSummaryFormatter {

    private DryRunSummaryFormatter() {
        throw new AssertionError("no instances");
    }

    /**
     * Formats a dry-run summary block.
     *
     * @param summary simulation summary
     * @return multi-line formatted output
     */
    public static String format(DryRunSummary summary) {
        Objects.requireNonNull(summary, "summary");
        StringBuilder sb = new StringBuilder();
        sb.append("=== DRY-RUN SUMMARY ===\n");
        sb.append(String.format(
                "Simulated version:  %s%n",
                summary.version()));
        sb.append(String.format(
                "Simulated phases:   %d / %d%n",
                summary.simulatedCount(),
                summary.totalPhases()));
        sb.append(String.format(
                "Skipped phases:     %d%n",
                summary.skippedCount()));
        if (summary.aborted()) {
            sb.append(String.format(
                    "Unreached phases:  %d (aborted)%n",
                    summary.notReachedCount()));
        }
        sb.append(String.format(
                "Predicted commands: %d (none executed)%n",
                summary.predictedCommands()));
        sb.append("\nDRY-RUN MODE — "
                + "no side effects were applied.\n");
        sb.append("Dummy state discarded.\n");
        return sb.toString();
    }
}
