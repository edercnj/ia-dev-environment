package dev.iadev.progress;

import dev.iadev.checkpoint.ExecutionMetrics;

/**
 * Formats execution metrics into human-readable text for terminal display.
 *
 * <p>All methods are static and return {@link String}. No state is
 * maintained. The progress bar uses a fixed width of 20 characters
 * with {@code \u2588} (filled) and {@code \u2591} (empty) characters.</p>
 */
public final class ProgressFormatter {

    private static final int BAR_WIDTH = 20;
    private static final char FILLED_CHAR = '\u2588';
    private static final char EMPTY_CHAR = '\u2591';
    private static final double MS_PER_MINUTE = 60_000.0;

    private ProgressFormatter() {
        // static utility class
    }

    /**
     * Formats a progress bar with completion counts.
     *
     * @param completed number of completed stories
     * @param total     total number of stories
     * @return formatted progress bar string
     */
    public static String formatProgressBar(
            int completed, int total) {
        int filledCount = computeFilledCount(completed, total);
        int emptyCount = BAR_WIDTH - filledCount;
        int percent = computePercent(completed, total);

        var bar = new StringBuilder("[");
        bar.append(String.valueOf(FILLED_CHAR).repeat(filledCount));
        bar.append(String.valueOf(EMPTY_CHAR).repeat(emptyCount));
        bar.append("] ");
        bar.append(completed);
        bar.append('/');
        bar.append(total);
        bar.append(" (");
        bar.append(percent);
        bar.append("%)");
        return bar.toString();
    }

    /**
     * Formats a status summary line.
     *
     * @param metrics the execution metrics
     * @return formatted status summary
     */
    public static String formatStatusSummary(
            ExecutionMetrics metrics) {
        int pending = metrics.storiesTotal()
                - metrics.storiesCompleted()
                - metrics.storiesFailed()
                - metrics.storiesBlocked();

        return "SUCCESS: " + metrics.storiesCompleted()
                + " | FAILED: " + metrics.storiesFailed()
                + " | BLOCKED: " + metrics.storiesBlocked()
                + " | PENDING: " + pending;
    }

    /**
     * Formats the estimated remaining time.
     *
     * @param estimatedMinutes minutes remaining, or -1 if unknown
     * @return formatted ETA string
     */
    public static String formatEta(double estimatedMinutes) {
        if (estimatedMinutes < 0) {
            return "Estimated remaining: unknown";
        }
        return "Estimated remaining: "
                + formatOneDecimal(estimatedMinutes) + " min";
    }

    /**
     * Formats the current phase progress.
     *
     * @param currentPhase current phase number
     * @param totalPhases  total number of phases
     * @return formatted phase progress string
     */
    public static String formatPhaseProgress(
            int currentPhase, int totalPhases) {
        return "Phase " + currentPhase + "/"
                + totalPhases + " in progress";
    }

    /**
     * Formats the average throughput.
     *
     * @param avgMs average story duration in milliseconds
     * @return formatted throughput string
     */
    public static String formatThroughput(double avgMs) {
        double minutes = avgMs / MS_PER_MINUTE;
        return "Average: "
                + formatOneDecimal(minutes) + " min/story";
    }

    /**
     * Formats a complete progress report from metrics.
     *
     * @param metrics      the execution metrics
     * @param currentPhase current phase number
     * @param totalPhases  total number of phases
     * @return formatted report string
     */
    public static String format(
            ExecutionMetrics metrics,
            int currentPhase, int totalPhases) {
        return formatProgressBar(
                    metrics.storiesCompleted(),
                    metrics.storiesTotal())
                + "\n" + formatStatusSummary(metrics)
                + "\n" + formatPhaseProgress(
                        currentPhase, totalPhases)
                + "\n" + formatEta(
                        metrics.estimatedRemainingMinutes())
                + "\n" + formatThroughput(
                        metrics.averageStoryDurationMs());
    }

    private static int computeFilledCount(
            int completed, int total) {
        if (total == 0) {
            return 0;
        }
        return (int) ((long) completed * BAR_WIDTH / total);
    }

    private static int computePercent(
            int completed, int total) {
        if (total == 0) {
            return 0;
        }
        return (int) ((long) completed * 100 / total);
    }

    private static String formatOneDecimal(double value) {
        return String.format("%.1f", value);
    }
}
