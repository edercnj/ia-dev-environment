package dev.iadev.release;

/**
 * Restriction on which Conventional Commit types may justify
 * a release bump under a given {@link ReleaseContext}.
 *
 * <p>{@link #ANY} — standard release flow accepts any commit
 * type ({@code feat}, {@code fix}, {@code perf}, and breaking
 * signals).</p>
 *
 * <p>{@link #PATCH_ONLY} — hotfix flow accepts only
 * {@code fix} and {@code perf} commits. Any {@code feat} or
 * breaking signal triggers
 * {@code HOTFIX_INVALID_COMMITS} (story-0039-0014 §5.4).</p>
 */
public enum BumpRestriction {
    /** Standard release: no restriction on bump type. */
    ANY,
    /** Hotfix: only PATCH bumps (fix/perf) are valid. */
    PATCH_ONLY
}
