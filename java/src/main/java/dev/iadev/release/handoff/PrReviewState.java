package dev.iadev.release.handoff;

/**
 * Lifecycle state of a pull request as reported by
 * {@code gh pr view} in the {@code state} field.
 *
 * <p>Introduced by story-0039-0011.
 */
public enum PrReviewState {

    /** PR is open, not yet merged or closed. */
    OPEN,

    /** PR was closed without being merged. */
    CLOSED,

    /** PR was merged. */
    MERGED
}
