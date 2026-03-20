package dev.iadev.assembler;

import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Utilities for copying template files with placeholder
 * replacement during artifact generation.
 *
 * <p>All operations use synchronous I/O by design. This module
 * is consumed exclusively by assemblers that run sequentially
 * in the pipeline.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * CopyHelpers.copyTemplateFile(
 *     src, dest, engine, context);
 * CopyHelpers.copyStaticFile(src, dest);
 * CopyHelpers.ensureDirectory(outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 */
public final class CopyHelpers {

    private CopyHelpers() {
        // Utility class — no instantiation
    }

    /**
     * Copies a single template file with placeholder
     * replacement.
     *
     * <p>Creates parent directories if needed. Reads the source
     * file, replaces {@code {{KEY}}} placeholders using the
     * engine and context, then writes the result to dest.</p>
     *
     * @param src     the source template file
     * @param dest    the destination file path
     * @param engine  the template engine for replacement
     * @param context the context map for placeholder values
     * @return the destination path as a string
     */
    public static String copyTemplateFile(
            Path src,
            Path dest,
            TemplateEngine engine,
            Map<String, Object> context) {
        try {
            Files.createDirectories(dest.getParent());
            String content = Files.readString(
                    src, StandardCharsets.UTF_8);
            String replaced = engine.replacePlaceholders(
                    content, context);
            Files.writeString(
                    dest, replaced, StandardCharsets.UTF_8);
            return dest.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to copy template: %s"
                            .formatted(src), e);
        }
    }

    /**
     * Copies a template file if the source exists.
     *
     * @param src     the source template file
     * @param dest    the destination file path
     * @param engine  the template engine for replacement
     * @param context the context map for placeholder values
     * @return the destination path, or null if source missing
     */
    public static String copyTemplateFileIfExists(
            Path src,
            Path dest,
            TemplateEngine engine,
            Map<String, Object> context) {
        if (!Files.exists(src)) {
            return null;
        }
        return copyTemplateFile(src, dest, engine, context);
    }

    /**
     * Copies a file without any template rendering.
     *
     * <p>Creates parent directories if needed.</p>
     *
     * @param src  the source file
     * @param dest the destination file
     * @return the destination path as a string
     */
    public static String copyStaticFile(
            Path src, Path dest) {
        try {
            Files.createDirectories(dest.getParent());
            Files.copy(src, dest,
                    StandardCopyOption.REPLACE_EXISTING);
            return dest.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to copy file: %s"
                            .formatted(src), e);
        }
    }

    /**
     * Copies a directory tree recursively.
     *
     * @param srcDir  the source directory
     * @param destDir the destination directory
     * @return the destination directory path as a string
     */
    public static String copyDirectory(
            Path srcDir, Path destDir) {
        try {
            Files.walkFileTree(srcDir,
                    new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(
                        Path dir,
                        BasicFileAttributes attrs)
                        throws IOException {
                    Path target = destDir.resolve(
                            srcDir.relativize(dir));
                    Files.createDirectories(target);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attrs)
                        throws IOException {
                    Files.copy(file,
                            destDir.resolve(
                                    srcDir.relativize(file)),
                            StandardCopyOption
                                    .REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
            return destDir.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to copy directory: %s"
                            .formatted(srcDir), e);
        }
    }

    /**
     * Writes content to a file, creating parent directories
     * if needed.
     *
     * @param path    target file path
     * @param content file content (UTF-8)
     * @throws UncheckedIOException if I/O fails
     */
    public static void writeFile(
            Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write file: %s"
                            .formatted(path), e);
        }
    }

    /**
     * Reads entire file content as a UTF-8 string.
     *
     * @param path source file path
     * @return file content
     * @throws UncheckedIOException if I/O fails
     */
    public static String readFile(Path path) {
        try {
            return Files.readString(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read file: %s"
                            .formatted(path), e);
        }
    }

    /**
     * Creates a directory and all parent directories.
     *
     * <p>Idempotent — does nothing if the directory already
     * exists.</p>
     *
     * @param dir the directory to create
     */
    public static void ensureDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to create directory: %s"
                            .formatted(dir), e);
        }
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
