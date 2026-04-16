package dev.iadev.smoke;

import dev.iadev.domain.model.PhaseMetric;
import dev.iadev.domain.model.PhaseOutcome;
import dev.iadev.domain.model.ReleaseType;
import dev.iadev.domain.telemetry.BenchmarkAnalyzer;
import dev.iadev.domain.telemetry.BenchmarkResult;
import dev.iadev.domain.telemetry.PhaseBenchmark;
import dev.iadev.infrastructure.adapter.output.telemetry.FileTelemetryWriter;
import dev.iadev.infrastructure.adapter.output.telemetry.TelemetryJsonlReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test per TASK-0039-0012-004 §7: seeds 5
 * historical releases, appends 1 "current" release's
 * worth of phases, and verifies the full
 * writer → reader → analyzer loop emits a Top-3 with
 * delta percentages.
 */
class TelemetryBenchmarkSmokeTest {

    private static final Instant T0 = Instant.parse(
            "2026-04-13T08:00:00Z");
    private static final List<String> PHASES = List.of(
            "INITIALIZED",
            "DETERMINED",
            "VALIDATED",
            "PR_OPENED",
            "CHANGELOG");

    @Test
    @DisplayName("fiveReleases_plusCurrent_benchmarkDeltas"
            + "Computed")
    void fiveReleasesBenchmark(
            @TempDir Path dir) throws Exception {
        Path jsonl = dir.resolve("release-metrics.jsonl");
        FileTelemetryWriter writer =
                new FileTelemetryWriter(jsonl);

        // Seed 5 historical releases with deterministic
        // durations per phase.
        for (int i = 0; i < 5; i++) {
            String v = "3.1." + i;
            for (int p = 0; p < PHASES.size(); p++) {
                writer.emit(metric(
                        v, PHASES.get(p),
                        100L + p * 10L,
                        PhaseOutcome.SUCCESS));
            }
        }
        // Current release — VALIDATED is 30% slower.
        String current = "3.2.0";
        writer.emit(metric(
                current, "INITIALIZED", 100L,
                PhaseOutcome.SUCCESS));
        writer.emit(metric(
                current, "DETERMINED", 110L,
                PhaseOutcome.SUCCESS));
        writer.emit(metric(
                current, "VALIDATED", 156L,
                PhaseOutcome.SUCCESS));
        writer.emit(metric(
                current, "PR_OPENED", 130L,
                PhaseOutcome.SUCCESS));
        writer.emit(metric(
                current, "CHANGELOG", 140L,
                PhaseOutcome.SUCCESS));

        // Line count equals (5 releases + 1 current) * 5
        // phases = 30.
        assertThat(Files.readAllLines(jsonl))
                .hasSize(30);

        // Feed the full file through the adapter stream
        // into the pure domain analyzer.
        BenchmarkResult result = new BenchmarkAnalyzer()
                .analyze(
                        new TelemetryJsonlReader()
                                .read(jsonl),
                        current);

        assertThat(result).isInstanceOf(
                BenchmarkResult.TopPhases.class);
        List<PhaseBenchmark> top =
                ((BenchmarkResult.TopPhases) result)
                        .entries();
        assertThat(top).hasSize(3);
        // Top entry is VALIDATED with +30% delta (156 vs 120 mean)
        PhaseBenchmark validated = top.stream()
                .filter(e -> e.phase().equals("VALIDATED"))
                .findFirst()
                .orElseThrow();
        assertThat(validated.durationSec()).isEqualTo(156L);
        assertThat(validated.meanSec()).isEqualTo(120L);
        assertThat(validated.deltaPercent()).isEqualTo(30);
    }

    private static PhaseMetric metric(
            String version,
            String phase,
            long duration,
            PhaseOutcome outcome) {
        return new PhaseMetric(
                version,
                ReleaseType.RELEASE,
                phase,
                T0,
                T0.plusSeconds(duration),
                duration,
                outcome);
    }
}
