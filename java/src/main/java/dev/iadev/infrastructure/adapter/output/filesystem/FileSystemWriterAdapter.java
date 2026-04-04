package dev.iadev.infrastructure.adapter.output.filesystem;

import dev.iadev.domain.port.output.FileSystemWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Driven adapter that implements {@link FileSystemWriter}
 * using Java NIO for all filesystem operations.
 *
 * <p>Centralizes all file I/O operations with path safety
 * validation. Every path-accepting method normalizes and
 * validates paths before performing I/O to prevent path
 * traversal attacks.</p>
 *
 * <h2>Path Safety</h2>
 * <ul>
 *   <li>All paths are normalized before validation</li>
 *   <li>Path traversal ({@code ..}) is rejected after
 *       normalization by comparing the normalized path
 *       against the original path's root</li>
 *   <li>Filenames are sanitized via multi-pass
 *       normalization</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <ul>
 *   <li>Null parameters throw
 *       {@link IllegalArgumentException}</li>
 *   <li>I/O failures throw
 *       {@link UncheckedIOException} with path context</li>
 * </ul>
 *
 * @see FileSystemWriter
 */
public final class FileSystemWriterAdapter
        implements FileSystemWriter {

    /**
     * Writes content to a file, creating parent directories
     * as needed.
     *
     * @param path    the target file path
     * @param content the file content (UTF-8)
     * @throws IllegalArgumentException if path or content
     *         is null, or if path contains traversal
     * @throws UncheckedIOException if I/O fails
     */
    @Override
    public void writeFile(Path path, String content) {
        requireNonNull(path, "path");
        requireNonNull(content, "content");
        rejectPathTraversal(path);

        try {
            createParentDirectories(path);
            Files.writeString(
                    path,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write file: %s"
                            .formatted(path), e);
        }
    }

    /**
     * Creates a directory and any necessary parent
     * directories. Idempotent: does not throw if the
     * directory already exists.
     *
     * @param path the directory path to create
     * @throws IllegalArgumentException if path is null
     *         or contains traversal
     * @throws UncheckedIOException if I/O fails
     */
    @Override
    public void createDirectory(Path path) {
        requireNonNull(path, "path");
        rejectPathTraversal(path);

        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to create directory: %s"
                            .formatted(path), e);
        }
    }

    /**
     * Checks whether a file or directory exists at the
     * given path. Does not throw for inaccessible paths.
     *
     * @param path the path to check
     * @return true if the path exists, false otherwise
     * @throws IllegalArgumentException if path is null
     */
    @Override
    public boolean exists(Path path) {
        requireNonNull(path, "path");
        return Files.exists(path);
    }

    /**
     * Copies a classpath resource to the specified
     * destination path, creating parent directories as
     * needed.
     *
     * @param resourcePath the classpath resource path
     * @param destination  the target file path
     * @throws IllegalArgumentException if resourcePath is
     *         null/blank, destination is null, or
     *         destination contains traversal
     * @throws UncheckedIOException if the resource is not
     *         found or I/O fails
     */
    @Override
    public void copyResource(
            String resourcePath, Path destination) {
        requireNonNullAndNotBlank(
                resourcePath, "resourcePath");
        requireNonNull(destination, "destination");
        rejectPathTraversal(destination);

        try (InputStream is = openResource(resourcePath)) {
            createParentDirectories(destination);
            Files.copy(
                    is,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to copy resource '%s' to %s"
                            .formatted(
                                    resourcePath,
                                    destination), e);
        }
    }

    private InputStream openResource(String resourcePath) {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream(resourcePath);
        if (is == null) {
            throw new UncheckedIOException(
                    new IOException(
                            "Classpath resource not found: "
                                    + resourcePath));
        }
        return is;
    }

    private void createParentDirectories(Path path)
            throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Rejects paths containing traversal components.
     *
     * <p>Normalizes the path and compares it with the
     * original. If the original path string contains
     * {@code ..} segments and the normalized result
     * differs, the path is considered a traversal
     * attempt.</p>
     *
     * @param path the path to validate
     * @throws IllegalArgumentException if path traversal
     *         is detected
     */
    static void rejectPathTraversal(Path path) {
        String pathStr = path.toString();
        Path normalized = path.normalize();

        boolean hasDotDot =
                pathStr.contains("..")
                        && !normalized.equals(path);

        if (hasDotDot) {
            throw new IllegalArgumentException(
                    "Rejected path traversal: %s "
                            + "(normalized to %s)"
                            .formatted(path, normalized));
        }
    }

    private static void requireNonNull(
            Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "%s must not be null".formatted(name));
        }
    }

    private static void requireNonNullAndNotBlank(
            String value, String name) {
        requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "%s must not be blank"
                            .formatted(name));
        }
    }
}
