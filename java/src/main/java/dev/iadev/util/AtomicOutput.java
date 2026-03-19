package dev.iadev.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;

/**
 * Atomic file output with temp-dir-first strategy.
 *
 * <p>Implements RULE-008 (Atomic Output) by writing all files to a
 * temporary directory first, then moving the result to the final
 * destination. This ensures that partial failures do not leave the
 * destination directory in an inconsistent state.
 *
 * <p>On failure, the temporary directory is preserved for debugging
 * and its path is included in the exception message.
 *
 * <p>All files are written with UTF-8 encoding and LF line endings
 * per RULE-009 (Cross-Platform compatibility).
 *
 * <p>Example usage:
 * <pre>{@code
 * Map<String, String> files = Map.of(
 *     ".claude/rules/01-identity.md", "# Identity\n...",
 *     ".github/copilot-instructions.md", "# Instructions\n..."
 * );
 * AtomicOutput.write(Path.of("./output"), files);
 * }</pre>
 *
 * @see PathUtils
 */
public final class AtomicOutput {

    private static final String TEMP_DIR_PREFIX = "ia-dev-env-";

    private AtomicOutput() {
        // Utility class — no instantiation
    }

    /**
     * Writes files atomically to the destination directory.
     *
     * <p>Execution steps:
     * <ol>
     *   <li>Create a temporary directory with prefix
     *       {@code ia-dev-env-}</li>
     *   <li>Write all files into the temporary directory, creating
     *       subdirectories as needed</li>
     *   <li>If the destination already exists, remove it</li>
     *   <li>Move the temporary directory to the destination using
     *       {@link StandardCopyOption#ATOMIC_MOVE} if supported,
     *       falling back to recursive copy + delete</li>
     * </ol>
     *
     * <p>On failure during the write phase, the temporary directory
     * is preserved for debugging and its path is included in the
     * thrown exception message.
     *
     * @param destDir destination directory path
     * @param files   map of relative paths to file contents
     * @throws IOException          if file I/O fails
     * @throws NullPointerException if destDir or files is null
     */
    public static void write(Path destDir, Map<String, String> files)
            throws IOException {
        Objects.requireNonNull(destDir, "destDir must not be null");
        Objects.requireNonNull(files, "files must not be null");

        Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);

        try {
            writeFilesToTempDir(tempDir, files);
        } catch (Exception ex) {
            throw new IOException(
                    "Failed to write files. "
                            + "Temp dir preserved for debug: "
                            + tempDir.toAbsolutePath(),
                    ex);
        }

        moveToDestination(tempDir, destDir);
    }

    private static void writeFilesToTempDir(
            Path tempDir, Map<String, String> files) throws IOException {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path filePath = tempDir.resolve(entry.getKey());
            Files.createDirectories(filePath.getParent());
            Files.write(filePath,
                    entry.getValue()
                            .getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void moveToDestination(
            Path tempDir, Path destDir) throws IOException {
        if (Files.exists(destDir)) {
            deleteRecursively(destDir);
        }

        Files.createDirectories(destDir.getParent());

        try {
            Files.move(tempDir, destDir,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            copyRecursively(tempDir, destDir);
            deleteRecursively(tempDir);
        }
    }

    private static void copyRecursively(Path source, Path target)
            throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(
                    Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path destPath = target.resolve(
                        source.relativize(dir));
                Files.createDirectories(destPath);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.copy(file,
                        target.resolve(source.relativize(file)),
                        StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(
                    Path d, IOException exc)
                    throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
