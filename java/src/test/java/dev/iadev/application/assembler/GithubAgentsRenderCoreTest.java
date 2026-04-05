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
 * Tests for GithubAgentsAssembler — renderAgent,
 * assembleCore, assembleDeveloper, assembleWithWarnings,
 * full pipeline, and AssemblerResult contract.
 */
@DisplayName("GithubAgentsAssembler — render + core")
class GithubAgentsRenderCoreTest {

    @Nested
    @DisplayName("renderAgent — .agent.md extension")
    class RenderAgent {

        @Test
        @DisplayName("renames .md to .agent.md")
        void renderAgent_whenCalled_renamesMdToAgentMd(
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
            String result = assembler.renderAgent(
                    srcFile, agentsDir,
                    new TemplateEngine(), Map.of());
            assertThat(result)
                    .endsWith("test-agent.agent.md");
            assertThat(agentsDir.resolve(
                    "test-agent.agent.md")).exists();
        }

        @Test
        @DisplayName("applies placeholder replacement")
        void renderAgent_whenCalled_appliesPlaceholders(
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
            assembler.renderAgent(
                    srcFile, agentsDir,
                    new TemplateEngine(), Map.of());
            String content = Files.readString(
                    agentsDir.resolve(
                            "my-agent.agent.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("{{PROJECT_NAME}}");
        }
    }

    @Nested
    @DisplayName("assembleCore — core agents")
    class AssembleCore {

        @Test
        @DisplayName("generates core agents")
        void assembleCore_whenCalled_generates(
                @TempDir Path tempDir)
                throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);
            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            List<String> files =
                    assembler.assembleCore(
                            agentsDir,
                            new TemplateEngine(),
                            Map.of());
            assertThat(files).isNotEmpty();
            assertThat(files)
                    .allMatch(f -> f.endsWith(
                            ".agent.md"));
        }

        @Test
        @DisplayName("core agents sorted")
        void assembleCore_sorted(
                @TempDir Path tempDir)
                throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);
            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();
            List<String> files =
                    assembler.assembleCore(
                            agentsDir,
                            new TemplateEngine(),
                            Map.of());
            List<String> fileNames = files.stream()
                    .map(f -> Path.of(f).getFileName()
                            .toString())
                    .toList();
            assertThat(fileNames).isSorted();
        }

        @Test
        @DisplayName("empty for missing dir")
        void assembleCore_emptyForMissingDir(
                @TempDir Path tempDir)
                throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);
            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler(
                            tempDir.resolve("nonexistent"));
            List<String> files =
                    assembler.assembleCore(
                            agentsDir,
                            new TemplateEngine(),
                            Map.of());
            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("assembleDeveloper — language agent")
    class AssembleDeveloper {

        @Test
        @DisplayName("generates for known language")
        void assembleDeveloper_known_generates(
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
            var result =
                    assembler.assembleDeveloper(
                            config, agentsDir,
                            new TemplateEngine(),
                            Map.of());
            assertThat(result).isPresent();
            assertThat(result.orElseThrow()).endsWith(
                    "typescript-developer.agent.md");
        }

        @Test
        @DisplayName("empty for unknown language")
        void assembleDeveloper_unknown_empty(
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
            var result =
                    assembler.assembleDeveloper(
                            config, agentsDir,
                            new TemplateEngine(),
                            Map.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty when dir absent")
        void assembleDeveloper_dirAbsent_empty(
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
            var result =
                    assembler.assembleDeveloper(
                            config, agentsDir,
                            new TemplateEngine(),
                            Map.of());
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("assembleWithWarnings")
    class AssembleWithWarnings {

        @Test
        @DisplayName("returns files no warnings")
        void assembleWithWarnings_known_noWarnings(
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
            AssemblerResult result =
                    assembler.assembleWithWarnings(
                            config, new TemplateEngine(),
                            outputDir);
            assertThat(result.files()).isNotEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("warning for unknown language")
        void assembleWithWarnings_unknown_warning(
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
            AssemblerResult result =
                    assembler.assembleWithWarnings(
                            config, new TemplateEngine(),
                            outputDir);
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "Developer agent template"
                                    + " missing"));
        }

        @Test
        @DisplayName("all .agent.md extension")
        void assembleWithWarnings_agentMdExtension(
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
            AssemblerResult result =
                    assembler.assembleWithWarnings(
                            config, new TemplateEngine(),
                            outputDir);
            assertThat(result.files())
                    .allMatch(f -> f.endsWith(
                            ".agent.md"));
        }
    }

    @Nested
    @DisplayName("assemble — full pipeline")
    class AssembleIntegration {

        @Test
        @DisplayName("generates from classpath")
        void assemble_classpath_generates(
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
            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(files).isNotEmpty();
            assertThat(files)
                    .allMatch(f -> f.endsWith(
                            ".agent.md"));
        }

        @Test
        @DisplayName("includes conditional agents")
        void assemble_conditional_included(
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
            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
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
