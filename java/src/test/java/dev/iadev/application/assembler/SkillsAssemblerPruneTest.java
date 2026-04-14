package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that SkillsAssembler prunes stale skill
 * directories in the output so that renames and removals
 * in the source of truth are reflected in
 * {@code .claude/skills/} after regeneration.
 *
 * <p>Motivated by the post-EPIC-0036 cleanup: 14 legacy
 * skill directories (e.g., {@code x-dev-implement},
 * {@code run-e2e}) persisted in the output because the
 * previous generator was additive-only.</p>
 */
@DisplayName("SkillsAssembler — stale output prune")
class SkillsAssemblerPruneTest {

    @Test
    @DisplayName("stale skill dir not in source is removed"
            + " after assemble")
    void assemble_whenStaleSkillExists_removesIt(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        createSkillInSource(core, "x-task-implement");

        Path outputDir = tempDir.resolve("output");
        Path staleSkill = outputDir.resolve(
                "skills/x-dev-implement");
        Files.createDirectories(staleSkill);
        Files.writeString(
                staleSkill.resolve("SKILL.md"),
                "stale",
                StandardCharsets.UTF_8);

        runAssemble(tempDir, outputDir);

        assertThat(Files.exists(staleSkill))
                .as("stale skill dir must be pruned")
                .isFalse();
        assertThat(Files.exists(outputDir.resolve(
                "skills/x-task-implement")))
                .as("fresh skill must be written")
                .isTrue();
    }

    @Test
    @DisplayName("knowledge-packs directory is protected"
            + " from prune")
    void assemble_whenKnowledgePacksDirExists_preserved(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        createSkillInSource(core, "x-task-implement");

        Path outputDir = tempDir.resolve("output");
        Path kpDir = outputDir.resolve(
                "skills/knowledge-packs");
        Files.createDirectories(kpDir);
        Path preExisting = kpDir.resolve("cloud-aws.md");
        Files.writeString(preExisting, "preexisting",
                StandardCharsets.UTF_8);

        runAssemble(tempDir, outputDir);

        assertThat(Files.exists(kpDir))
                .as("knowledge-packs dir must be retained")
                .isTrue();
        assertThat(Files.exists(preExisting))
                .as("pre-existing file inside must survive")
                .isTrue();
    }

    @Test
    @DisplayName("lib subdirectory retained when"
            + " lib/x-lib-* skills generated")
    void assemble_whenLibSkillGenerated_libDirRetained(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        Files.createDirectories(core);
        Path libSkill = core.resolve("lib/x-lib-tool");
        Files.createDirectories(libSkill);
        Files.writeString(
                libSkill.resolve("SKILL.md"),
                "---\nname: x-lib-tool\n---\n",
                StandardCharsets.UTF_8);

        Path outputDir = tempDir.resolve("output");

        runAssemble(tempDir, outputDir);

        assertThat(Files.exists(outputDir.resolve(
                "skills/lib/x-lib-tool")))
                .as("lib skill must be written")
                .isTrue();
        assertThat(Files.exists(outputDir.resolve(
                "skills/lib")))
                .as("lib dir must be retained")
                .isTrue();
    }

    @Test
    @DisplayName("stray file at skills root is NOT deleted"
            + " (only directories are pruned)")
    void assemble_whenStrayFileAtRoot_fileRetained(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        createSkillInSource(core, "x-task-implement");

        Path outputDir = tempDir.resolve("output");
        Path skillsRoot = outputDir.resolve("skills");
        Files.createDirectories(skillsRoot);
        Path strayFile = skillsRoot.resolve("README.md");
        Files.writeString(strayFile, "stray",
                StandardCharsets.UTF_8);

        runAssemble(tempDir, outputDir);

        assertThat(Files.exists(strayFile))
                .as("stray file at skills/ root must survive")
                .isTrue();
    }

    @Test
    @DisplayName("output skills dir missing is a no-op"
            + " (no exception thrown)")
    void assemble_whenSkillsDirMissing_noError(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        Files.createDirectories(core);

        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        List<String> files = runAssemble(tempDir, outputDir);

        assertThat(files).isEmpty();
    }

    @Test
    @DisplayName("multiple stale dirs are all removed")
    void assemble_whenMultipleStaleDirs_allRemoved(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        createSkillInSource(core, "x-task-implement");

        Path outputDir = tempDir.resolve("output");
        Path skillsRoot = outputDir.resolve("skills");
        Files.createDirectories(skillsRoot);
        List<String> staleNames = List.of(
                "x-dev-implement",
                "x-dev-story-implement",
                "x-epic-plan",
                "x-story-epic",
                "run-e2e");
        for (String name : staleNames) {
            Path d = skillsRoot.resolve(name);
            Files.createDirectories(d);
            Files.writeString(d.resolve("SKILL.md"),
                    "stale", StandardCharsets.UTF_8);
        }

        runAssemble(tempDir, outputDir);

        for (String name : staleNames) {
            assertThat(Files.exists(
                    skillsRoot.resolve(name)))
                    .as("stale %s must be pruned", name)
                    .isFalse();
        }
    }

    private static void createSkillInSource(
            Path coreDir, String skillName) throws IOException {
        Path skill = coreDir.resolve(skillName);
        Files.createDirectories(skill);
        Files.writeString(
                skill.resolve("SKILL.md"),
                "---\nname: " + skillName + "\n---\n",
                StandardCharsets.UTF_8);
    }

    private static List<String> runAssemble(
            Path resourcesDir, Path outputDir)
            throws IOException {
        Files.createDirectories(outputDir);
        ProjectConfig config = TestConfigBuilder.minimal();
        SkillsAssembler assembler =
                new SkillsAssembler(resourcesDir);
        return assembler.assemble(
                config, new TemplateEngine(), outputDir);
    }
}
