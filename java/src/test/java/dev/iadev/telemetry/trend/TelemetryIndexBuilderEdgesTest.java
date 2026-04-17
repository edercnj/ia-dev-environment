package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Edge-path coverage for {@link TelemetryIndexBuilder}: corrupt cache
 * recovery, non-matching directory names, empty epic folders, etc.
 */
class TelemetryIndexBuilderEdgesTest {

    @TempDir
    Path tmp;

    @Test
    void construct_nullBaseDir_throws() {
        assertThatThrownBy(() -> new TelemetryIndexBuilder(
                null, tmp.resolve("idx.json")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void construct_nullIndexPath_throws() {
        assertThatThrownBy(() -> new TelemetryIndexBuilder(
                tmp, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildOrRefresh_corruptCache_rebuildsTransparently()
            throws Exception {
        Path base = tmp.resolve("plans");
        Files.createDirectories(base);
        Path indexPath = tmp.resolve("idx.json");
        Files.writeString(indexPath, "{not-json",
                StandardCharsets.UTF_8);

        TelemetryIndex idx = new TelemetryIndexBuilder(
                base, indexPath).buildOrRefresh();
        // Empty series (no epic dirs) but the builder recovers from
        // unreadable cache without throwing.
        assertThat(idx.series()).isEmpty();
    }

    @Test
    void buildOrRefresh_directoryWithoutTelemetry_isSkipped()
            throws Exception {
        Path base = tmp.resolve("plans");
        // Epic dir exists but has no telemetry/events.ndjson.
        Files.createDirectories(base.resolve("epic-0042"));

        TelemetryIndex idx = new TelemetryIndexBuilder(
                base, tmp.resolve("idx.json")).buildOrRefresh();
        assertThat(idx.series()).isEmpty();
        assertThat(idx.epicMtimesEpochMs()).isEmpty();
    }

    @Test
    void buildOrRefresh_nonEpicDirectory_isIgnored()
            throws Exception {
        Path base = tmp.resolve("plans");
        Files.createDirectories(base.resolve("epic-abc"));
        Files.createDirectories(
                base.resolve("epic-abc").resolve("telemetry"));
        Files.writeString(base.resolve("epic-abc")
                        .resolve("telemetry")
                        .resolve("events.ndjson"),
                "",
                StandardCharsets.UTF_8);

        TelemetryIndex idx = new TelemetryIndexBuilder(
                base, tmp.resolve("idx.json")).buildOrRefresh();
        assertThat(idx.series()).isEmpty();
        assertThat(idx.epicMtimesEpochMs()).isEmpty();
    }

    @Test
    void defaultIndexPath_resolvesToClaudeTelemetry() {
        Path base = tmp.resolve("plans");
        TelemetryIndexBuilder builder = new TelemetryIndexBuilder(
                base);
        assertThat(builder.indexPath().toString())
                .contains(".claude")
                .contains("telemetry")
                .endsWith("index.json");
    }
}
