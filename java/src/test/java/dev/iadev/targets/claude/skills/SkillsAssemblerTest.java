package dev.iadev.targets.claude.skills;

import dev.iadev.application.assembler.SkillsAssembler;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the core skill catalog discoverable by
 * {@link SkillsAssembler} includes the skills introduced by
 * EPIC-0042 (Merge-Train Automation).
 *
 * <p>Tests use the default no-arg constructor so they scan
 * the real classpath resources under
 * {@code targets/claude/skills/core/}. A test that passes
 * here guarantees the corresponding {@code SKILL.md} has been
 * placed in the canonical location and is wired into the
 * assembly pipeline.</p>
 */
@DisplayName("SkillsAssembler — EPIC-0042 skill catalog")
class SkillsAssemblerTest {

    @Test
    @DisplayName("listCoreSkills includes x-pr-merge-train")
    void listCoreSkills_includesMergeTrain(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        new SkillsAssembler().assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(),
                outputDir);

        assertThat(
                outputDir.resolve(
                        "skills/x-pr-merge-train/SKILL.md"))
                .as("x-pr-merge-train must be discoverable "
                        + "in the core skill catalog — "
                        + "check that "
                        + "targets/claude/skills/core/pr/"
                        + "x-pr-merge-train/SKILL.md exists")
                .exists();
    }

    @Test
    @DisplayName("listSkills includes x-status-reconcile "
            + "(EPIC-0046 story-0006)")
    void listSkills_includesStatusReconcile(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        new SkillsAssembler().assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(),
                outputDir);

        assertThat(
                outputDir.resolve(
                        "skills/x-status-reconcile/"
                                + "SKILL.md"))
                .as("x-status-reconcile must be "
                        + "discoverable in the core skill "
                        + "catalog — check that "
                        + "targets/claude/skills/core/ops/"
                        + "x-status-reconcile/SKILL.md "
                        + "exists")
                .exists();
    }

    @Test
    @DisplayName("x-story-implement SKILL.md contains "
            + "## Review Policy section (EPIC-0053)")
    void xStoryImplement_containsReviewPolicySection(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        new SkillsAssembler().assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(),
                outputDir);

        String content = Files.readString(
                outputDir.resolve(
                        "skills/x-story-implement/SKILL.md"));

        assertThat(content)
                .as("Generated x-story-implement/SKILL.md "
                        + "must contain '## Review Policy' "
                        + "section (EPIC-0053 enforcement — "
                        + "source: targets/claude/skills/core/"
                        + "dev/x-story-implement/SKILL.md)")
                .contains("## Review Policy");
    }

    @Test
    @DisplayName("x-story-implement SKILL.md contains "
            + ">= 2 MANDATORY — NON-NEGOTIABLE markers (EPIC-0053)")
    void xStoryImplement_containsMandatoryMarkersOnBothReviewSteps(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        new SkillsAssembler().assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(),
                outputDir);

        String content = Files.readString(
                outputDir.resolve(
                        "skills/x-story-implement/SKILL.md"));

        int count = countOccurrences(
                content, MANDATORY_MARKER);

        assertThat(count)
                .as("Expected >= 2 MANDATORY — NON-NEGOTIABLE "
                        + "markers in generated x-story-implement/"
                        + "SKILL.md (one per review step: x-review "
                        + "and x-review-pr), found: " + count)
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("x-story-implement SKILL.md contains "
            + "PROTOCOL_VIOLATION and REVIEW_SKIPPED_WITHOUT_FLAG "
            + "error codes (EPIC-0053)")
    void xStoryImplement_containsProtocolViolationErrorCodes(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        new SkillsAssembler().assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(),
                outputDir);

        String content = Files.readString(
                outputDir.resolve(
                        "skills/x-story-implement/SKILL.md"));

        assertThat(content)
                .as("Generated x-story-implement/SKILL.md "
                        + "must contain '"
                        + REVIEW_SKIPPED_ERROR_CODE
                        + "' error code (EPIC-0053 Review Policy)")
                .contains(REVIEW_SKIPPED_ERROR_CODE);

        int violationCount = countOccurrences(
                content, PROTOCOL_VIOLATION_CODE);

        assertThat(violationCount)
                .as("Expected >= 2 '"
                        + PROTOCOL_VIOLATION_CODE
                        + "' error codes in generated "
                        + "x-story-implement/SKILL.md "
                        + "(one per review step), found: "
                        + violationCount)
                .isGreaterThanOrEqualTo(2);
    }

    private static final String MANDATORY_MARKER =
            "MANDATORY — NON-NEGOTIABLE";

    private static final String REVIEW_SKIPPED_ERROR_CODE =
            "REVIEW_SKIPPED_WITHOUT_FLAG";

    private static final String PROTOCOL_VIOLATION_CODE =
            "PROTOCOL_VIOLATION";

    private static int countOccurrences(
            String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
