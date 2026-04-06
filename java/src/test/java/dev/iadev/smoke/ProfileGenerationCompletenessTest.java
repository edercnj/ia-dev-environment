package dev.iadev.smoke;

import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that validates every registered
 * profile generates successfully and produces the
 * expected output structure.
 *
 * <p>Unlike {@link PipelineSmokeTest} which only covers
 * the smoke-testable profiles (excluding the project's
 * own profile), this test covers ALL profiles registered
 * in {@link ConfigProfiles} — including
 * {@code java-picocli-cli}.</p>
 *
 * <p>Validates per profile:</p>
 * <ul>
 *   <li>Pipeline executes without errors</li>
 *   <li>Output has a positive file count</li>
 *   <li>Mandatory directories exist (.claude, .github)</li>
 *   <li>Mandatory files exist (CLAUDE.md, settings)</li>
 *   <li>No empty files in output</li>
 *   <li>File count is within sane bounds</li>
 * </ul>
 *
 * @see ConfigProfiles
 * @see SmokeTestBase
 */
@DisplayName("Profile Generation Completeness")
class ProfileGenerationCompletenessTest
        extends SmokeTestBase {

    private static final int MIN_FILES_ANY_PROFILE = 200;
    private static final int MAX_FILES_ANY_PROFILE = 1500;

    static Stream<String> allRegisteredProfiles() {
        return ConfigProfiles.getAvailableStacks()
                .stream();
    }

    @Nested
    @DisplayName("Pipeline execution")
    class PipelineExecution {

        @ParameterizedTest(name = "{0}")
        @MethodSource("dev.iadev.smoke"
                + ".ProfileGenerationCompletenessTest"
                + "#allRegisteredProfiles")
        @DisplayName("succeeds for every profile")
        void pipeline_succeeds_forProfile(
                String profile) {
            PipelineResult result = runPipeline(profile);

            assertThat(result.success())
                    .as("Pipeline must succeed for: %s",
                            profile)
                    .isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dev.iadev.smoke"
                + ".ProfileGenerationCompletenessTest"
                + "#allRegisteredProfiles")
        @DisplayName(
                "produces positive file count")
        void pipeline_producesFiles_forProfile(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            long fileCount =
                    SmokeTestValidators.countFiles(
                            outputDir);

            assertThat(fileCount)
                    .as("File count for %s", profile)
                    .isGreaterThan(0);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dev.iadev.smoke"
                + ".ProfileGenerationCompletenessTest"
                + "#allRegisteredProfiles")
        @DisplayName(
                "file count is within sane bounds")
        void pipeline_fileCountInBounds_forProfile(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            long fileCount =
                    SmokeTestValidators.countFiles(
                            outputDir);

            assertThat(fileCount)
                    .as("File count for %s must be "
                                    + "between %d and %d",
                            profile,
                            MIN_FILES_ANY_PROFILE,
                            MAX_FILES_ANY_PROFILE)
                    .isBetween(
                            (long) MIN_FILES_ANY_PROFILE,
                            (long) MAX_FILES_ANY_PROFILE);
        }
    }

    @Nested
    @DisplayName("Mandatory structure")
    class MandatoryStructure {

        @ParameterizedTest(name = "{0}")
        @MethodSource("dev.iadev.smoke"
                + ".ProfileGenerationCompletenessTest"
                + "#allRegisteredProfiles")
        @DisplayName(
                "has mandatory directories")
        void profile_hasMandatoryDirs(
                String profile) {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            List<String> mandatoryDirs = List.of(
                    ".claude",
                    ".claude/rules",
                    ".claude/skills",
                    ".claude/agents",
                    ".github",
                    ".github/agents",
                    ".github/instructions",
                    ".github/skills");

            List<String> missing = mandatoryDirs.stream()
                    .filter(dir -> !Files.isDirectory(
                            outputDir.resolve(dir)))
                    .toList();

            assertThat(missing)
                    .as("Missing mandatory directories "
                            + "for %s", profile)
                    .isEmpty();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dev.iadev.smoke"
                + ".ProfileGenerationCompletenessTest"
                + "#allRegisteredProfiles")
        @DisplayName(
                "has mandatory root files")
        void profile_hasMandatoryFiles(
                String profile) {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            List<String> mandatoryFiles = List.of(
                    ".claude/README.md",
                    ".claude/settings.json",
                    ".claude/settings.local.json");

            List<String> missing = mandatoryFiles.stream()
                    .filter(f -> !Files.isRegularFile(
                            outputDir.resolve(f)))
                    .toList();

            assertThat(missing)
                    .as("Missing mandatory files "
                            + "for %s", profile)
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Output quality")
    class OutputQuality {

        @ParameterizedTest(name = "{0}")
        @MethodSource("dev.iadev.smoke"
                + ".ProfileGenerationCompletenessTest"
                + "#allRegisteredProfiles")
        @DisplayName("no empty files in output")
        void profile_noEmptyFiles(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            SmokeTestValidators.assertNoEmptyFiles(
                    outputDir);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dev.iadev.smoke"
                + ".ProfileGenerationCompletenessTest"
                + "#allRegisteredProfiles")
        @DisplayName(
                "no critical warnings emitted")
        void profile_noCriticalWarnings(
                String profile) {
            PipelineResult result = runPipeline(profile);

            List<String> critical =
                    result.warnings().stream()
                            .filter(w -> {
                                String l = w.toLowerCase();
                                return l.contains("error")
                                        || l.contains("fatal")
                                        || l.contains(
                                        "failed");
                            })
                            .toList();

            assertThat(critical)
                    .as("Critical warnings for %s",
                            profile)
                    .isEmpty();
        }
    }
}
