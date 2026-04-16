package dev.iadev.release.handoff;

/**
 * Port for the {@code gh pr view} read-only operation used in
 * the handoff loop. Abstracted so that tests can inject
 * deterministic stubs without invoking the real CLI.
 *
 * <p>Adapter implementations MUST:
 * <ul>
 *   <li>invoke {@code gh} via {@link ProcessBuilder} with
 *       separate argv entries (never shell concatenation —
 *       CWE-78)</li>
 *   <li>apply a configurable timeout (default 30s) to prevent
 *       resource exhaustion</li>
 *   <li>sanitize stderr before logging (no token leak)</li>
 *   <li>translate an HTTP 404 from the GitHub API into a
 *       {@link PrNotFoundException}</li>
 * </ul>
 *
 * <p>Introduced by story-0039-0011.
 */
@FunctionalInterface
public interface GhCliPort {

    /**
     * Fetches {@code state}, {@code mergedAt} and
     * {@code reviewDecision} for the given PR.
     *
     * @param prNumber PR number (positive integer)
     * @return fresh PR state snapshot
     * @throws PrNotFoundException when the PR cannot be found
     */
    PrState viewPr(int prNumber);
}
