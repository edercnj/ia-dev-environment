package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
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
 * Tests that the README template contains the
 * "Platform Selection" documentation section and that
 * the generated mapping table includes the related
 * conditionality note required by story-0025-0008.
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
        @DisplayName("platform selection table lists only claude-code")
        void readmeTemplate_whenRead_tableListsOnlyClaudeCode()
                throws IOException {
            String content = loadReadmeTemplate();
            int tableStart = content.indexOf("## Platform Selection");
            int tableEnd = content.indexOf("### CLI Examples");
            assertThat(tableStart).isPositive();
            assertThat(tableEnd).isGreaterThan(tableStart);
            String tableSection = content.substring(
                    tableStart, tableEnd);

            assertThat(tableSection)
                    .contains("| `claude-code` |")
                    .doesNotContain("| `copilot` |")
                    .doesNotContain("| `codex` |")
                    .doesNotContain("| `all` |");
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
        @DisplayName("contains YAML config example")
        void readmeTemplate_whenRead_containsYamlExample()
                throws IOException {
            String content = loadReadmeTemplate();

            assertThat(content)
                    .contains("platform:");
        }

        @Test
        @DisplayName("contains default behavior note")
        void readmeTemplate_whenRead_containsDefaultBehaviorNote()
                throws IOException {
            String content = loadReadmeTemplate();

            assertThat(content)
                    .containsIgnoringCase("default behavior")
                    .containsIgnoringCase("claude-code");
        }

        private String loadReadmeTemplate()
                throws IOException {
            var url = getClass().getClassLoader()
                    .getResource("readme-template.md");
            assertThat(url).isNotNull();
            try {
                return Files.readString(
                        Path.of(url.toURI()),
                        StandardCharsets.UTF_8);
            } catch (java.net.URISyntaxException e) {
                throw new IOException(
                        "Invalid URI: " + url, e);
            }
        }
    }

    @Nested
    @DisplayName("MappingTableBuilder conditionality note")
    class MappingTableConditionality {

        @Test
        @DisplayName("multi-platform table is empty"
                + " after codex removal")
        void build_multiPlatform_isEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = setupOutput(tempDir);

            String table = ReadmeTables.buildMappingTable(
                    outputDir, Set.of());

            assertThat(table).isEmpty();
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
