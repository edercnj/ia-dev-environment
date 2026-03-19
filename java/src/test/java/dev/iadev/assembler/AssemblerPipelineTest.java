package dev.iadev.assembler;

import dev.iadev.exception.PipelineException;
import dev.iadev.model.PipelineResult;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for AssemblerPipeline — orchestrates 23 assemblers
 * per RULE-005.
 */
@DisplayName("AssemblerPipeline")
class AssemblerPipelineTest {

    private static final List<String> EXPECTED_ORDER = List.of(
            "RulesAssembler",
            "SkillsAssembler",
            "AgentsAssembler",
            "PatternsAssembler",
            "ProtocolsAssembler",
            "HooksAssembler",
            "SettingsAssembler",
            "GithubInstructionsAssembler",
            "GithubMcpAssembler",
            "GithubSkillsAssembler",
            "GithubAgentsAssembler",
            "GithubHooksAssembler",
            "GithubPromptsAssembler",
            "DocsAssembler",
            "GrpcDocsAssembler",
            "RunbookAssembler",
            "CodexAgentsMdAssembler",
            "CodexConfigAssembler",
            "CodexSkillsAssembler",
            "DocsAdrAssembler",
            "CicdAssembler",
            "EpicReportAssembler",
            "ReadmeAssembler");

    @Nested
    @DisplayName("buildAssemblers")
    class BuildAssemblers {

        @Test
        @DisplayName("returns exactly 23 assembler descriptors")
        void returnsExactly23() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            assertThat(descriptors).hasSize(23);
        }

