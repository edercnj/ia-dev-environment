package dev.iadev.checkpoint;

import java.util.Map;

/**
 * Aggregate metrics for an epic execution.
 *
 * <p>Tracks story completion counts, timing data, and provides ETA
 * estimation based on average story duration.</p>
 *
 * @param storiesCompleted           total stories with SUCCESS status
 * @param storiesTotal               total stories in the epic
 * @param storiesFailed              total with FAILED status
 * @param storiesBlocked             total with BLOCKED status
 * @param estimatedRemainingMinutes  ETA based on average durations
 * @param elapsedMs                  total elapsed time in milliseconds
 * @param averageStoryDurationMs     average duration of completed stories
 * @param storyDurations             individual duration per story ID
 * @param phaseDurations             aggregate duration per phase
 */
public record ExecutionMetrics(
        int storiesCompleted,
        int storiesTotal,
        int storiesFailed,
        int storiesBlocked,
        double estimatedRemainingMinutes,
        long elapsedMs,
        double averageStoryDurationMs,
        Map<String, Long> storyDurations,
        Map<Integer, Long> phaseDurations
) {

    /**
     * Creates initial metrics for a new execution.
     *
     * @param storiesTotal total number of stories in the epic
     * @return metrics with zero progress
     */
    public static ExecutionMetrics initial(int storiesTotal) {
        return new ExecutionMetrics(
                0, storiesTotal, 0, 0, 0.0, 0L, 0.0,
                Map.of(), Map.of()
        );
    }
}
