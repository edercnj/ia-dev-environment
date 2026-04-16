package dev.iadev.infrastructure.adapter.output.telemetry;

import dev.iadev.domain.model.PhaseMetric;
import dev.iadev.domain.model.PhaseOutcome;
import dev.iadev.domain.model.ReleaseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryJsonlReaderTest {

    private static final Instant T0 = Instant.parse(
            "2026-04-13T08:00:00Z");

    @Test
    @DisplayName("read_missingFile_returnsEmptyStream")
    void read_missingFile_returnsEmptyStream(
            @TempDir Path dir) {
        TelemetryJsonlReader reader =
                new TelemetryJsonlReader();

        List<PhaseMetric> list = reader
                .read(dir.resolve("does-not-exist.jsonl"))
                .toList();

        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("read_roundTripsLinesWrittenByWriter")
    void read_roundTripsLinesWrittenByWriter(
            @TempDir Path dir) throws IOException {
        Path jsonl = dir.resolve("release-metrics.jsonl");
        FileTelemetryWriter writer =
                new FileTelemetryWriter(jsonl);
        writer.emit(new PhaseMetric(
                "3.2.0",
                ReleaseType.HOTFIX,
                "VALIDATED",
                T0, T0.plusSeconds(142), 142L,
                PhaseOutcome.SUCCESS));

        List<PhaseMetric> list = new TelemetryJsonlReader()
                .read(jsonl)
                .toList();

        assertThat(list).hasSize(1);
        PhaseMetric m = list.get(0);
        assertThat(m.releaseVersion()).isEqualTo("3.2.0");
        assertThat(m.releaseType())
                .isEqualTo(ReleaseType.HOTFIX);
        assertThat(m.phase()).isEqualTo("VALIDATED");
        assertThat(m.durationSec()).isEqualTo(142L);
        assertThat(m.outcome())
                .isEqualTo(PhaseOutcome.SUCCESS);
    }

    @Test
    @DisplayName("read_missingReleaseType_defaultsToRelease")
    void read_missingReleaseType_defaultsToRelease(
            @TempDir Path dir) throws IOException {
        Path jsonl = dir.resolve("release-metrics.jsonl");
        Files.writeString(jsonl,
                "{\"releaseVersion\":\"3.2.0\","
                        + "\"phase\":\"INIT\","
                        + "\"startedAt\":\"2026-04-13T08:00:00Z\","
                        + "\"endedAt\":\"2026-04-13T08:00:01Z\","
                        + "\"durationSec\":1,"
                        + "\"outcome\":\"SUCCESS\"}\n");

        List<PhaseMetric> list = new TelemetryJsonlReader()
                .read(jsonl)
                .toList();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).releaseType())
                .isEqualTo(ReleaseType.RELEASE);
    }
}
