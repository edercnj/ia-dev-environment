package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
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
import java.util.Map;

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
        void assemble_postgresql_addsDbPerms(
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
        void assemble_redisCache_addsPerms(
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
        void parseJsonStringArray_whenCalled_handlesEscapedQuotes() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray(
                            "[\"a\\\"b\"]");

            assertThat(result).containsExactly("a\\\"b");
        }

        @Test
        @DisplayName("handles whitespace-only inner")
        void parseJsonStringArray_whenCalled_handlesWhitespaceOnly() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray("[   ]");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("handles single element")
        void parseJsonStringArray_whenCalled_handlesSingleElement() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray(
                            "[\"single\"]");

            assertThat(result)
                    .containsExactly("single");
        }

        @Test
        @DisplayName("handles multiline JSON")
        void parseJsonStringArray_whenCalled_handlesMultiline() {
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
        void parseJsonStringArray_emptyString_returnsEmpty() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray("");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("non-bracket text returns empty")
        void parseJsonStringArray_nonBracket_returnsEmpty() {
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
        void readJsonArray_nonExistent_returnsEmpty(
                @TempDir Path tempDir) {
            Path missing = tempDir.resolve("missing.json");

            List<String> result = SettingsAssembler
                    .readJsonArray(missing);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("valid file returns parsed array")
        void readJsonArray_validFile_returnsParsed(
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
        void buildSettingsJson_emptyPermissions_succeeds() {
            String json = SettingsAssembler
                    .buildSettingsJson(List.of(), HookPresence.WITHOUT_HOOKS);

            assertThat(json)
                    .contains("\"allow\": [\n")
                    .contains("]\n");
        }

        @Test
        @DisplayName("multiple permissions separated"
                + " by commas")
        void buildSettingsJson_multiplePermissions_succeeds() {
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
        void collectInfra_podman_addsDockerPerms(
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
        writeTemplateFiles(templatesDir);
        return templatesDir;
    }

    private static void writeTemplateFiles(
            Path templatesDir) throws IOException {
        Map.of(
                "base.json", "[\"Bash(git *)\"]",
                "java-maven.json", "[\"Bash(mvn *)\"]",
                "docker.json",
                        "[\"Bash(docker build *)\"]",
                "kubernetes.json",
                        "[\"Bash(kubectl get *)\"]",
                "docker-compose.json",
                        "[\"Bash(docker compose *)\"]",
                "testing-newman.json",
                        "[\"Bash(newman *)\"]"
        ).forEach((name, content) -> {
            try {
                Files.writeString(
                        templatesDir.resolve(name),
                        content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
    }
}
