package dev.iadev.release.handoff;

/**
 * Thrown by a {@link SkillInvokerPort} implementation when the
 * underlying {@code Skill} tool invocation fails.
 *
 * <p>Message MUST NOT expose internal paths or stack traces
 * (Rule 06 — Security Baseline). Callers classify this
 * exception as {@link HandoffError#HANDOFF_SKILL_FAILED}
 * (warn-only).
 */
public class SkillInvocationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Creates an exception with a short, safe message. */
    public SkillInvocationException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a cause preserved for diagnosis.
     *
     * @param message short, safe message
     * @param cause   underlying cause (not exposed to end users)
     */
    public SkillInvocationException(
            String message, Throwable cause) {
        super(message, cause);
    }
}
