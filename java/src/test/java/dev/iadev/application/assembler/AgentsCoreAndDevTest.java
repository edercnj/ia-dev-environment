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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AgentsAssembler — interface contract,
 * core agents, and developer agent generation.
 */
@DisplayName("AgentsAssembler — core + developer")
class AgentsCoreAndDevTest {

    @Nested
    @DisplayName("assemble — implements Assembler")
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
        @DisplayName("generates 7 core agents")
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
                    config, new TemplateEngine(),
                    outputDir);

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
                    "sre-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "tech-lead.md"))
                    .exists();
        }

        @Test
        @DisplayName("core agents are sorted")
        void assemble_coreAgents_sorted() {
            AgentsAssembler assembler =
                    new AgentsAssembler();

            List<String> coreAgents =
                    assembler.selectCoreAgents();

            assertThat(coreAgents).isSorted();
        }

        @Test
        @DisplayName("core agents contain expected files")
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
                    "sre-engineer.md",
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
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("assemble — developer agent")
    class DeveloperAgent {

        @Test
        @DisplayName("java generates java-developer.md")
        void assemble_java_generatesJavaDeveloper(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(outputDir.resolve(
                    "agents/java-developer.md"))
                    .exists();
        }

        @Test
        @DisplayName("typescript generates"
                + " typescript-developer.md")
        void assemble_ts_generatesTsDeveloper(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(outputDir.resolve(
                    "agents/typescript-developer.md"))
                    .exists();
            assertThat(outputDir.resolve(
                    "agents/java-developer.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("go generates go-developer.md")
        void assemble_go_generatesGoDeveloper(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("go", "1.22")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(outputDir.resolve(
                    "agents/go-developer.md"))
                    .exists();
        }

        @Test
        @DisplayName("developer agent has content")
        void assemble_developerAgent_hasContent(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("go", "1.22")
                            .framework("gin", "")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "agents/go-developer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content).contains("Go");
        }
    }
}
