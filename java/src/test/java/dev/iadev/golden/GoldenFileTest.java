package dev.iadev.golden;

import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Golden file tests validating byte-for-byte parity between
 * Java pipeline output and TypeScript golden files for all 17
 * bundled profiles.
 *
 * <p>For each profile, the test:
 * <ol>
 *   <li>Loads the config via {@link ConfigProfiles}</li>
 *   <li>Runs the full pipeline into a temp directory</li>
 *   <li>Compares every generated file with its golden
 *       counterpart</li>
 *   <li>Validates file count and directory structure</li>
 *   <li>Reports detailed diffs on mismatch via
 *       {@link GoldenFileDiffReporter}</li>
 * </ol>
 *
 * <p>RULE-001: byte-for-byte parity between Java and
 * TypeScript output is the acceptance criterion.
 *
 * @see GoldenFileDiffReporter
 * @see AssemblerPipeline
 */
@DisplayName("Golden File Parity Tests")
class GoldenFileTest {

    private static final String GOLDEN_ROOT =
            "/golden/";

    @TempDir
    Path tempDir;

    /**
     * Provides the 17 profile names for parameterized tests.
     *
     * @return stream of profile name strings
     */
    static Stream<String> profiles() {
        return Stream.of(
                "go-gin",
                "java-quarkus",
                "java-spring",
                "java-spring-clickhouse",
                "java-spring-cqrs-es",
                "java-spring-elasticsearch",
                "java-spring-event-driven",
                "java-spring-fintech-pci",
                "java-spring-hexagonal",
                "java-spring-neo4j",
                "kotlin-ktor",
                "python-click-cli",
                "python-fastapi",
                "python-fastapi-timescale",
                "rust-axum",
                "typescript-commander-cli",
                "typescript-nestjs"
        );
    }

    /**
     * Validates that the pipeline output for the given profile
     * matches the golden files byte-for-byte.
     *
     * @param profile the bundled stack profile name
     * @throws IOException if file reading fails
     */
    @ParameterizedTest(
            name = "{0} — byte-for-byte parity")
    @MethodSource("profiles")
    void profile_whenCalled_matchesGoldenFiles(String profile)
            throws IOException {
        Path outputDir = tempDir.resolve(profile);
        Files.createDirectories(outputDir);

        ProjectConfig config =
                ConfigProfiles.getStack(profile);

        AssemblerPipeline pipeline = new AssemblerPipeline(
                AssemblerPipeline.buildAssemblers());
        PipelineOptions options = new PipelineOptions(
                false, true, false, null);

        PipelineResult result = pipeline.runPipeline(
                config, outputDir, options);
        assertThat(result.success())
                .as("Pipeline must succeed for profile: "
                        + profile)
                .isTrue();

        Path goldenDir = resolveGoldenDir(profile);
        Set<String> goldenFiles =
                collectRelativePaths(goldenDir);
        Set<String> generatedFiles =
                collectRelativePaths(outputDir);

        validateCompleteness(
                profile, goldenFiles, generatedFiles);

        List<String> mismatches = new ArrayList<>();
        for (String relativePath : goldenFiles) {
            Path goldenFile =
                    goldenDir.resolve(relativePath);
            Path generatedFile =
                    outputDir.resolve(relativePath);

            if (!Files.exists(generatedFile)) {
                mismatches.add(
                        "MISSING: " + relativePath);
                continue;
            }

            String goldenContent = Files.readString(
                    goldenFile, StandardCharsets.UTF_8);
            String generatedContent = Files.readString(
                    generatedFile, StandardCharsets.UTF_8);

            if (!goldenContent.equals(generatedContent)) {
                String diff =
                        GoldenFileDiffReporter.generateDiff(
                                relativePath,
                                goldenContent,
                                generatedContent);
                mismatches.add(diff);
            }
        }

        if (!mismatches.isEmpty()) {
            fail("[%s] %d file(s) differ:\n\n%s".formatted(
                    profile,
                    mismatches.size(),
                    String.join("\n\n", mismatches)));
        }
    }

    private Path resolveGoldenDir(String profile) {
        var url = getClass().getResource(
                GOLDEN_ROOT + profile);
        assertThat(url)
                .as("Golden dir must exist for: " + profile)
                .satisfies(u -> assertThat(
                        u.toString())
                        .contains(profile));
        return Path.of(url.getPath());
    }

    private void validateCompleteness(
            String profile,
            Set<String> goldenFiles,
            Set<String> generatedFiles) {
        Set<String> missing = new TreeSet<>(goldenFiles);
        missing.removeAll(generatedFiles);

        Set<String> extra = new TreeSet<>(generatedFiles);
        extra.removeAll(goldenFiles);

        if (!missing.isEmpty() || !extra.isEmpty()) {
            fail(buildMismatchMessage(
                    profile, goldenFiles.size(),
                    generatedFiles.size(),
                    missing, extra));
        }
    }

    private String buildMismatchMessage(
            String profile, int goldenCount,
            int generatedCount,
            Set<String> missing, Set<String> extra) {
        StringBuilder msg = new StringBuilder();
        msg.append("[").append(profile)
                .append("] File count mismatch: golden=")
                .append(goldenCount)
                .append(" generated=")
                .append(generatedCount).append("\n");
        appendFileList(
                msg, missing,
                "Missing files (in golden, "
                        + "not generated):");
        appendFileList(
                msg, extra,
                "Extra files (generated, "
                        + "not in golden):");
        return msg.toString();
    }

    private void appendFileList(
            StringBuilder msg,
            Set<String> files, String header) {
        if (!files.isEmpty()) {
            msg.append(header).append("\n");
            files.forEach(f ->
                    msg.append("  - ").append(f)
                            .append("\n"));
        }
    }

    /**
     * Collects all relative file paths under a directory,
     * sorted for deterministic comparison.
     *
     * @param dir the root directory to scan
     * @return sorted set of relative paths as strings
     * @throws IOException if directory traversal fails
     */
    static Set<String> collectRelativePaths(Path dir)
            throws IOException {
        Set<String> paths = new TreeSet<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs) {
                paths.add(
                        dir.relativize(file)
                                .toString()
                                .replace('\\', '/'));
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }
}
