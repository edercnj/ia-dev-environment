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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SettingsAssembler — golden file parity
 * and edge cases.
 */
@DisplayName("SettingsAssembler — golden + edge")
class SettingsGoldenEdgeCasesTest {

    @Nested
    @DisplayName("assemble — golden file parity")
    class GoldenFile {

        @Test
        @DisplayName("settings.json matches golden file"
                + " for kotlin-ktor")
        void assemble_settings_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    HooksAssemblerTest
                            .buildKotlinKtorConfig();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String expected = loadResource(
                    "golden/kotlin-ktor/.claude/"
                            + "settings.json");
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotEmpty();

            String actual = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(actual)
                    .as("settings.json must match golden"
                            + " file byte-for-byte")
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("settings.local.json matches golden"
                + " file for kotlin-ktor")
        void assemble_settingsLocal_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    HooksAssemblerTest
                            .buildKotlinKtorConfig();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String expected = loadResource(
                    "golden/kotlin-ktor/.claude/"
                            + "settings.local.json");
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotEmpty();

            String actual = Files.readString(
                    outputDir.resolve(
                            "settings.local.json"),
                    StandardCharsets.UTF_8);
            assertThat(actual)
                    .as("settings.local.json must match"
                            + " golden file byte-for-byte")
                    .isEqualTo(expected);
        }

        private String loadResource(String path) {
            var url = getClass().getClassLoader()
                    .getResource(path);
            if (url == null) {
                return null;
            }
            try {
                return Files.readString(
                        Path.of(url.getPath()),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }
    }

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("unknown language still includes"
                + " base permissions")
        void assemble_unknownLanguage_includesBase(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("unknown", "1.0")
                            .framework("unknown", "1.0")
                            .buildTool("unknown")
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
                    .contains("Bash(git *)");
        }

        @Test
        @DisplayName("podman container adds docker"
                + " permissions")
        void assemble_podman_addsDockerPerms(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("podman")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(docker build *)");
        }

        @Test
        @DisplayName("docker-compose orchestrator adds"
                + " compose permissions")
        void assemble_compose_addsComposePerms(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .orchestrator("docker-compose")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(docker compose *)");
        }
    }
}
