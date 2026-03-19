package dev.iadev.domain.implementationmap;

/**
 * Validation warning emitted during DAG construction.
 *
 * <p>Warnings are non-blocking issues (e.g., asymmetric
 * dependencies that were auto-corrected).</p>
 *
 * @param type    warning category
 * @param message human-readable description
 */
public record DagWarning(Type type, String message) {

    /** Categories of DAG validation warnings. */
    public enum Type {
        /** A blocks B, but B does not list A in blockedBy. */
        ASYMMETRIC_DEPENDENCY,
        /** A references a story ID that does not exist. */
        MISSING_STORY_REFERENCE
    }
}
