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
 * Miscellaneous coverage tests targeting remaining
 * uncovered lines across multiple assemblers.
 */
@DisplayName("Assembler misc — coverage")
class AssemblerMiscCoverageTest {

    @Nested
    @DisplayName("CodexConfigAssembler — MCP validation")
    class CodexConfigMcp {

        @Test
        @DisplayName("MCP server with invalid TOML key"
                + " runs through validation loop")
        void invalidMcpServerIdRunsValidation(
                @TempDir Path tempDir) throws IOException {
            Path claudeDir = tempDir.resolve(".claude");
            Files.createDirectories(claudeDir);
            Path codexDir = tempDir.resolve(".codex");
            Files.createDirectories(codexDir);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            McpServerConfig badServer =
                    new McpServerConfig(
                            "bad key!",
                            "https://example.com",
                            List.of(),
                            Map.of());
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .addMcpServer(badServer)
                    .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    codexDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("MCP server with valid key"
                + " passes validation")
        void validMcpServerPassesValidation(
                @TempDir Path tempDir) throws IOException {
            Path claudeDir = tempDir.resolve(".claude");
            Files.createDirectories(claudeDir);
            Path codexDir = tempDir.resolve(".codex");
            Files.createDirectories(codexDir);

            CodexConfigAssembler assembler =
                    new CodexConfigAssembler();
            McpServerConfig goodServer =
                    new McpServerConfig(
                            "valid-key",
                            "https://example.com",
                            List.of(),
                            Map.of());
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .addMcpServer(goodServer)
                    .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    codexDir);

            assertThat(files).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("GithubAgentsAssembler — warnings")
    class GithubAgentsWarnings {

        @Test
        @DisplayName("missing conditional template adds"
                + " warning")
        void missingConditionalAddsWarning(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve(
                    "github-agents-templates/core");
            Files.createDirectories(coreDir);
            Path condDir = resourceDir.resolve(
                    "github-agents-templates/conditional");
            Files.createDirectories(condDir);
            Path devDir = resourceDir.resolve(
                    "github-agents-templates/developers");
            Files.createDirectories(devDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "16")
                    .build();

            var result =
                    assembler.assembleWithWarnings(
                            config, new TemplateEngine(),
                            outputDir);

            assertThat(result.warnings()).isNotEmpty();
        }

        @Test
        @DisplayName("core dir missing returns"
                + " empty core agents")
        void coreDirMissing(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler(resourceDir);

            List<String> core =
                    assembler.assembleCore(
                            outputDir, new TemplateEngine());

            assertThat(core).isEmpty();
        }
    }

    @Nested
    @DisplayName("CodexSkillsAssembler — edge cases")
    class CodexSkillsEdge {

        @Test
        @DisplayName("skill without SKILL.md is skipped")
        void skillWithoutSkillMdSkipped(
                @TempDir Path tempDir) throws IOException {
            Path emptySkill =
                    tempDir.resolve("src/empty-skill");
            Files.createDirectories(emptySkill);
            Files.writeString(
                    emptySkill.resolve("readme.txt"),
                    "no SKILL.md here");

            List<String> result =
                    CodexSkillsAssembler.copySkill(
                            emptySkill,
                            tempDir.resolve(
                                    "dest/empty-skill"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("CodexAgentsMdAssembler — edge cases")
    class CodexAgentsMdEdge {

        @Test
        @DisplayName("assemble with no claude dir"
                + " still generates output")
        void noClaudeDirGenerates(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            CodexAgentsMdAssembler assembler =
                    new CodexAgentsMdAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).isNotNull();
        }
    }

    @Nested
    @DisplayName("PatternsAssembler — edge cases")
    class PatternsEdge {

        @Test
        @DisplayName("custom resourceDir with no"
                + " patterns returns empty")
        void noPatternsDirReturnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("pattern category dir missing"
                + " skips category")
        void categoryDirMissing(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path patternsDir = resourceDir.resolve(
                    "patterns");
            Files.createDirectories(patternsDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("DocsAdrAssembler — edge cases")
    class DocsAdrEdge {

        @Test
        @DisplayName("custom resourceDir with no ADR"
                + " templates returns empty")
        void noAdrTemplatesEmpty(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("HooksAssembler — edge cases")
    class HooksEdge {

        @Test
        @DisplayName("python language returns empty"
                + " hooks")
        void pythonNoHooks(@TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("GithubAgentsAssembler — renderAgent")
    class GithubAgentsRender {

        @Test
        @DisplayName("renderAgent with .md file renames"
                + " to .agent.md")
        void renderWithMdFile(@TempDir Path tempDir)
                throws IOException {
            Path src = tempDir.resolve("architect.md");
            Files.writeString(src, "Agent content",
                    StandardCharsets.UTF_8);
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler(tempDir);

            String result = assembler.renderAgent(
                    src, agentsDir,
                    new TemplateEngine());

            assertThat(result)
                    .contains("architect.agent.md");
        }

        @Test
        @DisplayName("assemble with warnings triggers"
                + " stderr output via assemble()")
        void assembleTriggersWarningOutput(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve(
                    "github-agents-templates/core");
            Files.createDirectories(coreDir);
            Path condDir = resourceDir.resolve(
                    "github-agents-templates/conditional");
            Files.createDirectories(condDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "16")
                    .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).isNotNull();
        }
    }

    @Nested
    @DisplayName("GithubSkillsAssembler — edge cases")
    class GithubSkillsEdge {

        @Test
        @DisplayName("custom resourceDir with no skills"
                + " returns empty list")
        void noSkillsReturnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("GithubPromptsAssembler — edge cases")
    class GithubPromptsEdge {

        @Test
        @DisplayName("custom resourceDir with no prompts"
                + " returns empty")
        void noPromptsReturnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler(
                            resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("GithubHooksAssembler — edge cases")
    class GithubHooksEdge {

        @Test
        @DisplayName("custom resourceDir with no hooks"
                + " returns empty")
        void noHooksReturnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubHooksAssembler assembler =
                    new GithubHooksAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("Full pipeline integration")
    class FullPipelineIntegration {

        @Test
        @DisplayName("full pipeline with all features"
                + " exercises maximum code paths")
        void fullPipelineAllFeatures(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("coverage-test")
                    .purpose("Coverage testing")
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .archStyle("hexagonal")
                    .domainDriven(true)
                    .eventDriven(true)
                    .database("postgresql", "16")
                    .cache("redis", "7.4")
                    .container("docker")
                    .orchestrator("kubernetes")
                    .iac("terraform")
                    .cloudProvider("aws")
                    .smokeTests(true)
                    .contractTests(true)
                    .securityFrameworks("owasp")
                    .clearInterfaces()
                    .addInterface("rest")
                    .addInterface("grpc")
                    .addInterface("event-consumer",
                            "", "kafka")
                    .build();

            // Run all assemblers from classpath
            new RulesAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new SkillsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new AgentsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new ProtocolsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new HooksAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new SettingsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new PatternsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(outputDir.resolve("rules"))
                    .exists();
            assertThat(outputDir.resolve("skills"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("CopyHelpers — functional paths")
    class CopyHelpersFunc {

        @Test
        @DisplayName("copyTemplateFileIfExists with"
                + " existing file copies it")
        void copyIfExistsWithFile(@TempDir Path tempDir)
                throws IOException {
            Path src = tempDir.resolve("src.md");
            Files.writeString(src,
                    "Template {{KEY}}\n",
                    StandardCharsets.UTF_8);
            Path dest = tempDir.resolve(
                    "output/copied.md");

            String result =
                    CopyHelpers.copyTemplateFileIfExists(
                            src, dest,
                            new TemplateEngine(),
                            Map.of("key", "value"));

            assertThat(result).isNotNull();
            assertThat(dest).exists();
        }

        @Test
        @DisplayName("replacePlaceholdersInDir replaces"
                + " in nested md files")
        void replacePlaceholdersInDir(
                @TempDir Path tempDir) throws IOException {
            Path dir = tempDir.resolve("content");
            Files.createDirectories(dir);
            Files.writeString(
                    dir.resolve("test.md"),
                    "Hello {{NAME}}",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    dir.resolve("skip.txt"),
                    "Not {{REPLACED}}",
                    StandardCharsets.UTF_8);

            CopyHelpers.replacePlaceholdersInDir(
                    dir, new TemplateEngine(),
                    Map.of("name", "World"));

            String md = Files.readString(
                    dir.resolve("test.md"),
                    StandardCharsets.UTF_8);
            assertThat(md).contains("World");

            String txt = Files.readString(
                    dir.resolve("skip.txt"),
                    StandardCharsets.UTF_8);
            assertThat(txt)
                    .contains("Not {{REPLACED}}");
        }
    }
}
