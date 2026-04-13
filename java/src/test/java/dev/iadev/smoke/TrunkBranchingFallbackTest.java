package dev.iadev.smoke;

import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.application.assembler.SkillsAssembler;
import dev.iadev.domain.model.BranchingModel;
import dev.iadev.domain.model.PipelineResult;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates backward compatibility with trunk-based
 * branching model at the configuration level.
 *
 * <p>Story-0027-0010: verifies that the
 * {@link BranchingModel#TRUNK} enum correctly resolves
 * to {@code main} as base branch, and that
 * {@link BranchingModel#GITFLOW} (default) resolves to
 * {@code develop}. Also validates that
 * {@link ProjectConfig#baseBranch()} returns the correct
 * value for each model, and that the pipeline succeeds
 * for both configurations.</p>
 *
 * <p>Note: skill templates are statically configured for
 * Git Flow (the default model). Dynamic base-branch
 * switching in template content is not supported --
 * trunk mode is a config-level designation only.</p>
 */
@DisplayName("Trunk Branching Fallback Tests")
class TrunkBranchingFallbackTest {

    @Nested
    @DisplayName("BranchingModel — config resolution")
    class ConfigResolution {

        @Test
        @DisplayName("TRUNK baseBranch returns main")
        void trunkModel_baseBranch_returnsMain() {
            assertThat(BranchingModel.TRUNK.baseBranch())
                    .isEqualTo("main");
        }

        @Test
        @DisplayName("GITFLOW baseBranch returns develop")
        void gitflowModel_baseBranch_returnsDevelop() {
            assertThat(
                    BranchingModel.GITFLOW.baseBranch())
                    .isEqualTo("develop");
        }

        @Test
        @DisplayName("default config uses GITFLOW")
        void defaultConfig_branchingModel_isGitflow() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            assertThat(config.branchingModel())
                    .isEqualTo(BranchingModel.GITFLOW);
        }

        @Test
        @DisplayName("default config baseBranch is develop")
        void defaultConfig_baseBranch_isDevelop() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            assertThat(config.baseBranch())
                    .isEqualTo("develop");
        }

        @Test
        @DisplayName("trunk config baseBranch is main")
        void trunkConfig_baseBranch_isMain() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .branchingModel(BranchingModel.TRUNK)
                    .build();
            assertThat(config.baseBranch())
                    .isEqualTo("main");
        }
    }

    @Nested
    @DisplayName("pipeline — trunk config succeeds")
    class TrunkPipelineSuccess {

        @Test
        @DisplayName("full pipeline succeeds with trunk")
        void trunkPipeline_fullRun_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("trunk");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .branchingModel(BranchingModel.TRUNK)
                    .build();
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            AssemblerPipeline
                                    .buildAssemblers());
            PipelineOptions options =
                    new PipelineOptions(
                            false, true, false, null);
            PipelineResult result =
                    pipeline.runPipeline(
                            config, outputDir, options);
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("trunk pipeline generates all"
                + " expected skills")
        void trunkPipeline_generates_allSkills(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("trunk");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .branchingModel(BranchingModel.TRUNK)
                    .build();
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            AssemblerPipeline
                                    .buildAssemblers());
            PipelineOptions options =
                    new PipelineOptions(
                            false, true, false, null);
            pipeline.runPipeline(
                    config, outputDir, options);

            assertThat(outputDir.resolve(
                    ".claude/skills/x-git-push/SKILL.md"))
                    .exists();
            assertThat(outputDir.resolve(
                    ".claude/skills/"
                            + "x-story-implement/SKILL.md"))
                    .exists();
            assertThat(outputDir.resolve(
                    ".claude/skills/"
                            + "x-epic-implement/"
                            + "SKILL.md"))
                    .exists();
            assertThat(outputDir.resolve(
                    ".claude/skills/x-release/SKILL.md"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("pipeline — gitflow config succeeds")
    class GitflowPipelineSuccess {

        @Test
        @DisplayName("full pipeline succeeds"
                + " with gitflow")
        void gitflowPipeline_fullRun_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("gitflow");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .branchingModel(
                            BranchingModel.GITFLOW)
                    .build();
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            AssemblerPipeline
                                    .buildAssemblers());
            PipelineOptions options =
                    new PipelineOptions(
                            false, true, false, null);
            PipelineResult result =
                    pipeline.runPipeline(
                            config, outputDir, options);
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("gitflow pipeline skills"
                + " reference develop")
        void gitflowPipeline_skills_refDevelop(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("gitflow");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .branchingModel(
                            BranchingModel.GITFLOW)
                    .build();
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            AssemblerPipeline
                                    .buildAssemblers());
            PipelineOptions options =
                    new PipelineOptions(
                            false, true, false, null);
            pipeline.runPipeline(
                    config, outputDir, options);

            String gitPush = Files.readString(
                    outputDir.resolve(
                            ".claude/skills/"
                                    + "x-git-push/SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(gitPush)
                    .contains("--base develop");
        }
    }

    @Nested
    @DisplayName("skill generation — both models")
    class SkillGenerationBothModels {

        @Test
        @DisplayName("SkillsAssembler succeeds with"
                + " trunk config")
        void skillsAssembler_trunk_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .branchingModel(BranchingModel.TRUNK)
                    .build();
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    config,
                    new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/x-git-push/SKILL.md"))
                    .exists();
            assertThat(outputDir.resolve(
                    "skills/x-story-implement/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("SkillsAssembler succeeds with"
                + " gitflow config")
        void skillsAssembler_gitflow_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .branchingModel(
                            BranchingModel.GITFLOW)
                    .build();
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    config,
                    new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/x-git-push/SKILL.md"))
                    .exists();
            assertThat(outputDir.resolve(
                    "skills/x-story-implement/SKILL.md"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("fromConfigValue — round-trip")
    class FromConfigValueRoundTrip {

        @Test
        @DisplayName("'trunk' round-trips correctly")
        void fromConfigValue_trunk_roundTrips() {
            BranchingModel model =
                    BranchingModel.fromConfigValue("trunk")
                            .orElseThrow();
            assertThat(model)
                    .isEqualTo(BranchingModel.TRUNK);
            assertThat(model.configValue())
                    .isEqualTo("trunk");
            assertThat(model.baseBranch())
                    .isEqualTo("main");
        }

        @Test
        @DisplayName("'gitflow' round-trips correctly")
        void fromConfigValue_gitflow_roundTrips() {
            BranchingModel model = BranchingModel
                    .fromConfigValue("gitflow")
                    .orElseThrow();
            assertThat(model)
                    .isEqualTo(BranchingModel.GITFLOW);
            assertThat(model.configValue())
                    .isEqualTo("gitflow");
            assertThat(model.baseBranch())
                    .isEqualTo("develop");
        }
    }
}
