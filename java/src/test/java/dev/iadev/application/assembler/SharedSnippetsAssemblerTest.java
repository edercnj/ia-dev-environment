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
 * Verifies that {@link SkillsAssembler} copies the
 * {@code _shared/} directory from the source of truth
 * (peer of {@code core/}, {@code conditional/},
 * {@code knowledge-packs/}) to the generated output
 * tree at {@code skills/_shared/}.
 *
 * <p>Story: story-0047-0001 (EPIC-0047 — Skill Body
 * Compression Framework). The {@code _shared/} directory
 * holds cross-cutting Markdown snippets referenced by
 * ≥ 2 skills via Markdown relative links (per
 * ADR-0011 — shared-snippets inclusion strategy).
 * For the links to resolve at runtime in a generated
 * project, {@code _shared/} must ship alongside the
 * consumer skills in the output tree.</p>
 *
 * <p>Tests follow TPP ordering:
 * degenerate ({@code _shared/} missing in source) →
 * constant (single file copied) →
 * collection (multiple files + nested structure) →
 * integration (link from a consumer skill resolves).</p>
 */
@DisplayName("SkillsAssembler — _shared/ directory copy")
class SharedSnippetsAssemblerTest {

    @Test
    @DisplayName("degenerate — source has no _shared/ dir;"
            + " output has no _shared/ either")
    void assemble_whenSharedMissing_noOutputDir(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        createSkillInSource(core, "x-task-implement");

        Path outputDir = tempDir.resolve("output");

        runAssemble(tempDir, outputDir);

        assertThat(Files.exists(outputDir.resolve(
                "skills/_shared")))
                .as("no _shared/ in source => no _shared/ in output")
                .isFalse();
    }

    @Test
    @DisplayName("constant — source has _shared/ with a single"
            + " file; output contains the same file")
    void assemble_whenSharedHasOneFile_copiedToOutput(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        createSkillInSource(core, "x-task-implement");
        Path sharedSrc = tempDir.resolve(
                "targets/claude/skills/_shared");
        Files.createDirectories(sharedSrc);
        Path singleFile = sharedSrc.resolve(
                "error-handling-pre-commit.md");
        Files.writeString(singleFile,
                "## Pre-commit error matrix\n",
                StandardCharsets.UTF_8);

        Path outputDir = tempDir.resolve("output");
        List<String> generated = runAssemble(
                tempDir, outputDir);

        Path outShared = outputDir.resolve(
                "skills/_shared/error-handling-pre-commit.md");
        assertThat(Files.exists(outShared))
                .as("_shared/ file must be copied to output")
                .isTrue();
        assertThat(Files.readString(outShared,
                StandardCharsets.UTF_8))
                .isEqualTo("## Pre-commit error matrix\n");
        assertThat(generated)
                .as("generated paths must include the _shared/ dir"
                        + " so pruneStaleSkills retains it")
                .anyMatch(p -> p.contains("_shared"));
    }

    @Test
    @DisplayName("collection — source has _shared/ with multiple"
            + " files + README; all copied preserving layout")
    void assemble_whenSharedHasMultipleFiles_allCopied(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        createSkillInSource(core, "x-task-implement");
        Path sharedSrc = tempDir.resolve(
                "targets/claude/skills/_shared");
        Files.createDirectories(sharedSrc);
        Files.writeString(
                sharedSrc.resolve("README.md"),
                "# _shared/\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                sharedSrc.resolve(
                        "error-handling-pre-commit.md"),
                "## Pre-commit errors\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                sharedSrc.resolve("tdd-tags-glossary.md"),
                "## TDD tags\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                sharedSrc.resolve("exit-codes-common.md"),
                "## Exit codes\n",
                StandardCharsets.UTF_8);

        Path outputDir = tempDir.resolve("output");
        runAssemble(tempDir, outputDir);

        Path outShared = outputDir.resolve("skills/_shared");
        assertThat(Files.exists(
                outShared.resolve("README.md")))
                .as("README.md copied").isTrue();
        assertThat(Files.exists(outShared.resolve(
                "error-handling-pre-commit.md")))
                .as("pre-commit snippet copied").isTrue();
        assertThat(Files.exists(outShared.resolve(
                "tdd-tags-glossary.md")))
                .as("tdd-tags snippet copied").isTrue();
        assertThat(Files.exists(outShared.resolve(
                "exit-codes-common.md")))
                .as("exit-codes snippet copied").isTrue();
    }

    @Test
    @DisplayName("integration — a Markdown relative link from"
            + " a consumer skill to _shared/ resolves in the"
            + " output tree (end-to-end validation)")
    void assemble_whenConsumerLinksShared_linkResolves(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        Path consumerSkillDir = core.resolve(
                "git/x-git-commit");
        Files.createDirectories(consumerSkillDir);
        Files.writeString(
                consumerSkillDir.resolve("SKILL.md"),
                "---\nname: x-git-commit\n---\n"
                        + "## Error Handling\n"
                        + "See [`error-handling`]"
                        + "(../../../_shared/"
                        + "error-handling-pre-commit.md)"
                        + " for the canonical matrix.\n",
                StandardCharsets.UTF_8);

        Path sharedSrc = tempDir.resolve(
                "targets/claude/skills/_shared");
        Files.createDirectories(sharedSrc);
        Files.writeString(sharedSrc.resolve(
                        "error-handling-pre-commit.md"),
                "## Pre-commit errors\n",
                StandardCharsets.UTF_8);

        Path outputDir = tempDir.resolve("output");
        runAssemble(tempDir, outputDir);

        Path outSkill = outputDir.resolve(
                "skills/x-git-commit/SKILL.md");
        Path outSharedFile = outputDir.resolve(
                "skills/_shared/error-handling-pre-commit.md");
        // Relative link from flat output is
        // ../_shared/error-handling-pre-commit.md
        Path resolved = outSkill.getParent().resolve(
                "../_shared/error-handling-pre-commit.md")
                .normalize();
        assertThat(Files.exists(resolved))
                .as("relative link target must exist in output")
                .isTrue();
        assertThat(resolved.toAbsolutePath().normalize())
                .as("link resolves to the same file copied")
                .isEqualTo(
                        outSharedFile.toAbsolutePath()
                                .normalize());
    }

    @Test
    @DisplayName("idempotence — re-running the assembler does"
            + " not delete _shared/ (prune respects it)")
    void assemble_whenRerun_sharedPreserved(
            @TempDir Path tempDir) throws IOException {
        Path core = tempDir.resolve(
                "targets/claude/skills/core");
        createSkillInSource(core, "x-task-implement");
        Path sharedSrc = tempDir.resolve(
                "targets/claude/skills/_shared");
        Files.createDirectories(sharedSrc);
        Files.writeString(
                sharedSrc.resolve(
                        "error-handling-pre-commit.md"),
                "## Pre-commit errors\n",
                StandardCharsets.UTF_8);

        Path outputDir = tempDir.resolve("output");
        runAssemble(tempDir, outputDir);
        runAssemble(tempDir, outputDir);

        assertThat(Files.exists(outputDir.resolve(
                "skills/_shared/"
                        + "error-handling-pre-commit.md")))
                .as("_shared/ content must survive re-run")
                .isTrue();
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
