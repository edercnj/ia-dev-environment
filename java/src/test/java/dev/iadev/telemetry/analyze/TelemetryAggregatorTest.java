package dev.iadev.telemetry.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.telemetry.EventStatus;
import dev.iadev.telemetry.EventType;
import dev.iadev.telemetry.TelemetryEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TelemetryAggregatorTest {

    private final TelemetryAggregator aggregator =
            new TelemetryAggregator();

    @Test
    void aggregate_emptyStream_returnsEmptyReport() {
        AnalysisReport report = aggregator.aggregate(
                java.util.stream.Stream.empty(),
                List.of("EPIC-0040"));

        assertThat(report.totalEvents()).isZero();
        assertThat(report.totalDurationMs()).isZero();
        assertThat(report.skills()).isEmpty();
        assertThat(report.phases()).isEmpty();
        assertThat(report.tools()).isEmpty();
        assertThat(report.epics()).containsExactly("EPIC-0040");
    }

    @Test
    void aggregate_singleSkillEnd_producesOneSkillStat() {
        List<TelemetryEvent> events = List.of(
                skillEnd("x-story-implement", 500L, "EPIC-0040"));

        AnalysisReport report = aggregator.aggregate(
                events.stream(), List.of("EPIC-0040"));

        assertThat(report.skills()).hasSize(1);
        Stat stat = report.skills().get(0);
        assertThat(stat.name()).isEqualTo("x-story-implement");
        assertThat(stat.invocations()).isEqualTo(1);
        assertThat(stat.totalMs()).isEqualTo(500L);
        assertThat(stat.avgMs()).isEqualTo(500L);
        assertThat(stat.p50Ms()).isEqualTo(500L);
        assertThat(stat.p95Ms()).isEqualTo(500L);
        assertThat(stat.epicIds()).containsExactly("EPIC-0040");
    }

    @Test
    void aggregate_multipleSkillEnds_computesPercentiles() {
        List<TelemetryEvent> events = new ArrayList<>();
        long[] durations = {100L, 200L, 300L, 400L, 500L,
                600L, 700L, 800L, 900L, 1000L};
        for (long d : durations) {
            events.add(skillEnd("x-task-implement", d, "EPIC-0040"));
        }

        AnalysisReport report = aggregator.aggregate(
                events.stream(), List.of("EPIC-0040"));

        Stat stat = report.skills().get(0);
        assertThat(stat.invocations()).isEqualTo(10);
        assertThat(stat.totalMs()).isEqualTo(5500L);
        assertThat(stat.avgMs()).isEqualTo(550L);
        // Nearest-Rank P50 at n=10 = sample(5) = 500
        assertThat(stat.p50Ms()).isEqualTo(500L);
        // Nearest-Rank P95 at n=10 = ceil(0.95 * 10) = 10 -> sample(10)=1000
        assertThat(stat.p95Ms()).isEqualTo(1000L);
    }

    @Test
    void aggregate_phaseEndWithoutStart_appearsInTimelineWithZeroDuration() {
        Instant ts = Instant.parse("2026-04-16T12:00:00Z");
        List<TelemetryEvent> events = List.of(phaseEnd(
                "x-story-implement",
                "Phase-1", 250L, "EPIC-0040", ts));

        AnalysisReport report = aggregator.aggregate(
                events.stream(), List.of("EPIC-0040"));

        assertThat(report.timeline()).hasSize(1);
        assertThat(report.phases()).hasSize(1);
        Stat phaseStat = report.phases().get(0);
        assertThat(phaseStat.name())
                .isEqualTo("x-story-implement/Phase-1");
        assertThat(phaseStat.totalMs()).isEqualTo(250L);
    }

    @Test
    void aggregate_phaseStartAndEnd_producesTimelineRow() {
        Instant start = Instant.parse("2026-04-16T12:00:00Z");
        Instant end = Instant.parse("2026-04-16T12:00:05Z");
        List<TelemetryEvent> events = List.of(
                phaseStart("x-story-implement", "Phase-1",
                        "EPIC-0040", start),
                phaseEnd("x-story-implement", "Phase-1",
                        5000L, "EPIC-0040", end));

        AnalysisReport report = aggregator.aggregate(
                events.stream(), List.of("EPIC-0040"));

        assertThat(report.timeline()).hasSize(1);
        PhaseTimeline row = report.timeline().get(0);
        assertThat(row.skill()).isEqualTo("x-story-implement");
        assertThat(row.phase()).isEqualTo("Phase-1");
        assertThat(row.startInstant()).isEqualTo(start);
        assertThat(row.endInstant()).isEqualTo(end);
        assertThat(row.durationMs()).isEqualTo(5000L);
    }

    @Test
    void aggregate_toolResultEvents_producesToolStats() {
        List<TelemetryEvent> events = List.of(
                toolResult("Bash", 100L, "EPIC-0040"),
                toolResult("Bash", 300L, "EPIC-0040"),
                toolResult("Write", 50L, "EPIC-0040"));

        AnalysisReport report = aggregator.aggregate(
                events.stream(), List.of("EPIC-0040"));

        assertThat(report.tools())
                .extracting(Stat::name, Stat::invocations,
                        Stat::totalMs)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple
                                .tuple("Bash", 2, 400L),
                        org.assertj.core.groups.Tuple
                                .tuple("Write", 1, 50L));
    }

    @Test
    void aggregate_crossEpicEvents_retainsEpicMembership() {
        List<TelemetryEvent> events = List.of(
                skillEnd("x-story-implement", 100L, "EPIC-0040"),
                skillEnd("x-story-implement", 200L, "EPIC-0041"));

        AnalysisReport report = aggregator.aggregate(
                events.stream(),
                List.of("EPIC-0040", "EPIC-0041"));

        Stat stat = report.skills().get(0);
        assertThat(stat.epicIds())
                .containsExactlyInAnyOrder("EPIC-0040", "EPIC-0041");
    }

    @Test
    void percentile_p95OfSingleSample_returnsSample() {
        long p = TelemetryAggregator.percentile(
                List.of(42L), 95);

        assertThat(p).isEqualTo(42L);
    }

    @Test
    void percentile_emptyList_returnsZero() {
        long p = TelemetryAggregator.percentile(List.of(), 95);

        assertThat(p).isZero();
    }

    // --- helpers ---

    private static TelemetryEvent skillEnd(
            String skill, long durationMs, String epicId) {
        return new TelemetryEvent(
                "1.0.0",
                UUID.randomUUID(),
                Instant.parse("2026-04-16T12:00:00Z"),
                "session-abc",
                epicId,
                null,
                null,
                EventType.SKILL_END,
                skill,
                null,
                null,
                durationMs,
                EventStatus.OK,
                null,
                Map.of());
    }

    private static TelemetryEvent phaseEnd(
            String skill, String phase, long durationMs,
            String epicId, Instant ts) {
        return new TelemetryEvent(
                "1.0.0",
                UUID.randomUUID(),
                ts,
                "session-abc",
                epicId,
                null,
                null,
                EventType.PHASE_END,
                skill,
                phase,
                null,
                durationMs,
                EventStatus.OK,
                null,
                Map.of());
    }

    private static TelemetryEvent phaseStart(
            String skill, String phase, String epicId,
            Instant ts) {
        return new TelemetryEvent(
                "1.0.0",
                UUID.randomUUID(),
                ts,
                "session-abc",
                epicId,
                null,
                null,
                EventType.PHASE_START,
                skill,
                phase,
                null,
                null,
                null,
                null,
                Map.of());
    }

    private static TelemetryEvent toolResult(
            String tool, long durationMs, String epicId) {
        return new TelemetryEvent(
                "1.0.0",
                UUID.randomUUID(),
                Instant.parse("2026-04-16T12:00:00Z"),
                "session-abc",
                epicId,
                null,
                null,
                EventType.TOOL_RESULT,
                null,
                null,
                tool,
                durationMs,
                EventStatus.OK,
                null,
                Map.of());
    }
}
