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
import java.util.Optional;

/**
 * Utilities for copying template files with placeholder
 * replacement during artifact generation.
 *
 * <p>File tree walking, deletion, and section validation
 * are in {@link CopyTreeWalker}.</p>
 *
 * @see Assembler
 * @see CopyTreeWalker
 */
public final class CopyHelpers {

    private CopyHelpers() {
        // Utility class — no instantiation
    }

    /**
     * Copies a single template file with placeholder
     * replacement.
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
     * @return Optional containing the destination path,
     *         or empty if source is missing
     */
    public static Optional<String> copyTemplateFileIfExists(
            Path src,
            Path dest,
            TemplateEngine engine,
            Map<String, Object> context) {
        if (!Files.exists(src)) {
            return Optional.empty();
        }
        return Optional.of(
                copyTemplateFile(src, dest, engine, context));
    }

    /**
     * Copies a file without any template rendering.
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

    /** Delegates to {@link CopyTreeWalker}. */
    public static void replacePlaceholdersInDir(
            Path directory,
            TemplateEngine engine,
            Map<String, Object> context) {
        CopyTreeWalker.replacePlaceholdersInDir(
                directory, engine, context);
    }

    /** Delegates to {@link CopyTreeWalker}. */
    public static List<Path> listMdFilesSorted(Path dir) {
        return CopyTreeWalker.listMdFilesSorted(dir);
    }

    /** Delegates to {@link CopyTreeWalker}. */
    public static boolean deleteQuietly(Path path) {
        return CopyTreeWalker.deleteQuietly(path);
    }

    /** Delegates to {@link CopyTreeWalker}. */
    public static boolean hasAllMandatorySections(
            String content, List<String> sections) {
        return CopyTreeWalker.hasAllMandatorySections(
                content, sections);
    }
}
