package dev.iadev.release.handoff;

import java.util.List;

/**
 * Outcome of {@link HandoffOrchestrator#handoff}: the option
 * set for the next prompt plus any error classification and
 * exit code.
 *
 * <p>Semantics (per story-0039-0011 §5.3 and §5.4):
 * <ul>
 *   <li>success — {@code error == null}, {@code exitCode == 0},
 *       {@code options} is the decision-table row for the
 *       refreshed PR state</li>
 *   <li>skill failed — {@code error ==
 *       HANDOFF_SKILL_FAILED}, {@code exitCode == 0} (warn
 *       only), {@code options} is the retry/continue/abort
 *       set</li>
 *   <li>PR not found — {@code error ==
 *       HANDOFF_PR_NOT_FOUND}, {@code exitCode == 1},
 *       {@code options} is empty</li>
 * </ul>
 *
 * @param options  option labels for the next AskUserQuestion
 * @param error    non-null when the handoff reported an error
 * @param exitCode process exit code (0 or 1)
 */
public record HandoffResult(
        List<String> options,
        HandoffError error,
        int exitCode) {

    /** Compact constructor — defensively copies options. */
    public HandoffResult {
        options = options == null
                ? List.of()
                : List.copyOf(options);
    }

    /** Factory for a successful handoff with a refreshed prompt. */
    public static HandoffResult success(List<String> options) {
        return new HandoffResult(options, null, 0);
    }

    /** Factory for HANDOFF_SKILL_FAILED (warn-only). */
    public static HandoffResult skillFailed(
            List<String> retryOptions) {
        return new HandoffResult(
                retryOptions,
                HandoffError.HANDOFF_SKILL_FAILED,
                0);
    }

    /** Factory for HANDOFF_PR_NOT_FOUND (exit 1). */
    public static HandoffResult prNotFound() {
        return new HandoffResult(
                List.of(),
                HandoffError.HANDOFF_PR_NOT_FOUND,
                1);
    }
}
