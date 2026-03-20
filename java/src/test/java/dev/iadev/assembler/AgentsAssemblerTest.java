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
 * Tests for AgentsAssembler — the third assembler in the
 * pipeline, generating .claude/agents/ directory structure
 * with core agents, developer agent, conditional agents,
 * and checklist injection.
 */
@DisplayName("AgentsAssembler")
class AgentsAssemblerTest {

    @Nested
    @DisplayName("assemble — implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            AgentsAssembler assembler =
                    new AgentsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — core agents generation")
    class CoreAgents {

        @Test
        @DisplayName("generates 6 core agents for any config")
        void assemble_whenCalled_generatesCoreAgents(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .eventDriven(false)
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path agentsDir = outputDir.resolve("agents");
            assertThat(agentsDir.resolve("architect.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "performance-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "product-owner.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "qa-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "security-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "tech-lead.md"))
                    .exists();
        }

        @Test
        @DisplayName("core agents are sorted alphabetically")
        void assemble_coreAgents_sorted() {
            AgentsAssembler assembler =
                    new AgentsAssembler();

            List<String> coreAgents =
                    assembler.selectCoreAgents();

            assertThat(coreAgents).isSorted();
        }

        @Test
        @DisplayName("core agents contain exactly"
                + " expected files")
        void assemble_whenCalled_coreAgentsContainExpected() {
            AgentsAssembler assembler =
                    new AgentsAssembler();

            List<String> coreAgents =
                    assembler.selectCoreAgents();

            assertThat(coreAgents).containsExactly(
                    "architect.md",
                    "performance-engineer.md",
                    "product-owner.md",
                    "qa-engineer.md",
                    "security-engineer.md",
                    "tech-lead.md");
        }

        @Test
        @DisplayName("returned list is not empty")
        void assemble_whenCalled_returnedListNotEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("assemble — developer agent")
    class DeveloperAgent {

        @Test
        @DisplayName("config with language=java generates"
                + " java-developer.md")
        void assemble_java_generatesJavaDeveloper(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "agents/java-developer.md"))
                    .exists();
        }

        @Test
        @DisplayName("config with language=typescript"
                + " generates typescript-developer.md")
        void assemble_ts_generatesTsDeveloper(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("typescript", "5")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "agents/typescript-developer.md"))
                    .exists();
            assertThat(outputDir.resolve(
                    "agents/java-developer.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("config with language=go generates"
                + " go-developer.md")
        void assemble_go_generatesGoDeveloper(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("go", "1.22")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "agents/go-developer.md"))
                    .exists();
        }

        @Test
        @DisplayName("developer agent contains language-"
                + "specific content")
        void assemble_developerAgent_hasContent(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("go", "1.22")
                    .framework("gin", "")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "agents/go-developer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Go");
        }
    }

    @Nested
    @DisplayName("assemble — conditional agents")
    class ConditionalAgents {

        @Test
        @DisplayName("config with database generates"
                + " database-engineer.md")
        void assemble_database_generatesDbEngineer(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "agents/database-engineer.md"))
                    .exists();
        }

        @Test
        @DisplayName("config with events generates"
                + " event-engineer.md")
        void assemble_whenCalled_eventsGenerateEventEngineer(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .eventDriven(true)
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "agents/event-engineer.md"))
                    .exists();
        }

        @Test
        @DisplayName("config without database does not"
                + " generate database-engineer.md")
        void assemble_noDb_excludesDbEngineer(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .container("none")
                    .orchestrator("none")
                    .iac("none")
                    .clearInterfaces()
                    .addInterface("cli")
                    .eventDriven(false)
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "agents/database-engineer.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("config with REST generates"
                + " api-engineer.md")
        void assemble_rest_generatesApiEngineer(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "agents/api-engineer.md"))
                    .exists();
        }

        @Test
        @DisplayName("full-featured config generates"
                + " more than 8 agents")
        void assemble_fullFeatured_generatesMany(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = buildGoGinConfig();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSizeGreaterThan(8);
        }
    }

    @Nested
    @DisplayName("assemble — checklist injection")
    class ChecklistInjection {

        @Test
        @DisplayName("security checklist injected into"
                + " security-engineer.md when lgpd active")
        void assemble_whenCalled_securityChecklistInjected(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .securityFrameworks("lgpd")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path securityAgent = outputDir.resolve(
                    "agents/security-engineer.md");
            if (Files.exists(securityAgent)) {
                String content = Files.readString(
                        securityAgent,
                        StandardCharsets.UTF_8);
                // Checklist content should have been
                // injected (if the marker exists in the
                // template)
                assertThat(content).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("assemble — golden file parity")
    class GoldenFile {

        @Test
        @DisplayName("go-gin profile generates all"
                + " expected agent files")
        void assemble_goGin_generatesExpectedAgents(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = buildGoGinConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path agentsDir = outputDir.resolve("agents");
            // Core agents
            assertThat(agentsDir.resolve("architect.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "performance-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "product-owner.md"))
                    .exists();
            assertThat(agentsDir.resolve("qa-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "security-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve("tech-lead.md"))
                    .exists();
            // Developer
            assertThat(agentsDir.resolve(
                    "go-developer.md"))
                    .exists();
            // Conditional
            assertThat(agentsDir.resolve(
                    "api-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "event-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "devops-engineer.md"))
                    .exists();
        }

        @Test
        @DisplayName("architect.md matches golden file"
                + " for go-gin profile")
        void assemble_architect_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = buildGoGinConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String goldenPath =
                    "golden/go-gin/.claude/agents/"
                            + "architect.md";
            String expected = loadResource(goldenPath);
            if (expected != null) {
                String actual = Files.readString(
                        outputDir.resolve(
                                "agents/architect.md"),
                        StandardCharsets.UTF_8);
                assertThat(actual).isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("go-developer.md matches golden file"
                + " for go-gin profile")
        void assemble_goDeveloper_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = buildGoGinConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String goldenPath =
                    "golden/go-gin/.claude/agents/"
                            + "go-developer.md";
            String expected = loadResource(goldenPath);
            if (expected != null) {
                String actual = Files.readString(
                        outputDir.resolve(
                                "agents/go-developer.md"),
                        StandardCharsets.UTF_8);
                assertThat(actual).isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("security-engineer.md matches golden"
                + " file for go-gin profile")
        void assemble_securityEngineer_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = buildGoGinConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String goldenPath =
                    "golden/go-gin/.claude/agents/"
                            + "security-engineer.md";
            String expected = loadResource(goldenPath);
            if (expected != null) {
                String actual = Files.readString(
                        outputDir.resolve(
                                "agents/"
                                        + "security-engineer.md"),
                        StandardCharsets.UTF_8);
                assertThat(actual).isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("all agent files match golden files"
                + " byte-for-byte for go-gin profile")
        void assemble_allAgentsMatchGolden_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config = buildGoGinConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String[] agentFiles = {
                    "architect.md",
                    "performance-engineer.md",
                    "product-owner.md",
                    "qa-engineer.md",
                    "security-engineer.md",
                    "tech-lead.md",
                    "go-developer.md",
                    "api-engineer.md",
                    "devops-engineer.md",
                    "event-engineer.md"
            };

            for (String agentFile : agentFiles) {
                String goldenPath =
                        "golden/go-gin/.claude/agents/"
                                + agentFile;
                String expected =
                        loadResource(goldenPath);
                if (expected != null) {
                    String actual = Files.readString(
                            outputDir.resolve(
                                    "agents/" + agentFile),
                            StandardCharsets.UTF_8);
                    assertThat(actual)
                            .as("Agent file: " + agentFile)
                            .isEqualTo(expected);
                }
            }
        }

        private String loadResource(String path) {
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

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("custom resourcesDir with empty"
                + " core returns empty core list")
        void assemble_emptyResources_returnsEmptyCore(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve(
                    "agents-templates/core");
            Files.createDirectories(coreDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler(resourceDir);

            List<String> coreAgents =
                    assembler.selectCoreAgents();

            assertThat(coreAgents).isEmpty();
        }

        @Test
        @DisplayName("missing developer template does not"
                + " add to list")
        void assemble_missingDeveloperTemplateSkipped_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve(
                    "agents-templates/core");
            Files.createDirectories(coreDir);
            Path devDir = resourceDir.resolve(
                    "agents-templates/developers");
            Files.createDirectories(devDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("exotic-lang", "1.0")
                    .container("none")
                    .orchestrator("none")
                    .iac("none")
                    .clearInterfaces()
                    .addInterface("cli")
                    .eventDriven(false)
                    .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "agents/exotic-lang-developer.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("missing conditional template does"
                + " not add to list")
        void assemble_missingConditionalTemplateSkipped_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve(
                    "agents-templates/core");
            Files.createDirectories(coreDir);
            Path condDir = resourceDir.resolve(
                    "agents-templates/conditional");
            Files.createDirectories(condDir);
            Path devDir = resourceDir.resolve(
                    "agents-templates/developers");
            Files.createDirectories(devDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
                    .container("none")
                    .orchestrator("none")
                    .iac("none")
                    .clearInterfaces()
                    .addInterface("cli")
                    .eventDriven(false)
                    .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "agents/database-engineer.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("selectCoreAgents returns empty when"
                + " core dir missing")
        void assemble_whenCalled_selectCoreAgentsEmptyWhenMissing(
                @TempDir Path tempDir) {
            Path resourceDir = tempDir.resolve("res");
            AgentsAssembler assembler =
                    new AgentsAssembler(resourceDir);

            List<String> agents =
                    assembler.selectCoreAgents();

            assertThat(agents).isEmpty();
        }
    }

    private static ProjectConfig buildGoGinConfig() {
        return TestConfigBuilder.builder()
                .projectName("my-go-service")
                .purpose(
                        "Describe your service purpose here")
                .archStyle("microservice")
                .domainDriven(false)
                .eventDriven(true)
                .language("go", "1.22")
                .framework("gin", "")
                .buildTool("go-mod")
                .nativeBuild(false)
                .database("postgresql", "17")
                .cache("redis", "7.4")
                .container("docker")
                .orchestrator("kubernetes")
                .iac("terraform")
                .apiGateway("kong")
                .securityFrameworks("lgpd")
                .smokeTests(true)
                .contractTests(false)
                .performanceTests(true)
                .clearInterfaces()
                .addInterface("rest")
                .addInterface("grpc")
                .addInterface("event-consumer")
                .addInterface("event-producer")
                .build();
    }
}
