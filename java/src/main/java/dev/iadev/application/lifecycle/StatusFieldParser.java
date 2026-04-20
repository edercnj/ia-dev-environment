package dev.iadev.application.lifecycle;

import dev.iadev.domain.lifecycle.LifecycleStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes the canonical {@code **Status:**} header
 * line of Epic / Story / Task markdown artifacts per Rule 22
 * — lifecycle-integrity. All I/O goes through this class so
 * that the regex, the atomic-write contract, and fail-loud
 * error reporting live in one place.
 *
 * <p>Writes use the temp-file-plus-rename pattern:
 * {@code file.tmp} is written first and then
 * {@link Files#move(Path, Path, java.nio.file.CopyOption...)}
 * with {@link StandardCopyOption#ATOMIC_MOVE} replaces the
 * target. Readers on other threads / processes either see the
 * old or new content — never a partial write.</p>
 */
public final class StatusFieldParser {

    /** Canonical regex from Rule 22. MULTILINE flag. */
    public static final String STATUS_REGEX =
            "^\\*\\*Status:\\*\\*\\s+"
            + "(Pendente|Planejada|Em Andamento|Concluída"
            + "|Falha|Bloqueada)\\s*$";

    private static final Pattern PATTERN =
            Pattern.compile(
                    STATUS_REGEX, Pattern.MULTILINE);

    private StatusFieldParser() {
        // Utility class — not instantiable.
    }

    /**
     * Reads the first {@code **Status:**} occurrence from the
     * file and returns the matching {@link LifecycleStatus}.
     * Returns {@link Optional#empty()} when the file has no
     * Status line or when the value is outside the six-valued
     * enum.
     *
     * @throws StatusSyncException when the file cannot be
     *     read (missing, permissions, I/O error).
     */
    public static Optional<LifecycleStatus> readStatus(
            Path file) {
        if (file == null) {
            throw new StatusSyncException(
                    null, "file path is null");
        }
        String content;
        try {
            content = Files.readString(
                    file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StatusSyncException(file,
                    "failed to read markdown file", e);
        }
        Matcher m = PATTERN.matcher(content);
        if (!m.find()) {
            return Optional.empty();
        }
        return LifecycleStatus.fromLabel(m.group(1));
    }

    /**
     * Writes the given status back to the file atomically.
     * The first occurrence of the Status line is replaced
     * in-place; every other line is preserved verbatim.
     *
     * @throws StatusSyncException when the file cannot be
     *     written (missing parent, permissions, atomic-move
     *     not supported on the filesystem, I/O error).
     */
    public static void writeStatus(Path file,
            LifecycleStatus newStatus) {
        if (file == null) {
            throw new StatusSyncException(
                    null, "file path is null");
        }
        if (newStatus == null) {
            throw new StatusSyncException(file,
                    "newStatus is null");
        }
        String content;
        try {
            content = Files.readString(
                    file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StatusSyncException(file,
                    "failed to read markdown file "
                            + "before write", e);
        }
        Matcher m = PATTERN.matcher(content);
        String replacement =
                "**Status:** " + newStatus.label();
        String updated;
        if (m.find()) {
            updated = content.substring(0, m.start())
                    + replacement
                    + content.substring(m.end());
        } else {
            // No existing Status line — prepend one so the
            // invariant is re-established. This matches the
            // template contract (every artifact carries the
            // Status header).
            updated = replacement
                    + System.lineSeparator() + content;
        }
        Path tmp = file.resolveSibling(
                file.getFileName().toString() + ".tmp");
        try {
            Files.writeString(tmp, updated,
                    StandardCharsets.UTF_8);
            Files.move(tmp, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Best-effort cleanup of the .tmp remnant.
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // Swallowed — primary cause is surfaced
                // via StatusSyncException below.
            }
            throw new StatusSyncException(file,
                    "failed to write status atomically", e);
        }
    }
}
