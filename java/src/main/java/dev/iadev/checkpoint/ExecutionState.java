package dev.iadev.checkpoint;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the complete execution state of an epic.
 *
 * <p>This is the top-level record persisted as {@code execution-state.json}.
 * All fields are immutable; use the {@code with*} methods or
 * {@link CheckpointEngine} to create updated copies.</p>
 *
 * @param epicId         epic identifier (e.g., "EPIC-0006")
 * @param branch         Git branch for execution
 * @param startedAt      timestamp of execution start
 * @param currentPhase   current phase number (0-based)
 * @param mode           execution mode (FULL, PARTIAL, DRY_RUN)
 * @param stories        per-story state keyed by story ID
 * @param integrityGates gate results keyed by gate name
 * @param metrics        aggregate execution metrics
 */
public record ExecutionState(
        String epicId,
        String branch,
        Instant startedAt,
        int currentPhase,
        ExecutionMode mode,
        Map<String, StoryEntry> stories,
        Map<String, IntegrityGateEntry> integrityGates,
        ExecutionMetrics metrics
) {

    /**
     * Returns a copy with the given story updated.
     *
     * @param storyId the story ID to update
     * @param entry   the new story entry
     * @return a new ExecutionState with the updated story
     */
    public ExecutionState withStory(
            String storyId, StoryEntry entry) {
        var updatedStories = new java.util.LinkedHashMap<>(stories);
        updatedStories.put(storyId, entry);
        return new ExecutionState(
                epicId, branch, startedAt, currentPhase, mode,
                Map.copyOf(updatedStories), integrityGates, metrics
        );
    }

    /**
     * Returns a copy with updated metrics.
     *
     * @param newMetrics the recalculated metrics
     * @return a new ExecutionState with updated metrics
     */
    public ExecutionState withMetrics(ExecutionMetrics newMetrics) {
        return new ExecutionState(
                epicId, branch, startedAt, currentPhase, mode,
                stories, integrityGates, newMetrics
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
                epicId, branch, startedAt, currentPhase, mode,
                Map.copyOf(newStories), integrityGates, metrics
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
                epicId, branch, startedAt, currentPhase, mode,
                stories, Map.copyOf(updatedGates), metrics
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
                epicId, branch, startedAt, newPhase, mode,
                stories, integrityGates, metrics
        );
    }
}
