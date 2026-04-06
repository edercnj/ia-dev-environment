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
class GithubSkillsAssemblerBaseTest {

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
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("story"))
                    .hasSize(7);
        }

        @Test
        @DisplayName("dev group has 12 skills")
        void assemble_devGroup_hasTwelveSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("dev"))
                    .hasSize(12);
        }

        @Test
        @DisplayName("review group has 16 skills")
        void assemble_reviewGroup_hasSixteenSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("review"))
                    .hasSize(16);
        }

        @Test
        @DisplayName("testing group has 6 skills")
        void assemble_testingGroup_hasSixSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("testing"))
                    .hasSize(6);
        }

        @Test
        @DisplayName("infrastructure group has 5 skills")
        void assemble_infraGroup_hasFiveSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("infrastructure"))
                    .hasSize(5);
        }

        @Test
        @DisplayName("knowledge-packs group has 18 skills")
        void assemble_knowledgePacksGroup_hasEighteenSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("knowledge-packs"))
                    .hasSize(18);
        }

        @Test
        @DisplayName("git-troubleshooting group"
                + " has 6 skills")
        void assemble_gitTroubleshootingGroup_hasSixSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("git-troubleshooting"))
                    .hasSize(6);
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
