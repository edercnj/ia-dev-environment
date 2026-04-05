package dev.iadev.domain.model;

/**
 * Summary of traceability coverage statistics for a
 * story or epic.
 *
 * <p>Immutable record providing aggregate counts of
 * stories covered, scenarios covered, and execution
 * status breakdown.</p>
 *
 * @param storiesCovered   stories covered ratio (e.g., "3/4")
 * @param scenariosCovered scenarios covered with percentage
 *                         (e.g., "10/12 (83%)")
 * @param passCount        total tests with PASS status
 * @param skipCount        total tests with SKIP status
 * @param failCount        total tests with FAIL status
 */
public record CoverageSummary(
        String storiesCovered,
        String scenariosCovered,
        int passCount,
        int skipCount,
        int failCount) {

    /**
     * Compact constructor enforcing non-null mandatory
     * fields and non-negative counts.
     */
    public CoverageSummary {
        if (storiesCovered == null
                || storiesCovered.isBlank()) {
            throw new IllegalArgumentException(
                    "storiesCovered must not be null or "
                            + "blank");
        }
        if (scenariosCovered == null
                || scenariosCovered.isBlank()) {
            throw new IllegalArgumentException(
                    "scenariosCovered must not be null or "
                            + "blank");
        }
        if (passCount < 0 || skipCount < 0
                || failCount < 0) {
            throw new IllegalArgumentException(
                    "Counts must not be negative");
        }
    }
}
