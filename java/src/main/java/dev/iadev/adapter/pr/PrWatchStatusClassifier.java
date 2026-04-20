package dev.iadev.adapter.pr;

import java.util.List;
import java.util.Set;

/**
 * Pure classification logic for {@code x-pr-watch-ci}.
 *
 * <p>Given a snapshot of PR state (checks, Copilot review, PR state,
 * elapsed times), returns the appropriate {@link PrWatchExitCode}.
 * This class has zero I/O — all inputs come from the caller; all
 * outputs are deterministic. Easy to unit-test with
 * {@code @ParameterizedTest} (RULE-045-05).</p>
 *
 * <p>Classification precedence (evaluated in order):
 * <ol>
 *   <li>PR not found ({@code prState == "NOT_FOUND"}) → {@code PR_NOT_FOUND}</li>
 *   <li>PR closed without merge → {@code PR_CLOSED}</li>
 *   <li>PR already merged → {@code PR_ALREADY_MERGED}</li>
 *   <li>Any failing check → {@code CI_FAILED}</li>
 *   <li>Global timeout elapsed → {@code TIMEOUT}</li>
 *   <li>All checks green + Copilot present → {@code SUCCESS}</li>
 *   <li>All checks green + Copilot timeout elapsed (or not required)
 *       → {@code CI_PENDING_PROCEED}</li>
 * </ol>
 * </p>
 */
public final class PrWatchStatusClassifier {

    /** Conclusions that map to {@link PrWatchExitCode#CI_FAILED}. */
    private static final Set<String> FAILING_CONCLUSIONS =
            Set.of("failure", "timed_out", "cancelled", "action_required");

    /**
     * Classify the current PR watch state.
     *
     * @param input snapshot of all relevant state; never {@code null}
     * @return the appropriate exit code
     */
    public PrWatchExitCode classify(ClassifyInput input) {
        if ("NOT_FOUND".equalsIgnoreCase(input.prState())) {
            return PrWatchExitCode.PR_NOT_FOUND;
        }
        if ("CLOSED".equalsIgnoreCase(input.prState())
                && !input.merged()) {
            return PrWatchExitCode.PR_CLOSED;
        }
        if (input.merged()
                || "MERGED".equalsIgnoreCase(input.prState())) {
            return PrWatchExitCode.PR_ALREADY_MERGED;
        }
        if (hasFailingCheck(input.checks())) {
            return PrWatchExitCode.CI_FAILED;
        }
        if (input.globalTimeoutElapsed()) {
            return PrWatchExitCode.TIMEOUT;
        }
        if (allChecksGreen(input.checks())) {
            if (!input.requireCopilotReview()
                    || input.copilotPresent()) {
                return PrWatchExitCode.SUCCESS;
            }
            if (input.copilotTimeoutElapsed()) {
                return PrWatchExitCode.CI_PENDING_PROCEED;
            }
        }
        return PrWatchExitCode.TIMEOUT;
    }

    private boolean hasFailingCheck(
            List<CheckResult> checks) {
        return checks.stream()
                .anyMatch(c -> FAILING_CONCLUSIONS.contains(
                        c.conclusion()));
    }

    private boolean allChecksGreen(
            List<CheckResult> checks) {
        if (checks.isEmpty()) {
            return false;
        }
        return checks.stream()
                .allMatch(c -> "success".equalsIgnoreCase(
                        c.conclusion())
                        || "neutral".equalsIgnoreCase(
                                c.conclusion())
                        || "skipped".equalsIgnoreCase(
                                c.conclusion()));
    }

    // ── Value types ───────────────────────────────────────────────────────

    /**
     * Immutable snapshot of a single CI check status.
     *
     * @param name       check name (e.g., {@code "build"})
     * @param conclusion check conclusion from GitHub API
     */
    public record CheckResult(String name, String conclusion) {
    }

    /**
     * All inputs required by {@link #classify(ClassifyInput)}.
     *
     * @param checks                snapshot of CI checks (may be empty)
     * @param copilotPresent        {@code true} if Copilot review was posted
     * @param copilotTimeoutElapsed {@code true} if Copilot-specific timeout
     *                              has elapsed
     * @param prState               one of {@code OPEN}, {@code CLOSED},
     *                              {@code MERGED}, {@code NOT_FOUND}
     * @param merged                {@code true} when {@code mergedAt != null}
     * @param globalTimeoutElapsed  {@code true} when global timeout exceeded
     * @param requireCopilotReview  mirrors {@code --require-copilot-review}
     *                              argument (default {@code true})
     */
    public record ClassifyInput(
            List<CheckResult> checks,
            boolean copilotPresent,
            boolean copilotTimeoutElapsed,
            String prState,
            boolean merged,
            boolean globalTimeoutElapsed,
            boolean requireCopilotReview) {
    }
}
