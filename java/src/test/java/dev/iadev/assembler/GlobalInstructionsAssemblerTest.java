package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GlobalInstructionsAssembler — generates the
 * global copilot-instructions.md file.
 */
@DisplayName("GlobalInstructionsAssembler")
class GlobalInstructionsAssemblerTest {

    @Nested
    @DisplayName("generate — file output")
    class Generate {

        @Test
        @DisplayName("generates copilot-instructions.md"
                + " at output dir")
        void generatesFile(@TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GlobalInstructionsAssembler assembler =
                    new GlobalInstructionsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("test-project")
                            .build();

            String path = assembler.generate(
                    config, outputDir);

            assertThat(path).contains(
                    "copilot-instructions.md");
            Path generated = outputDir.resolve(
                    "copilot-instructions.md");
            assertThat(generated).exists();
            String content = Files.readString(
                    generated, StandardCharsets.UTF_8);
            assertThat(content).contains(
                    "# Project Identity");
        }
    }

    @Nested
    @DisplayName("buildCopilotInstructions")
    class BuildCopilotInstructions {

        @Test
        @DisplayName("builds all sections")
        void buildsAllSections() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-project")
                            .build();

            String result =
                    GlobalInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .contains("# Project Identity")
                    .contains("## Identity")
                    .contains("## Technology Stack")
                    .contains("## Constraints")
                    .contains("## Contextual Instructions")
                    .endsWith("\n");
        }
    }

    @Nested
    @DisplayName("formatInterfaces")
    class FormatInterfaces {

        @Test
        @DisplayName("REST uppercased")
        void restUppercased() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            String result =
                    GlobalInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result).isEqualTo("REST");
        }

        @Test
        @DisplayName("empty interfaces returns none")
        void emptyReturnsNone() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .build();

            String result =
                    GlobalInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result).isEqualTo("none");
        }
    }

    @Nested
    @DisplayName("formatFrameworkVersion")
    class FormatFrameworkVersion {

        @Test
        @DisplayName("returns space-prefixed version")
        void returnsVersionWithSpace() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("spring", "3.4")
                            .build();

            String result =
                    GlobalInstructionsAssembler
                            .formatFrameworkVersion(config);

            assertThat(result).isEqualTo(" 3.4");
        }

        @Test
        @DisplayName("returns empty for null version")
        void returnsEmptyForNull() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("axum", null)
                            .build();

            String result =
                    GlobalInstructionsAssembler
                            .formatFrameworkVersion(config);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for blank version")
        void returnsEmptyForBlank() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("axum", "")
                            .build();

            String result =
                    GlobalInstructionsAssembler
                            .formatFrameworkVersion(config);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("capitalize — edge cases")
    class Capitalize {

        @Test
        @DisplayName("capitalizes normal string")
        void capitalizesNormalString() {
            assertThat(
                    GlobalInstructionsAssembler
                            .capitalize("docker"))
                    .isEqualTo("Docker");
        }

        @Test
        @DisplayName("returns null for null input")
        void returnsNullForNull() {
            assertThat(
                    GlobalInstructionsAssembler
                            .capitalize(null))
                    .isNull();
        }

        @Test
        @DisplayName("returns empty for empty input")
        void returnsEmptyForEmpty() {
            assertThat(
                    GlobalInstructionsAssembler
                            .capitalize(""))
                    .isEmpty();
        }
    }
}
