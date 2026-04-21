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
 * Atomically updates the {@code Status} column of a row in a
 * {@code task-implementation-map-STORY-*.md} markdown table.
 * Story-0046-0003 / TASK-0046-0003-001.
 *
 * <p>The row is matched by TASK-ID (first column). Only the
 * LAST column of the matched row is rewritten with the new
 * status label. All other content of the file — including
 * surrounding rows, prose, and the table header — is preserved
 * verbatim. The update is idempotent: running it twice with
 * the same inputs yields the same final content.</p>
 *
 * <p>Writes use the temp-file-plus-atomic-rename pattern
 * inherited from {@link StatusFieldParser} — readers never see
 * a partial write.</p>
 */
public final class TaskMapRowUpdater {

    /**
     * Matches a Markdown table row whose first column is a
     * canonical TASK-ID of the form {@code TASK-XXXX-YYYY-NNN}.
     * The TASK-ID is captured as group 1; the status cell
     * (value between the last two pipes of the row) is
     * captured as group 3 and replaced verbatim.
     *
     * <p>The regex is anchored to the start of a line
     * ({@code MULTILINE}) so prose mentions of TASK-IDs inside
     * paragraphs are never matched — only genuine table rows.
     * </p>
     */
    private static final Pattern ROW_PATTERN =
            Pattern.compile(
                    "^\\|[ \\t]*"
                            + "(TASK-\\d{4}-\\d{4}-\\d{3})"
                            + "[ \\t]*\\|(.*)"
                            + "\\|([^|\\n]*)\\|"
                            + "[ \\t]*$",
                    Pattern.MULTILINE);

    private TaskMapRowUpdater() {
        // Utility class — not instantiable.
    }

    /**
     * Rewrites the Status column of the row identified by
     * {@code taskId}. Throws {@link StatusSyncException} when
     * the file cannot be read, the TASK-ID row is absent, or
     * the atomic rename fails.
     *
     * @param mapFile path to the task-implementation-map file
     * @param taskId canonical TASK-ID (e.g.,
     *     {@code TASK-0046-0003-001})
     * @param newStatus the desired status to write in the
     *     last column of the matched row
     */
    public static void updateRow(Path mapFile, String taskId,
            LifecycleStatus newStatus) {
        if (mapFile == null) {
            throw new StatusSyncException(
                    null, "mapFile is null");
        }
        if (taskId == null || taskId.isBlank()) {
            throw new StatusSyncException(mapFile,
                    "taskId is null or blank");
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
        String updated =
                rewriteRow(content, taskId, newStatus);
        if (updated.equals(content)) {
            // No change: either already idempotent or the row
            // is absent. Distinguish by probing for a matching
            // row explicitly.
            if (!containsRow(content, taskId)) {
                throw new StatusSyncException(mapFile,
                        "no row matches taskId=" + taskId);
            }
            return; // idempotent no-op
        }
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
                    "failed to write map atomically", e);
        }
    }

    /**
     * Pure-function variant for unit testing. Returns the
     * rewritten content without any I/O. Returns the original
     * content unchanged when no row matches {@code taskId}.
     */
    static String rewriteRow(String content, String taskId,
            LifecycleStatus newStatus) {
        Matcher m = ROW_PATTERN.matcher(content);
        StringBuilder out = new StringBuilder(content.length());
        int lastEnd = 0;
        while (m.find()) {
            out.append(content, lastEnd, m.start());
            if (taskId.equals(m.group(1))) {
                out.append("| ")
                        .append(m.group(1))
                        .append(" |")
                        .append(m.group(2))
                        .append("| ")
                        .append(newStatus.label())
                        .append(" |");
            } else {
                out.append(m.group());
            }
            lastEnd = m.end();
        }
        out.append(content, lastEnd, content.length());
        return out.toString();
    }

    private static boolean containsRow(String content,
            String taskId) {
        Matcher m = ROW_PATTERN.matcher(content);
        while (m.find()) {
            if (taskId.equals(m.group(1))) {
                return true;
            }
        }
        return false;
    }
}
