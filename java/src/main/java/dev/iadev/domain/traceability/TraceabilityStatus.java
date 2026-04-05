package dev.iadev.domain.traceability;

/**
 * Classification of a traceability correlation between a
 * Gherkin requirement and a test method.
 */
public enum TraceabilityStatus {

    /** Requirement is linked to a test method via AT-N. */
    MAPPED,

    /** Requirement has no corresponding test method. */
    UNMAPPED_REQUIREMENT,

    /** Test method has no corresponding requirement. */
    UNMAPPED_TEST
}
