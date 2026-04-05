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
 * Tests for GithubInstructionsAssembler —
 * file generation and edge cases.
 */
@DisplayName("GithubInstructionsAssembler — file gen")
class GithubInstructionsFileGenTest {

    @Nested
    @DisplayName("assemble — file generation")
    class FileGeneration {

        @Test
        @DisplayName("generates copilot-instructions.md")
        void assemble_whenCalled_generatesCopilotInstructions(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("test-project")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).isNotEmpty();
            Path global = outputDir.resolve(
                    "copilot-instructions.md");
            assertThat(global).exists();
            String content = Files.readString(
                    global, StandardCharsets.UTF_8);
            assertThat(content).contains(
                    "# Project Identity");
        }

        @Test
        @DisplayName("generates 4 contextual files")
        void assemble_whenCalled_generates4ContextualFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).hasSize(5);

            Path instructions =
                    outputDir.resolve("instructions");
            assertThat(instructions.resolve(
                    "domain.instructions.md")).exists();
            assertThat(instructions.resolve(
                    "coding-standards.instructions.md"))
                    .exists();
            assertThat(instructions.resolve(
                    "architecture.instructions.md"))
                    .exists();
            assertThat(instructions.resolve(
                    "quality-gates.instructions.md"))
                    .exists();
        }

        @Test
        @DisplayName("contextual files have"
                + " placeholders replaced")
        void assemble_whenCalled_contextualFilesHavePlaceholdersReplaced(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-service")
                            .purpose("Test service")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            Path domain = outputDir.resolve(
                    "instructions/domain.instructions.md");
            String content = Files.readString(
                    domain, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("my-service")
                    .contains("Test service")
                    .doesNotContain("{project_name}")
                    .doesNotContain("{project_purpose}");
        }

        @Test
        @DisplayName("quality gates has coverage"
                + " values replaced")
        void assemble_whenCalled_qualityGatesCoverageReplaced(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            Path qg = outputDir.resolve(
                    "instructions/quality-gates"
                            + ".instructions.md");
            String content = Files.readString(
                    qg, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("95%")
                    .contains("90%")
                    .doesNotContain("{coverage_line}")
                    .doesNotContain("{coverage_branch}");
        }
    }

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("missing templates directory"
                + " returns global file only")
        void assemble_missingTemplates_returnsGlobal(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    tempDir.resolve("empty-resources");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler(
                            resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0))
                    .contains("copilot-instructions.md");
        }

        @Test
        @DisplayName("CONTEXTUAL_INSTRUCTIONS constant"
                + " has 4 entries")
        void assemble_contextualInstructions_has4Entries() {
            assertThat(GithubInstructionsAssembler
                    .CONTEXTUAL_INSTRUCTIONS)
                    .hasSize(4)
                    .containsExactly(
                            "domain",
                            "coding-standards",
                            "architecture",
                            "quality-gates");
        }
    }
}
