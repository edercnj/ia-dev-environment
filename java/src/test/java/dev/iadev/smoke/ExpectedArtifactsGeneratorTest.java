package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ExpectedArtifactsGenerator}.
 *
 * <p>Validates manifest generation from pipeline output
 * for all 12 bundled profiles.</p>
 */
@DisplayName("ExpectedArtifactsGenerator")
class ExpectedArtifactsGeneratorTest {

    private static final int PROFILE_COUNT = 12;

    @Nested
    @DisplayName("generate")
    class Generate {

        @Test
        @DisplayName("produces manifest with 8 profiles")
        void generate_allProfiles_producesManifest(
                @TempDir Path tempDir) throws IOException {
            Path output = tempDir.resolve(
                    "expected-artifacts.json");

            ExpectedArtifactsGenerator.generate(output);

            assertThat(output).exists();
            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(output);
            assertThat(artifacts.getProfileNames())
                    .hasSize(PROFILE_COUNT);
        }

        @Test
        @DisplayName(
                "each profile has positive file count")
        void generate_eachProfile_hasPositiveTotalFiles(
                @TempDir Path tempDir) throws IOException {
            Path output = tempDir.resolve(
                    "expected-artifacts.json");
            ExpectedArtifactsGenerator.generate(output);

            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(output);

            for (String name
                    : artifacts.getProfileNames()) {
                ProfileArtifacts profile =
                        artifacts.getProfile(name);
                assertThat(profile.totalFiles())
                        .as("totalFiles for %s", name)
                        .isPositive();
            }
        }

        @Test
        @DisplayName(
                "each profile has non-empty directories")
        void generate_eachProfile_hasDirectories(
                @TempDir Path tempDir) throws IOException {
            Path output = tempDir.resolve(
                    "expected-artifacts.json");
            ExpectedArtifactsGenerator.generate(output);

            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(output);

            for (String name
                    : artifacts.getProfileNames()) {
                ProfileArtifacts profile =
                        artifacts.getProfile(name);
                assertThat(profile.directories())
                        .as("directories for %s", name)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName(
                "each profile has non-empty categories")
        void generate_eachProfile_hasCategories(
                @TempDir Path tempDir) throws IOException {
            Path output = tempDir.resolve(
                    "expected-artifacts.json");
            ExpectedArtifactsGenerator.generate(output);

            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(output);

            for (String name
                    : artifacts.getProfileNames()) {
                ProfileArtifacts profile =
                        artifacts.getProfile(name);
                assertThat(profile.categories())
                        .as("categories for %s", name)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("rejects null output path")
        void generate_nullPath_throwsException() {
            assertThatThrownBy(
                    () -> ExpectedArtifactsGenerator
                            .generate(null))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("path");
        }

        @Test
        @DisplayName(
                "manifest contains expected profile names")
        void generate_profileNames_matchBundled(
                @TempDir Path tempDir) throws IOException {
            Path output = tempDir.resolve(
                    "expected-artifacts.json");
            ExpectedArtifactsGenerator.generate(output);

            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(output);

            assertThat(artifacts.getProfileNames())
                    .containsExactlyInAnyOrder(
                            "go-gin", "java-quarkus",
                            "java-spring",
                            "java-spring-hexagonal",
                            "java-spring-cqrs-es",
                            "java-spring-event-driven",
                            "java-spring-fintech-pci",
                            "kotlin-ktor",
                            "python-click-cli",
                            "python-fastapi", "rust-axum",
                            "typescript-nestjs");
        }

        @Test
        @DisplayName("output JSON is well-formed")
        void generate_output_isValidJson(
                @TempDir Path tempDir) throws IOException {
            Path output = tempDir.resolve(
                    "expected-artifacts.json");
            ExpectedArtifactsGenerator.generate(output);

            String content = Files.readString(output);
            assertThat(content)
                    .startsWith("{")
                    .contains("\"profiles\"")
                    .contains("\"totalFiles\"")
                    .contains("\"directories\"")
                    .contains("\"categories\"");
        }
    }
}
