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
}
