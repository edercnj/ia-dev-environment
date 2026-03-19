package dev.iadev.assembler;

import dev.iadev.model.McpServerConfig;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CodexShared utility class.
 */
@DisplayName("CodexShared")
class CodexSharedTest {

    @Nested
    @DisplayName("isAccessibleDirectory")
    class IsAccessibleDirectory {

        @Test
        @DisplayName("returns true for existing directory")
        void returnsTrueForDirectory(
                @TempDir Path tempDir) {
            assertThat(CodexShared.isAccessibleDirectory(
                    tempDir)).isTrue();
        }

        @Test
        @DisplayName("returns false for nonexistent path")
        void returnsFalseForNonexistent(
                @TempDir Path tempDir) {
            Path missing = tempDir.resolve("nope");
            assertThat(CodexShared.isAccessibleDirectory(
                    missing)).isFalse();
        }

        @Test
        @DisplayName("returns false for file path")
        void returnsFalseForFile(
                @TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("file.txt");
            Files.writeString(file, "content",
                    StandardCharsets.UTF_8);
            assertThat(CodexShared.isAccessibleDirectory(
                    file)).isFalse();
        }
    }

    @Nested
    @DisplayName("detectHooks")
    class DetectHooks {

        @Test
        @DisplayName("returns true when directory has files")
        void returnsTrueWhenHasFiles(
                @TempDir Path tempDir) throws IOException {
            Path hooksDir = tempDir.resolve("hooks");
            Files.createDirectories(hooksDir);
            Files.writeString(
                    hooksDir.resolve("hook.sh"),
                    "#!/bin/bash",
                    StandardCharsets.UTF_8);
            assertThat(CodexShared.detectHooks(hooksDir))
                    .isTrue();
        }

        @Test
        @DisplayName("returns false for empty directory")
        void returnsFalseForEmpty(
                @TempDir Path tempDir) throws IOException {
            Path hooksDir = tempDir.resolve("hooks");
            Files.createDirectories(hooksDir);
            assertThat(CodexShared.detectHooks(hooksDir))
                    .isFalse();
        }

        @Test
        @DisplayName("returns false for missing directory")
        void returnsFalseForMissing(
                @TempDir Path tempDir) {
            Path hooksDir = tempDir.resolve("hooks");
            assertThat(CodexShared.detectHooks(hooksDir))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("deriveApprovalPolicy")
    class DeriveApprovalPolicy {

        @Test
        @DisplayName("returns on-request when hooks present")
        void returnsOnRequest() {
            assertThat(
                    CodexShared.deriveApprovalPolicy(true))
                    .isEqualTo("on-request");
        }

        @Test
        @DisplayName("returns untrusted when no hooks")
        void returnsUntrusted() {
            assertThat(
                    CodexShared.deriveApprovalPolicy(false))
                    .isEqualTo("untrusted");
        }
    }

    @Nested
    @DisplayName("isValidTomlBareKey")
    class IsValidTomlBareKey {

        @Test
        @DisplayName("accepts alphanumeric with hyphens")
        void acceptsValid() {
            assertThat(
                    CodexShared.isValidTomlBareKey(
                            "my-server-1"))
                    .isTrue();
        }

        @Test
        @DisplayName("accepts underscores")
        void acceptsUnderscores() {
            assertThat(
                    CodexShared.isValidTomlBareKey(
                            "my_server"))
                    .isTrue();
        }

        @Test
        @DisplayName("rejects spaces")
        void rejectsSpaces() {
            assertThat(
                    CodexShared.isValidTomlBareKey(
                            "my server"))
                    .isFalse();
        }

        @Test
        @DisplayName("rejects special characters")
        void rejectsSpecialChars() {
            assertThat(
                    CodexShared.isValidTomlBareKey(
                            "server@home"))
                    .isFalse();
        }

        @Test
        @DisplayName("rejects dots")
        void rejectsDots() {
            assertThat(
                    CodexShared.isValidTomlBareKey(
                            "server.name"))
                    .isFalse();
        }

        @Test
        @DisplayName("rejects empty string")
        void rejectsEmpty() {
            assertThat(
                    CodexShared.isValidTomlBareKey(""))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("escapeTomlValue")
    class EscapeTomlValue {

        @Test
        @DisplayName("escapes backslashes")
        void escapesBackslashes() {
            assertThat(
                    CodexShared.escapeTomlValue("a\\b"))
                    .isEqualTo("a\\\\b");
        }

        @Test
        @DisplayName("escapes double quotes")
        void escapesQuotes() {
            assertThat(
                    CodexShared.escapeTomlValue("a\"b"))
                    .isEqualTo("a\\\"b");
        }

        @Test
        @DisplayName("escapes newlines and tabs")
        void escapesControlChars() {
            assertThat(
                    CodexShared.escapeTomlValue("a\nb\tc"))
                    .isEqualTo("a\\nb\\tc");
        }

        @Test
        @DisplayName("preserves plain strings")
        void preservesPlain() {
            assertThat(
                    CodexShared.escapeTomlValue("hello"))
                    .isEqualTo("hello");
        }
    }

    @Nested
    @DisplayName("mapMcpServers")
    class MapMcpServers {

        @Test
        @DisplayName("returns empty list for no servers")
        void returnsEmptyForNoServers() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            assertThat(CodexShared.mapMcpServers(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("maps server with URL to command list")
        void mapsServerWithUrl() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "my-server",
                                            "npx serve --port 3000",
                                            List.of(),
                                            Map.of()))
                            .build();
            List<Map<String, Object>> result =
                    CodexShared.mapMcpServers(config);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().get("id"))
                    .isEqualTo("my-server");
            @SuppressWarnings("unchecked")
            List<String> command =
                    (List<String>) result.getFirst()
                            .get("command");
            assertThat(command)
                    .containsExactly(
                            "npx", "serve", "--port", "3000");
        }

        @Test
        @DisplayName("escapes env values")
        void escapesEnvValues() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "s1",
                                            "cmd",
                                            List.of(),
                                            Map.of("KEY",
                                                    "val\"ue")))
                            .build();
            List<Map<String, Object>> result =
                    CodexShared.mapMcpServers(config);
            @SuppressWarnings("unchecked")
            Map<String, String> env =
                    (Map<String, String>) result
                            .getFirst().get("env");
            assertThat(env).isNotNull();
            assertThat(env.get("KEY"))
                    .isEqualTo("val\\\"ue");
        }

        @Test
        @DisplayName("env is null when empty")
        void envNullWhenEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "s1",
                                            "cmd",
                                            List.of(),
                                            Map.of()))
                            .build();
            List<Map<String, Object>> result =
                    CodexShared.mapMcpServers(config);
            assertThat(result.getFirst().get("env"))
                    .isNull();
        }
    }
}
