package dev.iadev.telemetry.trend;

import static dev.iadev.telemetry.trend.TelemetryTrendTestFixtures.writeFixture;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
        String md = Files.readString(outFile, StandardCharsets.UTF_8);
        assertThat(md).contains("**Baseline:** mean");
    }

    @Test
    void call_helpOption_exitsZero() {
        int code = new CommandLine(new TelemetryTrendCli())
                .execute("--help");
        assertThat(code).isZero();
    }
}
