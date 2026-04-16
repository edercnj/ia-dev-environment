package dev.iadev.release.summary;

import dev.iadev.release.ReleaseContext;

import java.util.Objects;

/**
 * Pure renderer that produces the final SUMMARY diagram
 * emitted at the end of a release / hotfix flow
 * (story-0039-0014 TASK-009).
 *
 * <p>Two variants are produced from a single shared
 * template, parameterised by {@link ReleaseContext}:</p>
 *
 * <ul>
 *   <li>Standard release: linear progression of
 *       {@code main ─ develop ─ release/X.Y.Z ─ main}.</li>
 *   <li>Hotfix: diagram shows {@code hotfix/X.Y.Z}
 *       branching from {@code main}, PR back to
 *       {@code main}, plus back-merge arrow to
 *       {@code develop} (story-0039-0014 §5.3).</li>
 * </ul>
 *
 * <p>No I/O — the caller prints. All inputs are validated
 * upfront and {@link #sanitize(String)} strips terminal
 * control characters (OWASP A03).</p>
 */
public final class SummaryRenderer {

    private SummaryRenderer() {
        throw new AssertionError("no instances");
    }

    /**
     * Renders the SUMMARY block for a completed release.
     *
     * @param previousVersion last released version (without
     *                        the {@code v} prefix); never null
     * @param targetVersion   version just released; never null
     * @param prNumber        merged PR number (≥ 0)
     * @param ctx             release context; never null
     * @return rendered summary text
     */
    public static String render(
            String previousVersion,
            String targetVersion,
            int prNumber,
            ReleaseContext ctx) {
        Objects.requireNonNull(
                previousVersion, "previousVersion");
        Objects.requireNonNull(
                targetVersion, "targetVersion");
        Objects.requireNonNull(ctx, "ctx");
        if (prNumber < 0) {
            throw new IllegalArgumentException(
                    "prNumber must be non-negative: "
                            + prNumber);
        }
        String prev = sanitize(previousVersion);
        String next = sanitize(targetVersion);
        if (ctx.hotfix()) {
            return renderHotfix(prev, next, prNumber);
        }
        return renderRelease(prev, next, prNumber);
    }

    private static String renderRelease(
            String prev, String next, int prNumber) {
        return String.format(
                "main:     v%s ──── v%s ──%n"
                        + "                          ↑%n"
                        + "                     (PR #%d "
                        + "merged)%n"
                        + "                          │%n"
                        + "release:          release/%s%n"
                        + "                          │   "
                        + "↓ back-merge%n"
                        + "develop:  ──────────●─────●─"
                        + "────%n",
                prev, next, prNumber, next);
    }

    private static String renderHotfix(
            String prev, String next, int prNumber) {
        return String.format(
                "main:     v%s ──── v%s ──%n"
                        + "                          ↑%n"
                        + "                     (PR #%d "
                        + "merged)%n"
                        + "                          │%n"
                        + "hotfix:           hotfix/%s%n"
                        + "                          │   "
                        + "↓ back-merge%n"
                        + "develop:  ──────────●─────●─"
                        + "────%n",
                prev, next, prNumber, next);
    }

    static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb =
                new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\u001b'
                    || (Character.isISOControl(c)
                            && c != '\n' && c != '\r'
                            && c != '\t')) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
