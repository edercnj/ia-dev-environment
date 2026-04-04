package dev.iadev.smoke;

import dev.iadev.assembler.AssemblerPipeline;
import dev.iadev.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for all smoke tests.
 *
 * <p>Provides shared infrastructure for running the
 * assembler pipeline against bundled profiles and
 * validating output. Subclasses inherit:</p>
 * <ul>
 *   <li>A {@code @TempDir} for isolated output</li>
 *   <li>{@link #runPipeline(String)} to execute the
 *       pipeline for a given profile</li>
 *   <li>{@link #getOutputDir(String)} to resolve a
 *       profile-specific output directory</li>
 * </ul>
 *
 * <p>RULE-006: All output is written to temporary
 * directories via {@code @TempDir}.</p>
 *
 * @see SmokeTestValidators
 * @see SmokeProfiles
 */
public abstract class SmokeTestBase {

    @TempDir
    protected Path tempDir;

    /**
     * Runs the assembler pipeline for the given profile,
     * writing output to a profile-specific subdirectory
     * under {@link #tempDir}.
     *
     * @param profile the bundled profile name
     * @return the pipeline execution result
     */
    protected PipelineResult runPipeline(String profile) {
        Path outputDir = getOutputDir(profile);
        SmokeTestValidators.createDirectoryQuietly(outputDir);

        ProjectConfig config =
                ConfigProfiles.getStack(profile);

        AssemblerPipeline pipeline = new AssemblerPipeline(
                AssemblerPipeline.buildAssemblers());
        PipelineOptions options = new PipelineOptions(
                false, true, false, null);

        PipelineResult result = pipeline.runPipeline(
                config, outputDir, options);
        assertThat(result.success())
                .as("Pipeline must succeed for profile: %s",
                        profile)
                .isTrue();

        return result;
    }

    /**
     * Returns the profile-specific output directory under
     * {@link #tempDir}.
     *
     * @param profile the bundled profile name
     * @return the output directory path
     */
    protected Path getOutputDir(String profile) {
        return tempDir.resolve(profile);
    }

}
