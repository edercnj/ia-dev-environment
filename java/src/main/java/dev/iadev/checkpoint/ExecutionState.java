package dev.iadev.checkpoint;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the complete execution state of an epic.
 *
 * <p>This is the top-level record persisted as {@code execution-state.json}.
 * All fields are immutable; use the {@code with*} methods or
 * {@link CheckpointEngine} to create updated copies.</p>
 *
 * <p>The {@code version} field supports schema evolution:
 * <ul>
 *   <li>{@code "1.0"} — original schema (no per-task tracking)</li>
 *   <li>{@code "2.0"} — adds per-task tracking in StoryEntry</li>
 * </ul>
 * Backward compatibility: missing version defaults to "1.0".</p>
 *
 * <p>The {@code telemetryPath} field is an opt-in extension introduced by
 * EPIC-0040 (story-0040-0002). It is a path relative to the epic root that
 * points at the per-epic NDJSON telemetry file
 * ({@code plans/epic-XXXX/telemetry/events.ndjson}). Legacy JSON files
 * without the field deserialize with {@code telemetryPath == null}; the
 * {@link #telemetryPath()} record accessor preserves that raw nullability
 * (null on legacy, set on EPIC-0040+) and {@link #telemetryPathOptional()}
 * offers an {@link Optional} view for callers that prefer non-null
 * semantics. On the wire, the field is elided when null, so existing
 * {@code execution-state.json} files remain byte-identical after a
 * round-trip through the persistence layer (EPIC-0040 RULE-001).</p>
 *
 * @param version        schema version (default "1.0")
 * @param epicId         epic identifier (e.g., "EPIC-0006")
 * @param branch         Git branch for execution
 * @param startedAt      timestamp of execution start
 * @param currentPhase   current phase number (0-based)
 * @param mode           execution mode (FULL, PARTIAL, DRY_RUN)
 * @param stories        per-story state keyed by story ID
 * @param integrityGates gate results keyed by gate name
 * @param metrics        aggregate execution metrics
 * @param telemetryPath  optional path to the per-epic NDJSON telemetry file
 *                       relative to the epic root (EPIC-0040);
 *                       {@code null} when the epic does not emit telemetry
 */
public record ExecutionState(
        String version,
        String epicId,
        String branch,
        Instant startedAt,
        int currentPhase,
        ExecutionMode mode,
        Map<String, StoryEntry> stories,
        Map<String, IntegrityGateEntry> integrityGates,
        ExecutionMetrics metrics,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String telemetryPath
) {

    /** Schema version for original format (no task tracking). */
    public static final String VERSION_1_0 = "1.0";

    /** Schema version with per-task tracking support. */
    public static final String VERSION_2_0 = "2.0";

    /**
     * Compact constructor that defaults version to "1.0" if null.
     *
     * <p>{@code telemetryPath} is intentionally preserved as-is (including
     * null) so serialization round-trips byte-identically for legacy
     * state files that never declared the field.</p>
     */
    public ExecutionState {
        version = version != null ? version : VERSION_1_0;
    }

    /**
     * Full-constructor overload without {@code telemetryPath}, kept for
     * callers that predate EPIC-0040.
     */
    public ExecutionState(
            String version,
            String epicId,
            String branch,
            Instant startedAt,
            int currentPhase,
            ExecutionMode mode,
            Map<String, StoryEntry> stories,
            Map<String, IntegrityGateEntry> integrityGates,
            ExecutionMetrics metrics) {
        this(version, epicId, branch, startedAt,
                currentPhase, mode, stories,
                integrityGates, metrics, null);
    }

    /**
     * Backward-compatible constructor without version parameter.
     *
     * <p>Defaults version to "1.0" for existing code that does not
     * specify a schema version and leaves telemetry absent.</p>
     */
    public ExecutionState(
            String epicId,
            String branch,
            Instant startedAt,
            int currentPhase,
            ExecutionMode mode,
            Map<String, StoryEntry> stories,
            Map<String, IntegrityGateEntry> integrityGates,
            ExecutionMetrics metrics) {
        this(VERSION_1_0, epicId, branch, startedAt,
                currentPhase, mode, stories,
                integrityGates, metrics, null);
    }

    /**
     * @return the telemetry path wrapped in an {@link Optional}
     *         ({@link Optional#empty()} when absent). Added by EPIC-0040.
     */
    public Optional<String> telemetryPathOptional() {
        return Optional.ofNullable(telemetryPath);
    }

    /**
     * Returns a copy with the given story updated.
     *
     * @param storyId the story ID to update
     * @param entry   the new story entry
     * @return a new ExecutionState with the updated story
     */
    public ExecutionState withStory(
            String storyId, StoryEntry entry) {
        var updatedStories =
                new java.util.LinkedHashMap<>(stories);
        updatedStories.put(storyId, entry);
        return new ExecutionState(
                version, epicId, branch, startedAt,
                currentPhase, mode,
                Map.copyOf(updatedStories),
                integrityGates, metrics, telemetryPath
        );
    }

    /**
     * Returns a copy with updated metrics.
     *
     * @param newMetrics the recalculated metrics
     * @return a new ExecutionState with updated metrics
     */
    public ExecutionState withMetrics(
            ExecutionMetrics newMetrics) {
        return new ExecutionState(
                version, epicId, branch, startedAt,
                currentPhase, mode, stories,
                integrityGates, newMetrics, telemetryPath
        );
    }

    /**
     * Returns a copy with updated stories map.
     *
     * @param newStories the new stories map
     * @return a new ExecutionState with updated stories
     */
    public ExecutionState withStories(
            Map<String, StoryEntry> newStories) {
        return new ExecutionState(
                version, epicId, branch, startedAt,
                currentPhase, mode,
                Map.copyOf(newStories),
                integrityGates, metrics, telemetryPath
        );
    }

    /**
     * Returns a copy with an updated integrity gate.
     *
     * @param gateName the gate name
     * @param gate     the gate result
     * @return a new ExecutionState with the updated gate
     */
    public ExecutionState withIntegrityGate(
            String gateName, IntegrityGateEntry gate) {
        var updatedGates =
                new java.util.LinkedHashMap<>(integrityGates);
        updatedGates.put(gateName, gate);
        return new ExecutionState(
                version, epicId, branch, startedAt,
                currentPhase, mode, stories,
                Map.copyOf(updatedGates), metrics,
                telemetryPath
        );
    }

    /**
     * Returns a copy with an updated current phase.
     *
     * @param newPhase the new phase number
     * @return a new ExecutionState with updated currentPhase
     */
    public ExecutionState withCurrentPhase(int newPhase) {
        return new ExecutionState(
                version, epicId, branch, startedAt,
                newPhase, mode, stories,
                integrityGates, metrics, telemetryPath
        );
    }

    /**
     * Returns a copy with an updated version.
     *
     * @param newVersion the new schema version
     * @return a new ExecutionState with updated version
     */
    public ExecutionState withVersion(String newVersion) {
        return new ExecutionState(
                newVersion, epicId, branch, startedAt,
                currentPhase, mode, stories,
                integrityGates, metrics, telemetryPath
        );
    }

    /**
     * Returns a copy with the given telemetry path. Pass {@code null} to
     * clear. Introduced by EPIC-0040.
     *
     * @param path the telemetry path (relative to epic root) or
     *             {@code null} to clear
     * @return a new ExecutionState with the updated telemetry path
     */
    public ExecutionState withTelemetryPath(String path) {
        return new ExecutionState(
                version, epicId, branch, startedAt,
                currentPhase, mode, stories,
                integrityGates, metrics, path
        );
    }
}
