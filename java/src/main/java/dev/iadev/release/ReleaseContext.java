package dev.iadev.release;

import java.util.Objects;

/**
 * Immutable context that parameterises the release flow as
 * either a standard release or a hotfix release (story-0039-0014
 * §3.2 — 95 % of code is shared; only bump restriction and
 * base branch differ).
 *
 * <p>Two canonical factories are provided:</p>
 * <ul>
 *   <li>{@link #release()} —
 *       {@code (BumpRestriction.ANY, "develop", hotfix=false)}</li>
 *   <li>{@link #forHotfix()} —
 *       {@code (BumpRestriction.PATCH_ONLY, "main",
 *       hotfix=true)}</li>
 * </ul>
 *
 * <p>The factory is named {@code forHotfix()} because a record
 * accessor already occupies the {@code hotfix()} symbol.</p>
 *
 * <p>Zero framework imports — this record lives in the domain
 * layer and MUST NOT leak configuration or I/O concerns.</p>
 *
 * @param restrictBumpTo bump restriction for auto-detect and
 *                       override validation; never {@code null}
 * @param baseBranch     branch the flow cuts from
 *                       ({@code develop} or {@code main});
 *                       never {@code null} or blank
 * @param hotfix         {@code true} when the flow is a hotfix;
 *                       kept as a separate flag to preserve the
 *                       existing state-file schema (S02) that
 *                       already carries {@code hotfix: true}
 */
public record ReleaseContext(
        BumpRestriction restrictBumpTo,
        String baseBranch,
        boolean hotfix) {

    private static final String DEVELOP = "develop";
    private static final String MAIN = "main";

    /** Cached instance for the default release flow. */
    private static final ReleaseContext RELEASE =
            new ReleaseContext(
                    BumpRestriction.ANY, DEVELOP, false);

    /** Cached instance for the default hotfix flow. */
    private static final ReleaseContext HOTFIX =
            new ReleaseContext(
                    BumpRestriction.PATCH_ONLY, MAIN, true);

    public ReleaseContext {
        Objects.requireNonNull(
                restrictBumpTo, "restrictBumpTo");
        Objects.requireNonNull(baseBranch, "baseBranch");
        if (baseBranch.isBlank()) {
            throw new IllegalArgumentException(
                    "baseBranch must not be blank");
        }
    }

    /**
     * Canonical standard-release context.
     *
     * @return {@code (ANY, "develop", false)}
     */
    public static ReleaseContext release() {
        return RELEASE;
    }

    /**
     * Canonical hotfix-release context.
     *
     * @return {@code (PATCH_ONLY, "main", true)}
     */
    public static ReleaseContext forHotfix() {
        return HOTFIX;
    }
}
