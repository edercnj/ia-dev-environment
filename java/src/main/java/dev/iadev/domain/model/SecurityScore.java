package dev.iadev.domain.model;

import java.util.Map;

/**
 * Immutable security score calculated from finding
 * counts per severity level.
 *
 * <p>Formula: {@code score = max(0, 100 - sum(weight * count))}.
 * The score ranges from 0 to 100 and maps to a letter
 * grade via {@link SecurityGrade}.</p>
 *
 * @param score the calculated score (0-100)
 * @param grade the letter grade (A-F)
 * @param totalFindings total number of findings
 * @param criticalCount count of CRITICAL findings
 * @param highCount count of HIGH findings
 * @param mediumCount count of MEDIUM findings
 * @param lowCount count of LOW findings
 * @param infoCount count of INFO findings
 *
 * @see SecuritySeverity
 * @see SecurityGrade
 */
public record SecurityScore(
        int score,
        SecurityGrade grade,
        int totalFindings,
        int criticalCount,
        int highCount,
        int mediumCount,
        int lowCount,
        int infoCount) {

    private static final int PERFECT_SCORE = 100;
    private static final int MINIMUM_SCORE = 0;

    /**
     * Calculates a security score from severity counts.
     *
     * @param counts a map of severity to finding count
     * @return a new SecurityScore instance
     */
    public static SecurityScore calculate(
            Map<SecuritySeverity, Integer> counts) {
        int critical = counts.getOrDefault(
                SecuritySeverity.CRITICAL, 0);
        int high = counts.getOrDefault(
                SecuritySeverity.HIGH, 0);
        int medium = counts.getOrDefault(
                SecuritySeverity.MEDIUM, 0);
        int low = counts.getOrDefault(
                SecuritySeverity.LOW, 0);
        int info = counts.getOrDefault(
                SecuritySeverity.INFO, 0);

        int penalty = computePenalty(
                critical, high, medium, low, info);
        int computedScore = Math.max(
                MINIMUM_SCORE, PERFECT_SCORE - penalty);
        SecurityGrade computedGrade =
                SecurityGrade.fromScore(computedScore);
        int total =
                critical + high + medium + low + info;

        return new SecurityScore(
                computedScore, computedGrade, total,
                critical, high, medium, low, info);
    }

    private static int computePenalty(
            int critical, int high, int medium,
            int low, int info) {
        return critical * SecuritySeverity.CRITICAL.weight()
                + high * SecuritySeverity.HIGH.weight()
                + medium * SecuritySeverity.MEDIUM.weight()
                + low * SecuritySeverity.LOW.weight()
                + info * SecuritySeverity.INFO.weight();
    }
}
