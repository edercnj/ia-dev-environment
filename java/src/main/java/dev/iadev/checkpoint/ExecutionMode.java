package dev.iadev.checkpoint;

/**
 * Represents the mode of epic execution.
 *
 * <ul>
 *   <li>{@code FULL} - execute all stories in the epic</li>
 *   <li>{@code PARTIAL} - execute a subset of stories or phases</li>
 *   <li>{@code DRY_RUN} - simulate execution without side effects</li>
 * </ul>
 */
public enum ExecutionMode {

    /** Execute all stories in the epic. */
    FULL,

    /** Execute a subset of stories or phases. */
    PARTIAL,

    /** Simulate execution without side effects. */
    DRY_RUN
}
