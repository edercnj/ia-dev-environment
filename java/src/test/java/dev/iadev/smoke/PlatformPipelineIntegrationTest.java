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
    @DisplayName("No filter (all platforms)")
    class NoFilter {

        @Test
        @DisplayName("claude directory is present")
        void noFilter_claudeDirPresent() {
            Path out = runWithPlatforms(Set.of());

            assertThat(out.resolve(".claude"))
                    .isDirectory();
        }

        @Test
        @DisplayName(".codex/ directory is ABSENT")
        void noFilter_codexDirAbsent() {
            Path out = runWithPlatforms(Set.of());

            assertThat(out.resolve(".codex"))
                    .doesNotExist();
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
