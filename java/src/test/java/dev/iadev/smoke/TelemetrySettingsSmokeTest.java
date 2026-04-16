package dev.iadev.smoke;

import dev.iadev.application.assembler.HooksAssembler;
import dev.iadev.application.assembler.SettingsAssembler;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end smoke test for the telemetry wiring introduced
 * by story-0040-0004. Runs {@link SettingsAssembler} and
 * {@link HooksAssembler} against a bundled profile and
 * asserts the resulting {@code .claude/settings.json}
 * contains the 5 telemetry hook entries and the
 * {@code .claude/hooks/} directory contains the 7 telemetry
 * shell scripts.
 *
 * <p>This approximates the smoke path of
 * {@code mvn process-resources} without needing to shell out
 * to Maven from inside the JVM — the assemblers are the same
 * code paths invoked by the Maven build, so coverage is
 * equivalent while staying fast.</p>
 */
@DisplayName("Telemetry settings smoke")
class TelemetrySettingsSmokeTest {

    @Test
    @DisplayName("java-spring profile assembles telemetry"
            + " hooks and scripts end-to-end")
    void javaSpring_fullPipeline_hasTelemetry(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        ProjectConfig config =
                ConfigProfiles.getStack("java-spring");

        new SettingsAssembler().assemble(
                config, new TemplateEngine(), outputDir);
        new HooksAssembler().assemble(
                config, new TemplateEngine(), outputDir);

        String settings = Files.readString(
                outputDir.resolve("settings.json"),
                StandardCharsets.UTF_8);
        assertThat(settings).contains("SessionStart");
        assertThat(settings).contains("telemetry-pretool.sh");
        assertThat(settings)
                .contains("telemetry-posttool.sh");
        assertThat(settings).contains("telemetry-subagent.sh");
        assertThat(settings).contains("telemetry-stop.sh");
        assertThat(settings).contains(
                "post-compile-check.sh");

        Path hooksDir = outputDir.resolve("hooks");
        for (String name : HooksAssembler.TELEMETRY_SCRIPTS) {
            Path script = hooksDir.resolve(name);
            assertThat(script).exists();
            assertThat(Files.isExecutable(script))
                    .as("%s must be executable", name)
                    .isTrue();
        }
        assertThat(hooksDir.resolve(
                "post-compile-check.sh")).exists();
    }

    @Test
    @DisplayName("python-fastapi profile (no compile hook)"
            + " still assembles telemetry")
    void pythonFastapi_fullPipeline_telemetryOnly(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        ProjectConfig config =
                ConfigProfiles.getStack("python-fastapi");

        new SettingsAssembler().assemble(
                config, new TemplateEngine(), outputDir);
        new HooksAssembler().assemble(
                config, new TemplateEngine(), outputDir);

        String settings = Files.readString(
                outputDir.resolve("settings.json"),
                StandardCharsets.UTF_8);
        assertThat(settings)
                .doesNotContain("post-compile-check.sh");
        assertThat(settings).contains(
                "telemetry-posttool.sh");

        Path hooksDir = outputDir.resolve("hooks");
        for (String name : HooksAssembler.TELEMETRY_SCRIPTS) {
            assertThat(hooksDir.resolve(name))
                    .as("missing %s", name).exists();
        }
    }
}
