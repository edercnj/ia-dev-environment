package dev.iadev.domain.taskmap.exception;

import java.util.Objects;

/**
 * Thrown when one task declares {@code COALESCED with TASK-Y} but TASK-Y does not declare
 * the reciprocal coalescence with the originating task. Coalescence is symmetric by
 * construction (both tasks land in one commit, RULE-TF-04).
 */
public final class InvalidCoalescenceException extends TaskMapGenerationException {

    private static final long serialVersionUID = 1L;

    private final String declaringTaskId;
    private final String partnerTaskId;

    public InvalidCoalescenceException(String declaringTaskId, String partnerTaskId) {
        super(declaringTaskId + " declara coalescencia com " + partnerTaskId
                + ", mas " + partnerTaskId + " nao declara reciproca");
        Objects.requireNonNull(declaringTaskId, "declaringTaskId");
        Objects.requireNonNull(partnerTaskId, "partnerTaskId");
        this.declaringTaskId = declaringTaskId;
        this.partnerTaskId = partnerTaskId;
    }

    public String declaringTaskId() {
        return declaringTaskId;
    }

    public String partnerTaskId() {
        return partnerTaskId;
    }
}
