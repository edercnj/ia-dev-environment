package dev.iadev.cli;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerTarget;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link VerbosePipelineRunner}.
 */
@DisplayName("VerbosePipelineRunner")
class VerbosePipelineRunnerTest {

    @Nested
    @DisplayName("runVerbose — platform filter output")
    class PlatformFilterOutput {

        @Test
        void noFilter_whenCalled_outputContainsNoFilterApplied(
                @TempDir Path tempDir) {
            List<AssemblerDescriptor> all =
                    stubAssemblers();
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            PipelineOptions options =
                    new PipelineOptions(
                            true, false, true,
                            false, null, Set.of());

            VerbosePipelineRunner.runVerbose(
                    TestConfigBuilder.minimal(),
                    tempDir, options,
                    all, all, out);
            out.flush();

            String output = sw.toString();
            assertThat(output).contains(
                    "no filter applied");
        }

        @Test
        void withFilter_whenCalled_outputContainsIncluded(
                @TempDir Path tempDir) {
            List<AssemblerDescriptor> all =
                    stubAssemblers();
            List<AssemblerDescriptor> filtered =
                    List.of(all.get(0), all.get(2));
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            PipelineOptions options =
                    new PipelineOptions(
                            true, false, true,
                            false, null,
                            Set.of(Platform.CLAUDE_CODE));

            VerbosePipelineRunner.runVerbose(
                    TestConfigBuilder.minimal(),
                    tempDir, options,
                    filtered, all, out);
            out.flush();

            String output = sw.toString();
            assertThat(output).contains("INCLUDED:");
            assertThat(output).contains("SKIPPED:");
        }

        @Test
        void dryRun_whenCalled_warningContainsPlatformInfo(
                @TempDir Path tempDir) {
            List<AssemblerDescriptor> all =
                    stubAssemblers();
            List<AssemblerDescriptor> filtered =
                    List.of(all.get(0), all.get(2));
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            PipelineOptions options =
                    new PipelineOptions(
                            true, false, true,
                            false, null,
                            Set.of(Platform.CLAUDE_CODE));

            PipelineResult result =
                    VerbosePipelineRunner.runVerbose(
                            TestConfigBuilder.minimal(),
                            tempDir, options,
                            filtered, all, out);

            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "Dry run -- no files written"));
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "claude-code"));
        }

        @Test
        void noFilterDryRun_whenCalled_warningShowsAll(
                @TempDir Path tempDir) {
            List<AssemblerDescriptor> all =
                    stubAssemblers();
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            PipelineOptions options =
                    new PipelineOptions(
                            true, false, true,
                            false, null, Set.of());

            PipelineResult result =
                    VerbosePipelineRunner.runVerbose(
                            TestConfigBuilder.minimal(),
                            tempDir, options,
                            all, all, out);

            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "Platform: all"));
        }

        @Test
        void verbose_whenCalled_outputContainsFilterHeader(
                @TempDir Path tempDir) {
            List<AssemblerDescriptor> all =
                    stubAssemblers();
            List<AssemblerDescriptor> filtered =
                    List.of(all.get(0), all.get(2));
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            PipelineOptions options =
                    new PipelineOptions(
                            false, false, true,
                            false, null,
                            Set.of(Platform.CLAUDE_CODE));

            VerbosePipelineRunner.runVerbose(
                    TestConfigBuilder.minimal(),
                    tempDir, options,
                    filtered, all, out);
            out.flush();

            String output = sw.toString();
            assertThat(output).contains(
                    "Platform filter: claude-code");
            assertThat(output).contains(
                    "-> 2 assemblers");
        }
    }

    // --- helpers ---

    private static List<AssemblerDescriptor>
            stubAssemblers() {
        return List.of(
                new AssemblerDescriptor(
                        "StubClaude",
                        AssemblerTarget.CLAUDE,
                        Set.of(Platform.CLAUDE_CODE),
                        (c, e, p) -> List.of("a.md")),
                new AssemblerDescriptor(
                        "StubCopilot",
                        AssemblerTarget.GITHUB,
                        Set.of(Platform.COPILOT),
                        (c, e, p) -> List.of("b.md")),
                new AssemblerDescriptor(
                        "StubShared",
                        AssemblerTarget.ROOT,
                        Set.of(Platform.SHARED),
                        (c, e, p) -> List.of("c.md")));
    }
}
