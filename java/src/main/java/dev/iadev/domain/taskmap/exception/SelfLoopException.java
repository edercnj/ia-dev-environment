package dev.iadev.domain.taskmap.exception;

import java.util.Objects;

/**
 * Thrown when a task declares a dependency on itself. Always invalid (a coalesced
 * declaration with another task is the only mutually-recursive case allowed).
 */
public final class SelfLoopException extends TaskMapGenerationException {

    private static final long serialVersionUID = 1L;

    private final String taskId;

    public SelfLoopException(String taskId) {
        super(taskId + " depende de si mesma — invalido");
        Objects.requireNonNull(taskId, "taskId");
        this.taskId = taskId;
    }

    public String taskId() {
        return taskId;
    }
}
