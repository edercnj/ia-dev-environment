package dev.iadev.assembler;

import dev.iadev.domain.model.McpServerConfig;
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
        void instanceOf_whenCreated_implementsAssemblerInterface() {
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
        void assemble_whenCalled_containsCodexFields() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            Map<String, Object> ctx =
                    CodexConfigAssembler
                            .buildConfigContext(
                                    config,
                                    HookPresence.WITHOUT_HOOKS,
                                    List.of());

            assertThat(ctx.get("model"))
                    .isEqualTo("o4-mini");
            assertThat(ctx.get("approval_policy"))
                    .isEqualTo("untrusted");
            assertThat(ctx.get("sandbox_mode"))
                    .isEqualTo("workspace-write");
        }

        @Test
        @DisplayName("has_mcp is false when no servers")
        void assemble_whenEmpty_hasMcpFalse() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            Map<String, Object> ctx =
                    CodexConfigAssembler
                            .buildConfigContext(
                                    config,
                                    HookPresence.WITHOUT_HOOKS,
                                    List.of());

            assertThat(ctx.get("has_mcp"))
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("has_mcp is true when servers exist")
        void assemble_whenServers_hasMcpTrue() {
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
                                    config,
                                    HookPresence.WITHOUT_HOOKS,
                                    List.of());

            assertThat(ctx.get("has_mcp"))
                    .isEqualTo(true);
        }

        @Test
        @DisplayName("approval_policy on-request"
                + " with hooks")
        void assemble_withHooks_approvalPolicy() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            Map<String, Object> ctx =
                    CodexConfigAssembler
                            .buildConfigContext(
                                    config,
                                    HookPresence.WITH_HOOKS,
                                    List.of());

            assertThat(ctx.get("approval_policy"))
                    .isEqualTo("on-request");
        }

        @Test
        @DisplayName("maps agents_list with sanitized name"
                + " and escaped description")
        void assemble_whenCalled_mapsAgentsList() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            Map<String, Object> ctx =
                    CodexConfigAssembler
                            .buildConfigContext(
                                    config,
                                    HookPresence.WITHOUT_HOOKS,
                                    List.of(new AgentInfo(
                                            "typescript developer",
                                            "Architect \"Lead\"")));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> agents =
                    (List<Map<String, Object>>) ctx.get(
                            "agents_list");

            assertThat(agents).hasSize(1);
            assertThat(agents.get(0).get("name"))
                    .isEqualTo("typescript-developer");
            assertThat(agents.get(0).get("description"))
                    .isEqualTo("Architect \\\"Lead\\\"");
        }
    }

    @Nested
    @DisplayName("assemble — generates config.toml")
    class Assemble {

        @Test
        @DisplayName("generates config.toml in outputDir")
        void assemble_whenCalled_generatesConfigToml(
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
        void assemble_whenCalled_containsModelValue(
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
        void assemble_whenCalled_containsProjectName(
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
        void assemble_whenCalled_containsSandboxMode(
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
        void assemble_whenCalled_untrustedWithoutHooks(
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
        void assemble_whenCalled_onRequestWithHooks(
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
        @DisplayName("config.toml renders [agents.*]"
                + " sections when agents exist")
        void assemble_whenCalled_rendersAgentsSections(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = setupDirs(tempDir);
            Path agentsDir = outputDir.getParent()
                    .resolve(".claude").resolve("agents");
            Files.createDirectories(agentsDir);
            Files.writeString(
                    agentsDir.resolve("architect.md"),
                    "# Architect \"Lead\"",
                    StandardCharsets.UTF_8);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("config.toml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("[agents.architect]")
                    .contains(
                            "description = \"Architect \\\"Lead\\\"\"");
        }

        @Test
        @DisplayName("golden file parity for kotlin-ktor")
        void assemble_whenCalled_goldenFileParity(
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
            Path agentsDir = outputDir.getParent()
                    .resolve(".claude").resolve("agents");
            Files.createDirectories(agentsDir);
            createGoldenAgents(agentsDir);

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
                    .isNotEmpty();
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

        private void createGoldenAgents(Path agentsDir)
                throws IOException {
            String[] names = {
                    "api-engineer",
                    "architect",
                    "devops-engineer",
                    "event-engineer",
                    "kotlin-developer",
                    "performance-engineer",
                    "product-owner",
                    "qa-engineer",
                    "security-engineer",
                    "sre-engineer",
                    "tech-lead"
            };
            for (String name : names) {
                Files.writeString(
                        agentsDir.resolve(name + ".md"),
                        "# Global Behavior & Language Policy",
                        StandardCharsets.UTF_8);
            }
        }
    }

    @Nested
    @DisplayName("collectTomlKeyWarnings")
    class CollectTomlKeyWarnings {

        @Test
        @DisplayName("no warnings for valid server IDs")
        void assemble_forValid_noWarnings() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "valid-id",
                                            "cmd",
                                            List.of(),
                                            Map.of()))
                            .build();

            List<String> warnings =
                    CodexConfigAssembler
                            .collectTomlKeyWarnings(
                                    config);

            assertThat(warnings).isEmpty();
        }

        @Test
        @DisplayName("warning for invalid TOML bare key")
        void assemble_forInvalidKey_warning() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "bad server",
                                            "cmd",
                                            List.of(),
                                            Map.of()))
                            .build();

            List<String> warnings =
                    CodexConfigAssembler
                            .collectTomlKeyWarnings(
                                    config);

            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0))
                    .contains("bad server")
                    .contains("invalid TOML");
        }

        @Test
        @DisplayName("no warnings when no MCP servers")
        void assemble_whenNoServers_noWarnings() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> warnings =
                    CodexConfigAssembler
                            .collectTomlKeyWarnings(
                                    config);

            assertThat(warnings).isEmpty();
        }

        @Test
        @DisplayName("multiple warnings for multiple"
                + " invalid IDs")
        void assemble_whenCalled_multipleWarnings() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "ok-id",
                                            "cmd",
                                            List.of(),
                                            Map.of()))
                            .addMcpServer(
                                    new McpServerConfig(
                                            "bad id",
                                            "cmd",
                                            List.of(),
                                            Map.of()))
                            .addMcpServer(
                                    new McpServerConfig(
                                            "also@bad",
                                            "cmd",
                                            List.of(),
                                            Map.of()))
                            .build();

            List<String> warnings =
                    CodexConfigAssembler
                            .collectTomlKeyWarnings(
                                    config);

            assertThat(warnings).hasSize(2);
        }
    }

    @Nested
    @DisplayName("assembleWithResult — warning"
            + " propagation")
    class AssembleWithResult {

        @Test
        @DisplayName("no warnings for valid config")
        void assembleWithResult_noWarnings_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = setupDirs(tempDir);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            AssemblerResult result =
                    assembler.assembleWithResult(
                            config,
                            new TemplateEngine(),
                            outputDir);

            assertThat(result.files()).hasSize(1);
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("warnings for invalid TOML keys"
                + " propagated via result")
        void assembleWithResult_whenCalled_warningsForInvalidKeys(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = setupDirs(tempDir);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "bad id",
                                            "cmd",
                                            List.of(),
                                            Map.of()))
                            .build();

            AssemblerResult result =
                    assembler.assembleWithResult(
                            config,
                            new TemplateEngine(),
                            outputDir);

            assertThat(result.files()).hasSize(1);
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0))
                    .contains("bad id")
                    .contains("invalid TOML");
        }

        private Path setupDirs(Path tempDir)
                throws IOException {
            Path outputDir =
                    tempDir.resolve("out")
                            .resolve(".codex");
            Files.createDirectories(outputDir);
            return outputDir;
        }
    }
}
