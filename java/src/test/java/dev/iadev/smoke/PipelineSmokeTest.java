package dev.iadev.smoke;

import dev.iadev.domain.model.PipelineResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parametrized smoke test that runs the full pipeline
 * for all 8 bundled profiles and validates structural
 * properties against the expected artifacts manifest.
 *
 * <p>Validates per profile:</p>
 * <ul>
 *   <li>Pipeline executes successfully</li>
 *   <li>File count matches manifest</li>
 *   <li>Directory structure matches manifest</li>
 *   <li>Category file counts match manifest</li>
 *   <li>No empty files exist</li>
 *   <li>No critical warnings emitted</li>
 * </ul>
 *
 * <p>RULE-001: Parametrized for all 8 profiles.</p>
 * <p>RULE-002: Independent of golden files.</p>
 * <p>RULE-006: Output in {@code @TempDir}.</p>
 *
 * @see SmokeTestBase
 * @see SmokeProfiles
 * @see ExpectedArtifacts
 */
class PipelineSmokeTest extends SmokeTestBase {

    private static final String MANIFEST_RESOURCE =
            "smoke/expected-artifacts.json";

    static Stream<String> profiles() {
        return SmokeProfiles.profiles();
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void pipeline_executesSuccessfully_forProfile(
            String profile) {
        PipelineResult result = runPipeline(profile);

        assertThat(result.success())
                .as("Pipeline must succeed for: %s",
                        profile)
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void pipeline_fileCountMatchesManifest_forProfile(
            String profile) throws IOException {
        runPipeline(profile);
        Path outputDir = getOutputDir(profile);

        ExpectedArtifacts manifest =
                ExpectedArtifacts.loadFromClasspath(
                        MANIFEST_RESOURCE);
        ProfileArtifacts expected =
                manifest.getProfile(profile);

        long actualCount =
                SmokeTestValidators.countFiles(outputDir);
        int expectedCount = expected.totalFiles();

        if (actualCount != expectedCount) {
            String diagnostic = buildFileCountDiagnostic(
                    profile, expectedCount, actualCount,
                    outputDir, expected);
            assertThat(actualCount)
                    .as("File count for %s.\n%s",
                            profile, diagnostic)
                    .isEqualTo(expectedCount);
        }
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void pipeline_directoryStructureMatchesManifest_forProfile(
            String profile) throws IOException {
        runPipeline(profile);
        Path outputDir = getOutputDir(profile);

        ExpectedArtifacts manifest =
                ExpectedArtifacts.loadFromClasspath(
                        MANIFEST_RESOURCE);
        ProfileArtifacts expected =
                manifest.getProfile(profile);

        Set<String> expectedDirs =
                new HashSet<>(expected.directories());

        SmokeTestValidators.assertDirectoryStructure(
                outputDir, expectedDirs);
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void pipeline_categoryCountsMatchManifest_forProfile(
            String profile) throws IOException {
        runPipeline(profile);
        Path outputDir = getOutputDir(profile);

        ExpectedArtifacts manifest =
                ExpectedArtifacts.loadFromClasspath(
                        MANIFEST_RESOURCE);
        ProfileArtifacts expected =
                manifest.getProfile(profile);

        Map<String, Integer> expectedCategories =
                expected.categories();

        List<String> mismatches = new ArrayList<>();
        for (var entry : expectedCategories.entrySet()) {
            String category = entry.getKey();
            int expectedCount = entry.getValue();
            int actualCount = countCategoryFiles(
                    outputDir, category);

            if (actualCount != expectedCount) {
                mismatches.add(
                        "  %s: expected=%d, actual=%d"
                                .formatted(category,
                                        expectedCount,
                                        actualCount));
            }
        }

        assertThat(mismatches)
                .as("Category count mismatches for %s",
                        profile)
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void pipeline_noEmptyFiles_forProfile(
            String profile) throws IOException {
        runPipeline(profile);
        Path outputDir = getOutputDir(profile);

        SmokeTestValidators.assertNoEmptyFiles(outputDir);
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void pipeline_noCriticalWarnings_forProfile(
            String profile) {
        PipelineResult result = runPipeline(profile);

        List<String> criticalWarnings =
                result.warnings().stream()
                        .filter(PipelineSmokeTest
                                ::isCriticalWarning)
                        .toList();

        assertThat(criticalWarnings)
                .as("Critical warnings for %s", profile)
                .isEmpty();
    }

    private static boolean isCriticalWarning(
            String warning) {
        String lower = warning.toLowerCase();
        return lower.contains("error")
                || lower.contains("fatal")
                || lower.contains("failed")
                || lower.contains("missing required");
    }

    private String buildFileCountDiagnostic(
            String profile,
            int expectedCount,
            long actualCount,
            Path outputDir,
            ProfileArtifacts expected) throws IOException {
        Set<String> actualFiles =
                collectRelativeFilePaths(outputDir);
        Set<String> expectedDirs =
                new HashSet<>(expected.directories());

        int delta = (int) (actualCount - expectedCount);
        StringBuilder sb = new StringBuilder();
        sb.append("Profile: %s\n".formatted(profile));
        sb.append("Expected: %d files\n"
                .formatted(expectedCount));
        sb.append("Actual: %d files\n"
                .formatted(actualCount));
        sb.append("Delta: %+d\n".formatted(delta));

        if (actualCount > expectedCount) {
            sb.append("Extra files (not in expected):\n");
            int shown = 0;
            for (String file : actualFiles) {
                if (shown >= 20) {
                    sb.append("  ... and %d more\n"
                            .formatted(
                                    actualFiles.size()
                                            - shown));
                    break;
                }
                sb.append("  + %s\n".formatted(file));
                shown++;
            }
        }

        appendCategoryDelta(sb, outputDir, expected);
        return sb.toString();
    }

    private void appendCategoryDelta(
            StringBuilder sb,
            Path outputDir,
            ProfileArtifacts expected)
            throws IOException {
        sb.append("Category deltas:\n");
        for (var entry
                : expected.categories().entrySet()) {
            String category = entry.getKey();
            int expectedCatCount = entry.getValue();
            int actualCatCount = countCategoryFiles(
                    outputDir, category);
            if (actualCatCount != expectedCatCount) {
                sb.append(("  %s: %+d (expected=%d,"
                        + " actual=%d)\n")
                        .formatted(
                                category,
                                actualCatCount
                                        - expectedCatCount,
                                expectedCatCount,
                                actualCatCount));
            }
        }
    }

    private static Set<String> collectRelativeFilePaths(
            Path outputDir) throws IOException {
        Set<String> paths = new TreeSet<>();
        Files.walkFileTree(outputDir,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attrs) {
                        paths.add(
                                SmokeTestValidators
                                        .relativizePosix(
                                                outputDir,
                                                file));
                        return FileVisitResult.CONTINUE;
                    }
                });
        return paths;
    }

    @SuppressWarnings("java:S1479")
    private static int countCategoryFiles(
            Path outputDir, String category)
            throws IOException {
        return switch (category) {
            case "claude-rules" ->
                    countFilesInSubDir(outputDir,
                            ".claude/rules");
            case "claude-skills" ->
                    countFilesInSubDir(outputDir,
                            ".claude/skills");
            case "claude-agents" ->
                    countFilesInSubDir(outputDir,
                            ".claude/agents");
            case "claude-hooks" ->
                    countFilesInSubDir(outputDir,
                            ".claude/hooks");
            case "claude-settings" ->
                    countTopLevelFiles(outputDir
                            .resolve(".claude"));
            case "github-instructions" ->
                    countFilesInSubDir(outputDir,
                            ".github/instructions");
            case "github-skills" ->
                    countFilesInSubDir(outputDir,
                            ".github/skills");
            case "github-agents" ->
                    countFilesInSubDir(outputDir,
                            ".github/agents");
            case "github-prompts" ->
                    countFilesInSubDir(outputDir,
                            ".github/prompts");
            case "github-hooks" ->
                    countFilesInSubDir(outputDir,
                            ".github/hooks");
            case "github-issue-templates" ->
                    countFilesInSubDir(outputDir,
                            ".github/ISSUE_TEMPLATE");
            case "github-top" ->
                    countTopLevelFiles(outputDir
                            .resolve(".github"));
            case "steering" ->
                    countFilesInSubDir(outputDir,
                            "steering");
            case "adr" ->
                    countFilesInSubDir(outputDir,
                            "adr");
            case "contracts" ->
                    countFilesInSubDir(outputDir,
                            "contracts");
            case "results" ->
                    countFilesInSubDir(outputDir,
                            "results");
            case "specs" ->
                    countFilesInSubDir(outputDir,
                            "specs");
            case "plans" ->
                    countFilesInSubDir(outputDir,
                            "plans");
            case "k8s" ->
                    countFilesInSubDir(outputDir,
                            "k8s");
            case "tests" ->
                    countFilesInSubDir(outputDir,
                            "tests");
            case "root-files" ->
                    countTopLevelFiles(outputDir);
            default -> 0;
        };
    }

    private static int countFilesInSubDir(
            Path baseDir, String subPath)
            throws IOException {
        Path targetDir = baseDir.resolve(subPath);
        if (!Files.isDirectory(targetDir)) {
            return 0;
        }
        return (int) SmokeTestValidators
                .countFiles(targetDir);
    }

    private static int countTopLevelFiles(Path dir)
            throws IOException {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
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
}
