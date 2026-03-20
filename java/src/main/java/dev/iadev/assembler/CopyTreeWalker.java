package dev.iadev.assembler;

import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * File tree walking operations for copying and
 * placeholder replacement.
 *
 * <p>Extracted from {@link CopyHelpers} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see CopyHelpers
 */
public final class CopyTreeWalker {

    private CopyTreeWalker() {
        // utility class
    }

    /**
     * Replaces placeholders in all {@code .md} files within
     * a directory tree recursively.
     *
     * <p>Only processes files ending with {@code .md}.
     * Non-markdown files are left unchanged.</p>
     *
     * @param directory the root directory to process
     * @param engine    the template engine for replacement
     * @param context   the context map for placeholder values
     */
    public static void replacePlaceholdersInDir(
            Path directory,
            TemplateEngine engine,
            Map<String, Object> context) {
        try {
            Files.walkFileTree(directory,
                    new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attrs)
                        throws IOException {
                    if (file.toString().endsWith(".md")) {
                        String content = Files.readString(
                                file, StandardCharsets.UTF_8);
                        String replaced =
                                engine.replacePlaceholders(
                                        content, context);
                        Files.writeString(file, replaced,
                                StandardCharsets.UTF_8);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to replace placeholders in: %s"
                            .formatted(directory), e);
        }
    }

    /**
     * Lists all {@code .md} files in the given directory,
     * sorted by filename.
     *
     * <p>Only regular files are included; subdirectories
     * whose names end in {@code .md} are excluded.</p>
     *
     * @param dir directory to scan
     * @return sorted list of .md file paths; empty list
     *         if directory contains no .md files
     * @throws UncheckedIOException if I/O fails
     */
    public static List<Path> listMdFilesSorted(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(f -> f.toString()
                            .endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: %s"
                            .formatted(dir), e);
        }
    }

    /**
     * Deletes a file or directory without throwing
     * exceptions. For directories, deletes recursively.
     *
     * @param path path to delete
     * @return true if deleted successfully, false otherwise
     */
    public static boolean deleteQuietly(Path path) {
        try {
            if (!Files.exists(path)) {
                return false;
            }
            if (Files.isDirectory(path)) {
                deleteTreeQuietly(path);
            } else {
                Files.deleteIfExists(path);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void deleteTreeQuietly(Path dir)
            throws IOException {
        Files.walkFileTree(dir,
                new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs)
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

    /**
     * Checks whether a Markdown content string contains
     * all required H2 sections.
     *
     * @param content  Markdown content
     * @param sections list of expected section names
     *                 (without {@code "## "} prefix)
     * @return true if all sections are present
     */
    public static boolean hasAllMandatorySections(
            String content, List<String> sections) {
        return sections.stream()
                .allMatch(section ->
                        content.contains("## " + section));
    }
}
