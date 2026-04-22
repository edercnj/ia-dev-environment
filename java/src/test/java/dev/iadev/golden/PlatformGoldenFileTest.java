package dev.iadev.golden;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerFactory;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Golden file tests for platform-filtered pipeline
 * output.
 *
 * <p>Validates that the pipeline output for the
 * java-spring profile with
 * {@code --platform claude-code} matches the golden
 * files stored in
 * {@code /golden/{profile}/platform-claude-code/}.</p>
 *
 * <p>RULE-001: existing golden files (representing
 * {@code --platform all}) remain untouched.</p>
 *
 * @see GoldenFileTest
 * @see GoldenFileDiffReporter
 */
@DisplayName("Platform Golden File Tests")
class PlatformGoldenFileTest {

    private static final String GOLDEN_ROOT =
            "/golden/";

    private static final String PLATFORM_SUBDIR =
            "platform-claude-code";

    @TempDir
    Path tempDir;

    static Stream<String> profiles() {
        return Stream.of("java-spring");
    }

    /**
     * Validates text-level parity between the
     * platform-filtered pipeline output and the golden
     * files for each profile (UTF-8 decoded comparison).
     *
     * @param profile the bundled stack profile name
     * @throws IOException if file reading fails
     */
    @ParameterizedTest(
            name = "{0} — claude-code golden parity")
    @MethodSource("profiles")
    void profile_claudeCode_matchesGoldenFiles(
            String profile) throws IOException {
        Path outputDir = tempDir.resolve(profile);
        Files.createDirectories(outputDir);

        ProjectConfig config =
                ConfigProfiles.getStack(profile);
        PipelineOptions options = new PipelineOptions(
                false, true, false, false,
                null,
                Set.of(Platform.CLAUDE_CODE));
        List<AssemblerDescriptor> assemblers =
                AssemblerFactory.buildAssemblers(options);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(assemblers);

        PipelineResult result = pipeline.runPipeline(
                config, outputDir, options);
        assertThat(result.success())
                .as("Pipeline must succeed for: %s",
                        profile)
                .isTrue();

        Path goldenDir = resolveGoldenDir(profile);
        if (goldenDir == null) {
            fail("Missing golden fixtures for profile"
                    + " '%s': expected test resource"
                    + " directory %s%s/%s/"
                    .formatted(profile, GOLDEN_ROOT,
                            profile, PLATFORM_SUBDIR));
        }

        Set<String> goldenFiles =
                GoldenFileTest.collectRelativePaths(
                        goldenDir);
        Set<String> generatedFiles =
                GoldenFileTest.collectRelativePaths(
                        outputDir);

        validateCompleteness(
                profile, goldenFiles, generatedFiles);
        validateContent(
                profile, goldenDir, outputDir,
                goldenFiles);
    }

    private Path resolveGoldenDir(String profile) {
        var url = getClass().getResource(
                GOLDEN_ROOT + profile
                        + "/" + PLATFORM_SUBDIR);
        if (url == null) {
            return null;
        }
        try {
            return Path.of(url.toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(
                    "Invalid golden dir URI: " + url, e);
        }
    }

    private void validateCompleteness(
            String profile,
            Set<String> goldenFiles,
            Set<String> generatedFiles) {
        Set<String> missing = new TreeSet<>(goldenFiles);
        missing.removeAll(generatedFiles);

        if (!missing.isEmpty()) {
            fail("[%s] %d golden file(s) missing in "
                    + "generated output:\n  %s"
                    .formatted(
                            profile,
                            missing.size(),
                            String.join("\n  ", missing)));
        }
    }

    private void validateContent(
            String profile,
            Path goldenDir,
            Path outputDir,
            Set<String> goldenFiles) throws IOException {
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
            fail("[%s] %d file(s) differ:\n\n%s"
                    .formatted(
                            profile,
                            mismatches.size(),
                            String.join(
                                    "\n\n", mismatches)));
        }
    }
}
