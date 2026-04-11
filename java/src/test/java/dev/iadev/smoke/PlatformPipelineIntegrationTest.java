package dev.iadev.smoke;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerFactory;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that platform-filtered
 * pipeline execution produces exactly the expected
 * directory structure: platform-specific directories
 * PRESENT and non-platform directories ABSENT.
 *
 * <p>Uses java-spring profile as representative
 * target for all platform combinations.</p>
 *
 * @see PlatformFilter
 * @see AssemblerPipeline
 */
@DisplayName("Platform Pipeline Integration")
class PlatformPipelineIntegrationTest {

    private static final String PROFILE = "java-spring";

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("CLAUDE_CODE filter")
    class ClaudeCodeFilter {

        @Test
        @DisplayName(".claude/ directory is present")
        void claudeCode_claudeDirPresent() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(out.resolve(".claude"))
                    .isDirectory();
        }

        @Test
        @DisplayName(".claude/rules/ subdirectory exists")
        void claudeCode_rulesSubdirExists() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(out.resolve(".claude/rules"))
                    .isDirectory();
        }

        @Test
        @DisplayName(".claude/skills/ subdirectory exists")
        void claudeCode_skillsSubdirExists() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(out.resolve(".claude/skills"))
                    .isDirectory();
        }

        @Test
        @DisplayName(".claude/agents/ subdirectory exists")
        void claudeCode_agentsSubdirExists() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(out.resolve(".claude/agents"))
                    .isDirectory();
        }

        @Test
        @DisplayName(".codex/ directory is ABSENT")
        void claudeCode_codexDirAbsent() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(out.resolve(".codex"))
                    .doesNotExist();
        }

        @Test
        @DisplayName(".agents/ directory is ABSENT")
        void claudeCode_agentsDirAbsent() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(out.resolve(".agents"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("shared directories are present")
        void claudeCode_sharedDirsPresent() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(out.resolve("adr"))
                    .isDirectory();
        }
    }

    @Nested
    @DisplayName("CODEX filter")
    class CodexFilter {

        @Test
        @DisplayName(".codex/ directory is present")
        void codex_codexDirPresent() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CODEX));

            assertThat(out.resolve(".codex"))
                    .isDirectory();
        }

        @Test
        @DisplayName("AGENTS.md root file is present")
        void codex_agentsMdPresent() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CODEX));

            assertThat(out.resolve("AGENTS.md"))
                    .isRegularFile();
        }

        @Test
        @DisplayName("AGENTS.override.md root file "
                + "is present")
        void codex_agentsOverrideMdPresent() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CODEX));

            assertThat(out.resolve("AGENTS.override.md"))
                    .isRegularFile();
        }

        @Test
        @DisplayName(".claude/rules/ is ABSENT "
                + "(claude-specific)")
        void codex_claudeRulesAbsent() {
            Path out = runWithPlatforms(
                    Set.of(Platform.CODEX));

            assertThat(out.resolve(".claude/rules"))
                    .doesNotExist();
        }

    }

    @Nested
    @DisplayName("CLAUDE_CODE + CODEX composition")
    class ClaudeAndCodex {

        @Test
        @DisplayName(".claude/ directory is present")
        void claudeAndCodex_claudePresent() {
            Path out = runWithPlatforms(Set.of(
                    Platform.CLAUDE_CODE,
                    Platform.CODEX));

            assertThat(out.resolve(".claude"))
                    .isDirectory();
        }

        @Test
        @DisplayName(".codex/ directory is present")
        void claudeAndCodex_codexPresent() {
            Path out = runWithPlatforms(Set.of(
                    Platform.CLAUDE_CODE,
                    Platform.CODEX));

            assertThat(out.resolve(".codex"))
                    .isDirectory();
        }

        @Test
        @DisplayName("shared adr/ directory is present")
        void claudeAndCodex_sharedPresent() {
            Path out = runWithPlatforms(Set.of(
                    Platform.CLAUDE_CODE,
                    Platform.CODEX));

            assertThat(out.resolve("adr"))
                    .isDirectory();
        }
    }

    @Nested
    @DisplayName("No filter (all platforms)")
    class NoFilter {

        @Test
        @DisplayName("all platform directories present")
        void noFilter_allDirsPresent() {
            Path out = runWithPlatforms(Set.of());

            assertThat(out.resolve(".claude"))
                    .isDirectory();
            assertThat(out.resolve(".codex"))
                    .isDirectory();
            assertThat(out.resolve(".agents"))
                    .isDirectory();
        }

        @Test
        @DisplayName("result matches unfiltered pipeline")
        void noFilter_matchesDefault()
                throws IOException {
            Path outFiltered = runWithPlatforms(Set.of());
            Path outDefault = runDefaultPipeline();

            long filteredCount =
                    SmokeTestValidators.countFiles(
                            outFiltered);
            long defaultCount =
                    SmokeTestValidators.countFiles(
                            outDefault);

            assertThat(filteredCount)
                    .as("no-filter should match default")
                    .isEqualTo(defaultCount);
        }
    }

    @Nested
    @DisplayName("CLI > YAML precedence")
    class CliOverridesYaml {

        @Test
        @DisplayName("CLI codex overrides YAML "
                + "claude-code — .codex/ present, "
                + ".claude/ absent")
        void cliOverridesYaml_codexWins() {
            ProjectConfig yamlConfig =
                    ConfigProfiles.getStack(PROFILE);
            ProjectConfig configWithClaude =
                    new ProjectConfig(
                            yamlConfig.project(),
                            yamlConfig.architecture(),
                            yamlConfig.interfaces(),
                            yamlConfig.language(),
                            yamlConfig.framework(),
                            yamlConfig.data(),
                            yamlConfig.infrastructure(),
                            yamlConfig.security(),
                            yamlConfig.testing(),
                            yamlConfig.mcp(),
                            yamlConfig.compliance(),
                            Set.of(Platform.CLAUDE_CODE),
                            yamlConfig.branchingModel());

            Set<Platform> cliPlatforms =
                    Set.of(Platform.CODEX);
            Path out = runWithConfigAndPlatforms(
                    configWithClaude, cliPlatforms);

            assertThat(out.resolve(".codex"))
                    .as("CLI codex wins: "
                            + ".codex present")
                    .isDirectory();
            assertThat(out.resolve(".claude/rules"))
                    .as("CLI codex wins: "
                            + ".claude/rules absent")
                    .doesNotExist();
        }
    }

    // --- helpers ---

    private Path runWithPlatforms(Set<Platform> platforms) {
        Path out = tempDir.resolve(
                "out-" + platforms.hashCode());
        SmokeTestValidators.createDirectoryQuietly(out);

        ProjectConfig config =
                ConfigProfiles.getStack(PROFILE);
        PipelineOptions options = new PipelineOptions(
                false, true, false, false,
                null, platforms);
        List<AssemblerDescriptor> assemblers =
                AssemblerFactory.buildAssemblers(options);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(assemblers);

        PipelineResult result =
                pipeline.runPipeline(config, out, options);
        assertThat(result.success())
                .as("Pipeline must succeed")
                .isTrue();

        return out;
    }

    private Path runWithConfigAndPlatforms(
            ProjectConfig config,
            Set<Platform> platforms) {
        Path out = tempDir.resolve(
                "out-cli-" + platforms.hashCode());
        SmokeTestValidators.createDirectoryQuietly(out);

        PipelineOptions options = new PipelineOptions(
                false, true, false, false,
                null, platforms);
        List<AssemblerDescriptor> assemblers =
                AssemblerFactory.buildAssemblers(options);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(assemblers);

        PipelineResult result =
                pipeline.runPipeline(config, out, options);
        assertThat(result.success())
                .as("Pipeline must succeed")
                .isTrue();

        return out;
    }

    private Path runDefaultPipeline() {
        Path out = tempDir.resolve("out-default");
        SmokeTestValidators.createDirectoryQuietly(out);

        ProjectConfig config =
                ConfigProfiles.getStack(PROFILE);
        AssemblerPipeline pipeline = new AssemblerPipeline(
                AssemblerPipeline.buildAssemblers());
        PipelineOptions options = new PipelineOptions(
                false, true, false, null);

        PipelineResult result =
                pipeline.runPipeline(config, out, options);
        assertThat(result.success()).isTrue();

        return out;
    }
}
