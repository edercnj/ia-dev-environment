package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
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
 * Tests for recursive traversal in SkillsAssembler
 * covering story-0036-0002: hierarchical source-of-truth
 * with flat output.
 *
 * <p>The source of truth reorganizes skills into 10 category
 * subfolders (plan/, dev/, test/, review/, etc.) but the
 * generated output under {@code .claude/skills/} remains
 * flat. A directory is a skill directory iff it contains
 * a SKILL.md file.</p>
 */
@DisplayName("SkillsAssembler — recursive traversal (story-0036-0002)")
class SkillsAssemblerRecursiveTest {

    @Nested
    @DisplayName("selectCoreSkills — hierarchical")
    class HierarchicalCore {

        @Test
        @DisplayName("skill in category subfolder is discovered")
        void select_skillInCategorySubfolder_isFound(
                @TempDir Path tempDir) throws IOException {
            Path coreDir = tempDir.resolve(
                    "targets/claude/skills/core");
            writeSkillMd(
                    coreDir.resolve("plan/x-epic-create"),
                    "x-epic-create");

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);

            List<String> skills = assembler.selectCoreSkills();

            assertThat(skills)
                    .containsExactly("x-epic-create");
        }

        @Test
        @DisplayName("skills across multiple categories are discovered")
        void select_multipleCategories_allDiscovered(
                @TempDir Path tempDir) throws IOException {
            Path coreDir = tempDir.resolve(
                    "targets/claude/skills/core");
            writeSkillMd(
                    coreDir.resolve("plan/x-epic-create"),
                    "x-epic-create");
            writeSkillMd(
                    coreDir.resolve("dev/x-task-implement"),
                    "x-task-implement");
            writeSkillMd(
                    coreDir.resolve("review/x-review-pr"),
                    "x-review-pr");

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);

            List<String> skills = assembler.selectCoreSkills();

            assertThat(skills).containsExactlyInAnyOrder(
                    "x-epic-create",
                    "x-task-implement",
                    "x-review-pr");
        }

        @Test
        @DisplayName("lib/ prefix preserved under hierarchy")
        void select_libSubdir_preservesLibPrefix(
                @TempDir Path tempDir) throws IOException {
            Path coreDir = tempDir.resolve(
                    "targets/claude/skills/core");
            writeSkillMd(
                    coreDir.resolve("lib/x-lib-tool"),
                    "x-lib-tool");
            writeSkillMd(
                    coreDir.resolve("plan/x-task-plan"),
                    "x-task-plan");

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);

            List<String> skills = assembler.selectCoreSkills();

            assertThat(skills).containsExactlyInAnyOrder(
                    "lib/x-lib-tool",
                    "x-task-plan");
        }

        @Test
        @DisplayName("intermediate category dir without SKILL.md "
                + "is not emitted")
        void select_categoryDir_isNotEmitted(
                @TempDir Path tempDir) throws IOException {
            Path coreDir = tempDir.resolve(
                    "targets/claude/skills/core");
            writeSkillMd(
                    coreDir.resolve("plan/x-foo"),
                    "x-foo");

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);

            List<String> skills = assembler.selectCoreSkills();

            assertThat(skills)
                    .doesNotContain("plan")
                    .containsExactly("x-foo");
        }

        @Test
        @DisplayName("legacy flat layout still works "
                + "(backward compatibility)")
        void select_flatLegacyLayout_stillWorks(
                @TempDir Path tempDir) throws IOException {
            Path coreDir = tempDir.resolve(
                    "targets/claude/skills/core");
            writeSkillMd(
                    coreDir.resolve("x-legacy-flat"),
                    "x-legacy-flat");

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);

            List<String> skills = assembler.selectCoreSkills();

            assertThat(skills)
                    .containsExactly("x-legacy-flat");
        }
    }

    @Nested
    @DisplayName("assemble — flat output from hierarchical SoT")
    class FlatOutput {

        @Test
        @DisplayName("hierarchical SoT produces flat output paths")
        void assemble_hierarchicalCore_producesFlatOutput(
                @TempDir Path tempDir) throws IOException {
            Path coreDir = tempDir.resolve(
                    "targets/claude/skills/core");
            writeSkillMd(
                    coreDir.resolve("plan/x-epic-create"),
                    "x-epic-create");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config = TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/x-epic-create/SKILL.md")).exists();
            assertThat(outputDir.resolve(
                    "skills/plan/x-epic-create/SKILL.md"))
                    .doesNotExist();
        }
    }

    @Nested
    @DisplayName("conditional — recursive traversal")
    class ConditionalRecursive {

        @Test
        @DisplayName("conditional skill in category subfolder "
                + "is resolved")
        void copy_condSkillInSubfolder_resolvesSrc(
                @TempDir Path tempDir) throws IOException {
            Path conditionalDir = tempDir.resolve(
                    "targets/claude/skills/conditional");
            writeSkillMd(
                    conditionalDir.resolve(
                            "test/run-e2e"),
                    "run-e2e");
            Path coreDir = tempDir.resolve(
                    "targets/claude/skills/core");
            Files.createDirectories(coreDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/run-e2e/SKILL.md")).exists();
        }
    }

    private static void writeSkillMd(
            Path skillDir, String name) throws IOException {
        Files.createDirectories(skillDir);
        Files.writeString(
                skillDir.resolve("SKILL.md"),
                "---\nname: " + name + "\n---\n# Test\n",
                StandardCharsets.UTF_8);
    }
}
