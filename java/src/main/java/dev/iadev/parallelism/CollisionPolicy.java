package dev.iadev.parallelism;

/**
 * Policy for whether SOFT (read-only) collisions should be
 * reported alongside HARD and REGEN collisions.
 *
 * <p>Replaces the prior {@code boolean includeSoft} flag
 * used by {@link ParallelismEvaluator} and
 * {@link CollisionDetector} (audit finding M-011,
 * Rule 03: no boolean flag parameters).</p>
 */
public enum CollisionPolicy {

    /** SOFT (read-only) overlaps are filtered out. */
    EXCLUDE_SOFT,

    /** SOFT (read-only) overlaps are reported. */
    INCLUDE_SOFT;

    /**
     * @return {@code true} when this policy reports SOFT
     *         (read-only) overlaps.
     */
    public boolean includesSoft() {
        return this == INCLUDE_SOFT;
    }

    /**
     * Translates a legacy {@code boolean includeSoft} flag
     * into the matching policy constant.
     *
     * @param includeSoft legacy flag value
     * @return {@link #INCLUDE_SOFT} when {@code true},
     *         {@link #EXCLUDE_SOFT} otherwise
     */
    public static CollisionPolicy fromIncludeSoftFlag(
            boolean includeSoft) {
        return includeSoft ? INCLUDE_SOFT : EXCLUDE_SOFT;
    }
}
