package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.telemetry.EventStatus;
import dev.iadev.telemetry.EventType;
import dev.iadev.telemetry.TelemetryEvent;
import dev.iadev.telemetry.TelemetryWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

/**
 * Performance boundary test: 5 epics × 10 000 events → report in under 10s
 * (story §3.5). Tagged {@code perf} so the default {@code mvn test} run
 * includes it (smoke perf tests are part of DoD) but operators can filter if
 * desired.
 */
@Tag("perf")
class TelemetryTrendPerfIT {

    private static final int EVENTS_PER_EPIC = 10_000;
    private static final int EPIC_COUNT = 5;

    @TempDir
    Path tmp;

    @Test
    void fiveEpicsTenThousandEvents_under10Seconds()
            throws Exception {
        Path base = tmp.resolve("plans");
        for (int i = 1; i <= EPIC_COUNT; i++) {
            String epicId = "EPIC-000" + i;
            prepare(base, epicId, EVENTS_PER_EPIC);
        }

        Path outFile = tmp.resolve("perf.md");
        long start = System.nanoTime();
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--last", "5",
                        "--threshold-pct", "20",
                        "--base-dir", base.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString(),
                        "--out", outFile.toString());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertThat(code).isZero();
        assertThat(Files.exists(outFile)).isTrue();
        assertThat(elapsedMs)
                .as("5x10k events must complete under 10s "
                        + "(actual: " + elapsedMs + "ms)")
                .isLessThan(10_000L);
    }

    private void prepare(
            Path base, String epicId, int count) throws Exception {
        String suffix = epicId.substring("EPIC-".length());
        Path events = base.resolve("epic-" + suffix)
                .resolve("telemetry")
                .resolve("events.ndjson");
        Files.createDirectories(events.getParent());
        try (TelemetryWriter writer = TelemetryWriter.open(events)) {
            Instant base0 = Instant.parse("2026-04-16T12:00:00Z");
            for (int i = 0; i < count; i++) {
                writer.write(skillEnd(
                        "x-story-implement",
                        100L + (i % 100),
                        epicId,
                        base0.plusSeconds(i)));
            }
        }
    }

    private static TelemetryEvent skillEnd(
            String skill, long durationMs, String epicId,
            Instant ts) {
        return new TelemetryEvent(
                "1.0.0",
                UUID.randomUUID(),
                ts,
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
}
