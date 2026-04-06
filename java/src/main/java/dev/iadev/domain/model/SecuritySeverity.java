package dev.iadev.domain.model;

/**
 * Security finding severity levels with associated
 * scoring weights.
 *
 * <p>Used by {@link SecurityScore} to calculate the
 * weighted penalty for each severity level. The weight
 * determines how much each finding of that severity
 * reduces the overall security score.</p>
 *
 * @see SecurityScore
 */
public enum SecuritySeverity {

    /** Critical severity: weight 10, SARIF level error. */
    CRITICAL(10),

    /** High severity: weight 5, SARIF level error. */
    HIGH(5),

    /** Medium severity: weight 2, SARIF level warning. */
    MEDIUM(2),

    /** Low severity: weight 1, SARIF level note. */
    LOW(1),

    /** Informational: weight 0, SARIF level none. */
    INFO(0);

    private final int weight;

    SecuritySeverity(int weight) {
        this.weight = weight;
    }

    /**
     * Returns the scoring weight for this severity.
     *
     * @return the weight value
     */
    public int weight() {
        return weight;
    }
}
