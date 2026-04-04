package dev.iadev.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the persisted state of an execution checkpoint.
 *
 * <p>A checkpoint captures the progress of a generation execution,
 * enabling resume after interruption. It records which steps have
 * completed and which remain pending.</p>
 *
 * <p>Domain purity: this record contains zero external library
 * imports. Only standard library types are used.</p>
 *
 * @param executionId unique identifier for the execution run
 * @param createdAt   timestamp when the checkpoint was first created
 * @param updatedAt   timestamp of the most recent update
 * @param completedSteps steps that have finished successfully
 * @param metadata    additional execution metadata
 */
public record CheckpointState(
        String executionId,
        Instant createdAt,
        Instant updatedAt,
        Map<String, String> completedSteps,
        Map<String, Object> metadata
) {

    /**
     * Creates a CheckpointState with defensive copies of mutable inputs.
     */
    public CheckpointState {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException(
                    "CheckpointState executionId must not be null or blank");
        }
        completedSteps = completedSteps == null
                ? Map.of()
                : Map.copyOf(completedSteps);
        metadata = metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
