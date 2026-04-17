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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

class TelemetryAnalyzeCliIT {

    @TempDir
    Path tmp;

    @Test
    void call_missingTelemetry_exitsWithCode2() throws Exception {
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-9999",
                        "--base-dir", tmp.toString());

        assertThat(code).isEqualTo(
                TelemetryAnalyzeCli.EXIT_NO_TELEMETRY);
    }

    @Test
    void call_exportWithoutOut_exitsWithCode4() {
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--export", "json");

        assertThat(code).isEqualTo(
                TelemetryAnalyzeCli.EXIT_EXPORT_MISSING_OUT);
    }

    @Test
    void call_noEpicArg_exitsWithValidationError() {
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute();

        assertThat(code).isEqualTo(
                TelemetryAnalyzeCli.EXIT_VALIDATION);
    }

    @Test
    void call_happyPath_writesMarkdownReport() throws Exception {
        Path ndjson = prepareFixture(tmp, "EPIC-0040", 10);

        Path outFile = tmp.resolve("report.md");
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--base-dir", tmp.toString(),
                        "--out", outFile.toString());

        assertThat(code).isZero();
        assertThat(Files.exists(outFile)).isTrue();
        String report = Files.readString(outFile,
                StandardCharsets.UTF_8);
        assertThat(report)
                .contains("# Telemetry Report")
                .contains("## Resumo geral")
                .contains("## Por skill")
                .contains("## Por fase")
                .contains("## Por tool")
                .contains("## Gantt")
                .contains("## Observacoes");
    }

    @Test
    void call_exportJson_writesValidJsonFile() throws Exception {
        prepareFixture(tmp, "EPIC-0040", 10);

        Path outFile = tmp.resolve("report.json");
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--base-dir", tmp.toString(),
                        "--export", "json",
                        "--out", outFile.toString());

        assertThat(code).isZero();
        String content = Files.readString(outFile,
                StandardCharsets.UTF_8);
        assertThat(content).startsWith("{")
                .contains("\"generatedAt\"")
                .contains("\"epics\"")
                .contains("\"skills\"");
    }

    @Test
    void call_exportCsv_writesValidCsvFile() throws Exception {
        prepareFixture(tmp, "EPIC-0040", 10);

        Path outFile = tmp.resolve("report.csv");
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--base-dir", tmp.toString(),
                        "--export", "csv",
                        "--out", outFile.toString());

        assertThat(code).isZero();
        String content = Files.readString(outFile,
                StandardCharsets.UTF_8);
        assertThat(content).startsWith(
                "type,name,invocations,totalMs,avgMs,p50Ms,"
                        + "p95Ms,epicIds\n");
    }

    @Test
    void call_crossEpic_mergesBothFixtures() throws Exception {
        prepareFixture(tmp, "EPIC-0040", 5);
        prepareFixture(tmp, "EPIC-0041", 5);

        Path outFile = tmp.resolve("cross.md");
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epics", "EPIC-0040,EPIC-0041",
                        "--base-dir", tmp.toString(),
                        "--out", outFile.toString());

        assertThat(code).isZero();
        String report = Files.readString(outFile,
                StandardCharsets.UTF_8);
        assertThat(report)
                .contains("**Epics:** EPIC-0040, EPIC-0041");
    }

    @Test
    void call_tenThousandEvents_under5Seconds() throws Exception {
        prepareFixture(tmp, "EPIC-0040", 10_000);

        Path outFile = tmp.resolve("perf.md");
        long start = System.nanoTime();
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--base-dir", tmp.toString(),
                        "--out", outFile.toString());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertThat(code).isZero();
        assertThat(elapsedMs)
                .as("10k events must be processed in < 5s (actual: "
                        + elapsedMs + "ms)")
                .isLessThan(5000L);
    }

    @Test
    void call_sinceFlag_filtersEarlierEvents() throws Exception {
        Path base = tmp;
        Path events = base.resolve("epic-0040")
                .resolve("telemetry")
                .resolve("events.ndjson");
        Files.createDirectories(events.getParent());
        try (TelemetryWriter writer = TelemetryWriter.open(events)) {
            writer.write(skillEnd("x-story-implement", 100L,
                    "EPIC-0040",
                    Instant.parse("2024-01-01T00:00:00Z")));
            writer.write(skillEnd("x-story-implement", 200L,
                    "EPIC-0040",
                    Instant.parse("2026-04-16T12:00:00Z")));
        }

        Path outFile = tmp.resolve("filtered.json");
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(
                        "--epic", "EPIC-0040",
                        "--base-dir", tmp.toString(),
                        "--since", "2026-01-01",
                        "--export", "json",
                        "--out", outFile.toString());

        assertThat(code).isZero();
        String json = Files.readString(outFile,
                StandardCharsets.UTF_8);
        // Only the 200ms event remains
        assertThat(json).contains("\"totalMs\" : 200");
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
                        "x-story-implement", 100L + (i % 10),
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
