package dev.iadev.assembler;

import dev.iadev.model.McpServerConfig;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CodexConfigAssembler — generates
 * .codex/config.toml from Pebble template.
 */
@DisplayName("CodexConfigAssembler")
class CodexConfigAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void isAssemblerInstance() {
            assertThat(new CodexConfigAssembler())
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("buildConfigContext")
    class BuildConfigContext {

        @Test
        @DisplayName("contains model, approval_policy,"
                + " sandbox_mode")
        void containsCodexFields() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            Map<String, Object> ctx =
                    CodexConfigAssembler
                            .buildConfigContext(
                                    config, HookPresence.WITHOUT_HOOKS);

            assertThat(ctx.get("model"))
                    .isEqualTo("o4-mini");
            assertThat(ctx.get("approval_policy"))
                    .isEqualTo("untrusted");
            assertThat(ctx.get("sandbox_mode"))
                    .isEqualTo("workspace-write");
        }

        @Test
        @DisplayName("has_mcp is false when no servers")
        void hasMcpFalseWhenEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            Map<String, Object> ctx =
                    CodexConfigAssembler
                            .buildConfigContext(
                                    config, HookPresence.WITHOUT_HOOKS);

            assertThat(ctx.get("has_mcp"))
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("has_mcp is true when servers exist")
        void hasMcpTrueWhenServers() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "s1", "cmd",
                                            List.of(),
                                            Map.of()))
                            .build();
            Map<String, Object> ctx =
                    CodexConfigAssembler
                            .buildConfigContext(
                                    config, HookPresence.WITHOUT_HOOKS);

            assertThat(ctx.get("has_mcp"))
                    .isEqualTo(true);
        }

        @Test
        @DisplayName("approval_policy on-request"
                + " with hooks")
        void approvalPolicyWithHooks() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            Map<String, Object> ctx =
                    CodexConfigAssembler
                            .buildConfigContext(
                                    config, HookPresence.WITH_HOOKS);

            assertThat(ctx.get("approval_policy"))
                    .isEqualTo("on-request");
        }
    }

    @Nested
    @DisplayName("assemble — generates config.toml")
    class Assemble {

        @Test
        @DisplayName("generates config.toml in outputDir")
        void generatesConfigToml(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = setupDirs(tempDir);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(1);
            Path configToml =
                    outputDir.resolve("config.toml");
            assertThat(configToml).exists();
        }

        @Test
        @DisplayName("config.toml contains model value")
        void containsModelValue(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = setupDirs(tempDir);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("config.toml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("model = \"o4-mini\"");
        }

        @Test
        @DisplayName("config.toml contains project name")
        void containsProjectName(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = setupDirs(tempDir);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-api")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("config.toml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("my-api");
        }

        @Test
        @DisplayName("config.toml contains sandbox mode")
        void containsSandboxMode(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = setupDirs(tempDir);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("config.toml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("mode = \"workspace-write\"");
        }

        @Test
        @DisplayName("config.toml has untrusted policy"
                + " without hooks")
        void untrustedWithoutHooks(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = setupDirs(tempDir);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("config.toml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains(
                            "approval_policy ="
                                    + " \"untrusted\"");
        }

        @Test
        @DisplayName("config.toml has on-request policy"
                + " with hooks")
        void onRequestWithHooks(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = setupDirs(tempDir);

            // Create hooks
            Path hooksDir = outputDir.getParent()
                    .resolve(".claude").resolve("hooks");
            Files.createDirectories(hooksDir);
            Files.writeString(
                    hooksDir.resolve("hook.sh"),
                    "#!/bin/bash",
                    StandardCharsets.UTF_8);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("config.toml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains(
                            "approval_policy ="
                                    + " \"on-request\"");
        }

        @Test
        @DisplayName("golden file parity for kotlin-ktor")
        void goldenFileParity(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = setupDirs(tempDir);

            // Create hooks to get on-request policy
            Path hooksDir = outputDir.getParent()
                    .resolve(".claude").resolve("hooks");
            Files.createDirectories(hooksDir);
            Files.writeString(
                    hooksDir.resolve("hook.sh"),
                    "#!/bin/bash",
                    StandardCharsets.UTF_8);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-ktor-service")
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String actual = Files.readString(
                    outputDir.resolve("config.toml"),
                    StandardCharsets.UTF_8);

            String expected = loadGolden(
                    "golden/kotlin-ktor/"
                            + ".codex/config.toml");
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotNull();
            assertThat(actual)
                    .as("Must match golden file"
                            + " byte-for-byte")
                    .isEqualTo(expected);
        }

        private Path setupDirs(Path tempDir)
                throws IOException {
            Path outputDir =
                    tempDir.resolve("out").resolve(".codex");
            Files.createDirectories(outputDir);
            return outputDir;
        }

        private String loadGolden(String path) {
            var url = getClass().getClassLoader()
                    .getResource(path);
            if (url == null) {
                return null;
            }
            try {
                return Files.readString(
                        Path.of(url.getPath()),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }
    }
}
