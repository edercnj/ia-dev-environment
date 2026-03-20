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
import java.util.Map;

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
                    "Failed to copy template: " + src, e);
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
                    "Failed to copy file: " + src, e);
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
                    "Failed to copy directory: " + srcDir, e);
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
                    "Failed to write file: " + path, e);
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
                    "Failed to read file: " + path, e);
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
                    "Failed to create directory: " + dir, e);
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
                    "Failed to replace placeholders in: "
                            + directory, e);
        }
    }
}
