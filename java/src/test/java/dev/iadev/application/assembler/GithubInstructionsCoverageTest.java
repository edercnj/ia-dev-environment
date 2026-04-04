package dev.iadev.application.assembler;

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
 * Additional coverage tests for
 * GithubInstructionsAssembler — targeting uncovered
 * branches.
 */
@DisplayName("GithubInstructionsAssembler — coverage")
class GithubInstructionsCoverageTest {

    @Nested
    @DisplayName("buildCopilotInstructions — edge cases")
    class BuildCopilotEdge {

        @Test
        @DisplayName("null framework version handled")
        void assemble_nullFrameworkVersion_succeeds() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .framework("axum", "")
                    .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .contains("| Framework | Axum |");
        }

        @Test
        @DisplayName("capitalize handles null input")
        void assemble_whenCalled_capitalizeNull() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("test")
                    .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .isNotEmpty()
                    .contains("test");
        }
    }

    @Nested
    @DisplayName("buildPlaceholderContext")
    class BuildPlaceholderContext {

        @Test
        @DisplayName("contains all 10 expected keys")
        void create_whenCalled_allKeysPresent() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("ctx-test")
                    .purpose("Testing")
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .archStyle("hexagonal")
                    .build();

            Map<String, String> context =
                    GithubInstructionsAssembler
                            .buildPlaceholderContext(config);

            assertThat(context).hasSize(10);
            assertThat(context)
                    .containsEntry("project_name",
                            "ctx-test")
                    .containsEntry("project_purpose",
                            "Testing")
                    .containsEntry("language_name", "java")
                    .containsEntry("language_version", "21")
                    .containsEntry("framework_name",
                            "quarkus")
                    .containsEntry("framework_version",
                            "3.17")
                    .containsEntry("build_tool", "maven")
                    .containsEntry("architecture_style",
                            "hexagonal")
                    .containsEntry("coverage_line", "95")
                    .containsEntry("coverage_branch", "90");
        }
    }

    @Nested
    @DisplayName("replaceSingleBracePlaceholders"
            + " — edge cases")
    class ReplacePlaceholdersEdge {

        @Test
        @DisplayName("no placeholders returns content"
                + " unchanged")
        void replaceSingleBracePlaceholders_noPlaceholders_succeeds() {
            String content = "No placeholders here";
            Map<String, String> context = Map.of(
                    "key", "value");

            String result =
                    GithubInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("No placeholders here");
        }

        @Test
        @DisplayName("multiple replacements in same"
                + " line")
        void replaceSingleBracePlaceholders_multipleInSameLine_succeeds() {
            String content = "{a} and {b} and {c}";
            Map<String, String> context = Map.of(
                    "a", "A", "b", "B", "c", "C");

            String result =
                    GithubInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("A and B and C");
        }

        @Test
        @DisplayName("mixed known and unknown"
                + " placeholders")
        void replaceSingleBracePlaceholders_whenCalled_mixedKnownUnknown() {
            String content = "{known} and {unknown}";
            Map<String, String> context = Map.of(
                    "known", "REPLACED");

            String result =
                    GithubInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("REPLACED and {unknown}");
        }

        @Test
        @DisplayName("empty content returns empty")
        void replaceSingleBracePlaceholders_emptyContent_succeeds() {
            String result =
                    GithubInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    "", Map.of());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("generateContextual — edge cases")
    class GenerateContextualEdge {

        @Test
        @DisplayName("template file missing for one"
                + " instruction is skipped")
        void generateContextual_whenCalled_templateMissing(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path templatesDir = resourceDir.resolve(
                    "github-instructions-templates");
            Files.createDirectories(templatesDir);
            Files.writeString(
                    templatesDir.resolve("domain.md"),
                    "# Domain {project_name}");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler(
                            resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("test-project")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(
                    f -> f.contains(
                            "copilot-instructions.md"));
            assertThat(files).anyMatch(
                    f -> f.contains(
                            "domain.instructions.md"));
            assertThat(files).noneMatch(
                    f -> f.contains(
                            "coding-standards"
                                    + ".instructions.md"));
        }
    }

    @Nested
    @DisplayName("formatInterfaces — edge cases")
    class FormatInterfacesEdge {

        @Test
        @DisplayName("single event-consumer interface")
        void formatInterfaces_singleEventConsumer_succeeds() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .clearInterfaces()
                    .addInterface("event-consumer")
                    .build();

            String result =
                    GithubInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result)
                    .isEqualTo("event-consumer");
        }
    }
}
