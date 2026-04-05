package dev.iadev.domain.model;

/**
 * Represents the execution status of a test associated
 * with a traceability entry.
 *
 * <p>Used in {@link TraceabilityRow} to indicate whether
 * the corresponding test passed, failed, was skipped,
 * or has no associated test.</p>
 */
public enum ExecutionStatus {

    /** Test executed and passed. */
    PASS,

    /** Test executed and failed. */
    FAIL,

    /** Test exists but was skipped (e.g., @Disabled). */
    SKIP,

    /** No test is associated with this requirement. */
    UNMAPPED
}
