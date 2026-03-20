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
 * Tests for SettingsAssembler — hooks config,
 * JSON validity, buildSettingsJson,
 * buildSettingsLocalJson, parseJsonStringArray,
 * and deduplicate.
 */
@DisplayName("SettingsAssembler — hooks and JSON")
class SettingsHooksAndJsonTest {

    @Nested
    @DisplayName("assemble — hooks configuration")
    class HooksConfig {

        @Test
        @DisplayName("settings.json contains PostToolUse"
                + " hooks for compiled language")
        void assemble_whenCalled_containsHooksForCompiledLang(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("PostToolUse");
            assertThat(content)
                    .contains("Write|Edit");
            assertThat(content)
                    .contains("post-compile-check.sh");
        }

        @Test
        @DisplayName("settings.json does NOT contain"
                + " hooks for python (no compile)")
        void assemble_noHooksForPython_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain("PostToolUse");
            assertThat(content)
                    .doesNotContain("hooks");
        }
    }

    @Nested
    @DisplayName("assemble — JSON validity")
    class JsonValidity {

        @Test
        @DisplayName("settings.json is valid JSON with"
                + " required keys")
        void assemble_settings_isValidJson(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("\"permissions\"");
            assertThat(content)
                    .contains("\"allow\"");
            assertThat(content.trim())
                    .startsWith("{");
            assertThat(content.trim())
                    .endsWith("}");
        }

        @Test
        @DisplayName("settings.local.json is valid JSON"
                + " with empty allow list")
        void assemble_settingsLocal_isValidJson(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "settings.local.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("\"permissions\"");
            assertThat(content)
                    .contains("\"allow\": []");
        }
    }

}
