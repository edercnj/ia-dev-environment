package dev.iadev.telemetry.trend;

import java.util.List;
import java.util.Objects;

/**
 * A detected skill-level performance regression: the most recent epic's P95
 * exceeds the historical baseline (mean or median) by at least the configured
 * threshold percentage.
 *
 * <p>Value object produced by {@link RegressionDetector}. {@link #epicsAnalyzed()}
 * is defensively copied and the record is otherwise immutable.</p>
 *
 * @param skill            the skill name (kebab-case, matching the telemetry
 *                         event's {@code skill} field)
 * @param baselineP95Ms    the baseline P95 in milliseconds (mean or median of
 *                         the N-1 oldest epic samples, excluding the current
 *                         epic)
 * @param currentP95Ms     the latest epic's P95 in milliseconds
 * @param deltaPct         the percentage change from baseline to current; a
 *                         regression has {@code deltaPct >= threshold}
 * @param epicsAnalyzed    the list of epic IDs contributing to the series,
 *                         oldest-first
 */
public record Regression(
        String skill,
        long baselineP95Ms,
        long currentP95Ms,
        double deltaPct,
        List<String> epicsAnalyzed) {

    /**
     * Canonical constructor with fail-fast validation and defensive copying
     * of {@code epicsAnalyzed}.
     *
     * @throws IllegalArgumentException when {@code skill} is blank or any
     *                                  P95 value is negative
     */
    public Regression {
        Objects.requireNonNull(skill, "skill is required");
        if (skill.isBlank()) {
            throw new IllegalArgumentException(
                    "skill must not be blank");
        }
        if (baselineP95Ms < 0) {
            throw new IllegalArgumentException(
                    "baselineP95Ms must be >= 0");
        }
        if (currentP95Ms < 0) {
            throw new IllegalArgumentException(
                    "currentP95Ms must be >= 0");
        }
        epicsAnalyzed = epicsAnalyzed == null
                ? List.of()
                : List.copyOf(epicsAnalyzed);
    }
}
