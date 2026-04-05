package dev.iadev.domain.model;

import java.util.Optional;

/**
 * A single row in the traceability matrix mapping a
 * requirement to its corresponding test and execution
 * result.
 *
 * <p>Immutable record following domain model conventions.
 * Nullable fields use {@link Optional} accessors.</p>
 *
 * @param reqId           the requirement identifier (e.g., AT-1)
 * @param scenarioTitle   the Gherkin scenario title
 * @param testClassName   the test class name, empty if unmapped
 * @param testMethodName  the test method name, empty if unmapped
 * @param executionStatus the execution result
 * @param lineCoverage    line coverage percentage, empty
 *                        if unmapped or skipped
 */
public record TraceabilityRow(
        String reqId,
        String scenarioTitle,
        String testClassName,
        String testMethodName,
        ExecutionStatus executionStatus,
        Integer lineCoverage) {

    /**
     * Compact constructor enforcing non-null mandatory
     * fields.
     */
    public TraceabilityRow {
        if (reqId == null || reqId.isBlank()) {
            throw new IllegalArgumentException(
                    "reqId must not be null or blank");
        }
        if (scenarioTitle == null
                || scenarioTitle.isBlank()) {
            throw new IllegalArgumentException(
                    "scenarioTitle must not be null or "
                            + "blank");
        }
        if (executionStatus == null) {
            throw new IllegalArgumentException(
                    "executionStatus must not be null");
        }
    }

    /**
     * Returns the test class name if present.
     *
     * @return optional test class name
     */
    public Optional<String> optionalTestClassName() {
        return Optional.ofNullable(testClassName);
    }

    /**
     * Returns the test method name if present.
     *
     * @return optional test method name
     */
    public Optional<String> optionalTestMethodName() {
        return Optional.ofNullable(testMethodName);
    }

    /**
     * Returns the line coverage if present.
     *
     * @return optional line coverage percentage
     */
    public Optional<Integer> optionalLineCoverage() {
        return Optional.ofNullable(lineCoverage);
    }
}
