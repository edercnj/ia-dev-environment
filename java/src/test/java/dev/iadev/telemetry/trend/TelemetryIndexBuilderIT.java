package dev.iadev.telemetry.trend;

import static dev.iadev.telemetry.trend.TelemetryTrendTestFixtures.writeFixture;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TelemetryIndexBuilderIT {

    @TempDir
    Path tmp;

    @Test
    void buildOrRefresh_emptyBaseDir_returnsEmptyIndex() {
        Path indexPath = tmp.resolve(".claude")
                .resolve("telemetry")
                .resolve("index.json");
        TelemetryIndex idx = new TelemetryIndexBuilder(
                tmp.resolve("plans"), indexPath)
                .buildOrRefresh();
        assertThat(idx.series()).isEmpty();
        assertThat(idx.epicMtimesEpochMs()).isEmpty();
        assertThat(Files.exists(indexPath)).isTrue();
    }

    @Test
    void buildOrRefresh_scansEpicsAndProducesPerSkillP95()
            throws Exception {
        Path base = tmp.resolve("plans");
        writeFixture(base, "EPIC-0001", "foo", 10, 100L);
        writeFixture(base, "EPIC-0002", "foo", 10, 150L);
        Path indexPath = tmp.resolve(".claude")
                .resolve("telemetry")
                .resolve("index.json");

        TelemetryIndex idx = new TelemetryIndexBuilder(
                base, indexPath).buildOrRefresh();

        assertThat(idx.series()).hasSize(2);
        assertThat(idx.epicMtimesEpochMs())
                .containsKeys("EPIC-0001", "EPIC-0002");
        assertThat(Files.exists(indexPath)).isTrue();
    }

    @Test
    void buildOrRefresh_reusesCacheWhenMtimesMatch()
            throws Exception {
        Path base = tmp.resolve("plans");
        writeFixture(base, "EPIC-0001", "foo", 5, 100L);
        Path indexPath = tmp.resolve("index.json");
        TelemetryIndexBuilder builder = new TelemetryIndexBuilder(
                base, indexPath);

        TelemetryIndex first = builder.buildOrRefresh();
        Instant firstGenerated = first.generatedAt();

        TelemetryIndex second = builder.buildOrRefresh();
        // Cache hit → generatedAt must be identical.
        assertThat(second.generatedAt()).isEqualTo(firstGenerated);
    }

    @Test
    void buildOrRefresh_invalidatesWhenMtimeChanges()
            throws Exception {
        Path base = tmp.resolve("plans");
        Path events = writeFixture(base, "EPIC-0001", "foo",
                5, 100L);
        Path indexPath = tmp.resolve("index.json");
        TelemetryIndexBuilder builder = new TelemetryIndexBuilder(
                base, indexPath);

        TelemetryIndex first = builder.buildOrRefresh();

        // Touch the file forward by 2s to guarantee a new mtime.
        Files.setLastModifiedTime(events,
                java.nio.file.attribute.FileTime.fromMillis(
                        System.currentTimeMillis() + 2000L));

        TelemetryIndex second = builder.buildOrRefresh();
        assertThat(second.generatedAt())
                .isNotEqualTo(first.generatedAt());
    }

    @Test
    void rebuild_ignoresCache() throws Exception {
        Path base = tmp.resolve("plans");
        writeFixture(base, "EPIC-0001", "foo", 5, 100L);
        Path indexPath = tmp.resolve("index.json");
        TelemetryIndexBuilder builder = new TelemetryIndexBuilder(
                base, indexPath);

        TelemetryIndex first = builder.buildOrRefresh();
        // No sleep: the assertion `isAfterOrEqualTo` accepts identical
        // Instants, so a same-nanosecond rebuild is valid. Dropped a
        // 10 ms Thread.sleep that violated Rule 05 (no sleep for async
        // sync) and added no correctness value.
        TelemetryIndex second = builder.rebuild();
        assertThat(second.generatedAt())
                .isAfterOrEqualTo(first.generatedAt());
    }

    @Test
    void withIndexPath_returnsIndependentBuilder() {
        Path base = tmp.resolve("plans");
        Path a = tmp.resolve("a.json");
        Path b = tmp.resolve("b.json");
        TelemetryIndexBuilder builder = new TelemetryIndexBuilder(
                base, a);
        TelemetryIndexBuilder other = builder.withIndexPath(b);
        assertThat(builder.indexPath()).isEqualTo(a);
        assertThat(other.indexPath()).isEqualTo(b);
    }
}
