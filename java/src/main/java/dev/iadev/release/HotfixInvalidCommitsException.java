package dev.iadev.release;

import java.util.Objects;

/**
 * Raised by {@link VersionDetector} when a hotfix flow
 * ({@link BumpRestriction#PATCH_ONLY}) observes one or more
 * commits that classify as {@code feat} or breaking. Hotfix
 * releases may only ship {@code fix} / {@code perf} commits
 * (story-0039-0014 §5.4).
 *
 * <p>Error code: {@code HOTFIX_INVALID_COMMITS} — exit 1.</p>
 */
public final class HotfixInvalidCommitsException
        extends RuntimeException {

    /**
     * Standardised error code surfaced to the operator. Kept as
     * a typed constant so callers can match on identity without
     * parsing the message.
     */
    public static final String CODE = "HOTFIX_INVALID_COMMITS";

    private static final long serialVersionUID = 1L;

    private final int featCount;
    private final int breakingCount;

    public HotfixInvalidCommitsException(
            int featCount, int breakingCount) {
        super(buildMessage(featCount, breakingCount));
        if (featCount < 0 || breakingCount < 0) {
            throw new IllegalArgumentException(
                    "counts must be non-negative");
        }
        this.featCount = featCount;
        this.breakingCount = breakingCount;
    }

    public int featCount() {
        return featCount;
    }

    public int breakingCount() {
        return breakingCount;
    }

    /** Stable error code. */
    public String code() {
        return CODE;
    }

    private static String buildMessage(int feat, int breaking) {
        Objects.checkIndex(0, Integer.MAX_VALUE);
        return String.format(
                "Hotfix flow rejects non-PATCH commits: "
                        + "feat=%d, breaking=%d. "
                        + "Only fix/perf commits are allowed.",
                feat, breaking);
    }
}
