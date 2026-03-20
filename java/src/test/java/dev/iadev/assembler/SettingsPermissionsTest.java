package dev.iadev.assembler;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SettingsAssembler — interface contract,
 * file generation, and permission tests.
 */
@DisplayName("SettingsAssembler — permissions")
class SettingsPermissionsTest {

    @Nested
    @DisplayName("assemble — implements Assembler")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            SettingsAssembler assembler =
                    new SettingsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — file generation")
    class FileGeneration {

        @Test
        @DisplayName("generates settings.json and"
                + " settings.local.json")
        void assemble_whenCalled_generatesBothFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).hasSize(2);
            assertThat(outputDir.resolve("settings.json"))
                    .exists();
            assertThat(outputDir.resolve(
                    "settings.local.json"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("assemble — maven permissions")
    class MavenPermissions {

        @Test
        @DisplayName("settings.json contains maven"
                + " commands for java-quarkus")
        void assemble_whenCalled_containsMavenCommands(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(mvn *)");
        }

        @Test
        @DisplayName("settings.json contains universal"
                + " git commands")
        void assemble_whenCalled_containsGitCommands(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(git *)");
        }
    }

    @Nested
    @DisplayName("assemble — npm permissions")
    class NpmPermissions {

        @Test
        @DisplayName("settings.json contains npm commands"
                + " for typescript-nestjs")
        void assemble_whenCalled_containsNpmCommands(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5")
                            .framework("nestjs", "10")
                            .buildTool("npm")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(npm *)")
                    .contains("Bash(npx *)")
                    .contains("Bash(node *)");
        }

        @Test
        @DisplayName("npm config does NOT contain maven"
                + " commands")
        void assemble_whenCalled_doesNotContainMavenCommands(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5")
                            .framework("nestjs", "10")
                            .buildTool("npm")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain("Bash(mvn *)");
        }
    }
}
