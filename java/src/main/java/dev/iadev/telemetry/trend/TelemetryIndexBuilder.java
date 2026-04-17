package dev.iadev.telemetry.trend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.iadev.telemetry.EventType;
import dev.iadev.telemetry.TelemetryEvent;
import dev.iadev.telemetry.TelemetryReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Builds and caches the per-skill per-epic P95 index consumed by
 * {@code /x-telemetry-trend}.
 *
 * <p>Cache invalidation is mtime-driven: if the on-disk index's recorded
 * {@code epicMtimesEpochMs} map matches the current NDJSON mtimes exactly
 * (same set of epics and same mtime per epic), the cached index is reused;
 * otherwise it is rebuilt from scratch. Directory scanning is delegated to
 * {@link EpicDirectoryScanner} (Rule 03 — single responsibility).</p>
 */
public final class TelemetryIndexBuilder {

    private final Path baseDir;
    private final EpicDirectoryScanner scanner;
    private final Path indexPath;
    private final ObjectMapper mapper;

    /**
     * Creates a builder scanning {@code baseDir/epic-*} and caching to the
     * default index path {@code baseDir/../.claude/telemetry/index.json}.
     *
     * @param baseDir the directory containing the {@code epic-XXXX} folders
     */
    public TelemetryIndexBuilder(Path baseDir) {
        this(baseDir, defaultIndexPath(baseDir));
    }

    /**
     * Creates a builder with an explicit index path (test-only).
     *
     * @param baseDir   the directory containing the {@code epic-XXXX} folders
     * @param indexPath the file to read / write as the cached index
     */
    public TelemetryIndexBuilder(Path baseDir, Path indexPath) {
        this.baseDir = Objects.requireNonNull(baseDir,
                "baseDir is required");
        this.indexPath = Objects.requireNonNull(indexPath,
                "indexPath is required");
        this.scanner = new EpicDirectoryScanner(baseDir);
        this.mapper = buildMapper();
    }

    /**
     * Returns a new builder with the given {@code indexPath}, preserving the
     * {@code baseDir}.
     *
     * @param newIndexPath the override path
     * @return a new, independent builder instance
     */
    public TelemetryIndexBuilder withIndexPath(Path newIndexPath) {
        return new TelemetryIndexBuilder(baseDir, newIndexPath);
    }

    /**
     * Returns the cached or freshly-built index. The cache is reused only
     * when every tracked NDJSON's mtime matches the stored value exactly.
     *
     * @return the telemetry index
     */
    public TelemetryIndex buildOrRefresh() {
        Map<String, Long> currentMtimes = scanner.scanEpicMtimes();
        TelemetryIndex cached = readCached();
        if (cached != null
                && cached.epicMtimesEpochMs().equals(currentMtimes)) {
            return cached;
        }
        TelemetryIndex fresh = build(currentMtimes);
        writeCache(fresh);
        return fresh;
    }

    /**
     * Forces a rebuild, ignoring the on-disk cache.
     *
     * @return the freshly-built index
     */
    public TelemetryIndex rebuild() {
        Map<String, Long> currentMtimes = scanner.scanEpicMtimes();
        TelemetryIndex fresh = build(currentMtimes);
        writeCache(fresh);
        return fresh;
    }

    /** @return the cache file path used by this builder */
    public Path indexPath() {
        return indexPath;
    }

    private TelemetryIndex build(Map<String, Long> mtimes) {
        List<EpicSkillP95> series = new ArrayList<>();
        for (String epicId : mtimes.keySet()) {
            series.addAll(aggregateEpic(epicId));
        }
        return new TelemetryIndex(
                TelemetryIndex.CURRENT_SCHEMA_VERSION,
                Instant.now(),
                mtimes,
                series);
    }

    private List<EpicSkillP95> aggregateEpic(String epicId) {
        Path events = scanner.eventsPath(epicId);
        Map<String, List<Long>> perSkill = new LinkedHashMap<>();
        try (Stream<TelemetryEvent> stream =
                TelemetryReader.open(events)
                        .streamSkippingInvalid()) {
            stream.forEach(ev -> {
                if (ev.type() == EventType.SKILL_END
                        && ev.skill() != null
                        && ev.durationMs() != null) {
                    perSkill.computeIfAbsent(ev.skill(),
                            k -> new ArrayList<>())
                            .add(ev.durationMs());
                }
            });
        }
        List<EpicSkillP95> out = new ArrayList<>(perSkill.size());
        for (Map.Entry<String, List<Long>> entry
                : perSkill.entrySet()) {
            List<Long> samples = entry.getValue();
            Collections.sort(samples);
            long p95 = percentile(samples, 95);
            out.add(new EpicSkillP95(
                    epicId, entry.getKey(),
                    p95, (long) samples.size()));
        }
        return out;
    }

    private TelemetryIndex readCached() {
        if (!Files.isRegularFile(indexPath)) {
            return null;
        }
        try {
            String json = Files.readString(indexPath,
                    StandardCharsets.UTF_8);
            return mapper.readValue(json, TelemetryIndex.class);
        } catch (IOException e) {
            // Corrupt / unreadable cache → rebuild from scratch.
            return null;
        }
    }

    private void writeCache(TelemetryIndex index) {
        try {
            Path parent = indexPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(index);
            Files.writeString(indexPath, json,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to write telemetry index at "
                            + indexPath, e);
        }
    }

    private static Path defaultIndexPath(Path baseDir) {
        Path root = baseDir.toAbsolutePath().getParent();
        if (root == null) {
            root = baseDir.toAbsolutePath();
        }
        return root.resolve(".claude")
                .resolve("telemetry")
                .resolve("index.json");
    }

    private static long percentile(List<Long> sortedSamples, int p) {
        int n = sortedSamples.size();
        if (n == 0) {
            return 0L;
        }
        int rank = (int) Math.ceil((p / 100.0) * n);
        if (rank < 1) {
            rank = 1;
        }
        if (rank > n) {
            rank = n;
        }
        return sortedSamples.get(rank - 1);
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return m;
    }
}
