package dev.iadev.domain.taskmap.exception;

import java.util.Objects;

/**
 * Thrown when a task's {@code Depends on} declaration references a TASK-ID that does not
 * appear among the parsed tasks for the story.
 */
public final class MissingTaskReferenceException extends TaskMapGenerationException {

    private static final long serialVersionUID = 1L;

    private final String referencingTaskId;
    private final String missingTaskId;

    public MissingTaskReferenceException(String referencingTaskId, String missingTaskId) {
        super(referencingTaskId + " referencia " + missingTaskId + " inexistente");
        Objects.requireNonNull(referencingTaskId, "referencingTaskId");
        Objects.requireNonNull(missingTaskId, "missingTaskId");
        this.referencingTaskId = referencingTaskId;
        this.missingTaskId = missingTaskId;
    }

    public String referencingTaskId() {
        return referencingTaskId;
    }

    public String missingTaskId() {
        return missingTaskId;
    }
}
