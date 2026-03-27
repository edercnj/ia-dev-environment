package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
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
 * Tests for GithubSkillsAssembler -- the tenth assembler
 * in the pipeline, generating .github/skills/ with GitHub
 * Copilot skills mirroring .claude/skills/.
 */
@DisplayName("GithubSkillsAssembler")
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
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("story"))
                    .hasSize(7);
        }

        @Test
        @DisplayName("dev group has 10 skills")
        void assemble_devGroup_hasNineSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("dev"))
                    .hasSize(10);
        }

        @Test
        @DisplayName("review group has 9 skills")
        void assemble_reviewGroup_hasNineSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("review"))
                    .hasSize(9);
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
        @DisplayName("knowledge-packs group has 15 skills")
        void assemble_knowledgePacksGroup_hasFifteenSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("knowledge-packs"))
                    .hasSize(15);
        }

        @Test
        @DisplayName("git-troubleshooting group"
                + " has 4 skills")
        void assemble_gitTroubleshootingGroup_hasFourSkills() {
            assertThat(
                    GithubSkillsAssembler.SKILL_GROUPS
                            .get("git-troubleshooting"))
                    .hasSize(4);
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
    @DisplayName("filterSkills — non-infrastructure")
    class FilterSkillsNonInfra {

        @Test
        @DisplayName("returns all skills for"
                + " non-infrastructure group")
        void filterSkills_whenCalled_returnsAllForNonInfra() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            List<String> skills = List.of(
                    "x-story-epic", "x-story-create");

            List<String> filtered =
                    assembler.filterSkills(
                            config, "story", skills);

            assertThat(filtered)
                    .containsExactly(
                            "x-story-epic",
                            "x-story-create");
        }
    }

    @Nested
    @DisplayName("filterSkills — infrastructure"
            + " feature gates")
    class FilterSkillsInfra {

        @Test
        @DisplayName("dockerfile included when"
                + " container is docker")
        void filterSkills_whenCalled_dockerfileIncludedWhenDocker() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("none")
                            .build();
            List<String> skills = List.of(
                    "setup-environment",
                    "k8s-deployment",
                    "k8s-kustomize",
                    "dockerfile",
                    "iac-terraform");

            List<String> filtered =
                    assembler.filterSkills(
                            config, "infrastructure",
                            skills);

            assertThat(filtered)
                    .contains("dockerfile")
                    .doesNotContain("setup-environment")
                    .doesNotContain("k8s-deployment");
        }

        @Test
        @DisplayName("k8s skills included when"
                + " orchestrator is kubernetes")
        void filterSkills_whenCalled_k8sSkillsIncludedWhenKubernetes() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("kubernetes")
                            .build();
            List<String> skills = List.of(
                    "setup-environment",
                    "k8s-deployment",
                    "k8s-kustomize",
                    "dockerfile",
                    "iac-terraform");

            List<String> filtered =
                    assembler.filterSkills(
                            config, "infrastructure",
                            skills);

            assertThat(filtered)
                    .contains("setup-environment")
                    .contains("k8s-deployment")
                    .contains("k8s-kustomize")
                    .contains("dockerfile");
        }

        @Test
        @DisplayName("no infra skills when all none")
        void filterSkills_noInfraSkillsWhenAllNone_succeeds() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .templating("none")
                            .build();
            List<String> skills = List.of(
                    "setup-environment",
                    "k8s-deployment",
                    "k8s-kustomize",
                    "dockerfile",
                    "iac-terraform");

            List<String> filtered =
                    assembler.filterSkills(
                            config, "infrastructure",
                            skills);

            assertThat(filtered).isEmpty();
        }

        @Test
        @DisplayName("iac-terraform included when"
                + " iac is terraform")
        void filterSkills_whenCalled_iacTerraformIncluded() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("terraform")
                            .templating("none")
                            .build();
            List<String> skills = List.of(
                    "setup-environment",
                    "k8s-deployment",
                    "k8s-kustomize",
                    "dockerfile",
                    "iac-terraform");

            List<String> filtered =
                    assembler.filterSkills(
                            config, "infrastructure",
                            skills);

            assertThat(filtered)
                    .containsExactly("iac-terraform");
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

    @Nested
    @DisplayName("copyReferences — copies references dir")
    class CopyReferences {

        @Test
        @DisplayName("copies references directory"
                + " when present")
        void copyReferences_whenCalled_copiesReferencesWhenPresent(
                @TempDir Path tempDir)
                throws IOException {
            Path srcDir = tempDir.resolve("src");
            Path refsDir = srcDir.resolve(
                    "references/test-skill");
            Files.createDirectories(refsDir);
            Files.writeString(
                    refsDir.resolve("ref.md"),
                    "# Reference {{PROJECT_NAME}}",
                    StandardCharsets.UTF_8);

            Path skillDir = tempDir.resolve(
                    "output/skills/test-skill");
            Files.createDirectories(skillDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler(tempDir);
            TemplateEngine engine = new TemplateEngine();

            SkillRenderContext ctx =
                    new SkillRenderContext(
                            srcDir, skillDir,
                            null, Map.of());
            assembler.copyReferences(
                    ctx, engine, skillDir,
                    "test-skill");

            Path destRef = skillDir.resolve(
                    "references/ref.md");
            assertThat(destRef).exists();
        }

        @Test
        @DisplayName("does nothing when references"
                + " dir absent")
        void copyReferences_whenCalled_doesNothingWhenAbsent(
                @TempDir Path tempDir)
                throws IOException {
            Path srcDir = tempDir.resolve("src");
            Files.createDirectories(srcDir);

            Path skillDir = tempDir.resolve(
                    "output/skills/test-skill");
            Files.createDirectories(skillDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler(tempDir);
            TemplateEngine engine = new TemplateEngine();

            SkillRenderContext ctx =
                    new SkillRenderContext(
                            srcDir, skillDir,
                            null, Map.of());
            assembler.copyReferences(
                    ctx, engine, skillDir,
                    "test-skill");

            Path refs = skillDir.resolve("references");
            assertThat(refs).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble — full pipeline integration")
    class AssembleIntegration {

        @Test
        @DisplayName("generates skills from classpath"
                + " templates")
        void assemble_whenCalled_generatesSkillsFromClasspath(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("none")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isNotEmpty();
            assertThat(files.stream()
                    .filter(f -> f.contains("SKILL.md")))
                    .isNotEmpty();
        }

        @Test
        @DisplayName("generates lib skills in nested"
                + " subdirectory")
        void assemble_whenCalled_generatesLibSkillsNested(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .filter(f -> f.contains(
                            "/lib/")))
                    .isNotEmpty();
        }

        @Test
        @DisplayName("applies infrastructure feature"
                + " gates")
        void assemble_whenCalled_appliesInfraFeatureGates(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("none")
                            .iac("none")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .filter(f -> f.contains(
                            "dockerfile")))
                    .isNotEmpty();
            assertThat(files.stream()
                    .filter(f -> f.contains(
                            "k8s-deployment")))
                    .isEmpty();
        }

        @Test
        @DisplayName("copies references for skills"
                + " that have them")
        void assemble_whenCalled_copiesReferences(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            Path lifecycle = outputDir.resolve(
                    "skills/x-dev-lifecycle/references");
            assertThat(lifecycle).exists();
        }
    }
}
