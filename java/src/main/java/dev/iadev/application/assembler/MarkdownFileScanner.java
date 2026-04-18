package dev.iadev.application.assembler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Shared helper for listing Markdown ({@code .md}) files
 * in a single directory (non-recursive).
 *
 * <p>Consolidates the {@code Files.list(dir) + filter ".md"}
 * idiom previously duplicated across assemblers. Each helper
 * filters on {@link Files#isRegularFile} so that directories
 * whose names end in {@code .md} are excluded, and returns
 * an immutable {@link List}.</p>
 *
 * <p>Missing or non-directory input paths yield an empty
 * list (no exception). Any other I/O failure is wrapped in
 * {@link UncheckedIOException} with a descriptive message,
 * matching the convention used by {@link CopyHelpers} and
 * {@link CopyTreeWalker}.</p>
 */
public final class MarkdownFileScanner {

    private MarkdownFileScanner() {
        // utility class — no instantiation
    }

    /**
     * Lists regular {@code .md} files directly under
     * {@code dir} in filesystem iteration order.
     *
     * @param dir directory to scan (non-recursive)
     * @return immutable list of matching files; empty list
     *         if the directory does not exist or is not a
     *         directory
     * @throws UncheckedIOException if listing fails for
     *         reasons other than absence
     */
    public static List<Path> listMarkdownFiles(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString()
                            .endsWith(".md"))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: %s"
                            .formatted(dir), e);
        }
    }

    /**
     * Lists regular {@code .md} files directly under
     * {@code dir} sorted alphabetically by full path.
     *
     * @param dir directory to scan (non-recursive)
     * @return immutable sorted list of matching files;
     *         empty list if the directory does not exist or
     *         is not a directory
     * @throws UncheckedIOException if listing fails for
     *         reasons other than absence
     */
    public static List<Path> listMarkdownFilesSorted(
            Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString()
                            .endsWith(".md"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: %s"
                            .formatted(dir), e);
        }
    }
}
