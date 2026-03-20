package dev.iadev.assembler;

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
 * Tests for AgentsAssembler — golden file match
 * and edge cases.
 */
@DisplayName("AgentsAssembler — golden match + edge")
class AgentsGoldenMatchTest {

    @Nested
    @DisplayName("golden — individual + batch")
    class GoldenMatch {

        @Test
        @DisplayName("go-developer.md matches golden")
        void assemble_goDeveloper_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            assembler.assemble(
                    AgentsTestFixtures.buildGoGinConfig(),
                    new TemplateEngine(), outputDir);

            String expected = loadResource(
                    "golden/go-gin/.claude/agents/"
                            + "go-developer.md");
            if (expected != null) {
                String actual = Files.readString(
                        outputDir.resolve(
                                "agents/go-developer.md"),
                        StandardCharsets.UTF_8);
                assertThat(actual).isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("security-engineer.md matches golden")
        void assemble_securityEngineer_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            assembler.assemble(
                    AgentsTestFixtures.buildGoGinConfig(),
                    new TemplateEngine(), outputDir);

            String expected = loadResource(
                    "golden/go-gin/.claude/agents/"
                            + "security-engineer.md");
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
        @DisplayName("all agents match golden")
        void assemble_allAgentsMatchGolden_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler();
            assembler.assemble(
                    AgentsTestFixtures.buildGoGinConfig(),
                    new TemplateEngine(), outputDir);

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
                String expected = loadResource(
                        "golden/go-gin/.claude/agents/"
                                + agentFile);
                if (expected != null) {
                    String actual = Files.readString(
                            outputDir.resolve(
                                    "agents/" + agentFile),
                            StandardCharsets.UTF_8);
                    assertThat(actual)
                            .as("Agent: " + agentFile)
                            .isEqualTo(expected);
                }
            }
        }
    }

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("empty resources returns empty core")
        void assemble_emptyResources_returnsEmptyCore(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve(
                    "agents-templates/core");
            Files.createDirectories(coreDir);

            AgentsAssembler assembler =
                    new AgentsAssembler(resourceDir);

            List<String> coreAgents =
                    assembler.selectCoreAgents();

            assertThat(coreAgents).isEmpty();
        }

        @Test
        @DisplayName("missing developer template skipped")
        void assemble_missingDeveloperTemplateSkipped_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir.resolve(
                    "agents-templates/core"));
            Files.createDirectories(resourceDir.resolve(
                    "agents-templates/developers"));
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler(resourceDir);
            var config = TestConfigBuilder.builder()
                    .language("exotic-lang", "1.0")
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

            assertThat(outputDir.resolve(
                    "agents/exotic-lang-developer.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("missing conditional template skipped")
        void assemble_missingConditionalTemplateSkipped_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir.resolve(
                    "agents-templates/core"));
            Files.createDirectories(resourceDir.resolve(
                    "agents-templates/conditional"));
            Files.createDirectories(resourceDir.resolve(
                    "agents-templates/developers"));
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler(resourceDir);
            var config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
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

            assertThat(outputDir.resolve(
                    "agents/database-engineer.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("selectCoreAgents empty when missing")
        void assemble_whenCalled_selectCoreAgentsEmptyWhenMissing(
                @TempDir Path tempDir) {
            AgentsAssembler assembler =
                    new AgentsAssembler(
                            tempDir.resolve("res"));

            List<String> agents =
                    assembler.selectCoreAgents();

            assertThat(agents).isEmpty();
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
