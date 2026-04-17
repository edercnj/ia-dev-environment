package dev.iadev.telemetry.trend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Top-level aggregate produced by {@link TelemetryTrendAnalyzer} and consumed
 * by the Markdown / JSON renderers.
 *
 * <p>All collections are defensively copied; the record is otherwise
 * immutable.</p>
 *
 * @param generatedAt     when the report was produced
 * @param epicsAnalyzed   the epic IDs contributing to the window, oldest first
 * @param thresholdPct    the regression threshold (percentage)
 * @param baseline        the baseline strategy label (MEAN / MEDIAN)
 * @param regressions     the detected regressions, delta-desc sorted
 * @param slowest         the top-N slowest skills, avgP95-desc sorted
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "generatedAt",
        "epicsAnalyzed",
        "thresholdPct",
        "baseline",
        "regressions",
        "slowest"
})
public record TrendReport(
        Instant generatedAt,
        List<String> epicsAnalyzed,
        double thresholdPct,
        String baseline,
        List<Regression> regressions,
        List<SlowSkill> slowest) {

    /**
     * Canonical constructor applying defensive copying and fail-fast
     * validation.
     *
     * @throws IllegalArgumentException when {@code baseline} is blank or
     *                                  {@code thresholdPct} is negative
     */
    public TrendReport {
        Objects.requireNonNull(generatedAt, "generatedAt is required");
        Objects.requireNonNull(baseline, "baseline is required");
        if (baseline.isBlank()) {
            throw new IllegalArgumentException(
                    "baseline must not be blank");
        }
        if (thresholdPct < 0) {
            throw new IllegalArgumentException(
                    "thresholdPct must be >= 0");
        }
        epicsAnalyzed = epicsAnalyzed == null
                ? List.of()
                : List.copyOf(epicsAnalyzed);
        regressions = regressions == null
                ? List.of()
                : List.copyOf(regressions);
        slowest = slowest == null
                ? List.of()
                : List.copyOf(slowest);
    }
}
