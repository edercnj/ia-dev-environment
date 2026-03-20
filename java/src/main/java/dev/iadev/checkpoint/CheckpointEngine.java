package dev.iadev.checkpoint;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRUD operations for execution state persistence.
 *
 * <p>Delegates serialization to a {@link CheckpointPersistence}
 * port injected via constructor, keeping this class free of
 * framework dependencies. Pure business logic methods
 * ({@code updateStory}, {@code updateMetrics}) remain static.</p>
 */
public final class CheckpointEngine {

    /**
     * Conversion factor from milliseconds to minutes.
     */
    private static final double MILLIS_PER_MINUTE = 60_000.0;

    private final CheckpointPersistence persistence;

    /**
     * Creates a checkpoint engine with the given persistence port.
     *
     * @param persistence the serialization/deserialization strategy
     */
    public CheckpointEngine(CheckpointPersistence persistence) {
        this.persistence = persistence;
    }

    /**
     * Serializes execution state and writes to the given path.
     *
     * @param state the execution state to persist
     * @param path  the file path for the JSON file
     * @throws dev.iadev.exception.CheckpointIOException
     *         if writing fails
     */
    public void save(ExecutionState state, Path path) {
        persistence.save(state, path);
    }

    /**
     * Reads and deserializes execution state from JSON.
     *
     * @param path the file path to read from
     * @return the deserialized and validated execution state
     * @throws dev.iadev.exception.CheckpointIOException
     *         if reading or parsing fails
     * @throws dev.iadev.exception.CheckpointValidationException
     *         if state validation fails
     */
    public ExecutionState load(Path path) {
        return persistence.load(path);
    }

    /**
     * Returns a new state with the specified story updated.
     *
     * <p>The original state is not modified (immutability).</p>
     *
     * @param state   the current execution state
     * @param storyId the story ID to update
     * @param entry   the new story entry
     * @return a new ExecutionState with the story updated
     */
    public static ExecutionState updateStory(
            ExecutionState state,
            String storyId,
            StoryEntry entry) {
        return state.withStory(storyId, entry);
    }

    /**
     * Recalculates metrics from the current story states.
     *
     * <p>Computes completed, failed, blocked counts, average
     * duration, ETA, per-story and per-phase durations.</p>
     *
     * @param state the current execution state
     * @return a new ExecutionState with recalculated metrics
     */
    public static ExecutionState updateMetrics(
            ExecutionState state) {
        var stories = state.stories();
        var counts = countByStatus(stories);
        var durations = collectDurations(stories);

        double avgDuration = counts.completed > 0
                ? (double) counts.totalDuration
                / counts.completed
                : 0.0;

        var metrics = buildMetrics(
                stories.size(), counts, durations,
                avgDuration);

        return state.withMetrics(metrics);
    }

    private static StatusCounts countByStatus(
            Map<String, StoryEntry> stories) {
        int completed = 0;
        int failed = 0;
        int blocked = 0;
        long totalDuration = 0;

        for (var entry : stories.values()) {
            switch (entry.status()) {
                case SUCCESS -> {
                    completed++;
                    totalDuration += entry.duration();
                }
                case FAILED -> failed++;
                case BLOCKED -> blocked++;
                default -> { /* PENDING, IN_PROGRESS */ }
            }
        }
        return new StatusCounts(
                completed, failed, blocked, totalDuration);
    }

    private static DurationMaps collectDurations(
            Map<String, StoryEntry> stories) {
        var storyDurations =
                new LinkedHashMap<String, Long>();
        var phaseDurations =
                new LinkedHashMap<Integer, Long>();

        for (var e : stories.entrySet()) {
            var entry = e.getValue();
            if (entry.duration() > 0) {
                storyDurations.put(
                        e.getKey(), entry.duration());
            }
            phaseDurations.merge(
                    entry.phase(), entry.duration(),
                    Long::sum);
        }
        return new DurationMaps(
                storyDurations, phaseDurations);
    }

    private static ExecutionMetrics buildMetrics(
            int totalStories,
            StatusCounts counts,
            DurationMaps durations,
            double avgDuration) {
        int remaining = totalStories - counts.completed;
        double eta = counts.completed > 0
                ? (remaining * avgDuration)
                / MILLIS_PER_MINUTE
                : 0.0;

        long elapsedMs = durations.storyDurations.values()
                .stream().mapToLong(Long::longValue).sum();

        return new ExecutionMetrics(
                counts.completed, totalStories,
                counts.failed, counts.blocked, eta,
                elapsedMs, avgDuration,
                Map.copyOf(durations.storyDurations),
                Map.copyOf(durations.phaseDurations));
    }

    private record StatusCounts(
            int completed, int failed,
            int blocked, long totalDuration) {
    }

    private record DurationMaps(
            LinkedHashMap<String, Long> storyDurations,
            LinkedHashMap<Integer, Long> phaseDurations) {
    }
}
