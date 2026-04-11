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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GithubSkillsAssembler — base skills,
 * SKILL_GROUPS constant, renderSkill, and copyReferences.
 */
@DisplayName("GithubSkillsAssembler — base")
class GithubSkillsAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("SKILL_GROUPS constant")
    class SkillGroupsConstant {

        @Test
        @DisplayName("contains exactly 8 groups")
        void assemble_whenCalled_containsEightGroups() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS)
                    .hasSize(8);
        }

        @Test
        @DisplayName("story group has 7 skills")
        void assemble_storyGroup_hasSevenSkills() {
            // Post EPIC-0036: SKILL_GROUPS is derived from
            // targets/github-copilot/skills/story/*.md. The
            // old hardcoded registry listed x-story-plan,
            // x-task-plan, and x-epic-orchestrate but these .md
            // files never existed on disk (silently dropped
            // at assembly time before this story).
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("story"))
                    .hasSize(7);
        }

        @Test
        @DisplayName("dev group has 17 skills")
        void assemble_devGroup_hasSeventeenSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("dev"))
                    .hasSize(17);
        }

        @Test
        @DisplayName("review group has 18 skills")
        void assemble_reviewGroup_hasEighteenSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("review"))
                    .hasSize(18);
        }

        @Test
        @DisplayName("testing group has 7 skills")
        void assemble_testingGroup_hasSevenSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("testing"))
                    .hasSize(7);
        }

        @Test
        @DisplayName("infrastructure group has 4 skills")
        void assemble_infraGroup_hasFourSkills() {
            // Post EPIC-0036: SKILL_GROUPS is derived from
            // targets/github-copilot/skills/infrastructure.
            // The old hardcoded registry listed
            // setup-environment but the .md file never
            // existed. An orphan x-setup-stack.md file was
            // deleted as dead code. Net: 4 skills
            // (dockerfile, iac-terraform, k8s-deployment,
            // k8s-kustomize).
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("infrastructure"))
                    .hasSize(4);
        }

        @Test
        @DisplayName("knowledge-packs group has 17 skills")
        void assemble_knowledgePacksGroup_hasSeventeenSkills() {
            // Post EPIC-0036: SKILL_GROUPS is derived from
            // targets/github-copilot/skills/knowledge-packs.
            // The old hardcoded registry listed
            // patterns-outbox but no file exists on disk.
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("knowledge-packs"))
                    .hasSize(17);
        }

        @Test
        @DisplayName("git-troubleshooting group"
                + " has 8 skills")
        void assemble_gitTroubleshootingGroup_hasEightSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("git-troubleshooting"))
                    .hasSize(8);
        }

        @Test
        @DisplayName("lib group has 3 skills")
        void assemble_libGroup_hasThreeSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("lib"))
                    .hasSize(3);
        }
    }

    @Nested
    @DisplayName("NESTED_GROUPS constant")
    class NestedGroupsConstant {

        @Test
        @DisplayName("contains only lib")
        void assemble_whenCalled_containsOnlyLib() {
            assertThat(
                    GithubSkillsAssembler.NESTED_GROUPS)
                    .containsExactly("lib");
        }
    }

    @Nested
    @DisplayName("renderSkill — generates SKILL.md")
    class RenderSkill {

        @Test
        @DisplayName("creates SKILL.md with placeholder"
                + " replacement")
        void renderSkill_whenCalled_createsSkillMd(
                @TempDir Path tempDir)
                throws IOException {
            Path srcDir = tempDir.resolve("src");
            Files.createDirectories(srcDir);
            Files.writeString(
                    srcDir.resolve("test-skill.md"),
                    "# Skill for {{PROJECT_NAME}}",
                    StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler(tempDir);
            TemplateEngine engine = new TemplateEngine();

            SkillRenderContext ctx =
                    new SkillRenderContext(
                            srcDir, outputDir,
                            null, Map.of());
            var result = assembler.renderSkill(
                    engine, ctx, "test-skill");

            assertThat(result).isPresent();
            Path skillMd = outputDir.resolve(
                    "skills/test-skill/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("returns empty for missing template")
        void renderSkill_whenCalled_returnsEmptyForMissing(
                @TempDir Path tempDir) {
            Path srcDir = tempDir.resolve("src");
            Path outputDir = tempDir.resolve("output");

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler(tempDir);
            TemplateEngine engine = new TemplateEngine();

            SkillRenderContext ctx =
                    new SkillRenderContext(
                            srcDir, outputDir,
                            null, Map.of());
            var result = assembler.renderSkill(
                    engine, ctx, "nonexistent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("creates nested skill for lib group")
        void renderSkill_whenCalled_createsNestedSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path srcDir = tempDir.resolve("src");
            Files.createDirectories(srcDir);
            Files.writeString(
                    srcDir.resolve("x-lib-test.md"),
                    "# Lib skill",
                    StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler(tempDir);
            TemplateEngine engine = new TemplateEngine();

            SkillRenderContext ctx =
                    new SkillRenderContext(
                            srcDir, outputDir,
                            "lib", Map.of());
            var result = assembler.renderSkill(
                    engine, ctx, "x-lib-test");

            assertThat(result).isPresent();
            Path skillMd = outputDir.resolve(
                    "skills/lib/x-lib-test/SKILL.md");
            assertThat(skillMd).exists();
        }
    }

}
