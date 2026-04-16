package dev.iadev.domain.model;

/**
 * Outcome classification for a single release phase
 * execution (story-0039-0012 §5.1). The wire
 * representation is the enum name (SCREAMING_SNAKE_CASE)
 * as required by the schema.
 */
public enum PhaseOutcome {
    SUCCESS,
    FAILED,
    SKIPPED
}
