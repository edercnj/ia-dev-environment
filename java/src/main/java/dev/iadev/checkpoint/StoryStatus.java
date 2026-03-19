package dev.iadev.checkpoint;

/**
 * Represents the lifecycle status of a story during epic execution.
 *
 * <p>Valid transitions:
 * <ul>
 *   <li>PENDING -> IN_PROGRESS (execution starts)</li>
 *   <li>IN_PROGRESS -> SUCCESS | FAILED | PARTIAL (execution result)</li>
 *   <li>FAILED -> IN_PROGRESS (retry)</li>
 *   <li>PARTIAL -> IN_PROGRESS (retry)</li>
 *   <li>PENDING -> BLOCKED (dependency failed)</li>
 *   <li>BLOCKED -> PENDING (dependency resolved)</li>
 * </ul>
 */
public enum StoryStatus {

    /** Awaiting execution (initial state). */
    PENDING,

    /** Execution in progress. */
    IN_PROGRESS,

    /** Completed successfully (terminal). */
    SUCCESS,

    /** Failed during execution. */
    FAILED,

    /** Blocked by a failed dependency. */
    BLOCKED,

    /** Partially completed (some sub-artifacts generated). */
    PARTIAL
}
