package dev.iadev.application.lifecycle;

import dev.iadev.domain.lifecycle.LifecycleStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates the {@code Status} column of a single row in the
 * epic-level {@code IMPLEMENTATION-MAP.md} artifact. The map
 * uses a 6-column schema:
 * {@code | Story | Título | Chave Jira | Blocked By |
 * Blocks | Status |}. This helper is distinct from the
 * task-map updater (story-0046-0003), which operates on a
 * different shape. Atomic write via temp-file-plus-rename per
 * Rule 22 — lifecycle-integrity.
 */
public final class EpicMapRowUpdater {

    /**
     * Matches a 6-column row whose first cell is the target
     * story id. Group 1 captures the entire prefix through the
     * final pipe that delimits the Status column; group 2
     * captures the current Status cell content (trimmed); the
     * regex must accept optional whitespace on every side of a
     * pipe, tolerating both padded and compact tables.
     */
    private static final String ROW_REGEX_TEMPLATE =
            "^(\\|\\s*%s\\s*"
            + "\\|[^|\\n]*"      // Título
            + "\\|[^|\\n]*"      // Chave Jira
            + "\\|[^|\\n]*"      // Blocked By
            + "\\|[^|\\n]*"      // Blocks
            + "\\|\\s*)([^|\\n]+?)(\\s*\\|\\s*)$";

    private EpicMapRowUpdater() {
        // Utility class — not instantiable.
    }

    /**
     * Rewrites the Status cell of the row whose first column
     * equals {@code storyId}. Every other line of the file is
     * preserved verbatim. Throws {@link StatusSyncException}
     * when the file is missing, the row is absent, or the
     * atomic write fails — fail-loud per Rule 22.
     *
     * @param mapFile the {@code IMPLEMENTATION-MAP.md} path
     * @param storyId the story identifier (first column)
     * @param newStatus the new {@link LifecycleStatus}
     */
    public static void updateRow(Path mapFile,
            String storyId, LifecycleStatus newStatus) {
        if (mapFile == null) {
            throw new StatusSyncException(
                    null, "mapFile is null");
        }
        if (storyId == null || storyId.isBlank()) {
            throw new StatusSyncException(mapFile,
                    "storyId is null or blank");
        }
        if (newStatus == null) {
            throw new StatusSyncException(mapFile,
                    "newStatus is null");
        }
        String content;
        try {
            content = Files.readString(
                    mapFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StatusSyncException(mapFile,
                    "failed to read map file", e);
        }
        String regex = String.format(
                ROW_REGEX_TEMPLATE,
                Pattern.quote(storyId));
        Pattern pattern = Pattern.compile(
                regex, Pattern.MULTILINE);
        Matcher m = pattern.matcher(content);
        if (!m.find()) {
            throw new StatusSyncException(mapFile,
                    "row not found for storyId="
                            + storyId);
        }
        String prefix = m.group(1);
        String suffix = m.group(3);
        String replacement =
                prefix + newStatus.label() + suffix;
        String updated = content.substring(0, m.start())
                + replacement
                + content.substring(m.end());
        Path tmp = mapFile.resolveSibling(
                mapFile.getFileName().toString() + ".tmp");
        try {
            Files.writeString(tmp, updated,
                    StandardCharsets.UTF_8);
            Files.move(tmp, mapFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // Swallowed — primary cause surfaced below.
            }
            throw new StatusSyncException(mapFile,
                    "failed to write map row atomically", e);
        }
    }
}
