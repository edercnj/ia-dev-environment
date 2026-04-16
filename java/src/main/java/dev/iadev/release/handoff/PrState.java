package dev.iadev.release.handoff;

import java.time.Instant;
import java.util.Optional;

/**
 * Snapshot of a pull request state after the {@code x-pr-fix}
 * handoff, used by {@link HandoffOrchestrator#resolveOptions}
 * to pick the next option set.
 *
 * <p>Wire contract is the subset of {@code gh pr view}
 * documented in story-0039-0011 §5.2:
 * <ul>
 *   <li>{@code state} — OPEN / CLOSED / MERGED</li>
 *   <li>{@code mergedAt} — ISO-8601 timestamp or absent</li>
 *   <li>{@code reviewDecision} — APPROVED / CHANGES_REQUESTED
 *       / REVIEW_REQUIRED</li>
 * </ul>
 *
 * <p>Introduced by story-0039-0011.
 *
 * @param state          PR lifecycle state
 * @param mergedAt       timestamp of merge when merged
 * @param reviewDecision review decision
 */
public record PrState(
        PrReviewState state,
        Optional<Instant> mergedAt,
        PrReviewDecision reviewDecision) {

    /** Compact constructor — normalizes null fields. */
    public PrState {
        if (state == null) {
            throw new IllegalArgumentException(
                    "state must not be null");
        }
        if (mergedAt == null) {
            mergedAt = Optional.empty();
        }
        if (reviewDecision == null) {
            reviewDecision = PrReviewDecision.REVIEW_REQUIRED;
        }
    }
}
