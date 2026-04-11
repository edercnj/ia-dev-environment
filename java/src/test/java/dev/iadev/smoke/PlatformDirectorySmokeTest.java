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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests verifying directory PRESENCE and ABSENCE
 * for each platform across multiple profiles.
 *
 * <p>For each platform, validates:
 * <ul>
 *   <li>Expected directories exist and contain files</li>
 *   <li>Non-expected directories do NOT exist</li>
 *   <li>Shared directories always present</li>
 * </ul>
 *
 * <p>Tests run against 3 representative profiles
 * (java-spring, go-gin, python-fastapi) to ensure
 * platform filtering is profile-independent.</p>
 */
@DisplayName("Platform Directory Smoke Tests")
class PlatformDirectorySmokeTest {

    @TempDir
    Path tempDir;

    static Stream<String> representativeProfiles() {
        return Stream.of(
                "java-spring", "go-gin",
                "python-fastapi");
    }

    @Nested
    @DisplayName("claude-code — directories")
    class ClaudeCode {

        @ParameterizedTest(name = "{0}")
        @MethodSource("dev.iadev.smoke."
                + "PlatformDirectorySmokeTest"
                + "#representativeProfiles")
        @DisplayName(".claude/ exists with rules, skills, "
                + "agents")
        void claudeCode_claudeDirHasContent(
                String profile) throws IOException {
            Path out = runForPlatform(
                    profile, Set.of(Platform.CLAUDE_CODE));

            assertThat(out.resolve(".claude"))
                    .isDirectory();
            assertThat(out.resolve(".claude/rules"))
                    .isDirectory();
            assertThat(out.resolve(".claude/skills"))
                    .isDirectory();
            assertThat(out.resolve(".claude/agents"))
                    .isDirectory();
            assertThat(countFilesIn(
                    out.resolve(".claude")))
                    .as("claude dir should have files")
                    .isPositive();
        }

    }

    @Nested
    @DisplayName("shared directories always present")
    class SharedAlways {

        @ParameterizedTest(name = "{0}")
        @MethodSource("dev.iadev.smoke."
                + "PlatformDirectorySmokeTest"
                + "#representativeProfiles")
        @DisplayName("adr/ present for CLAUDE_CODE")
        void claude_adrPresent(String profile) {
            Path out = runForPlatform(
                    profile, Set.of(Platform.CLAUDE_CODE));

            assertThat(out.resolve("adr"))
                    .isDirectory();
        }
    }

    // --- helpers ---

    private Path runForPlatform(
            String profile, Set<Platform> platforms) {
        Path out = tempDir.resolve(
                profile + "-" + platforms.hashCode());
        SmokeTestValidators.createDirectoryQuietly(out);

        ProjectConfig config =
                ConfigProfiles.getStack(profile);
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
                .as("Pipeline must succeed for %s/%s",
                        profile, platforms)
                .isTrue();

        return out;
    }

    private static long countFilesIn(Path dir)
            throws IOException {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        return SmokeTestValidators.countFiles(dir);
    }
}
