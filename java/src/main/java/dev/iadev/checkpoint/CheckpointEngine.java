package dev.iadev.checkpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.iadev.exception.CheckpointIOException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRUD operations for execution state persistence.
 *
 * <p>Serializes {@link ExecutionState} to JSON via Jackson and writes
 * atomically using a temp file + rename strategy. Validates state
 * on load using {@link CheckpointValidation}.</p>
 */
public final class CheckpointEngine {

    private static final String STATE_FILE = "execution-state.json";
    private static final ObjectMapper MAPPER = createMapper();

    private CheckpointEngine() {
        // utility class
    }

    private static ObjectMapper createMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );
        return mapper;
    }

    /**
     * Serializes execution state to JSON and writes to the given path.
     *
     * <p>Uses atomic write: writes to a temp file first, then renames.</p>
     *
     * @param state the execution state to persist
     * @param path  the file path for the JSON file
     * @throws CheckpointIOException if writing fails
     */
    public static void save(ExecutionState state, Path path) {
        try {
            var json = MAPPER.writeValueAsString(state);
            var tmpFile = path.resolveSibling(
                    "." + path.getFileName() + ".tmp"
            );
            Files.writeString(tmpFile, json, StandardCharsets.UTF_8);
            Files.move(
                    tmpFile, path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException e) {
            throw new CheckpointIOException(
                    "Failed to save checkpoint", path.toString(), e
            );
        }
    }

    /**
     * Reads and deserializes execution state from JSON.
     *
     * <p>Validates the loaded state via {@link CheckpointValidation}.</p>
     *
     * @param path the file path to read from
     * @return the deserialized and validated execution state
     * @throws CheckpointIOException if reading or parsing fails
     * @throws dev.iadev.exception.CheckpointValidationException
     *         if state validation fails
     */
    public static ExecutionState load(Path path) {
        try {
            var json = Files.readString(path, StandardCharsets.UTF_8);
            var state = MAPPER.readValue(
                    json, ExecutionState.class
            );
            var errors = CheckpointValidation.validate(state);
            if (!errors.isEmpty()) {
                throw new dev.iadev.exception
                        .CheckpointValidationException(
                        "Invalid checkpoint: " + String.join("; ", errors),
                        "ExecutionState"
                );
            }
            return state;
        } catch (IOException e) {
            throw new CheckpointIOException(
                    "Failed to load checkpoint", path.toString(), e
            );
        }
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
     * <p>Computes completed, failed, blocked counts, average duration,
     * ETA, per-story durations, and per-phase durations.</p>
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
        var storyDurations = new LinkedHashMap<String, Long>();
        var phaseDurations = new LinkedHashMap<Integer, Long>();

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
                storyDurations.put(e.getKey(), entry.duration());
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

    /**
     * Returns the ObjectMapper used for serialization.
     *
     * <p>Exposed for testing purposes only.</p>
     *
     * @return the configured ObjectMapper
     */
    static ObjectMapper mapper() {
        return MAPPER;
    }
}
