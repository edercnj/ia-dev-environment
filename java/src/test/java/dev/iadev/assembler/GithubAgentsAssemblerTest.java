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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GithubAgentsAssembler -- the eleventh
 * assembler in the pipeline, generating
 * .github/agents/*.agent.md files for GitHub Copilot.
 */
@DisplayName("GithubAgentsAssembler")
class GithubAgentsAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void isAssemblerInstance() {
            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("selectGithubConditionalAgents")
    class SelectConditionalAgents {

        @Test
        @DisplayName("devops-engineer when container"
                + " is docker")
        void devopsWhenDocker() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("none")
                            .iac("none")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("devops-engineer when orchestrator"
                + " is kubernetes")
        void devopsWhenKubernetes() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("kubernetes")
                            .iac("none")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("devops-engineer when iac present")
        void devopsWhenIac() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("terraform")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("devops-engineer when service"
                + " mesh present")
        void devopsWhenServiceMesh() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .serviceMesh("istio")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("no devops-engineer when all none")
        void noDevopsWhenAllNone() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .doesNotContain(
                            "devops-engineer.md");
        }

        @Test
        @DisplayName("api-engineer when REST interface")
        void apiEngineerWhenRest() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("api-engineer.md");
        }

        @Test
        @DisplayName("api-engineer when gRPC interface")
        void apiEngineerWhenGrpc() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("grpc")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("api-engineer.md");
        }

        @Test
        @DisplayName("api-engineer when GraphQL"
                + " interface")
        void apiEngineerWhenGraphql() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("graphql")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("api-engineer.md");
        }

        @Test
        @DisplayName("no api-engineer when no REST"
                + " gRPC or GraphQL")
        void noApiEngineerWhenNoApiInterface() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .doesNotContain("api-engineer.md");
        }

        @Test
        @DisplayName("event-engineer when event-driven")
        void eventEngineerWhenEventDriven() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .eventDriven(true)
                            .clearInterfaces()
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("event-engineer.md");
        }

        @Test
        @DisplayName("event-engineer when"
                + " event-consumer interface")
        void eventEngineerWhenEventConsumer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("event-consumer")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("event-engineer.md");
        }

        @Test
        @DisplayName("event-engineer when"
                + " event-producer interface")
        void eventEngineerWhenEventProducer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("event-producer")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("event-engineer.md");
        }

        @Test
        @DisplayName("no event-engineer when not"
                + " event-driven and no event interfaces")
        void noEventEngineerWhenNoEvents() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .eventDriven(false)
                            .clearInterfaces()
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .doesNotContain(
                            "event-engineer.md");
        }

        @Test
        @DisplayName("empty when all conditions false")
        void emptyWhenAllFalse() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .eventDriven(false)
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents).isEmpty();
        }
    }

    @Nested
    @DisplayName("renderAgent — .agent.md extension")
    class RenderAgent {

        @Test
        @DisplayName("renames .md to .agent.md")
        void renamesMdToAgentMd(
                @TempDir Path tempDir)
                throws IOException {
            Path srcFile = tempDir.resolve(
                    "test-agent.md");
            Files.writeString(srcFile,
                    "---\ntools:\n  - tool1\n---\n"
                            + "# Agent",
                    StandardCharsets.UTF_8);

            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler(tempDir);
            TemplateEngine engine = new TemplateEngine();

            String result = assembler.renderAgent(
                    srcFile, agentsDir, engine,
                    Map.of());

            assertThat(result)
                    .endsWith("test-agent.agent.md");
            Path dest = agentsDir.resolve(
                    "test-agent.agent.md");
            assertThat(dest).exists();
        }

        @Test
        @DisplayName("applies placeholder replacement")
        void appliesPlaceholderReplacement(
                @TempDir Path tempDir)
                throws IOException {
            Path srcFile = tempDir.resolve(
                    "my-agent.md");
            Files.writeString(srcFile,
                    "# Agent for {{PROJECT_NAME}}",
                    StandardCharsets.UTF_8);

            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler(tempDir);
            TemplateEngine engine = new TemplateEngine();

            assembler.renderAgent(
                    srcFile, agentsDir, engine,
                    Map.of());

            Path dest = agentsDir.resolve(
                    "my-agent.agent.md");
            String content = Files.readString(
                    dest, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("{{PROJECT_NAME}}");
        }
    }

    @Nested
    @DisplayName("assembleCore — core agents")
    class AssembleCore {

        @Test
        @DisplayName("generates core agents from"
                + " classpath templates")
        void generatesCoreAgents(
                @TempDir Path tempDir)
                throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            TemplateEngine engine = new TemplateEngine();

            List<String> files =
                    assembler.assembleCore(
                            agentsDir, engine,
                            java.util.Map.of());

            assertThat(files).isNotEmpty();
            assertThat(files)
                    .allMatch(f -> f.endsWith(
                            ".agent.md"));
        }

        @Test
        @DisplayName("core agents sorted alphabetically")
        void coreAgentsSortedAlphabetically(
                @TempDir Path tempDir)
                throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            TemplateEngine engine = new TemplateEngine();

            List<String> files =
                    assembler.assembleCore(
                            agentsDir, engine,
                            java.util.Map.of());

            List<String> fileNames = files.stream()
                    .map(f -> Path.of(f).getFileName()
                            .toString())
                    .toList();
            assertThat(fileNames).isSorted();
        }

        @Test
        @DisplayName("returns empty for missing"
                + " core directory")
        void returnsEmptyForMissingDir(
                @TempDir Path tempDir)
                throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler(
                            tempDir.resolve("nonexistent"));
            TemplateEngine engine = new TemplateEngine();

            List<String> files =
                    assembler.assembleCore(
                            agentsDir, engine,
                            java.util.Map.of());

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("assembleDeveloper — language agent")
    class AssembleDeveloper {

        @Test
        @DisplayName("generates developer agent for"
                + " known language")
        void generatesDeveloperForKnown(
                @TempDir Path tempDir)
                throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            String result =
                    assembler.assembleDeveloper(
                            config, agentsDir, engine,
                            java.util.Map.of());

            assertThat(result).isNotNull();
            assertThat(result).endsWith(
                    "typescript-developer.agent.md");
        }

        @Test
        @DisplayName("returns null for unknown language")
        void returnsNullForUnknown(
                @TempDir Path tempDir)
                throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("cobol", "85")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            String result =
                    assembler.assembleDeveloper(
                            config, agentsDir, engine,
                            java.util.Map.of());

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("returns null when developers"
                + " dir absent")
        void returnsNullWhenDirAbsent(
                @TempDir Path tempDir)
                throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler(
                            tempDir.resolve("nonexistent"));
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            String result =
                    assembler.assembleDeveloper(
                            config, agentsDir, engine,
                            java.util.Map.of());

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("assembleWithWarnings — full result")
    class AssembleWithWarnings {

        @Test
        @DisplayName("returns files and no warnings"
                + " for known language")
        void returnsFilesNoWarnings(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            AssemblerResult result =
                    assembler.assembleWithWarnings(
                            config, engine, outputDir);

            assertThat(result.files()).isNotEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("warning for unknown developer"
                + " language")
        void warningForUnknownLanguage(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("cobol", "85")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            AssemblerResult result =
                    assembler.assembleWithWarnings(
                            config, engine, outputDir);

            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "Developer agent template"
                                    + " missing"));
        }

        @Test
        @DisplayName("all agents have .agent.md"
                + " extension")
        void allAgentsHaveAgentMdExtension(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            AssemblerResult result =
                    assembler.assembleWithWarnings(
                            config, engine, outputDir);

            assertThat(result.files())
                    .allMatch(f -> f.endsWith(
                            ".agent.md"));
        }
    }

    @Nested
    @DisplayName("assemble — full pipeline integration")
    class AssembleIntegration {

        @Test
        @DisplayName("generates agents from classpath"
                + " templates")
        void generatesAgentsFromClasspath(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isNotEmpty();
            assertThat(files)
                    .allMatch(f -> f.endsWith(
                            ".agent.md"));
        }

        @Test
        @DisplayName("includes conditional agents"
                + " based on config")
        void includesConditionalAgents(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .language("typescript", "5")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .filter(f -> f.contains(
                            "devops-engineer")))
                    .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("AssemblerResult — record contract")
    class AssemblerResultContract {

        @Test
        @DisplayName("immutable files and warnings")
        void immutableCollections() {
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
        void emptyResult() {
            AssemblerResult result =
                    AssemblerResult.empty();

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }
    }
}
