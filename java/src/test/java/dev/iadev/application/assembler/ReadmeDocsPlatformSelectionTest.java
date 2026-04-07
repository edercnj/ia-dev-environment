package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
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
 * Tests that the README template and java/README.md
 * contain the "Platform Selection" documentation section
 * as required by story-0025-0008.
 */
@DisplayName("README Platform Selection documentation")
class ReadmeDocsPlatformSelectionTest {

    @Nested
    @DisplayName("readme-template.md")
    class ReadmeTemplate {

        @Test
        @DisplayName("contains Platform Selection section")
        void readmeTemplate_whenRead_containsPlatformSelectionSection()
                throws IOException {
            String content = loadReadmeTemplate();

            assertThat(content)
                    .contains("## Platform Selection");
        }

        @Test
        @DisplayName("contains platform values table")
        void readmeTemplate_whenRead_containsPlatformValuesTable()
                throws IOException {
            String content = loadReadmeTemplate();

            assertThat(content)
                    .contains("claude-code")
                    .contains("copilot")
                    .contains("codex")
                    .contains("all");
        }

        @Test
        @DisplayName("contains CLI example with --platform")
        void readmeTemplate_whenRead_containsCliExample()
                throws IOException {
            String content = loadReadmeTemplate();

            assertThat(content)
                    .contains("--platform");
        }

        @Test
        @DisplayName("contains multi-value example")
        void readmeTemplate_whenRead_containsMultiValueExample()
                throws IOException {
            String content = loadReadmeTemplate();

            assertThat(content)
                    .contains("-p claude-code,copilot");
        }

        @Test
        @DisplayName("contains YAML config example")
        void readmeTemplate_whenRead_containsYamlExample()
                throws IOException {
            String content = loadReadmeTemplate();

            assertThat(content)
                    .contains("platform:");
        }

        @Test
        @DisplayName("contains backward compatibility note")
        void readmeTemplate_whenRead_containsBackwardCompatNote()
                throws IOException {
            String content = loadReadmeTemplate();

            assertThat(content)
                    .containsIgnoringCase("backward")
                    .containsIgnoringCase("compatible");
        }

        private String loadReadmeTemplate()
                throws IOException {
            var url = getClass().getClassLoader()
                    .getResource("readme-template.md");
            assertThat(url).isNotNull();
            return Files.readString(
                    Path.of(url.getPath()),
                    StandardCharsets.UTF_8);
        }
    }

    @Nested
    @DisplayName("MappingTableBuilder conditionality note")
    class MappingTableConditionality {

        @Test
        @DisplayName("multi-platform table includes"
                + " conditionality note")
        void build_multiPlatform_includesConditionalityNote(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = setupOutput(tempDir);

            String table = ReadmeTables.buildMappingTable(
                    outputDir, Set.of());

            assertThat(table)
                    .contains("Generated only when the "
                            + "corresponding platform is "
                            + "selected");
        }

        @Test
        @DisplayName("single-platform returns empty")
        void build_singlePlatform_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = setupOutput(tempDir);

            String table = ReadmeTables.buildMappingTable(
                    outputDir,
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(table).isEmpty();
        }

        private Path setupOutput(Path tempDir)
                throws IOException {
            Path outputDir = Files.createDirectories(
                    tempDir.resolve("output")
                            .resolve(".claude"));
            Files.createDirectories(
                    tempDir.resolve("output")
                            .resolve(".github"));
            return outputDir;
        }
    }
}
