package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for platform-aware ReadmeAssembler generation.
 */
@DisplayName("ReadmeAssembler — platform filtering")
class ReadmeAssemblerPlatformTest {

    @Nested
    @DisplayName("generateReadme with platforms")
    class GenerateReadmeFiltered {

        @Test
        @DisplayName("claude-only omits github in summary")
        void generate_claudeOnly_omitsGithubInSummary(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = setupOutput(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String content =
                    ReadmeAssembler.generateReadme(
                            config, outputDir,
                            resourcesDir.resolve(
                                    "readme-template.md"),
                            Set.of(Platform.CLAUDE_CODE));

            String summarySection = extractSection(
                    content, "## Generation Summary");
            assertThat(summarySection)
                    .contains("Rules (.claude)")
                    .doesNotContain("(.github)")
                    .doesNotContain("(.codex)");
        }

        @Test
        @DisplayName("claude-only omits mapping table")
        void generate_claudeOnly_omitsMappingTable(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = setupOutput(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String content =
                    ReadmeAssembler.generateReadme(
                            config, outputDir,
                            resourcesDir.resolve(
                                    "readme-template.md"),
                            Set.of(Platform.CLAUDE_CODE));

            assertThat(content)
                    .doesNotContain(
                            "| .claude/ | .github/");
        }

        @Test
        @DisplayName("all platforms shows mapping table")
        void generate_allPlatforms_showsMappingTable(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = setupOutput(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String content =
                    ReadmeAssembler.generateReadme(
                            config, outputDir,
                            resourcesDir.resolve(
                                    "readme-template.md"),
                            Set.of());

            assertThat(content)
                    .contains("| .claude/ | .codex/");
        }

        @Test
        @DisplayName("replaces all placeholders")
        void generate_claudeOnly_noPlaceholders(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = setupOutput(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String content =
                    ReadmeAssembler.generateReadme(
                            config, outputDir,
                            resourcesDir.resolve(
                                    "readme-template.md"),
                            Set.of(Platform.CLAUDE_CODE));

            assertThat(content)
                    .doesNotContain("{{PROJECT_NAME}}")
                    .doesNotContain("{{GENERATION_SUMMARY}}")
                    .doesNotContain("{{MAPPING_TABLE}}");
        }
    }

    private static Path setupResources(Path tempDir)
            throws IOException {
        Path resourcesDir = Files.createDirectories(
                tempDir.resolve("resources"));
        var url = ReadmeAssemblerPlatformTest.class
                .getClassLoader()
                .getResource("readme-template.md");
        if (url != null) {
            String templateContent = Files.readString(
                    Path.of(url.getPath()),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    resourcesDir.resolve(
                            "readme-template.md"),
                    templateContent,
                    StandardCharsets.UTF_8);
        }
        return resourcesDir;
    }

    private static Path setupOutput(Path tempDir)
            throws IOException {
        Path outputDir = Files.createDirectories(
                tempDir.resolve("output")
                        .resolve(".claude"));
        Files.createDirectories(
                tempDir.resolve("output")
                        .resolve(".github"));
        return outputDir;
    }

    private static String extractSection(
            String content, String header) {
        int start = content.indexOf(header);
        if (start < 0) {
            return "";
        }
        return content.substring(start);
    }
}
