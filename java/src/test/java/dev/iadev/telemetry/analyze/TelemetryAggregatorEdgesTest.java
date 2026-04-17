package dev.iadev.telemetry.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.telemetry.EventStatus;
import dev.iadev.telemetry.EventType;
import dev.iadev.telemetry.TelemetryEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class TelemetryAggregatorEdgesTest {

    private final TelemetryAggregator aggregator =
            new TelemetryAggregator();

    @Test
    void aggregate_nullStream_throws() {
        assertThatThrownBy(() -> aggregator.aggregate(
                null, List.of("EPIC-0040")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aggregate_nullEpics_throws() {
        assertThatThrownBy(() -> aggregator.aggregate(
                Stream.empty(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aggregate_toolResultWithoutTool_skipped() {
        TelemetryEvent event = new TelemetryEvent(
                "1.0.0", UUID.randomUUID(),
                Instant.parse("2026-04-16T12:00:00Z"),
                "session-x", "EPIC-0040", null, null,
                EventType.TOOL_RESULT, null, null, null,
                100L, EventStatus.OK, null, Map.of());

        AnalysisReport report = aggregator.aggregate(
                Stream.of(event), List.of("EPIC-0040"));

        assertThat(report.totalEvents()).isEqualTo(1L);
        assertThat(report.tools()).isEmpty();
    }

    @Test
    void aggregate_phaseStartWithoutEnd_appearsWithZeroDuration() {
        TelemetryEvent start = new TelemetryEvent(
                "1.0.0", UUID.randomUUID(),
                Instant.parse("2026-04-16T12:00:00Z"),
                "session-x", "EPIC-0040", null, null,
                EventType.PHASE_START, "x-skill", "Phase-1",
                null, null, null, null, Map.of());

        AnalysisReport report = aggregator.aggregate(
                Stream.of(start), List.of("EPIC-0040"));

        assertThat(report.timeline()).hasSize(1);
        PhaseTimeline row = report.timeline().get(0);
        assertThat(row.durationMs()).isZero();
        assertThat(row.startInstant()).isEqualTo(row.endInstant());
    }

    @Test
    void aggregate_skillEndWithoutSkillName_skipped() {
        TelemetryEvent event = new TelemetryEvent(
                "1.0.0", UUID.randomUUID(),
                Instant.parse("2026-04-16T12:00:00Z"),
                "session-x", "EPIC-0040", null, null,
                EventType.SKILL_END, null, null, null,
                100L, EventStatus.OK, null, Map.of());

        AnalysisReport report = aggregator.aggregate(
                Stream.of(event), List.of("EPIC-0040"));

        assertThat(report.skills()).isEmpty();
        assertThat(report.totalEvents()).isEqualTo(1L);
    }

    @Test
    void aggregate_sessionStart_countedButNotAggregated() {
        TelemetryEvent event = new TelemetryEvent(
                "1.0.0", UUID.randomUUID(),
                Instant.parse("2026-04-16T12:00:00Z"),
                "session-x", "EPIC-0040", null, null,
                EventType.SESSION_START, null, null, null,
                null, null, null, Map.of());

        AnalysisReport report = aggregator.aggregate(
                Stream.of(event), List.of("EPIC-0040"));

        assertThat(report.totalEvents()).isEqualTo(1L);
        assertThat(report.skills()).isEmpty();
    }

    @Test
    void percentile_halfSamples_returnsMiddleElement() {
        List<Long> samples = List.of(10L, 20L, 30L, 40L);

        long p50 = TelemetryAggregator.percentile(samples, 50);
        long p95 = TelemetryAggregator.percentile(samples, 95);

        assertThat(p50).isEqualTo(20L);
        assertThat(p95).isEqualTo(40L);
    }
}
