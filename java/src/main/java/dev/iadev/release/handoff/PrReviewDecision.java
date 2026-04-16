package dev.iadev.release.handoff;

/**
 * Review decision of a pull request as reported by
 * {@code gh pr view} in the {@code reviewDecision} field.
 *
 * <p>Introduced by story-0039-0011.
 */
public enum PrReviewDecision {

    /** Review approved. */
    APPROVED,

    /** Changes requested by reviewer. */
    CHANGES_REQUESTED,

    /** Review still required (pending). */
    REVIEW_REQUIRED
}
