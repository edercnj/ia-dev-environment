package dev.iadev.domain.traceability;

import java.util.Optional;

/**
 * A single row in the traceability matrix linking a Gherkin
 * requirement to a test method.
 *
 * @param gherkinId        the {@code @GK-N} identifier
 * @param acceptanceTestId the {@code AT-N} identifier (empty if
 *                         unmapped)
 * @param testClassName    the test class (empty if unmapped)
 * @param testMethodName   the test method (empty if unmapped)
 * @param status           the correlation status
 */
public record TraceabilityEntry(
        String gherkinId,
        Optional<String> acceptanceTestId,
        Optional<String> testClassName,
        Optional<String> testMethodName,
        TraceabilityStatus status) {

    static final String UNLINKED_ID = "UNLINKED";

    /**
     * Compact constructor enforcing non-null invariants.
     */
    public TraceabilityEntry {
        if (gherkinId == null || gherkinId.isBlank()) {
            throw new IllegalArgumentException(
                    "gherkinId must not be null or blank");
        }
        if (status == null) {
            throw new IllegalArgumentException(
                    "status must not be null");
        }
        if (acceptanceTestId == null) {
            acceptanceTestId = Optional.empty();
        }
        if (testClassName == null) {
            testClassName = Optional.empty();
        }
        if (testMethodName == null) {
            testMethodName = Optional.empty();
        }
    }

    /**
     * Factory for a fully mapped entry.
     */
    public static TraceabilityEntry mapped(
            String gherkinId,
            String acceptanceTestId,
            String testClassName,
            String testMethodName) {
        return new TraceabilityEntry(
                gherkinId,
                Optional.of(acceptanceTestId),
                Optional.of(testClassName),
                Optional.of(testMethodName),
                TraceabilityStatus.MAPPED);
    }

    /**
     * Factory for a requirement with no test found.
     */
    public static TraceabilityEntry unmappedRequirement(
            String gherkinId,
            String acceptanceTestId) {
        return new TraceabilityEntry(
                gherkinId,
                Optional.of(acceptanceTestId),
                Optional.empty(),
                Optional.empty(),
                TraceabilityStatus.UNMAPPED_REQUIREMENT);
    }

    /**
     * Factory for a test method with no requirement linkage.
     */
    public static TraceabilityEntry unmappedTest(
            String testClassName,
            String testMethodName) {
        return new TraceabilityEntry(
                UNLINKED_ID,
                Optional.empty(),
                Optional.of(testClassName),
                Optional.of(testMethodName),
                TraceabilityStatus.UNMAPPED_TEST);
    }
}
