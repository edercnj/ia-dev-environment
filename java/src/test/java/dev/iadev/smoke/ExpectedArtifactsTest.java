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
 * Unit tests for {@link ExpectedArtifacts}.
 *
 * <p>Validates loading from JSON, profile access,
 * error handling for null/invalid inputs, and
 * profile name retrieval.</p>
 */
@DisplayName("ExpectedArtifacts")
class ExpectedArtifactsTest {

    private static final String VALID_JSON = """
            {
              "profiles": {
                "java-quarkus": {
                  "totalFiles": 125,
                  "directories": [".claude", "docs", "k8s"],
                  "categories": {
                    "codex-skills": 106,
                    "claude-rules": 6,
                    "docs": 5,
                    "k8s": 3
                  }
                },
                "go-gin": {
                  "totalFiles": 149,
                  "directories": [".claude", "docs", "k8s"],
                  "categories": {
                    "codex-skills": 98,
                    "docs": 5,
                    "k8s": 3
                  }
                }
              }
            }
            """;

    private static final String EMPTY_PROFILES_JSON = """
            {
              "profiles": {}
            }
            """;

    @Nested
    @DisplayName("load")
    class Load {

        @Test
        @DisplayName("rejects null path")
        void load_nullPath_throwsException() {
            assertThatThrownBy(
                    () -> ExpectedArtifacts.load(null))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("path");
        }

        @Test
        @DisplayName("rejects non-existent file")
        void load_nonExistentFile_throwsException(
                @TempDir Path tempDir) {
            Path missing = tempDir.resolve("missing.json");

            assertThatThrownBy(
                    () -> ExpectedArtifacts.load(missing))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("does not exist");
        }

        @Test
        @DisplayName("loads valid JSON with profiles")
        void load_validJson_returnsExpectedArtifacts(
                @TempDir Path tempDir) throws IOException {
            Path jsonFile = writeJson(tempDir, VALID_JSON);

            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(jsonFile);

            assertThat(artifacts.getProfileNames())
                    .containsExactlyInAnyOrder(
                            "java-quarkus", "go-gin");
        }

        @Test
        @DisplayName("loads empty profiles map")
        void load_emptyProfiles_returnsEmptyArtifacts(
                @TempDir Path tempDir) throws IOException {
            Path jsonFile = writeJson(
                    tempDir, EMPTY_PROFILES_JSON);

            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(jsonFile);

            assertThat(artifacts.getProfileNames()).isEmpty();
        }

        @Test
        @DisplayName("rejects malformed JSON")
        void load_malformedJson_throwsException(
                @TempDir Path tempDir) throws IOException {
            Path jsonFile = writeJson(
                    tempDir, "{ invalid json }");

            assertThatThrownBy(
                    () -> ExpectedArtifacts.load(jsonFile))
                    .isInstanceOf(
                            IllegalStateException.class)
                    .hasMessageContaining("parse");
        }
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("returns data for existing profile")
        void getProfile_existing_returnsProfileArtifacts(
                @TempDir Path tempDir) throws IOException {
            Path jsonFile = writeJson(tempDir, VALID_JSON);
            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(jsonFile);

            ProfileArtifacts profile =
                    artifacts.getProfile("java-quarkus");

            assertThat(profile.totalFiles()).isEqualTo(125);
            assertThat(profile.directories())
                    .contains(".claude", "docs", "k8s");
            assertThat(profile.getCategoryCount(
                    "codex-skills")).isEqualTo(106);
            assertThat(profile.getCategoryCount(
                    "claude-rules")).isEqualTo(6);
        }

        @Test
        @DisplayName(
                "throws for non-existent profile")
        void getProfile_nonExistent_throwsException(
                @TempDir Path tempDir) throws IOException {
            Path jsonFile = writeJson(tempDir, VALID_JSON);
            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(jsonFile);

            assertThatThrownBy(
                    () -> artifacts.getProfile("ruby-rails"))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("ruby-rails")
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("throws for null profile name")
        void getProfile_null_throwsException(
                @TempDir Path tempDir) throws IOException {
            Path jsonFile = writeJson(tempDir, VALID_JSON);
            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(jsonFile);

            assertThatThrownBy(
                    () -> artifacts.getProfile(null))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getProfileNames")
    class GetProfileNames {

        @Test
        @DisplayName("returns immutable set")
        void getProfileNames_returnsImmutableSet(
                @TempDir Path tempDir) throws IOException {
            Path jsonFile = writeJson(tempDir, VALID_JSON);
            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.load(jsonFile);

            assertThatThrownBy(
                    () -> artifacts.getProfileNames()
                            .add("new-profile"))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("loadFromClasspath")
    class LoadFromClasspath {

        @Test
        @DisplayName(
                "loads manifest from test resources")
        void loadFromClasspath_validResource_loads() {
            ExpectedArtifacts artifacts =
                    ExpectedArtifacts.loadFromClasspath(
                            "smoke/expected-artifacts.json");

            assertThat(artifacts.getProfileNames())
                    .hasSize(12)
                    .contains("java-quarkus", "go-gin",
                            "python-click-cli",
                            "java-spring-hexagonal",
                            "java-spring-cqrs-es",
                            "java-spring-event-driven",
                            "java-spring-fintech-pci");
        }
    }

    private static Path writeJson(
            Path dir, String content) throws IOException {
        Path file = dir.resolve(
                "expected-artifacts.json");
        Files.writeString(file, content);
        return file;
    }
}
