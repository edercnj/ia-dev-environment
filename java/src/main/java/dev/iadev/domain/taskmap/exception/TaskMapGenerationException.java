package dev.iadev.domain.taskmap.exception;

/**
 * Base type for all errors detected by the topological sort while building the
 * task-implementation map. Subclasses carry the specific failure mode (cycle, self-loop,
 * missing reference, asymmetric coalescence).
 */
public abstract class TaskMapGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected TaskMapGenerationException(String message) {
        super(message);
    }
}