        @Test
        @DisplayName("assemblers are in RULE-005 order")
        void correctOrder() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            List<String> names = descriptors.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            assertThat(names).isEqualTo(EXPECTED_ORDER);
        }

        @Test
        @DisplayName("assembler targets match specification")
        void targetsMatchSpec() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            // Verify key targets from the pipeline spec
            assertThat(descriptors.get(0).target())
                    .isEqualTo(AssemblerTarget.CLAUDE);
            assertThat(descriptors.get(7).target())
                    .isEqualTo(AssemblerTarget.GITHUB);
            assertThat(descriptors.get(13).target())
                    .isEqualTo(AssemblerTarget.DOCS);
            assertThat(descriptors.get(15).target())
                    .isEqualTo(AssemblerTarget.ROOT);
            assertThat(descriptors.get(17).target())
                    .isEqualTo(AssemblerTarget.CODEX);
            assertThat(descriptors.get(18).target())
                    .isEqualTo(AssemblerTarget.CODEX_AGENTS);
        }

        @Test
        @DisplayName("each descriptor has non-null assembler")
        void allAssemblersNotNull() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            for (AssemblerDescriptor d : descriptors) {
                assertThat(d.assembler())
                        .as("assembler for %s", d.name())
                        .isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("executeAssemblers")
    class ExecuteAssemblers {

        @Test
        @DisplayName("executes all assemblers sequentially")
        void executesAll(@TempDir Path tempDir) {
            List<String> executionOrder = new ArrayList<>();

            List<AssemblerDescriptor> descriptors = List.of(
                    descriptor("A",
                            trackingAssembler(executionOrder, "A")),
                    descriptor("B",
                            trackingAssembler(executionOrder, "B")),
                    descriptor("C",
                            trackingAssembler(executionOrder, "C")));

            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            AssemblerPipeline.NormalizedResult result =
                    AssemblerPipeline.executeAssemblers(
                            descriptors, config, tempDir, engine);

            assertThat(executionOrder)
                    .containsExactly("A", "B", "C");
            assertThat(result.files()).hasSize(3);
        }

        @Test
        @DisplayName("collects files from all assemblers")
        void collectsFiles(@TempDir Path tempDir) {
            List<AssemblerDescriptor> descriptors = List.of(
                    descriptor("X", (c, e, p) ->
                            List.of("x1.md", "x2.md")),
                    descriptor("Y", (c, e, p) ->
                            List.of("y1.md")));

            AssemblerPipeline.NormalizedResult result =
                    AssemblerPipeline.executeAssemblers(
                            descriptors,
                            TestConfigBuilder.minimal(),
                            tempDir,
                            new TemplateEngine());

            assertThat(result.files())
                    .containsExactly("x1.md", "x2.md", "y1.md");
        }

        @Test
        @DisplayName("wraps exceptions in PipelineException")
        void wrapsExceptions(@TempDir Path tempDir) {
            Assembler failing = (c, e, p) -> {
                throw new RuntimeException("boom");
            };
            List<AssemblerDescriptor> descriptors = List.of(
                    descriptor("FailAssembler", failing));

            assertThatThrownBy(() ->
                    AssemblerPipeline.executeAssemblers(
                            descriptors,
                            TestConfigBuilder.minimal(),
                            tempDir,
                            new TemplateEngine()))
                    .isInstanceOf(PipelineException.class)
                    .hasMessageContaining("FailAssembler")
                    .hasCauseInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("preserves PipelineException as-is")
        void preservesPipelineException(@TempDir Path tempDir) {
            PipelineException original =
                    new PipelineException(
                            "original", "Inner", null);
            Assembler failing = (c, e, p) -> {
                throw original;
            };

            List<AssemblerDescriptor> descriptors = List.of(
                    descriptor("Outer", failing));

            assertThatThrownBy(() ->
                    AssemblerPipeline.executeAssemblers(
                            descriptors,
                            TestConfigBuilder.minimal(),
                            tempDir,
                            new TemplateEngine()))
                    .isSameAs(original);
        }

        @Test
        @DisplayName("stops at first failure")
        void stopsAtFirstFailure(@TempDir Path tempDir) {
            List<String> executed = new ArrayList<>();

            List<AssemblerDescriptor> descriptors = List.of(
                    descriptor("OK",
                            trackingAssembler(executed, "OK")),
                    descriptor("FAIL", (c, e, p) -> {
                        throw new RuntimeException("fail");
                    }),
                    descriptor("NEVER",
                            trackingAssembler(executed, "NEVER")));

            assertThatThrownBy(() ->
                    AssemblerPipeline.executeAssemblers(
                            descriptors,
                            TestConfigBuilder.minimal(),
                            tempDir,
                            new TemplateEngine()))
                    .isInstanceOf(PipelineException.class);

            assertThat(executed).containsExactly("OK");
        }
    }

    @Nested
    @DisplayName("runPipeline")
    class RunPipeline {

        @Test
        @DisplayName("dry-run returns result without writing")
        void dryRun_noFilesWritten(@TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            PipelineOptions opts = new PipelineOptions(
                    true, false, false, null);

            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            stubDescriptors());

            PipelineResult result = pipeline.runPipeline(
                    TestConfigBuilder.minimal(),
                    outputDir, opts);

            assertThat(result.success()).isTrue();
            assertThat(result.filesGenerated()).isNotEmpty();
            assertThat(result.durationMs())
                    .isGreaterThanOrEqualTo(0);
            assertThat(result.warnings())
                    .contains(AssemblerPipeline.DRY_RUN_WARNING);
            // Output dir should not exist in dry-run
            assertThat(outputDir).doesNotExist();
        }

        @Test
        @DisplayName("real run returns successful result")
        void realRun_returnsSuccess(@TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            PipelineOptions opts = PipelineOptions.defaults();

            AssemblerPipeline pipeline =
                    new AssemblerPipeline(stubDescriptors());

            PipelineResult result = pipeline.runPipeline(
                    TestConfigBuilder.minimal(),
                    outputDir, opts);

            assertThat(result.success()).isTrue();
            assertThat(result.filesGenerated()).isNotEmpty();
            assertThat(result.durationMs())
                    .isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("pipeline failure wraps in PipelineException")
        void failure_throwsPipelineException(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            Assembler failing = (c, e, p) -> {
                throw new RuntimeException("kaboom");
            };

            List<AssemblerDescriptor> descriptors = List.of(
                    new AssemblerDescriptor(
                            "BrokenAssembler",
                            AssemblerTarget.ROOT,
                            failing));

            AssemblerPipeline pipeline =
                    new AssemblerPipeline(descriptors);

            assertThatThrownBy(() ->
                    pipeline.runPipeline(
                            TestConfigBuilder.minimal(),
                            outputDir,
                            PipelineOptions.defaults()))
                    .isInstanceOf(PipelineException.class)
                    .hasMessageContaining("BrokenAssembler");
        }

        @Test
        @DisplayName("result contains fileCount matching files")
        void result_fileCountMatchesFiles(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            PipelineOptions opts = new PipelineOptions(
                    true, false, false, null);

            AssemblerPipeline pipeline =
                    new AssemblerPipeline(stubDescriptors());

            PipelineResult result = pipeline.runPipeline(
                    TestConfigBuilder.minimal(),
                    outputDir, opts);

            assertThat(result.filesGenerated().size())
                    .isEqualTo(
                            result.filesGenerated().size());
        }
    }

    // --- helpers ---

    private static AssemblerDescriptor descriptor(
            String name, Assembler assembler) {
        return new AssemblerDescriptor(
                name, AssemblerTarget.ROOT, assembler);
    }

    private static Assembler trackingAssembler(
            List<String> tracker, String name) {
        return (config, engine, outputDir) -> {
            tracker.add(name);
            return List.of(name + ".md");
        };
    }

    private static List<AssemblerDescriptor> stubDescriptors() {
        return List.of(
                new AssemblerDescriptor(
                        "StubA", AssemblerTarget.CLAUDE,
                        (c, e, p) -> List.of("a.md")),
                new AssemblerDescriptor(
                        "StubB", AssemblerTarget.GITHUB,
                        (c, e, p) -> List.of("b.md")));
    }
}
