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
 * Tests for SettingsAssembler — telemetry hook injection
 * (story-0040-0004). Validates that the 5 telemetry events
 * (SessionStart, PreToolUse, PostToolUse, SubagentStop,
 * Stop) are emitted in .claude/settings.json when
 * {@code telemetryEnabled=true}, coexist with
 * {@code post-compile-check.sh} for compiled stacks, and
 * are fully omitted when telemetry is disabled.
 */
@DisplayName("SettingsAssembler — telemetry hooks")
class SettingsAssemblerTelemetryTest {

    private static String assembleSettings(
            ProjectConfig config, Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        new SettingsAssembler().assemble(
                config, new TemplateEngine(), outputDir);
        return Files.readString(
                outputDir.resolve("settings.json"),
                StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("telemetryEnabled=false (degenerate)")
    class Disabled {

        @Test
        @DisplayName("python project emits no hooks section"
                + " when telemetry is off")
        void pythonDisabled_noHooksSection(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .telemetryEnabled(false)
                            .build();

            String content =
                    assembleSettings(config, tempDir);

            assertThat(content)
                    .doesNotContain("telemetry-");
            assertThat(content).doesNotContain("hooks");
        }

        @Test
        @DisplayName("compiled project keeps legacy hook"
                + " block but no telemetry entries")
        void compiledDisabled_onlyLegacyHook(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .telemetryEnabled(false)
                            .build();

            String content =
                    assembleSettings(config, tempDir);

            assertThat(content)
                    .contains("post-compile-check.sh");
            assertThat(content)
                    .doesNotContain("telemetry-");
        }
    }

    @Nested
    @DisplayName("telemetryEnabled=true (happy path)")
    class Enabled {

        @Test
        @DisplayName("python project injects 5 telemetry"
                + " events")
        void pythonEnabled_fiveTelemetryEvents(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .telemetryEnabled(true)
                            .build();

            String content =
                    assembleSettings(config, tempDir);

            assertThat(content).contains("\"SessionStart\"");
            assertThat(content).contains("\"PreToolUse\"");
            assertThat(content).contains("\"PostToolUse\"");
            assertThat(content).contains("\"SubagentStop\"");
            assertThat(content).contains("\"Stop\"");
            assertThat(content).contains(
                    "telemetry-session.sh");
            assertThat(content).contains(
                    "telemetry-pretool.sh");
            assertThat(content).contains(
                    "telemetry-posttool.sh");
            assertThat(content).contains(
                    "telemetry-subagent.sh");
            assertThat(content).contains(
                    "telemetry-stop.sh");
        }

        @Test
        @DisplayName("telemetry entries use 5s timeout and"
                + " $CLAUDE_PROJECT_DIR prefix")
        void telemetryEntries_correctShape(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .telemetryEnabled(true)
                            .build();

            String content =
                    assembleSettings(config, tempDir);

            assertThat(content).contains(
                    "\"timeout\": 5");
            assertThat(content).contains(
                    "$CLAUDE_PROJECT_DIR");
            assertThat(content).contains(
                    "/.claude/hooks/telemetry-session.sh");
        }

        @Test
        @DisplayName("settings.json remains valid JSON")
        void telemetry_validJson(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .telemetryEnabled(true)
                            .build();

            String content =
                    assembleSettings(config, tempDir);

            assertThat(content.trim()).startsWith("{");
            assertThat(content.trim()).endsWith("}");
            int opens = content.length()
                    - content.replace("{", "").length();
            int closes = content.length()
                    - content.replace("}", "").length();
            assertThat(opens).isEqualTo(closes);
        }
    }

    @Nested
    @DisplayName("coexistence with post-compile-check.sh")
    class Coexistence {

        @Test
        @DisplayName("compiled language emits 2 PostToolUse"
                + " entries: Write|Edit + *")
        void compiledLang_postToolUseHasTwoEntries(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .telemetryEnabled(true)
                            .build();

            String content =
                    assembleSettings(config, tempDir);

            assertThat(content)
                    .contains("post-compile-check.sh");
            assertThat(content)
                    .contains("telemetry-posttool.sh");
            assertThat(content)
                    .contains("\"Write|Edit\"");
            assertThat(content).contains("\"*\"");
        }
    }

    @Nested
    @DisplayName("idempotency (boundary)")
    class Idempotency {

        @Test
        @DisplayName("re-running assemble yields identical"
                + " content byte-for-byte")
        void assemble_twice_identicalOutput(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .telemetryEnabled(true)
                            .build();

            String first =
                    assembleSettings(config, tempDir);
            Path outputDir = tempDir.resolve("output");
            new SettingsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String second = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);

            assertThat(second).isEqualTo(first);
        }
    }
}
