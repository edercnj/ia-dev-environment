package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PermissionCollector — collects CLI permissions
 * from JSON files based on project configuration.
 */
@DisplayName("PermissionCollector")
class PermissionCollectorTest {

    @Nested
    @DisplayName("collect — deduplicated results")
    class Collect {

        @Test
        @DisplayName("returns deduplicated permissions"
                + " for java-maven")
        void deduplicatedForJavaMaven(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);
            PermissionCollector collector =
                    new PermissionCollector();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    collector.collect(config, templatesDir);

            assertThat(perms)
                    .contains("Bash(git *)")
                    .contains("Bash(mvn *)")
                    .doesNotHaveDuplicates();
        }
    }

    @Nested
    @DisplayName("collectRaw — permission merging")
    class CollectRaw {

        @Test
        @DisplayName("java-maven includes base + maven")
        void javaMavenIncludesBaseAndMaven(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);
            PermissionCollector collector =
                    new PermissionCollector();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    collector.collectRaw(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(git *)")
                    .contains("Bash(mvn *)");
        }

        @Test
        @DisplayName("docker container adds docker"
                + " permissions")
        void dockerAddsDockerPerms(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);
            PermissionCollector collector =
                    new PermissionCollector();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    collector.collectRaw(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(docker build *)");
        }

        @Test
        @DisplayName("podman adds docker permissions")
        void podmanAddsDockerPerms(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);
            PermissionCollector collector =
                    new PermissionCollector();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("podman")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    collector.collectRaw(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(docker build *)");
        }

        @Test
        @DisplayName("kubernetes adds k8s permissions")
        void k8sAddsK8sPerms(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);
            PermissionCollector collector =
                    new PermissionCollector();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("kubernetes")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    collector.collectRaw(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(kubectl get *)");
        }

        @Test
        @DisplayName("docker-compose adds compose"
                + " permissions")
        void composeAddsComposePerms(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);
            PermissionCollector collector =
                    new PermissionCollector();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("docker-compose")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    collector.collectRaw(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(docker compose *)");
        }

        @Test
        @DisplayName("smoke tests add newman permissions")
        void smokeTestsAddNewman(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);
            PermissionCollector collector =
                    new PermissionCollector();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(true)
                            .build();

            List<String> perms =
                    collector.collectRaw(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(newman *)");
        }

        @Test
        @DisplayName("database adds db permissions")
        void databaseAddsDbPerms(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);
            Files.writeString(
                    templatesDir.resolve("database-pg.json"),
                    "[\"Bash(psql *)\"]",
                    StandardCharsets.UTF_8);
            PermissionCollector collector =
                    new PermissionCollector();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    collector.collectRaw(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(git *)");
        }

        @Test
        @DisplayName("cache adds cache permissions")
        void cacheAddsCachePerms(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);
            Files.writeString(
                    templatesDir.resolve("cache-redis.json"),
                    "[\"Bash(redis-cli *)\"]",
                    StandardCharsets.UTF_8);
            PermissionCollector collector =
                    new PermissionCollector();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .cache("redis", "7.4")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    collector.collectRaw(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(git *)");
        }
    }

    @Nested
    @DisplayName("deduplicate — removes duplicates")
    class Deduplicate {

        @Test
        @DisplayName("preserves order and removes dupes")
        void preservesOrderRemovesDupes() {
            List<String> input = List.of(
                    "a", "b", "a", "c", "b");

            List<String> result =
                    PermissionCollector.deduplicate(input);

            assertThat(result)
                    .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("no duplicates returns same")
        void noDuplicatesUnchanged() {
            List<String> input = List.of("a", "b", "c");

            List<String> result =
                    PermissionCollector.deduplicate(input);

            assertThat(result)
                    .containsExactly("a", "b", "c");
        }
    }

    @Nested
    @DisplayName("parseJsonStringArray — parsing")
    class ParseJsonStringArray {

        @Test
        @DisplayName("parses simple JSON array")
        void parsesSimpleArray() {
            List<String> result = PermissionCollector
                    .parseJsonStringArray(
                            "[\"a\", \"b\", \"c\"]");

            assertThat(result)
                    .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("returns empty for empty array")
        void emptyForEmptyArray() {
            List<String> result = PermissionCollector
                    .parseJsonStringArray("[]");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for non-array")
        void emptyForNonArray() {
            List<String> result = PermissionCollector
                    .parseJsonStringArray("{}");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("handles parentheses in entries")
        void handlesParentheses() {
            List<String> result = PermissionCollector
                    .parseJsonStringArray(
                            "[\"Bash(git *)\","
                                    + " \"Bash(ls *)\"]");

            assertThat(result)
                    .containsExactly(
                            "Bash(git *)",
                            "Bash(ls *)");
        }

        @Test
        @DisplayName("handles escaped quotes")
        void handlesEscapedQuotes() {
            List<String> result = PermissionCollector
                    .parseJsonStringArray(
                            "[\"a\\\"b\"]");

            assertThat(result).containsExactly("a\\\"b");
        }

        @Test
        @DisplayName("handles whitespace-only inner")
        void handlesWhitespaceOnly() {
            List<String> result = PermissionCollector
                    .parseJsonStringArray("[   ]");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty string returns empty")
        void emptyStringReturnsEmpty() {
            List<String> result = PermissionCollector
                    .parseJsonStringArray("");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("multiline JSON parsed correctly")
        void handlesMultiline() {
            String json = "[\n"
                    + "  \"first\",\n"
                    + "  \"second\"\n"
                    + "]";
            List<String> result = PermissionCollector
                    .parseJsonStringArray(json);

            assertThat(result)
                    .containsExactly("first", "second");
        }
    }

    @Nested
    @DisplayName("readJsonArray — file I/O")
    class ReadJsonArray {

        @Test
        @DisplayName("non-existent file returns empty")
        void nonExistentReturnsEmpty(
                @TempDir Path tempDir) {
            Path missing = tempDir.resolve("missing.json");

            List<String> result = PermissionCollector
                    .readJsonArray(missing);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("valid file returns parsed array")
        void validFileReturnsParsed(
                @TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("test.json");
            Files.writeString(file, "[\"a\", \"b\"]",
                    StandardCharsets.UTF_8);

            List<String> result = PermissionCollector
                    .readJsonArray(file);

            assertThat(result)
                    .containsExactly("a", "b");
        }
    }

    private static Path setupTemplatesDir(Path tempDir)
            throws IOException {
        Path templatesDir = tempDir.resolve(
                "settings-templates");
        Files.createDirectories(templatesDir);
        Files.writeString(
                templatesDir.resolve("base.json"),
                "[\"Bash(git *)\"]",
                StandardCharsets.UTF_8);
        Files.writeString(
                templatesDir.resolve("java-maven.json"),
                "[\"Bash(mvn *)\"]",
                StandardCharsets.UTF_8);
        Files.writeString(
                templatesDir.resolve("docker.json"),
                "[\"Bash(docker build *)\"]",
                StandardCharsets.UTF_8);
        Files.writeString(
                templatesDir.resolve("kubernetes.json"),
                "[\"Bash(kubectl get *)\"]",
                StandardCharsets.UTF_8);
        Files.writeString(
                templatesDir.resolve("docker-compose.json"),
                "[\"Bash(docker compose *)\"]",
                StandardCharsets.UTF_8);
        Files.writeString(
                templatesDir.resolve("testing-newman.json"),
                "[\"Bash(newman *)\"]",
                StandardCharsets.UTF_8);
        return templatesDir;
    }
}
