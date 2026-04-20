package dev.iadev.adapter.pr;

/**
 * Stable exit codes for {@code x-pr-watch-ci} (RULE-045-05).
 *
 * <p>These codes are a public contract — adding a new code is a MINOR
 * change; changing the numeric value or semantics of an existing code
 * is a MAJOR breaking change (Rule 08 — SemVer).</p>
 *
 * <p>Codes: SUCCESS(0), CI_PENDING_PROCEED(10), CI_FAILED(20),
 * TIMEOUT(30), PR_ALREADY_MERGED(40), NO_CI_CONFIGURED(50),
 * PR_CLOSED(60), PR_NOT_FOUND(70).</p>
 */
public enum PrWatchExitCode {

    /** Checks green + Copilot review present
     *  (or {@code --require-copilot-review=false}). */
    SUCCESS(0),

    /** Checks green + Copilot review timeout elapsed without review.
     *  Orchestrator may proceed, but the interactive menu should signal. */
    CI_PENDING_PROCEED(10),

    /** At least one check concluded with a failing outcome
     *  ({@code failure}, {@code timed_out}, {@code cancelled},
     *  {@code action_required}). */
    CI_FAILED(20),

    /** Global timeout elapsed with checks still pending. */
    TIMEOUT(30),

    /** PR was already merged — idempotent exit. */
    PR_ALREADY_MERGED(40),

    /** {@code gh pr checks} returned an empty list — no CI configured. */
    NO_CI_CONFIGURED(50),

    /** PR closed without merge. */
    PR_CLOSED(60),

    /** PR does not exist or caller has no permission. */
    PR_NOT_FOUND(70);

    private final int code;

    PrWatchExitCode(int code) {
        this.code = code;
    }

    /** Returns the numeric exit code to pass to the OS. */
    public int code() {
        return code;
    }
}
