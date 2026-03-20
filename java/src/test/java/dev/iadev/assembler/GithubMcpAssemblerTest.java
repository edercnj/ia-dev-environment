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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for GithubMcpAssembler — the ninth assembler in
 * the pipeline, generating .github/copilot-mcp.json with
 * MCP server configuration for GitHub Copilot.
 */
@DisplayName("GithubMcpAssembler")
class GithubMcpAssemblerTest {

    @Nested
    @DisplayName("assemble — implements Assembler")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — no-op when empty")
    class NoOpWhenEmpty {

        @Test
        @DisplayName("returns empty list when"
                + " no MCP servers configured")
        void assemble_whenCalled_returnsEmptyForNoServers(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create file when"
                + " no MCP servers")
        void assemble_whenCalled_doesNotCreateFile(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(outputDir.resolve(
                    "copilot-mcp.json")).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assembleWithWarnings — full result")
    class AssembleWithWarnings {

        @Test
        @DisplayName("returns empty result when"
                + " no servers configured")
        void assembleWithWarnings_whenCalled_returnsEmptyResult() {
            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            AssemblerResult result =
                    assembler.assembleWithWarnings(
                            config,
                            Path.of("/tmp/unused"));

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("returns files and no warnings"
                + " for valid env vars")
        void assembleWithWarnings_whenCalled_returnsFilesNoWarnings(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "test-server",
                                            "https://mcp.example.com",
                                            List.of("read"),
                                            Map.of("KEY",
                                                    "$SECRET")))
                            .build();

            AssemblerResult result =
                    assembler.assembleWithWarnings(
                            config, outputDir);

            assertThat(result.files()).hasSize(1);
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("returns warnings for literal"
                + " env values")
        void assembleWithWarnings_whenCalled_returnsWarningsForLiterals(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "my-server",
                                            "https://mcp.test",
                                            List.of(),
                                            Map.of("API_KEY",
                                                    "literal-value")))
                            .build();

            AssemblerResult result =
                    assembler.assembleWithWarnings(
                            config, outputDir);

            assertThat(result.files()).hasSize(1);
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0))
                    .contains("my-server")
                    .contains("API_KEY")
                    .contains("literal value");
        }
    }

    @Nested
    @DisplayName("warnLiteralEnvValues — validation")
    class WarnLiteralEnvValues {

        @Test
        @DisplayName("no warning for $VARIABLE format")
        void warnLiteralEnvValues_noWarningForDollarVariable_succeeds() {
            List<McpServerConfig> servers = List.of(
                    new McpServerConfig(
                            "s1", "https://mcp.test",
                            List.of(),
                            Map.of("KEY", "$SECRET")));

            List<String> warnings =
                    GithubMcpAssembler
                            .warnLiteralEnvValues(
                                    servers);

            assertThat(warnings).isEmpty();
        }

        @Test
        @DisplayName("warning for literal value")
        void warnLiteralEnvValues_whenCalled_warningForLiteralValue() {
            List<McpServerConfig> servers = List.of(
                    new McpServerConfig(
                            "firecrawl",
                            "https://mcp.test",
                            List.of(),
                            Map.of("API_KEY",
                                    "actual-key-123")));

            List<String> warnings =
                    GithubMcpAssembler
                            .warnLiteralEnvValues(
                                    servers);

            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0))
                    .contains("firecrawl")
                    .contains("API_KEY")
                    .contains("literal value")
                    .contains("$VARIABLE format");
        }

        @Test
        @DisplayName("multiple warnings for"
                + " multiple literals")
        void warnLiteralEnvValues_multipleWarnings_succeeds() {
            List<McpServerConfig> servers = List.of(
                    new McpServerConfig(
                            "s1", "https://mcp.test",
                            List.of(),
                            Map.of("K1", "literal1",
                                    "K2", "$OK")),
                    new McpServerConfig(
                            "s2", "https://mcp2.test",
                            List.of(),
                            Map.of("K3", "literal2")));

            List<String> warnings =
                    GithubMcpAssembler
                            .warnLiteralEnvValues(
                                    servers);

            assertThat(warnings).hasSize(2);
        }

        @Test
        @DisplayName("empty env map produces"
                + " no warnings")
        void warnLiteralEnvValues_emptyEnvNoWarnings_succeeds() {
            List<McpServerConfig> servers = List.of(
                    new McpServerConfig(
                            "s1", "https://mcp.test",
                            List.of(), Map.of()));

            List<String> warnings =
                    GithubMcpAssembler
                            .warnLiteralEnvValues(
                                    servers);

            assertThat(warnings).isEmpty();
        }
    }

    @Nested
    @DisplayName("assemble — JSON generation")
    class JsonGeneration {

        @Test
        @DisplayName("generates copilot-mcp.json"
                + " with valid structure")
        void assemble_whenCalled_generatesValidJson(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "firecrawl",
                                            "https://mcp.example.com",
                                            List.of("scrape",
                                                    "crawl"),
                                            Map.of("API_KEY",
                                                    "$FIRECRAWL_KEY")))
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).hasSize(1);
            Path mcpFile =
                    outputDir.resolve("copilot-mcp.json");
            assertThat(mcpFile).exists();

            String content = Files.readString(
                    mcpFile, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("\"mcpServers\"")
                    .contains("\"firecrawl\"")
                    .contains(
                            "\"url\": \"https://mcp"
                                    + ".example.com\"")
                    .contains("\"capabilities\"")
                    .contains("\"scrape\"")
                    .contains("\"crawl\"")
                    .contains("\"env\"")
                    .contains("\"API_KEY\"")
                    .contains("\"$FIRECRAWL_KEY\"");
        }

        @Test
        @DisplayName("JSON has 2-space indentation")
        void assemble_json_has2SpaceIndent(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "s1",
                                            "https://test",
                                            List.of(),
                                            Map.of()))
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "copilot-mcp.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("  \"mcpServers\"");
        }

        @Test
        @DisplayName("JSON has trailing newline")
        void assemble_json_hasTrailingNewline(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "s1",
                                            "https://test",
                                            List.of(),
                                            Map.of()))
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "copilot-mcp.json"),
                    StandardCharsets.UTF_8);
            assertThat(content).endsWith("}\n");
        }

        @Test
        @DisplayName("server without capabilities"
                + " omits capabilities key")
        void assemble_whenCalled_serverWithoutCapabilities(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "s1",
                                            "https://test",
                                            List.of(),
                                            Map.of()))
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "copilot-mcp.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain("capabilities");
        }

        @Test
        @DisplayName("server without env omits"
                + " env key")
        void assemble_whenCalled_serverWithoutEnv(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "s1",
                                            "https://test",
                                            List.of("read"),
                                            Map.of()))
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "copilot-mcp.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("capabilities")
                    .doesNotContain("\"env\"");
        }

        @Test
        @DisplayName("multiple servers serialized"
                + " correctly")
        void assemble_multipleServers_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "server1",
                                            "https://s1.test",
                                            List.of("r"),
                                            Map.of()))
                            .addMcpServer(
                                    new McpServerConfig(
                                            "server2",
                                            "https://s2.test",
                                            List.of(),
                                            Map.of("K",
                                                    "$V")))
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "copilot-mcp.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("\"server1\"")
                    .contains("\"server2\"")
                    .contains("\"https://s1.test\"")
                    .contains("\"https://s2.test\"");
        }
    }

    @Nested
    @DisplayName("buildCopilotMcpJson — structure")
    class BuildCopilotMcpJson {

        @Test
        @DisplayName("builds valid JSON structure")
        void buildCopilotMcpJson_whenCalled_buildsValidStructure() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "test",
                                            "https://url",
                                            List.of("cap1"),
                                            Map.of("K",
                                                    "$V")))
                            .build();

            String json =
                    GithubMcpAssembler
                            .buildCopilotMcpJson(config);

            assertThat(json)
                    .startsWith("{\n")
                    .endsWith("}\n")
                    .contains("\"mcpServers\"")
                    .contains("\"test\"")
                    .contains("\"url\": \"https://url\"")
                    .contains("\"capabilities\":"
                            + " [\"cap1\"]")
                    .contains("\"K\": \"$V\"");
        }
    }

    @Nested
    @DisplayName("constructor — two-constructor pattern")
    class TwoConstructorPattern {

        @Test
        @DisplayName("default constructor creates"
                + " valid instance")
        void defaultConstructor_whenCalled_createsInstance() {
            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }

        @Test
        @DisplayName("Path constructor creates"
                + " valid instance")
        void pathConstructor_whenCalled_createsInstance(
                @TempDir Path tempDir) {
            GithubMcpAssembler assembler =
                    new GithubMcpAssembler(tempDir);

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }

        @Test
        @DisplayName("Path constructor assembles"
                + " with custom resourcesDir")
        void pathConstructor_withCustomResourcesdir_assemblesCorrectly(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "test",
                                            "https://mcp.test",
                                            List.of("read"),
                                            Map.of("K",
                                                    "$V")))
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).hasSize(1);
            Path mcpFile =
                    outputDir.resolve("copilot-mcp.json");
            assertThat(mcpFile).exists();
            String content = Files.readString(
                    mcpFile, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("\"mcpServers\"")
                    .contains("\"test\"");
        }

        @Test
        @DisplayName("default constructor assembles"
                + " identically to before")
        void defaultConstructor_whenCalled_assemblesIdentically(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "srv",
                                            "https://mcp.test",
                                            List.of(),
                                            Map.of()))
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).hasSize(1);
            Path mcpFile =
                    outputDir.resolve("copilot-mcp.json");
            assertThat(mcpFile).exists();
        }

        @Test
        @DisplayName("assembleWithWarnings works"
                + " with Path constructor")
        void pathConstructor_withPathConstructor_assembleWithWarnings(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubMcpAssembler assembler =
                    new GithubMcpAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addMcpServer(
                                    new McpServerConfig(
                                            "srv",
                                            "https://mcp.test",
                                            List.of(),
                                            Map.of("KEY",
                                                    "literal")))
                            .build();

            AssemblerResult result =
                    assembler.assembleWithWarnings(
                            config, outputDir);

            assertThat(result.files()).hasSize(1);
            assertThat(result.warnings()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("AssemblerResult — record contract")
    class AssemblerResultContract {

        @Test
        @DisplayName("immutable files and warnings")
        void create_whenCalled_immutableCollections() {
            AssemblerResult result =
                    AssemblerResult.of(
                            List.of("file1"),
                            List.of("warn1"));

            assertThat(result.files())
                    .containsExactly("file1");
            assertThat(result.warnings())
                    .containsExactly("warn1");
        }

        @Test
        @DisplayName("empty result has empty lists")
        void create_emptyResult_succeeds() {
            AssemblerResult result =
                    AssemblerResult.empty();

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }
    }
}
