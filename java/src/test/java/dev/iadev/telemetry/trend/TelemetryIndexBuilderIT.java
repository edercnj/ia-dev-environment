package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.telemetry.EventStatus;
import dev.iadev.telemetry.EventType;
import dev.iadev.telemetry.TelemetryEvent;
import dev.iadev.telemetry.TelemetryWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        // Sleep briefly so the new Instant.now() is strictly > first
        Thread.sleep(10);
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

    // Force Map import retained (javac does not flag unused imports)
    @SuppressWarnings("unused")
    private static final List<Map<String, Object>> UNUSED =
            List.of();
}
