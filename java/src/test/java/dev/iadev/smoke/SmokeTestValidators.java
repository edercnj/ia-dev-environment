package dev.iadev.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.fail;

/**
 * Static validation utilities for smoke tests.
 *
 * <p>Each method performs a specific structural or content
 * integrity check on pipeline output directories. Methods
 * throw {@link AssertionError} on validation failure with
 * descriptive messages including file paths and details.</p>
 *
 * @see SmokeTestBase
 */
public final class SmokeTestValidators {

    private static final ObjectMapper JSON_MAPPER =
            new ObjectMapper();

    private SmokeTestValidators() {
        // utility class
    }

    /**
     * Asserts that no file under {@code outputDir} has zero
     * bytes. Passes if the directory contains no files.
     *
     * @param outputDir the directory to scan recursively
     * @throws AssertionError if any file has 0 bytes
     * @throws IOException    if directory traversal fails
     */
    public static void assertNoEmptyFiles(Path outputDir)
            throws IOException {
        List<String> emptyFiles = new ArrayList<>();

        Files.walkFileTree(outputDir,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attrs)
                            throws IOException {
                        if (attrs.size() == 0) {
                            emptyFiles.add(
                                    outputDir.relativize(file)
                                            .toString()
                                            .replace('\\',
                                                    '/'));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

        if (!emptyFiles.isEmpty()) {
            fail("Found %d empty file(s):\n  - %s"
                    .formatted(
                            emptyFiles.size(),
                            String.join("\n  - ",
                                    emptyFiles)));
        }
    }

    /**
     * Asserts that no file under {@code outputDir} contains
     * unresolved placeholders matching any of the given regex
     * patterns.
     *
     * @param outputDir the directory to scan recursively
     * @param patterns  regex patterns to detect (e.g.,
     *                  {@code \{\{.*?\}\}})
     * @throws AssertionError if any placeholder is found
     * @throws IOException    if file reading fails
     */
    public static void assertNoUnresolvedPlaceholders(
            Path outputDir, Set<String> patterns)
            throws IOException {
        List<Pattern> compiled = patterns.stream()
                .map(Pattern::compile)
                .toList();

        List<String> violations = new ArrayList<>();

        Files.walkFileTree(outputDir,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attrs)
                            throws IOException {
                        scanFileForPlaceholders(
                                file, outputDir,
                                compiled, violations);
                        return FileVisitResult.CONTINUE;
                    }
                });

        if (!violations.isEmpty()) {
            fail("Found %d unresolved placeholder(s):\n%s"
                    .formatted(
                            violations.size(),
                            String.join("\n",
                                    violations)));
        }
    }

    /**
     * Asserts that all directories in {@code expectedDirs}
     * exist as immediate or nested children of
     * {@code outputDir}.
     *
     * @param outputDir    the root output directory
     * @param expectedDirs relative directory paths expected
     *                     to exist
     * @throws AssertionError if any directory is missing
     */
    public static void assertDirectoryStructure(
            Path outputDir, Set<String> expectedDirs) {
        List<String> missing = expectedDirs.stream()
                .filter(dir -> !Files.isDirectory(
                        outputDir.resolve(dir)))
                .sorted()
                .toList();

        if (!missing.isEmpty()) {
            fail(("Missing %d expected directory(ies):"
                    + "\n  - %s")
                    .formatted(
                            missing.size(),
                            String.join(
                                    "\n  - ", missing)));
        }
    }

    /**
     * Asserts that the total file count under
     * {@code outputDir} equals {@code expectedCount}.
     *
     * @param outputDir     the directory to count files in
     * @param expectedCount the expected number of files
     * @throws AssertionError if the count does not match
     * @throws IOException    if directory traversal fails
     */
    public static void assertFileCount(
            Path outputDir, int expectedCount)
            throws IOException {
        long actualCount = countFiles(outputDir);

        if (actualCount != expectedCount) {
            fail("Expected %d files but found %d"
                    .formatted(
                            expectedCount, actualCount));
        }
    }

    /**
     * Asserts that the given file is valid, parseable YAML.
     *
     * @param file the YAML file to validate
     * @throws AssertionError if the file is not valid YAML
     * @throws IOException    if file reading fails
     */
    public static void assertValidYaml(Path file)
            throws IOException {
        String content = Files.readString(
                file, StandardCharsets.UTF_8);
        try {
            new Yaml(new SafeConstructor(
                    new LoaderOptions())).load(content);
        } catch (Exception e) {
            fail("Invalid YAML in %s: %s"
                    .formatted(
                            file.getFileName(),
                            e.getMessage()));
        }
    }

    /**
     * Asserts that the given file is valid, parseable JSON.
     *
     * @param file the JSON file to validate
     * @throws AssertionError if the file is not valid JSON
     * @throws IOException    if file reading fails
     */
    public static void assertValidJson(Path file)
            throws IOException {
        String content = Files.readString(
                file, StandardCharsets.UTF_8);
        try {
            JSON_MAPPER.readTree(content);
        } catch (Exception e) {
            fail("Invalid JSON in %s: %s"
                    .formatted(
                            file.getFileName(),
                            e.getMessage()));
        }
    }

    private static void scanFileForPlaceholders(
            Path file, Path outputDir,
            List<Pattern> compiled,
            List<String> violations) throws IOException {
        String content = Files.readString(
                file, StandardCharsets.UTF_8);
        String relativePath = outputDir.relativize(file)
                .toString().replace('\\', '/');

        List<String> lines = content.lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (Pattern pattern : compiled) {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    violations.add(
                            "  %s:%d — %s"
                                    .formatted(
                                            relativePath,
                                            i + 1,
                                            matcher.group()));
                }
            }
        }
    }

    private static long countFiles(Path dir)
            throws IOException {
        long[] count = {0};
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs) {
                count[0]++;
                return FileVisitResult.CONTINUE;
            }
        });
        return count[0];
    }
}
