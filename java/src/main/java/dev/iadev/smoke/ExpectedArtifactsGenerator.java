package dev.iadev.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates the expected artifacts manifest by running
 * the pipeline for all 8 bundled profiles and collecting
 * file counts, directories, and categories from the
 * output.
 *
 * <p>Similar to {@code GoldenFileRegenerator} in purpose:
 * regenerate the manifest when the pipeline changes
 * legitimately.</p>
 *
 * <p>Usage:
 * <pre>{@code
 * ExpectedArtifactsGenerator.generate(
 *     Path.of("expected-artifacts.json"));
 * }</pre>
 * </p>
 */
public final class ExpectedArtifactsGenerator {

    private static final List<String> PROFILES = List.of(
            "go-gin", "java-quarkus", "java-spring",
            "kotlin-ktor", "python-click-cli",
            "python-fastapi", "rust-axum",
            "typescript-nestjs");

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .enable(SerializationFeature
                            .INDENT_OUTPUT);

    private ExpectedArtifactsGenerator() {
    }

    /**
     * Generates the manifest JSON at the given path.
     *
     * <p>Runs the pipeline for each of the 8 profiles,
     * collects output metrics, and writes them as JSON.</p>
     *
     * @param outputPath target file path for the JSON
     * @throws IllegalArgumentException if outputPath is
     *         null
     * @throws IOException if file operations fail
     */
    public static void generate(Path outputPath)
            throws IOException {
        if (outputPath == null) {
            throw new IllegalArgumentException(
                    "Output path must not be null");
        }

        Map<String, Object> manifest =
                new LinkedHashMap<>();
        Map<String, Object> profilesMap =
                new LinkedHashMap<>();

        for (String profile : PROFILES) {
            profilesMap.put(profile,
                    generateForProfile(profile));
        }

        manifest.put("profiles", profilesMap);

        if (outputPath.getParent() != null) {
            Files.createDirectories(
                    outputPath.getParent());
        }
        MAPPER.writeValue(outputPath.toFile(), manifest);
    }

    /**
     * Runs as standalone tool to regenerate the manifest.
     *
     * @param args optional: output path (defaults to
     *             src/test/resources/smoke/expected
     *             -artifacts.json)
     * @throws IOException if file operations fail
     */
    public static void main(String[] args)
            throws IOException {
        Path output = args.length > 0
                ? Path.of(args[0])
                : Path.of("src/test/resources/smoke/"
                        + "expected-artifacts.json");
        generate(output);
    }

    private static Map<String, Object> generateForProfile(
            String profile) throws IOException {
        Path tempDir = Files.createTempDirectory(
                "manifest-gen-",
                PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString(
                                "rwx------")));
        try {
            runPipeline(profile, tempDir);
            return collectMetrics(tempDir);
        } finally {
            deleteTree(tempDir);
        }
    }

    private static void runPipeline(
            String profile, Path outputDir) {
        ProjectConfig config =
                ConfigProfiles.getStack(profile);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(
                        AssemblerPipeline
                                .buildAssemblers());
        PipelineOptions options = new PipelineOptions(
                false, true, false, null);

        PipelineResult result = pipeline.runPipeline(
                config, outputDir, options);
        if (!result.success()) {
            throw new IllegalStateException(
                    "Pipeline failed for profile: "
                            + profile);
        }
    }

    private static Map<String, Object> collectMetrics(
            Path outputDir) throws IOException {
        Map<String, Object> metrics = new LinkedHashMap<>();

        int totalFiles = countFiles(outputDir);
        List<String> directories =
                listDirectories(outputDir);
        Map<String, Integer> categories =
                categorizeFiles(outputDir);

        metrics.put("totalFiles", totalFiles);
        metrics.put("directories", directories);
        metrics.put("categories", categories);
        return metrics;
    }

    private static int countFiles(Path dir)
            throws IOException {
        int[] count = {0};
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

    private static List<String> listDirectories(Path dir)
            throws IOException {
        Set<String> dirs = new TreeSet<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(
                    Path d,
                    BasicFileAttributes attrs) {
                if (!d.equals(dir)) {
                    dirs.add(dir.relativize(d)
                            .toString()
                            .replace('\\', '/'));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return List.copyOf(dirs);
    }

    private static Map<String, Integer> categorizeFiles(
            Path dir) throws IOException {
        Map<String, Integer> categories =
                new LinkedHashMap<>();

        countCategory(categories, dir,
                "codex-skills", ".agents/skills");
        countCategory(categories, dir,
                "claude-rules", ".claude/rules");
        countCategory(categories, dir,
                "claude-skills", ".claude/skills");
        countCategory(categories, dir,
                "claude-agents", ".claude/agents");
        countCategory(categories, dir,
                "claude-hooks", ".claude/hooks");
        countCategory(categories, dir,
                "claude-settings", ".claude",
                true);
        countCategory(categories, dir,
                "codex-config", ".codex");
        countCategory(categories, dir,
                "github-instructions",
                ".github/instructions");
        countCategory(categories, dir,
                "github-skills", ".github/skills");
        countCategory(categories, dir,
                "github-agents", ".github/agents");
        countCategory(categories, dir,
                "github-prompts", ".github/prompts");
        countCategory(categories, dir,
                "github-hooks", ".github/hooks");
        countCategory(categories, dir,
                "github-issue-templates",
                ".github/ISSUE_TEMPLATE");
        countCategory(categories, dir,
                "github-top", ".github", true);
        countCategory(categories, dir,
                "docs", "docs");
        countCategory(categories, dir, "k8s", "k8s");
        countCategory(categories, dir,
                "tests", "tests");
        countCategory(categories, dir,
                "root-files", "", true);

        categories.entrySet()
                .removeIf(e -> e.getValue() == 0);

        return categories;
    }

    private static void countCategory(
            Map<String, Integer> categories,
            Path baseDir,
            String categoryName,
            String subPath) throws IOException {
        countCategory(categories, baseDir,
                categoryName, subPath, false);
    }

    private static void countCategory(
            Map<String, Integer> categories,
            Path baseDir,
            String categoryName,
            String subPath,
            boolean topLevelOnly) throws IOException {
        Path targetDir = subPath.isEmpty()
                ? baseDir
                : baseDir.resolve(subPath);
        if (!Files.isDirectory(targetDir)) {
            return;
        }

        int count;
        if (topLevelOnly) {
            count = countTopLevelFiles(targetDir);
        } else {
            count = countFiles(targetDir);
        }
        categories.put(categoryName, count);
    }

    private static int countTopLevelFiles(Path dir)
            throws IOException {
        int count = 0;
        try (var stream = Files.list(dir)) {
            for (Path p : stream.toList()) {
                if (Files.isRegularFile(p)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void deleteTree(Path dir)
            throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
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
}
