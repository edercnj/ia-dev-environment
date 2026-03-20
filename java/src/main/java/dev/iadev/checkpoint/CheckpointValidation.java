package dev.iadev.checkpoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates checkpoint state integrity.
 *
 * <p>Checks required fields, valid enum values, and structural
 * constraints. Returns a list of human-readable error messages;
 * an empty list means the state is valid.</p>
 */
public final class CheckpointValidation {

    private CheckpointValidation() {
        // utility class
    }

    /**
     * Validates an {@link ExecutionState} and returns errors found.
     *
     * @param state the execution state to validate
     * @return list of error messages; empty if valid
     */
    public static List<String> validate(
            ExecutionState state) {
        if (state == null) {
            return List.of("ExecutionState is null");
        }
        var errors = new ArrayList<String>();
        validateRequiredFields(state, errors);
        validateCollections(state, errors);
        return List.copyOf(errors);
    }

    private static void validateRequiredFields(
            ExecutionState state,
            List<String> errors) {
        if (state.epicId() == null
                || state.epicId().isBlank()) {
            errors.add("epicId is required");
        }
        if (state.branch() == null
                || state.branch().isBlank()) {
            errors.add("branch is required");
        }
        if (state.startedAt() == null) {
            errors.add("startedAt is required");
        }
        if (state.mode() == null) {
            errors.add("mode is required");
        }
    }

    private static void validateCollections(
            ExecutionState state,
            List<String> errors) {
        if (state.stories() == null) {
            errors.add("stories is required");
        } else {
            validateStories(state.stories(), errors);
        }
        if (state.integrityGates() == null) {
            errors.add("integrityGates is required");
        }
        if (state.metrics() == null) {
            errors.add("metrics is required");
        }
    }

    private static void validateStories(
            java.util.Map<String, StoryEntry> stories,
            List<String> errors) {
        for (var entry : stories.entrySet()) {
            var storyId = entry.getKey();
            var storyEntry = entry.getValue();
            if (storyEntry.status() == null) {
                errors.add(
                        "story '%s': status is required"
                                .formatted(storyId)
                );
            }
        }
    }
}
