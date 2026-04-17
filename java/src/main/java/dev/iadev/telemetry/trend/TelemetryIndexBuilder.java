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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Builds and caches the per-skill per-epic P95 index consumed by
 * {@code /x-telemetry-trend}.
 *
 * <p>The builder scans {@code plans/epic-*\/telemetry/events.ndjson} under the
 * configured base directory, collapses each epic's {@code skill.end}
 * durations into per-skill P95 (Nearest-Rank) values, and persists the result
 * to {@code .claude/telemetry/index.json} (path overridable via
 * {@link #withIndexPath(Path)}). The index file is {@code .gitignore}d per
 * the repository policy: only the per-epic NDJSON logs are versioned.</p>
 *
 * <p>Invalidation is mtime-driven: if the on-disk index's recorded
 * {@code epicMtimesEpochMs} map matches the current NDJSON mtimes exactly
 * (same set of epics and same mtime per epic), the cached index is reused;
 * otherwise it is rebuilt from scratch.</p>
 */
public final class TelemetryIndexBuilder {

    private static final String SKILL_TELEMETRY_DIR = "telemetry";
    private static final String EVENTS_FILENAME = "events.ndjson";
    private static final Pattern EPIC_DIR_PATTERN =
            Pattern.compile("^epic-(\\d{4})$");

    private final Path baseDir;
    private final Path indexPath;
    private final ObjectMapper mapper;

    /**
     * Creates a builder scanning {@code baseDir/epic-*} and caching to the
     * default index path {@code baseDir/../.claude/telemetry/index.json}.
     * When {@code baseDir} is {@code plans}, the default resolves to
     * {@code .claude/telemetry/index.json} as documented in the story.
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
        this.mapper = buildMapper();
    }

    /**
     * Returns a new builder with the given {@code indexPath}, preserving
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
        Map<String, Long> currentMtimes = scanEpicMtimes();
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
        Map<String, Long> currentMtimes = scanEpicMtimes();
        TelemetryIndex fresh = build(currentMtimes);
        writeCache(fresh);
        return fresh;
    }

    /** @return the cache file path used by this builder */
    public Path indexPath() {
        return indexPath;
    }

    private TelemetryIndex build(Map<String, Long> mtimes) {
        // For each epic, stream events.ndjson once and compute per-skill P95.
        List<EpicSkillP95> series = new ArrayList<>();
        for (String epicId : mtimes.keySet()) {
            Path events = eventsPath(epicId);
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
            for (Map.Entry<String, List<Long>> entry
                    : perSkill.entrySet()) {
                List<Long> samples = entry.getValue();
                Collections.sort(samples);
                long p95 = percentile(samples, 95);
                series.add(new EpicSkillP95(
                        epicId, entry.getKey(),
                        p95, (long) samples.size()));
            }
        }
        return new TelemetryIndex(
                TelemetryIndex.CURRENT_SCHEMA_VERSION,
                Instant.now(),
                mtimes,
                series);
    }

    private Map<String, Long> scanEpicMtimes() {
        Map<String, Long> out = new TreeMap<>();
        if (!Files.exists(baseDir)) {
            return out;
        }
        try (DirectoryStream<Path> epics = Files.newDirectoryStream(
                baseDir, "epic-*")) {
            for (Path dir : epics) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                Matcher m = EPIC_DIR_PATTERN.matcher(
                        dir.getFileName().toString());
                if (!m.matches()) {
                    continue;
                }
                Path events = dir.resolve(SKILL_TELEMETRY_DIR)
                        .resolve(EVENTS_FILENAME);
                if (!Files.isRegularFile(events)) {
                    continue;
                }
                long mtime = Files.getLastModifiedTime(events)
                        .toMillis();
                out.put("EPIC-" + m.group(1), mtime);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to scan epic dirs under " + baseDir, e);
        }
        return out;
    }

    private Path eventsPath(String epicId) {
        String suffix = epicId.startsWith("EPIC-")
                ? epicId.substring("EPIC-".length())
                : epicId;
        return baseDir.resolve("epic-" + suffix)
                .resolve(SKILL_TELEMETRY_DIR)
                .resolve(EVENTS_FILENAME);
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
