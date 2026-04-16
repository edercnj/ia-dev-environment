package dev.iadev.release.handoff;

/**
 * Error codes emitted by {@link HandoffOrchestrator} per
 * story-0039-0011 §5.4.
 *
 * <p>Error-code semantics:
 * <ul>
 *   <li>{@link #HANDOFF_SKILL_FAILED} — warn-only, exitCode
 *       stays 0; operator is offered "Tentar novamente /
 *       Continuar mesmo assim / Abortar"</li>
 *   <li>{@link #HANDOFF_PR_NOT_FOUND} — exitCode 1; the PR
 *       was deleted during handoff</li>
 * </ul>
 */
public enum HandoffError {

    /** x-pr-fix skill invocation failed (warn-only). */
    HANDOFF_SKILL_FAILED,

    /** gh pr view returned 404 (PR deleted). */
    HANDOFF_PR_NOT_FOUND
}
