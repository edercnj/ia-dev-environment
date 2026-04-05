package dev.iadev.domain.model;

/**
 * Computes the review checklist score based on active
 * conditional sections.
 *
 * <p>The base checklist has 45 points (sections A-K).
 * Conditional sections add points when activated:</p>
 * <ul>
 *   <li>Section L (Event-Driven): +8 points</li>
 *   <li>Section M (PCI-DSS): +7 points</li>
 *   <li>Section N (LGPD): +4 points</li>
 * </ul>
 *
 * <p>GO threshold is >= 84% of the maximum possible
 * score, rounded up (ceil).</p>
 *
 * @param maxScore      the maximum possible score
 * @param goThreshold   the minimum score for GO decision
 */
public record ReviewChecklistScore(
        int maxScore, int goThreshold) {

    private static final int BASE_SCORE = 45;
    private static final int EVENT_DRIVEN_SCORE = 8;
    private static final int PCI_DSS_SCORE = 7;
    private static final int LGPD_SCORE = 4;
    private static final double GO_PERCENT = 0.84;

    /**
     * Computes the checklist score from active flags.
     *
     * @param hasEvent  true if event interfaces exist
     * @param hasPciDss true if PCI-DSS compliance active
     * @param hasLgpd   true if LGPD compliance active
     * @return computed score with max and threshold
     */
    public static ReviewChecklistScore compute(
            boolean hasEvent,
            boolean hasPciDss,
            boolean hasLgpd) {
        int max = BASE_SCORE;
        if (hasEvent) {
            max += EVENT_DRIVEN_SCORE;
        }
        if (hasPciDss) {
            max += PCI_DSS_SCORE;
        }
        if (hasLgpd) {
            max += LGPD_SCORE;
        }
        int threshold = (int) Math.ceil(max * GO_PERCENT);
        return new ReviewChecklistScore(max, threshold);
    }
}
