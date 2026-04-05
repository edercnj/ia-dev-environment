package dev.iadev.domain.model;

/**
 * Security grade derived from the security score.
 *
 * <p>Grades map to score ranges as follows:
 * A (90-100), B (80-89), C (70-79), D (60-69),
 * F (0-59).</p>
 *
 * @see SecurityScore
 */
public enum SecurityGrade {

    /** Excellent: score 90-100. */
    A(90),

    /** Good: score 80-89. */
    B(80),

    /** Acceptable: score 70-79. */
    C(70),

    /** Below standard: score 60-69. */
    D(60),

    /** Failing: score 0-59. */
    F(0);

    private final int minimumScore;

    SecurityGrade(int minimumScore) {
        this.minimumScore = minimumScore;
    }

    /**
     * Returns the minimum score for this grade.
     *
     * @return the minimum score threshold
     */
    public int minimumScore() {
        return minimumScore;
    }

    /**
     * Determines the grade for a given score.
     *
     * @param score the security score (0-100)
     * @return the corresponding grade
     */
    public static SecurityGrade fromScore(int score) {
        if (score >= A.minimumScore) {
            return A;
        }
        if (score >= B.minimumScore) {
            return B;
        }
        if (score >= C.minimumScore) {
            return C;
        }
        if (score >= D.minimumScore) {
            return D;
        }
        return F;
    }
}
