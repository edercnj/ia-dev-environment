package dev.iadev.meta;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification test for story-0040-0009 TASK-0040-0009-002: asserts the
 * project root {@code CLAUDE.md} exposes the skill authoring template
 * entry point and points new contributors at the {@code Telemetry}
 * section of {@code _TEMPLATE-SKILL.md}.
 *
 * <p>The test runs from the {@code java/} Maven module, so the root
 * {@code CLAUDE.md} lives one directory up.
 */
class ClaudeMdStructureTest {

    private static final Path CLAUDE_MD = Paths.get("..", "CLAUDE.md");

    @Test
    @DisplayName("claudeMd_fileExists_atProjectRoot")
    void claudeMd_fileExists_atProjectRoot() {
        assertThat(CLAUDE_MD)
                .as("Project root CLAUDE.md must exist at %s", CLAUDE_MD.toAbsolutePath())
                .exists()
                .isRegularFile();
    }

    @Test
    @DisplayName("claudeMd_authoringSection_referencesSkillTemplate")
    void claudeMd_authoringSection_referencesSkillTemplate() throws IOException {
        String body = Files.readString(CLAUDE_MD, StandardCharsets.UTF_8);

        assertThat(body)
                .as("CLAUDE.md must link contributors to _TEMPLATE-SKILL.md")
                .contains("_TEMPLATE-SKILL.md");
    }

    @Test
    @DisplayName("claudeMd_authoringSection_mentionsTelemetrySection")
    void claudeMd_authoringSection_mentionsTelemetrySection() throws IOException {
        String body = Files.readString(CLAUDE_MD, StandardCharsets.UTF_8);

        assertThat(body)
                .as("CLAUDE.md must mention the Telemetry section so authors know "
                        + "the plug-and-play block exists (story-0040-0009 §3.2)")
                .contains("Telemetry");
    }

    @Test
    @DisplayName("claudeMd_authoringSection_linksCanonicalExample")
    void claudeMd_authoringSection_linksCanonicalExample() throws IOException {
        String body = Files.readString(CLAUDE_MD, StandardCharsets.UTF_8);

        assertThat(body)
                .as("CLAUDE.md must point at x-dev-story-implement as the canonical "
                        + "instrumented example (story-0040-0009 §3.3)")
                .contains("x-dev-story-implement");
    }
}
