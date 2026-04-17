package dev.iadev.telemetry.trend;

import static dev.iadev.telemetry.trend.TelemetryTrendTestFixtures.writeFixture;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

class TelemetryTrendCliIT {

    @TempDir
    Path tmp;

    @Test
    void call_negativeThreshold_exitsCode6() {
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--threshold-pct", "-10",
                        "--base-dir", tmp.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString());
        assertThat(code).isEqualTo(
                TelemetryTrendCli.EXIT_THRESHOLD_NEGATIVE);
    }

    @Test
    void call_invalidLast_exitsValidation() {
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--last", "0",
                        "--base-dir", tmp.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString());
        assertThat(code).isEqualTo(TelemetryTrendCli.EXIT_VALIDATION);
    }

    @Test
    void call_invalidBaseline_exitsValidation() {
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--baseline", "mode",
                        "--base-dir", tmp.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString());
        assertThat(code).isEqualTo(TelemetryTrendCli.EXIT_VALIDATION);
    }

    @Test
    void call_invalidFormat_exitsValidation() {
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--format", "yaml",
                        "--base-dir", tmp.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString());
        assertThat(code).isEqualTo(TelemetryTrendCli.EXIT_VALIDATION);
    }

    @Test
    void call_singleEpic_exitsCode5() throws Exception {
        Path base = tmp.resolve("plans");
        writeFixture(base, "EPIC-0001", "foo", 5, 100L);
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--base-dir", base.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString());
        assertThat(code).isEqualTo(
                TelemetryTrendCli.EXIT_INSUFFICIENT_EPICS);
    }

    @Test
    void call_happyPath_detectsRegression() throws Exception {
        Path base = tmp.resolve("plans");
        // 4 stable epics at 100ms, 5th at ~140ms → 40% regression
        writeFixture(base, "EPIC-0001", "foo", 10, 100L);
        writeFixture(base, "EPIC-0002", "foo", 10, 100L);
        writeFixture(base, "EPIC-0003", "foo", 10, 100L);
        writeFixture(base, "EPIC-0004", "foo", 10, 100L);
        writeFixture(base, "EPIC-0005", "foo", 10, 140L);

        Path outFile = tmp.resolve("trends.md");
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--last", "5",
                        "--threshold-pct", "20",
                        "--baseline", "median",
                        "--base-dir", base.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString(),
                        "--out", outFile.toString());
        assertThat(code).isZero();
        String md = Files.readString(outFile, StandardCharsets.UTF_8);
        assertThat(md)
                .contains("# Telemetry Trend Report")
                .contains("foo")
                .contains("regressed");
    }

    @Test
    void call_stableSeries_noRegressionReported() throws Exception {
        Path base = tmp.resolve("plans");
        for (int i = 1; i <= 5; i++) {
            String id = "EPIC-000" + i;
            writeFixture(base, id, "foo", 10, 100L);
        }
        Path outFile = tmp.resolve("trends.md");
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--threshold-pct", "20",
                        "--base-dir", base.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString(),
                        "--out", outFile.toString());
        assertThat(code).isZero();
        String md = Files.readString(outFile, StandardCharsets.UTF_8);
        assertThat(md).contains("Nenhuma regressão detectada");
    }

    @Test
    void call_stdoutWhenNoOut() throws Exception {
        Path base = tmp.resolve("plans");
        writeFixture(base, "EPIC-0001", "foo", 5, 100L);
        writeFixture(base, "EPIC-0002", "foo", 5, 110L);

        StringWriter sw = new StringWriter();
        int code = new CommandLine(new TelemetryTrendCli())
                .setOut(new PrintWriter(sw))
                .execute(
                        "--base-dir", base.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString());
        assertThat(code).isZero();
        assertThat(sw.toString())
                .contains("# Telemetry Trend Report");
    }

    @Test
    void call_jsonFormat_outputsJson() throws Exception {
        Path base = tmp.resolve("plans");
        writeFixture(base, "EPIC-0001", "foo", 5, 100L);
        writeFixture(base, "EPIC-0002", "foo", 5, 110L);

        Path outFile = tmp.resolve("trends.json");
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--format", "json",
                        "--base-dir", base.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString(),
                        "--out", outFile.toString());
        assertThat(code).isZero();
        String json = Files.readString(outFile, StandardCharsets.UTF_8);
        assertThat(json)
                .startsWith("{")
                .contains("\"epicsAnalyzed\"")
                .contains("\"regressions\"")
                .contains("\"slowest\"");
    }

    @Test
    void call_topTenOrdering_slowestFirst() throws Exception {
        Path base = tmp.resolve("plans");
        // 3 epics, 3 skills with different P95 magnitudes
        for (int i = 1; i <= 3; i++) {
            String id = "EPIC-000" + i;
            writeFixture(base, id, "foo", 5, 500L);
            writeFixture(base, id, "bar", 5, 1500L);
            writeFixture(base, id, "baz", 5, 800L);
        }
        Path outFile = tmp.resolve("trends.md");
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--last", "3",
                        "--base-dir", base.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString(),
                        "--out", outFile.toString());
        assertThat(code).isZero();
        String md = Files.readString(outFile, StandardCharsets.UTF_8);
        int barIdx = md.indexOf("| bar |");
        int bazIdx = md.indexOf("| baz |");
        int fooIdx = md.indexOf("| foo |");
        assertThat(barIdx).isPositive();
        assertThat(bazIdx).isPositive();
        assertThat(fooIdx).isPositive();
        assertThat(barIdx).isLessThan(bazIdx);
        assertThat(bazIdx).isLessThan(fooIdx);
    }

    @Test
    void call_rebuildIndex_works() throws Exception {
        Path base = tmp.resolve("plans");
        writeFixture(base, "EPIC-0001", "foo", 5, 100L);
        writeFixture(base, "EPIC-0002", "foo", 5, 120L);

        int code = new CommandLine(new TelemetryTrendCli())
                .execute(
                        "--rebuild-index",
                        "--base-dir", base.toString(),
                        "--index-path",
                        tmp.resolve("idx.json").toString(),
                        "--out", tmp.resolve("r.md").toString());
        assertThat(code).isZero();
    }
}
