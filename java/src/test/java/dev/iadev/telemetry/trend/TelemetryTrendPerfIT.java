package dev.iadev.telemetry.trend;

import static dev.iadev.telemetry.trend.TelemetryTrendTestFixtures.writeFixture;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

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
            writeFixture(base, epicId, "x-story-implement",
                    EVENTS_PER_EPIC, 100L);
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
}
