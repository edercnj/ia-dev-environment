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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

/**
 * Additional edge-path tests for {@link TelemetryTrendCli}:
 * {@code --out} write failure, meanBaseline CLI path, help invocation.
 */
class TelemetryTrendCliEdgesTest {

    @TempDir
    Path tmp;

    @Test
    void call_writeToReadOnlyPath_returnsValidationError()
            throws Exception {
        Path base = tmp.resolve("plans");
        writeFixture(base, "EPIC-0001", "foo", 5, 100L);
        writeFixture(base, "EPIC-0002", "foo", 5, 110L);

        // Point --out at a path whose parent cannot be created:
        // a path containing a null byte is rejected by the filesystem.
        Path badOut = Path.of("/dev/null/cannot-create/sub/file.md");

        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--base-dir", base.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString(),
                        "--out", badOut.toString());
        assertThat(code).isEqualTo(
                TelemetryTrendCli.EXIT_VALIDATION);
    }

    @Test
    void call_meanBaseline_succeeds() throws Exception {
        Path base = tmp.resolve("plans");
        writeFixture(base, "EPIC-0001", "foo", 5, 100L);
        writeFixture(base, "EPIC-0002", "foo", 5, 110L);

        Path outFile = tmp.resolve("mean.md");
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--baseline", "mean",
                        "--base-dir", base.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString(),
                        "--out", outFile.toString());
        assertThat(code).isZero();
        String md = Files.readString(outFile,
                java.nio.charset.StandardCharsets.UTF_8);
        assertThat(md).contains("**Baseline:** mean");
    }

    @Test
    void call_helpOption_exitsZero() {
        int code = new CommandLine(new TelemetryTrendCli())
                .execute("--help");
        assertThat(code).isZero();
    }

    // --- helpers ---

    private Path writeFixture(
            Path base, String epicId, String skill,
            int count, long baseDurationMs) throws Exception {
        String suffix = epicId.substring("EPIC-".length());
        Path events = base.resolve("epic-" + suffix)
                .resolve("telemetry")
                .resolve("events.ndjson");
        Files.createDirectories(events.getParent());
        try (TelemetryWriter writer = TelemetryWriter.open(events)) {
            for (int i = 0; i < count; i++) {
                writer.write(skillEnd(skill,
                        baseDurationMs + i, epicId,
                        Instant.parse("2026-04-16T12:00:00Z")
                                .plusSeconds(i)));
            }
        }
        return events;
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
