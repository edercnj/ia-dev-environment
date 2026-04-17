package dev.iadev.telemetry.trend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans the filesystem for {@code plans/epic-XXXX/telemetry/events.ndjson}
 * files and exposes the per-epic NDJSON paths and mtimes.
 *
 * <p>Extracted from {@link TelemetryIndexBuilder} so the builder itself can
 * stay focused on cache read/write + aggregation orchestration (Rule 03 —
 * file length and SRP).</p>
 */
final class EpicDirectoryScanner {

    static final String TELEMETRY_DIR = "telemetry";
    static final String EVENTS_FILENAME = "events.ndjson";
    static final Pattern EPIC_DIR_PATTERN =
            Pattern.compile("^epic-(\\d{4})$");

    private final Path baseDir;

    EpicDirectoryScanner(Path baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Returns a sorted map of {@code epicId -> mtime (epoch ms)} for every
     * {@code epic-XXXX/telemetry/events.ndjson} under the base directory.
     * Non-matching directories (wrong prefix, non-4-digit suffix) are skipped
     * silently. Epic directories without an NDJSON file are also skipped.
     *
     * @return an ordered map by epic ID
     */
    TreeMap<String, Long> scanEpicMtimes() {
        TreeMap<String, Long> out = new TreeMap<>();
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
                Path events = dir.resolve(TELEMETRY_DIR)
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

    /**
     * Resolves the NDJSON path for the given epic.
     *
     * @param epicId the epic ID (with or without the {@code EPIC-} prefix)
     * @return the NDJSON path
     */
    Path eventsPath(String epicId) {
        String suffix = epicId.startsWith("EPIC-")
                ? epicId.substring("EPIC-".length())
                : epicId;
        return baseDir.resolve("epic-" + suffix)
                .resolve(TELEMETRY_DIR)
                .resolve(EVENTS_FILENAME);
    }
}
