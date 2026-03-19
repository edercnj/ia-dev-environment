package dev.iadev.progress;

import dev.iadev.checkpoint.ExecutionMetrics;
import dev.iadev.checkpoint.ExecutionState;
import dev.iadev.checkpoint.StoryEntry;
import dev.iadev.checkpoint.StoryStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Computes aggregate execution metrics from an {@link ExecutionState}.
 *
 * <p>All calculations are pure (no side effects). The {@link #calculate}
 * method returns a new {@link ExecutionMetrics} instance without
 * modifying the input state.</p>
 */
public final class MetricsCalculator {

    private static final double MS_PER_MINUTE = 60_000.0;
    private static final double INDETERMINATE_ETA = -1.0;

    private MetricsCalculator() {
        // static utility class
    }

    /**
     * Calculates aggregate metrics from the execution state.
     *
     * @param state the current execution state
     * @return computed metrics
     */
    public static ExecutionMetrics calculate(
            ExecutionState state) {
        var stories = state.stories();
        int total = stories.size();
        int completed = countByStatus(stories, StoryStatus.SUCCESS);
        int failed = countByStatus(stories, StoryStatus.FAILED);
        int blocked = countByStatus(stories, StoryStatus.BLOCKED);
        long elapsedMs = computeElapsedMs(state.startedAt());
        double avgMs = computeAverageDuration(stories);
        double eta = computeEta(total, completed, avgMs);
        var storyDurations = buildStoryDurations(stories);
        var phaseDurations = buildPhaseDurations(stories);

        return new ExecutionMetrics(
                completed, total, failed, blocked,
                eta, elapsedMs, avgMs,
                Map.copyOf(storyDurations),
                Map.copyOf(phaseDurations)
        );
    }

    private static int countByStatus(
            Map<String, StoryEntry> stories,
            StoryStatus status) {
        return (int) stories.values().stream()
                .filter(e -> e.status() == status)
                .count();
    }

    private static long computeElapsedMs(Instant startedAt) {
        return Duration.between(startedAt, Instant.now())
                .toMillis();
    }

    private static double computeAverageDuration(
            Map<String, StoryEntry> stories) {
        var successDurations = stories.values().stream()
                .filter(e -> e.status() == StoryStatus.SUCCESS)
                .mapToLong(StoryEntry::duration)
                .toArray();

        if (successDurations.length == 0) {
            return 0.0;
        }

        long sum = 0;
        for (long d : successDurations) {
            sum += d;
        }
        return (double) sum / successDurations.length;
    }

    private static double computeEta(
            int total, int completed, double avgMs) {
        if (completed == 0) {
            return INDETERMINATE_ETA;
        }
        int remaining = total - completed;
        return (remaining * avgMs) / MS_PER_MINUTE;
    }

    private static Map<String, Long> buildStoryDurations(
            Map<String, StoryEntry> stories) {
        var durations = new LinkedHashMap<String, Long>();
        for (var entry : stories.entrySet()) {
            long duration = entry.getValue().duration();
            if (duration > 0) {
                durations.put(entry.getKey(), duration);
            }
        }
        return durations;
    }

    private static Map<Integer, Long> buildPhaseDurations(
            Map<String, StoryEntry> stories) {
        var durations = new LinkedHashMap<Integer, Long>();
        for (var entry : stories.values()) {
            long duration = entry.duration();
            if (duration > 0) {
                durations.merge(
                        entry.phase(), duration, Long::sum);
            }
        }
        return durations;
    }
}
