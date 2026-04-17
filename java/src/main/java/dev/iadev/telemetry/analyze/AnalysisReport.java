package dev.iadev.telemetry.analyze;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Consolidated telemetry analysis result for one or more epics.
 *
 * <p>Produced by {@link TelemetryAggregator} and consumed by report
 * renderers ({@code MarkdownReportRenderer}, {@code JsonReportRenderer},
 * {@code CsvReportRenderer}). Immutable value object — all list fields are
 * defensively copied in the canonical constructor.</p>
 */
public record AnalysisReport(
        Instant generatedAt,
        List<String> epics,
        long totalEvents,
        long totalDurationMs,
        List<Stat> skills,
        List<Stat> phases,
        List<Stat> tools,
        List<PhaseTimeline> timeline,
        List<String> observations) {

    /**
     * Canonical constructor applying fail-fast validation and defensive
     * copying of collection fields.
     *
     * @throws IllegalArgumentException when required fields are null or
     *                                  when numeric fields are negative
     */
    public AnalysisReport {
        Objects.requireNonNull(generatedAt, "generatedAt is required");
        if (totalEvents < 0) {
            throw new IllegalArgumentException(
                    "totalEvents must be >= 0");
        }
        if (totalDurationMs < 0) {
            throw new IllegalArgumentException(
                    "totalDurationMs must be >= 0");
        }
        epics = epics == null ? List.of() : List.copyOf(epics);
        skills = skills == null ? List.of() : List.copyOf(skills);
        phases = phases == null ? List.of() : List.copyOf(phases);
        tools = tools == null ? List.of() : List.copyOf(tools);
        timeline = timeline == null
                ? List.of()
                : List.copyOf(timeline);
        observations = observations == null
                ? List.of()
                : List.copyOf(observations);
    }
}
