package dev.iadev.telemetry.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.telemetry.EventStatus;
import dev.iadev.telemetry.EventType;
import dev.iadev.telemetry.TelemetryEvent;
import dev.iadev.telemetry.TelemetryWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

class TelemetryAnalyzeCliEdgesIT {

    @TempDir
    Path tmp;

    @Test
    void call_invalidSinceValue_exitsWithValidationError()
            throws Exception {
        prepareFixture(tmp, "EPIC-0040", 1);

        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--base-dir", tmp.toString(),
                        "--since", "not-a-date",
                        "--out", tmp.resolve("r.md").toString());

        assertThat(code).isEqualTo(
                TelemetryAnalyzeCli.EXIT_VALIDATION);
    }

    @Test
    void call_sinceIsoInstant_acceptedAsFilter() throws Exception {
        prepareFixture(tmp, "EPIC-0040", 3);

        Path outFile = tmp.resolve("iso.md");
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--base-dir", tmp.toString(),
                        "--since", "2024-01-01T00:00:00Z",
                        "--out", outFile.toString());

        assertThat(code).isZero();
        assertThat(Files.exists(outFile)).isTrue();
    }

    @Test
    void call_defaultReportPath_writesUnderPlansDir()
            throws Exception {
        prepareFixture(tmp, "EPIC-0040", 1);

        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--base-dir", tmp.toString());

        Path expected = tmp.resolve("epic-0040")
                .resolve("reports")
                .resolve("telemetry-report-EPIC-0040.md");

        assertThat(code).isZero();
        assertThat(Files.exists(expected)).isTrue();
        assertThat(Files.readString(expected,
                StandardCharsets.UTF_8))
                .contains("# Telemetry Report");
    }

    @Test
    void call_byToolFlag_acceptedWithoutSideEffect()
            throws Exception {
        prepareFixture(tmp, "EPIC-0040", 1);

        Path outFile = tmp.resolve("by-tool.md");
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--base-dir", tmp.toString(),
                        "--by-tool",
                        "--out", outFile.toString());

        assertThat(code).isZero();
    }

    @Test
    void call_epicIdWithoutPrefix_extractsSuffixCorrectly()
            throws Exception {
        prepareFixture(tmp, "EPIC-0040", 1);

        Path outFile = tmp.resolve("raw.md");
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--base-dir", tmp.toString(),
                        "--out", outFile.toString());

        assertThat(code).isZero();
    }

    // --- helpers ---

    private Path prepareFixture(
            Path base, String epicId, int count) throws Exception {
        String suffix = epicId.substring("EPIC-".length());
        Path ndjson = base.resolve("epic-" + suffix)
                .resolve("telemetry")
                .resolve("events.ndjson");
        Files.createDirectories(ndjson.getParent());
        try (TelemetryWriter writer = TelemetryWriter.open(ndjson)) {
            for (int i = 0; i < count; i++) {
                writer.write(skillEnd(
                        "x-story-implement", 100L + i,
                        epicId,
                        Instant.parse("2026-04-16T12:00:00Z")
                                .plusSeconds(i)));
            }
        }
        return ndjson;
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
