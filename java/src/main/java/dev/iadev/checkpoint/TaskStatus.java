package dev.iadev.checkpoint;

import java.util.Map;
import java.util.Set;

/**
 * Represents the lifecycle status of a task during story execution.
 *
 * <p>Valid transitions:
 * <ul>
 *   <li>PENDING -> IN_PROGRESS, BLOCKED, SKIPPED</li>
 *   <li>IN_PROGRESS -> PR_CREATED, FAILED, BLOCKED</li>
 *   <li>PR_CREATED -> PR_APPROVED, FAILED</li>
 *   <li>PR_APPROVED -> PR_MERGED, FAILED</li>
 *   <li>PR_MERGED -> DONE</li>
 *   <li>BLOCKED -> PENDING (dependency resolved)</li>
 *   <li>FAILED -> PENDING (retry)</li>
 *   <li>DONE -> (terminal)</li>
 *   <li>SKIPPED -> (terminal)</li>
 * </ul>
 */
public enum TaskStatus {

    /** Awaiting execution (initial state). */
    PENDING,

    /** Execution in progress. */
    IN_PROGRESS,

    /** PR opened, awaiting review. */
    PR_CREATED,

    /** PR approved by human reviewer. */
    PR_APPROVED,

    /** PR merged successfully. */
    PR_MERGED,

    /** Task completed and verified (terminal). */
    DONE,

    /** Blocked by unresolved dependency. */
    BLOCKED,

    /** Failed during execution or PR rejected. */
    FAILED,

    /** Skipped intentionally (terminal). */
    SKIPPED;

    private static final Map<TaskStatus, Set<TaskStatus>>
            VALID_TRANSITIONS = Map.of(
            PENDING, Set.of(IN_PROGRESS, BLOCKED, SKIPPED),
            IN_PROGRESS, Set.of(PR_CREATED, FAILED, BLOCKED),
            PR_CREATED, Set.of(PR_APPROVED, FAILED),
            PR_APPROVED, Set.of(PR_MERGED, FAILED),
            PR_MERGED, Set.of(DONE),
            BLOCKED, Set.of(PENDING),
            FAILED, Set.of(PENDING),
            DONE, Set.of(),
            SKIPPED, Set.of()
    );

    /**
     * Checks whether transitioning to the target status is valid.
     *
     * @param target the desired target status
     * @return true if the transition is allowed
     */
    public boolean canTransitionTo(TaskStatus target) {
        var allowed = VALID_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    /**
     * Validates the transition and throws if invalid.
     *
     * @param target the desired target status
     * @throws IllegalStateException if the transition is not valid
     */
    public void validateTransition(TaskStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "%s -> %s is not a valid transition"
                            .formatted(this, target)
            );
        }
    }

    /**
     * Returns whether this status is terminal (no outgoing transitions).
     *
     * @return true if DONE or SKIPPED
     */
    public boolean isTerminal() {
        return this == DONE || this == SKIPPED;
    }
}
