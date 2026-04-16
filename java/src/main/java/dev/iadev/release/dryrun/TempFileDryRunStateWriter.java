package dev.iadev.release.dryrun;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;

/**
 * Adapter that writes a dummy release state file to the
 * OS temp directory with owner-only permissions (POSIX
 * {@code 0600} where supported).
 *
 * <p>Uses {@link Files#createTempFile} so filenames are
 * atomic, unique, and never derived from operator input
 * (defends against CWE-22 / Rule 6).
 */
public final class TempFileDryRunStateWriter
        implements DryRunStatePort {

    private static final String PREFIX =
            "release-state-dryrun-";
    private static final String SUFFIX = ".json";

    private static final Set<PosixFilePermission> OWNER_ONLY =
            PosixFilePermissions.fromString("rw-------");

    /**
     * Creates a dummy state file with an auto-generated
     * unique name in the JDK-managed temp directory.
     */
    @Override
    public Path create(String version) {
        Objects.requireNonNull(version, "version");
        try {
            Path path = createSecureTempFile();
            Files.writeString(path,
                    "{\"dryRun\":true,\"version\":\""
                            + escape(version) + "\"}",
                    StandardCharsets.UTF_8);
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to create dry-run state file",
                    e);
        }
    }

    private static Path createSecureTempFile()
            throws IOException {
        if (supportsPosix()) {
            FileAttribute<Set<PosixFilePermission>> attr =
                    PosixFilePermissions.asFileAttribute(
                            OWNER_ONLY);
            return Files.createTempFile(
                    PREFIX, SUFFIX, attr);
        }
        return Files.createTempFile(PREFIX, SUFFIX);
    }

    private static boolean supportsPosix() {
        FileSystem fs = FileSystems.getDefault();
        return fs.supportedFileAttributeViews()
                .contains("posix");
    }

    @Override
    public void delete(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to delete dry-run state file",
                    e);
        }
    }

    private static String escape(String value) {
        StringBuilder escaped =
                new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        escaped.append(String.format(
                                "\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
            }
        }
        return escaped.toString();
    }
}
