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
    public static List<String> validate(ExecutionState state) {
        var errors = new ArrayList<String>();

        if (state == null) {
            errors.add("ExecutionState is null");
            return errors;
        }
        if (state.epicId() == null || state.epicId().isBlank()) {
            errors.add("epicId is required");
        }
        if (state.branch() == null || state.branch().isBlank()) {
            errors.add("branch is required");
        }
        if (state.startedAt() == null) {
            errors.add("startedAt is required");
        }
        if (state.mode() == null) {
            errors.add("mode is required");
        }
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

        return List.copyOf(errors);
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
