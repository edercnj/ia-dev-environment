package dev.iadev.assembler;

import dev.iadev.domain.model.ProjectConfig;
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
 * Tests for ContextualInstructionsAssembler — generates
 * contextual instruction files from templates.
 */
@DisplayName("ContextualInstructionsAssembler")
class ContextualInstructionsAssemblerTest {

    @Nested
    @DisplayName("generate — file output")
    class Generate {

        @Test
        @DisplayName("generates 4 instruction files from"
                + " classpath resources")
        void assemble_whenCalled_generates4Files(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    resolveClasspathResources();
            Path instructionsDir =
                    tempDir.resolve("instructions");
            Files.createDirectories(instructionsDir);

            ContextualInstructionsAssembler assembler =
                    new ContextualInstructionsAssembler(
                            resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.generate(
                    config, instructionsDir);

            assertThat(files).hasSize(4);
        }

        @Test
        @DisplayName("missing templates dir returns empty")
        void assemble_missingTemplates_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    tempDir.resolve("empty-resources");
            Files.createDirectories(resourceDir);
            Path instructionsDir =
                    tempDir.resolve("instructions");
            Files.createDirectories(instructionsDir);

            ContextualInstructionsAssembler assembler =
                    new ContextualInstructionsAssembler(
                            resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.generate(
                    config, instructionsDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("missing individual template skips"
                + " that file")
        void assemble_missingTemplate_skipsFile(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    tempDir.resolve("partial-res");
            Path templatesDir = resourceDir.resolve(
                    "github-instructions-templates");
            Files.createDirectories(templatesDir);
            Files.writeString(
                    templatesDir.resolve("domain.md"),
                    "Domain: {project_name}\n");

            Path instructionsDir =
                    tempDir.resolve("instructions");
            Files.createDirectories(instructionsDir);

            ContextualInstructionsAssembler assembler =
                    new ContextualInstructionsAssembler(
                            resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("test")
                            .build();

            List<String> files = assembler.generate(
                    config, instructionsDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0)).contains(
                    "domain.instructions.md");
        }
    }

    @Nested
    @DisplayName("buildPlaceholderContext")
    class BuildPlaceholderContext {

        @Test
        @DisplayName("contains all 10 expected keys")
        void assemble_whenCalled_containsAllKeys() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-project")
                            .purpose("Test purpose")
                            .language("java", "21")
                            .framework("spring", "3.4")
                            .buildTool("maven")
                            .archStyle("hexagonal")
                            .build();

            Map<String, String> context =
                    ContextualInstructionsAssembler
                            .buildPlaceholderContext(config);

            assertThat(context).hasSize(10);
            assertThat(context)
                    .containsEntry("project_name",
                            "my-project")
                    .containsEntry("project_purpose",
                            "Test purpose")
                    .containsEntry("language_name", "java")
                    .containsEntry("language_version", "21")
                    .containsEntry("framework_name",
                            "spring")
                    .containsEntry("framework_version",
                            "3.4")
                    .containsEntry("build_tool", "maven")
                    .containsEntry("architecture_style",
                            "hexagonal");
        }
    }

    @Nested
    @DisplayName("replaceSingleBracePlaceholders")
    class ReplacePlaceholders {

        @Test
        @DisplayName("replaces known placeholders")
        void assemble_whenCalled_replacesKnown() {
            String content = "Hello {name}, v{version}";
            Map<String, String> context = Map.of(
                    "name", "world",
                    "version", "1.0");

            String result =
                    ContextualInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("Hello world, v1.0");
        }

        @Test
        @DisplayName("preserves unknown placeholders")
        void assemble_whenCalled_preservesUnknown() {
            String content = "Value: {unknown}";
            Map<String, String> context = Map.of();

            String result =
                    ContextualInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("Value: {unknown}");
        }

        @Test
        @DisplayName("does not match double braces")
        void assemble_whenCalled_doesNotMatchDoubleBraces() {
            String content = "Keep {{this}} intact";
            Map<String, String> context = Map.of(
                    "this", "replaced");

            String result =
                    ContextualInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("Keep {{this}} intact");
        }
    }

    @Nested
    @DisplayName("CONTEXTUAL_INSTRUCTIONS constant")
    class ContextualInstructionsConstant {

        @Test
        @DisplayName("has 4 entries")
        void assemble_whenCalled_has4Entries() {
            assertThat(ContextualInstructionsAssembler
                    .CONTEXTUAL_INSTRUCTIONS)
                    .hasSize(4)
                    .containsExactly(
                            "domain",
                            "coding-standards",
                            "architecture",
                            "quality-gates");
        }
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(
                        "github-instructions-templates");
    }
}
