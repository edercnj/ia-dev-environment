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
        int completed = 0;
        int failed = 0;
        int blocked = 0;
        long totalDuration = 0;
        var storyDurations =
                new LinkedHashMap<String, Long>();
        var phaseDurations =
                new LinkedHashMap<Integer, Long>();

        for (var e : stories.entrySet()) {
            var entry = e.getValue();
            switch (entry.status()) {
                case SUCCESS -> {
                    completed++;
                    totalDuration += entry.duration();
                }
                case FAILED -> failed++;
                case BLOCKED -> blocked++;
                default -> { /* PENDING, IN_PROGRESS, PARTIAL */ }
            }

            if (entry.duration() > 0) {
                storyDurations.put(
                        e.getKey(), entry.duration()
                );
            }
            phaseDurations.merge(
                    entry.phase(), entry.duration(), Long::sum
            );
        }

        double avgDuration = completed > 0
                ? (double) totalDuration / completed
                : 0.0;

        int remaining = stories.size() - completed;
        double estimatedRemainingMinutes = completed > 0
                ? (remaining * avgDuration) / 60_000.0
                : 0.0;

        long elapsedMs = storyDurations.values().stream()
                .mapToLong(Long::longValue).sum();

        var metrics = new ExecutionMetrics(
                completed,
                stories.size(),
                failed,
                blocked,
                estimatedRemainingMinutes,
                elapsedMs,
                avgDuration,
                Map.copyOf(storyDurations),
                Map.copyOf(phaseDurations)
        );

        return state.withMetrics(metrics);
    }
}
