package dev.iadev.release;

/**
 * Immutable counts of Conventional Commits grouped by release impact.
 *
 * <p>Field semantics:</p>
 * <ul>
 *   <li>{@code feat} — non-breaking feature ({@code feat:} or {@code feat(scope):})</li>
 *   <li>{@code fix} — bug fix ({@code fix:} or {@code fix(scope):})</li>
 *   <li>{@code perf} — performance improvement ({@code perf:} or {@code perf(scope):})</li>
 *   <li>{@code breaking} — any commit with {@code !} after type or
 *       {@code BREAKING CHANGE:} in body (regardless of primary type)</li>
 *   <li>{@code ignored} — {@code docs}, {@code chore}, {@code test},
 *       {@code refactor}, {@code style}, {@code build}, {@code ci}, or
 *       any unrecognised line</li>
 * </ul>
 *
 * <p>A commit that is both a feature and breaking contributes to {@code feat}
 * AND {@code breaking} (the two dimensions are orthogonal for banner
 * display — {@code "7 feat, 2 fix, 1 breaking"}).</p>
 */
public record CommitCounts(int feat, int fix, int perf, int breaking, int ignored) {

    /**
     * Canonical zero value used as the starting accumulator before classification.
     */
    public static final CommitCounts ZERO = new CommitCounts(0, 0, 0, 0, 0);

    public CommitCounts {
        if (feat < 0 || fix < 0 || perf < 0 || breaking < 0 || ignored < 0) {
            throw new IllegalArgumentException(
                    "CommitCounts components must be non-negative: "
                            + "feat=" + feat + ", fix=" + fix + ", perf=" + perf
                            + ", breaking=" + breaking + ", ignored=" + ignored);
        }
    }

    /** Total number of commits classified (including ignored). */
    public int total() {
        return feat + fix + perf + ignored;
    }
}
