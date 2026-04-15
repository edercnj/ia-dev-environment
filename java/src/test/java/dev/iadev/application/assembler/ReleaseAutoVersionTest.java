package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification test for story-0039-0001 — asserts the {@code x-release} source
 * {@code SKILL.md} and the new {@code auto-version-detection.md} reference
 * contain the contract items described in the story.
 *
 * <p>This test reads the SOURCE-OF-TRUTH under
 * {@code java/src/main/resources/targets/claude/skills/core/ops/x-release/}
 * per the project-wide rule that {@code .claude/} is a generated output.</p>
 */
@DisplayName("x-release SKILL.md contract — auto-version detection (story-0039-0001)")
class ReleaseAutoVersionTest {

    private static final Path SKILL_SOURCE = Paths.get(
            "src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md");
    private static final Path AUTO_REFERENCE = Paths.get(
            "src/main/resources/targets/claude/skills/core/ops/x-release/references/auto-version-detection.md");

    @Test
    @DisplayName("skillMd_documentsVersionFlag_andAutoDetectAlgorithm")
    void skillMd_documentsVersionFlag_andAutoDetectAlgorithm() throws Exception {
        assertThat(SKILL_SOURCE).exists();
        String content = Files.readString(SKILL_SOURCE);

        assertThat(content)
                .as("SKILL.md must document the --version flag")
                .contains("--version X.Y.Z");
        assertThat(content)
                .as("SKILL.md must reference auto-version-detection.md")
                .contains("references/auto-version-detection.md");
        assertThat(content)
                .as("SKILL.md must document the auto-detect option")
                .contains("Auto-detect from Conventional Commits");
        assertThat(content)
                .as("SKILL.md must document the VERSION_NO_BUMP_SIGNAL error")
                .contains("VERSION_NO_BUMP_SIGNAL");
        assertThat(content)
                .as("SKILL.md must document the VERSION_INVALID_FORMAT error")
                .contains("VERSION_INVALID_FORMAT");
    }

    @Test
    @DisplayName("autoVersionDetectionReference_exists_andCoversAlgorithm")
    void autoVersionDetectionReference_exists_andCoversAlgorithm() throws Exception {
        assertThat(AUTO_REFERENCE).exists();
        String content = Files.readString(AUTO_REFERENCE);

        assertThat(content)
                .contains("Algorithm Overview")
                .contains("Classification Table")
                .contains("Bump Rules")
                .contains("Error Codes")
                .contains("Security Posture")
                .contains("ConventionalCommitsParser")
                .contains("VersionBumper")
                .contains("GitTagReader");
    }
}
