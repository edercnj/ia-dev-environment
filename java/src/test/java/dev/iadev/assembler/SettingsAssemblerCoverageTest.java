package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
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
 * Additional coverage tests for SettingsAssembler —
 * targeting uncovered branches.
 */
@DisplayName("SettingsAssembler — coverage")
class SettingsAssemblerCoverageTest {

    @Nested
    @DisplayName("collectPermissions — database and cache")
    class DataPermissions {

        @Test
        @DisplayName("postgresql database adds db"
                + " permissions")
        void postgresqlAddsDbPerms(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir = setupTemplatesDir(tempDir);
            Files.writeString(
                    templatesDir.resolve("database-pg.json"),
                    "[\"Bash(psql *)\"]",
                    StandardCharsets.UTF_8);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(git *)");
        }

        @Test
        @DisplayName("redis cache adds cache permissions")
        void redisCacheAddsPerms(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir = setupTemplatesDir(tempDir);
            Files.writeString(
                    templatesDir.resolve("cache-redis.json"),
                    "[\"Bash(redis-cli *)\"]",
                    StandardCharsets.UTF_8);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .cache("redis", "7.4")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(git *)");
        }
    }

    @Nested
    @DisplayName("parseJsonStringArray — edge cases")
    class ParseEdgeCases {

        @Test
        @DisplayName("handles escaped quotes in strings")
        void handlesEscapedQuotes() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray(
                            "[\"a\\\"b\"]");

            assertThat(result).containsExactly("a\\\"b");
        }

        @Test
        @DisplayName("handles whitespace-only inner")
        void handlesWhitespaceOnly() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray("[   ]");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("handles single element")
        void handlesSingleElement() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray(
                            "[\"single\"]");

            assertThat(result)
                    .containsExactly("single");
        }

        @Test
        @DisplayName("handles multiline JSON")
        void handlesMultiline() {
            String json = "[\n"
                    + "  \"first\",\n"
                    + "  \"second\"\n"
                    + "]";
            List<String> result = SettingsAssembler
                    .parseJsonStringArray(json);

            assertThat(result)
                    .containsExactly("first", "second");
        }

        @Test
        @DisplayName("empty string returns empty")
        void emptyStringReturnsEmpty() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray("");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("non-bracket text returns empty")
        void nonBracketReturnsEmpty() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray("just text");

            assertThat(result).isEmpty();
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

            List<String> result = SettingsAssembler
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

            List<String> result = SettingsAssembler
                    .readJsonArray(file);

            assertThat(result)
                    .containsExactly("a", "b");
        }
    }

    @Nested
    @DisplayName("buildSettingsJson — edge cases")
    class BuildSettingsJsonEdge {

        @Test
        @DisplayName("empty permissions produces"
                + " empty allow array")
        void emptyPermissions() {
            String json = SettingsAssembler
                    .buildSettingsJson(List.of(), HookPresence.WITHOUT_HOOKS);

            assertThat(json)
                    .contains("\"allow\": [\n")
                    .contains("]\n");
        }

        @Test
        @DisplayName("multiple permissions separated"
                + " by commas")
        void multiplePermissions() {
            List<String> perms = List.of(
                    "Bash(git *)",
                    "Bash(mvn *)",
                    "Bash(npm *)");
            String json = SettingsAssembler
                    .buildSettingsJson(perms, HookPresence.WITHOUT_HOOKS);

            assertThat(json)
                    .contains("\"Bash(git *)\"")
                    .contains("\"Bash(mvn *)\"")
                    .contains("\"Bash(npm *)\"");
            // Verify commas between entries
            assertThat(json)
                    .contains("\"Bash(git *)\",");
            assertThat(json)
                    .contains("\"Bash(mvn *)\",");
        }
    }

    @Nested
    @DisplayName("collectInfra — podman branch")
    class CollectInfraPodman {

        @Test
        @DisplayName("podman adds docker permissions"
                + " via collectPermissions")
        void podmanAddsDockerPerms(
                @TempDir Path tempDir) throws IOException {
            Path templatesDir = setupTemplatesDir(tempDir);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("podman")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(docker build *)");
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
