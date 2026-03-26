package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SmokeTestValidators}.
 *
 * <p>Covers all 6 validator methods with positive and negative
 * scenarios derived from the Gherkin acceptance criteria in
 * story-0012-0001.</p>
 */
@DisplayName("SmokeTestValidators")
class SmokeTestValidatorsTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("assertNoEmptyFiles")
    class AssertNoEmptyFiles {

        @Test
        @DisplayName("passes when directory is empty "
                + "(no files to check)")
        void assertNoEmptyFiles_emptyDir_passes()
                throws IOException {
            assertThatCode(() ->
                    SmokeTestValidators
                            .assertNoEmptyFiles(tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("passes when all files have content")
        void assertNoEmptyFiles_allFilesHaveContent_passes()
                throws IOException {
            Files.writeString(
                    tempDir.resolve("file1.txt"),
                    "content");
            Files.writeString(
                    tempDir.resolve("file2.md"),
                    "more content");

            assertThatCode(() ->
                    SmokeTestValidators
                            .assertNoEmptyFiles(tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fails when a file has 0 bytes")
        void assertNoEmptyFiles_zeroByteFile_fails()
                throws IOException {
            Files.createFile(tempDir.resolve("empty.txt"));

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertNoEmptyFiles(tempDir))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("empty.txt");
        }

        @Test
        @DisplayName("detects empty file in nested directory")
        void assertNoEmptyFiles_nestedEmptyFile_fails()
                throws IOException {
            Path subDir = tempDir.resolve("sub/dir");
            Files.createDirectories(subDir);
            Files.createFile(subDir.resolve("nested.txt"));

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertNoEmptyFiles(tempDir))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("nested.txt");
        }

        @Test
        @DisplayName("reports all empty files, not just first")
        void assertNoEmptyFiles_multipleEmpty_reportsAll()
                throws IOException {
            Files.createFile(tempDir.resolve("a.txt"));
            Files.createFile(tempDir.resolve("b.txt"));

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertNoEmptyFiles(tempDir))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("a.txt")
                    .hasMessageContaining("b.txt");
        }
    }

    @Nested
    @DisplayName("assertNoUnresolvedPlaceholders")
    class AssertNoUnresolvedPlaceholders {

        private static final Set<String> DEFAULT_PATTERNS =
                Set.of("\\{\\{.*?\\}\\}");

        @Test
        @DisplayName("passes when no files contain "
                + "placeholders")
        void assertNoUnresolvedPlaceholders_clean_passes()
                throws IOException {
            Files.writeString(
                    tempDir.resolve("clean.txt"),
                    "Hello world");

            assertThatCode(() ->
                    SmokeTestValidators
                            .assertNoUnresolvedPlaceholders(
                                    tempDir,
                                    DEFAULT_PATTERNS))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fails when file contains "
                + "{{PLACEHOLDER}}")
        void assertNoUnresolvedPlaceholders_found_fails()
                throws IOException {
            Files.writeString(
                    tempDir.resolve("template.md"),
                    "Name: {{PLACEHOLDER}}");

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertNoUnresolvedPlaceholders(
                                    tempDir,
                                    DEFAULT_PATTERNS))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("template.md")
                    .hasMessageContaining(
                            "{{PLACEHOLDER}}");
        }

        @Test
        @DisplayName("passes when directory is empty")
        void assertNoUnresolvedPlaceholders_emptyDir_passes()
                throws IOException {
            assertThatCode(() ->
                    SmokeTestValidators
                            .assertNoUnresolvedPlaceholders(
                                    tempDir,
                                    DEFAULT_PATTERNS))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("detects multiple patterns")
        void assertNoUnresolvedPlaceholders_multiPattern_fails()
                throws IOException {
            Files.writeString(
                    tempDir.resolve("mixed.md"),
                    "Key: <CHAVE-JIRA>\n"
                            + "Value: {{VALUE}}");

            Set<String> patterns = Set.of(
                    "\\{\\{.*?\\}\\}",
                    "<CHAVE-JIRA>");

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertNoUnresolvedPlaceholders(
                                    tempDir, patterns))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("mixed.md");
        }

        @Test
        @DisplayName("detects placeholders in nested files")
        void assertNoUnresolvedPlaceholders_nested_fails()
                throws IOException {
            Path subDir = tempDir.resolve("sub");
            Files.createDirectories(subDir);
            Files.writeString(
                    subDir.resolve("deep.yml"),
                    "value: {{UNRESOLVED}}");

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertNoUnresolvedPlaceholders(
                                    tempDir,
                                    DEFAULT_PATTERNS))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("deep.yml");
        }
    }

    @Nested
    @DisplayName("assertDirectoryStructure")
    class AssertDirectoryStructure {

        @Test
        @DisplayName("passes when all expected directories "
                + "exist")
        void assertDirectoryStructure_allExist_passes()
                throws IOException {
            Files.createDirectories(
                    tempDir.resolve(".claude"));
            Files.createDirectories(
                    tempDir.resolve(".github"));
            Files.createDirectories(
                    tempDir.resolve("docs"));

            Set<String> expected =
                    Set.of(".claude", ".github", "docs");

            assertThatCode(() ->
                    SmokeTestValidators
                            .assertDirectoryStructure(
                                    tempDir, expected))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fails when a directory is missing")
        void assertDirectoryStructure_missing_fails()
                throws IOException {
            Files.createDirectories(
                    tempDir.resolve(".claude"));

            Set<String> expected =
                    Set.of(".claude", ".github");

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertDirectoryStructure(
                                    tempDir, expected))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining(".github");
        }

        @Test
        @DisplayName("passes with empty expected set")
        void assertDirectoryStructure_emptyExpected_passes() {
            assertThatCode(() ->
                    SmokeTestValidators
                            .assertDirectoryStructure(
                                    tempDir, Set.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("reports all missing directories")
        void assertDirectoryStructure_multipleMissing_fails() {
            Set<String> expected =
                    Set.of(".claude", ".github", "docs");

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertDirectoryStructure(
                                    tempDir, expected))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining(".claude")
                    .hasMessageContaining(".github")
                    .hasMessageContaining("docs");
        }
    }

    @Nested
    @DisplayName("assertFileCount")
    class AssertFileCount {

        @Test
        @DisplayName("passes when count matches")
        void assertFileCount_matches_passes()
                throws IOException {
            Files.writeString(
                    tempDir.resolve("a.txt"), "a");
            Files.writeString(
                    tempDir.resolve("b.txt"), "b");

            assertThatCode(() ->
                    SmokeTestValidators
                            .assertFileCount(tempDir, 2))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fails when count does not match")
        void assertFileCount_mismatch_fails()
                throws IOException {
            Files.writeString(
                    tempDir.resolve("a.txt"), "a");

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertFileCount(tempDir, 5))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("5")
                    .hasMessageContaining("1");
        }

        @Test
        @DisplayName("counts files in nested directories")
        void assertFileCount_nested_counts()
                throws IOException {
            Files.writeString(
                    tempDir.resolve("root.txt"), "r");
            Path sub = tempDir.resolve("sub");
            Files.createDirectories(sub);
            Files.writeString(
                    sub.resolve("nested.txt"), "n");

            assertThatCode(() ->
                    SmokeTestValidators
                            .assertFileCount(tempDir, 2))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("passes for empty dir with count 0")
        void assertFileCount_emptyDirZero_passes() {
            assertThatCode(() ->
                    SmokeTestValidators
                            .assertFileCount(tempDir, 0))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("assertValidYaml")
    class AssertValidYaml {

        @Test
        @DisplayName("passes for well-formed YAML")
        void assertValidYaml_valid_passes()
                throws IOException {
            Path yamlFile = tempDir.resolve("config.yaml");
            Files.writeString(yamlFile,
                    "name: test\nversion: 1.0\n");

            assertThatCode(() ->
                    SmokeTestValidators
                            .assertValidYaml(yamlFile))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fails for malformed YAML")
        void assertValidYaml_invalid_fails()
                throws IOException {
            Path yamlFile = tempDir.resolve("bad.yaml");
            Files.writeString(yamlFile,
                    "name: test\n  bad indent:\n"
                            + "    - [unclosed");

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertValidYaml(yamlFile))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("bad.yaml");
        }

        @Test
        @DisplayName("passes for YAML with list content")
        void assertValidYaml_listContent_passes()
                throws IOException {
            Path yamlFile = tempDir.resolve("list.yaml");
            Files.writeString(yamlFile,
                    "items:\n  - one\n  - two\n");

            assertThatCode(() ->
                    SmokeTestValidators
                            .assertValidYaml(yamlFile))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("assertValidJson")
    class AssertValidJson {

        @Test
        @DisplayName("passes for well-formed JSON")
        void assertValidJson_valid_passes()
                throws IOException {
            Path jsonFile = tempDir.resolve("data.json");
            Files.writeString(jsonFile,
                    "{\"name\": \"test\"}");

            assertThatCode(() ->
                    SmokeTestValidators
                            .assertValidJson(jsonFile))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fails for malformed JSON")
        void assertValidJson_invalid_fails()
                throws IOException {
            Path jsonFile = tempDir.resolve("bad.json");
            Files.writeString(jsonFile, "{broken json");

            assertThatThrownBy(() ->
                    SmokeTestValidators
                            .assertValidJson(jsonFile))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("bad.json");
        }

        @Test
        @DisplayName("passes for JSON array")
        void assertValidJson_array_passes()
                throws IOException {
            Path jsonFile = tempDir.resolve("array.json");
            Files.writeString(jsonFile,
                    "[{\"a\":1},{\"b\":2}]");

            assertThatCode(() ->
                    SmokeTestValidators
                            .assertValidJson(jsonFile))
                    .doesNotThrowAnyException();
        }
    }
}
