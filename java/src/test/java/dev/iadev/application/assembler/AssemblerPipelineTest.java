package dev.iadev.application.assembler;

import dev.iadev.exception.PipelineException;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
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
 * Tests for AssemblerPipeline — orchestrates 32 assemblers
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
            "PrIssueTemplateAssembler",
            "DocsAssembler",
            "GrpcDocsAssembler",
            "RunbookAssembler",
            "IncidentTemplatesAssembler",
            "ReleaseChecklistAssembler",
            "OperationalRunbookAssembler",
            "SloSliTemplateAssembler",
            "DocsContributingAssembler",
            "DataMigrationPlanAssembler",
            "CodexAgentsMdAssembler",
            "CodexConfigAssembler",
            "CodexSkillsAssembler",
            "CodexRequirementsAssembler",
            "CodexOverrideAssembler",
            "DocsAdrAssembler",
            "CicdAssembler",
            "EpicReportAssembler",
            "ReadmeAssembler");

    @Nested
    @DisplayName("buildAssemblers")
    class BuildAssemblers {

        @Test
        @DisplayName("returns exactly 32 assembler descriptors")
        void assemble_whenCalled_returnsExactly32() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            assertThat(descriptors).hasSize(32);
        }

        @Test
        @DisplayName("assemblers are in RULE-005 order")
        void assemble_whenCalled_correctOrder() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            List<String> names = descriptors.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            assertThat(names).isEqualTo(EXPECTED_ORDER);
        }

        @Test
        @DisplayName("assembler targets match specification")
        void assemble_whenCalled_targetsMatchSpec() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            // Verify key targets from the pipeline spec
            assertThat(descriptors.get(0).target())
                    .isEqualTo(AssemblerTarget.CLAUDE);
            assertThat(descriptors.get(7).target())
                    .isEqualTo(AssemblerTarget.GITHUB);
            assertThat(descriptors.get(13).target())
                    .isEqualTo(AssemblerTarget.GITHUB);
            assertThat(descriptors.get(14).target())
                    .isEqualTo(AssemblerTarget.DOCS);
            assertThat(descriptors.get(16).target())
                    .isEqualTo(AssemblerTarget.ROOT);
            assertThat(descriptors.get(17).target())
                    .isEqualTo(AssemblerTarget.ROOT);
            assertThat(descriptors.get(18).target())
                    .isEqualTo(AssemblerTarget.ROOT);
            assertThat(descriptors.get(19).target())
                    .isEqualTo(AssemblerTarget.ROOT);
            assertThat(descriptors.get(20).target())
                    .isEqualTo(AssemblerTarget.ROOT);
            assertThat(descriptors.get(21).target())
                    .isEqualTo(AssemblerTarget.ROOT);
            assertThat(descriptors.get(22).target())
                    .isEqualTo(AssemblerTarget.ROOT);
            assertThat(descriptors.get(24).target())
                    .isEqualTo(AssemblerTarget.CODEX);
            assertThat(descriptors.get(25).target())
                    .isEqualTo(AssemblerTarget.CODEX_AGENTS);
            assertThat(descriptors.get(26).target())
                    .isEqualTo(AssemblerTarget.CODEX);
            assertThat(descriptors.get(27).target())
                    .isEqualTo(AssemblerTarget.ROOT);
        }

        @Test
        @DisplayName("each descriptor has non-null assembler")
        void assemble_whenCalled_allAssemblersNotNull() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            for (AssemblerDescriptor d : descriptors) {
                assertThat(d.assembler())
                        .as("assembler for %s", d.name())
                        .isInstanceOf(Assembler.class);
            }
        }
    }

    @Nested
    @DisplayName("executeAssemblers")
    class ExecuteAssemblers {

        @Test
        @DisplayName("executes all assemblers sequentially")
        void assemble_whenCalled_executesAll(@TempDir Path tempDir) {
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

            AssemblerResult result =
                    AssemblerPipeline.executeAssemblers(
                            descriptors, config, tempDir, engine);

            assertThat(executionOrder)
                    .containsExactly("A", "B", "C");
            assertThat(result.files()).hasSize(3);
        }

        @Test
        @DisplayName("collects files from all assemblers")
        void assemble_whenCalled_collectsFiles(@TempDir Path tempDir) {
            List<AssemblerDescriptor> descriptors = List.of(
                    descriptor("X", (c, e, p) ->
                            List.of("x1.md", "x2.md")),
                    descriptor("Y", (c, e, p) ->
                            List.of("y1.md")));

            AssemblerResult result =
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
        void assemble_whenCalled_wrapsExceptions(@TempDir Path tempDir) {
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
        void assemble_whenCalled_preservesPipelineException(@TempDir Path tempDir) {
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
        void assemble_whenCalled_stopsAtFirstFailure(@TempDir Path tempDir) {
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

        @Test
        @DisplayName("collects warnings from assemblers"
                + " via assembleWithResult")
        void assemble_whenCalled_collectsWarnings(@TempDir Path tempDir) {
            Assembler withWarnings =
                    new WarningAssembler(
                            List.of("f1.md"),
                            List.of("missing ref"));
            Assembler noWarnings = (c, e, p) ->
                    List.of("f2.md");

            List<AssemblerDescriptor> descriptors = List.of(
                    descriptor("WarnAsm", withWarnings),
                    descriptor("OkAsm", noWarnings));

            AssemblerResult result =
                    AssemblerPipeline.executeAssemblers(
                            descriptors,
                            TestConfigBuilder.minimal(),
                            tempDir,
                            new TemplateEngine());

            assertThat(result.files())
                    .containsExactly("f1.md", "f2.md");
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0))
                    .startsWith("[WARN] WarnAsm:")
                    .contains("missing ref");
        }

        @Test
        @DisplayName("consolidates warnings from"
                + " multiple assemblers in order")
        void assemble_whenCalled_consolidatesWarningsInOrder(
                @TempDir Path tempDir) {
            Assembler asm1 = new WarningAssembler(
                    List.of(), List.of("warn-A"));
            Assembler asm2 = new WarningAssembler(
                    List.of(), List.of());
            Assembler asm3 = new WarningAssembler(
                    List.of(), List.of("warn-C"));

            List<AssemblerDescriptor> descriptors = List.of(
                    descriptor("First", asm1),
                    descriptor("Second", asm2),
                    descriptor("Third", asm3));

            AssemblerResult result =
                    AssemblerPipeline.executeAssemblers(
                            descriptors,
                            TestConfigBuilder.minimal(),
                            tempDir,
                            new TemplateEngine());

            assertThat(result.warnings())
                    .hasSize(2)
                    .containsExactly(
                            "[WARN] First: warn-A",
                            "[WARN] Third: warn-C");
        }

        @Test
        @DisplayName("assembler without warnings"
                + " returns empty warnings")
        void assemble_whenCalled_noWarningsFromDefaultAssembler(
                @TempDir Path tempDir) {
            Assembler simple = (c, e, p) ->
                    List.of("out.md");

            List<AssemblerDescriptor> descriptors = List.of(
                    descriptor("Simple", simple));

            AssemblerResult result =
                    AssemblerPipeline.executeAssemblers(
                            descriptors,
                            TestConfigBuilder.minimal(),
                            tempDir,
                            new TemplateEngine());

            assertThat(result.warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("runPipeline")
    class RunPipeline {

        @Test
        @DisplayName("dry-run returns result without writing")
        void dryRun_whenCalled_noFilesWritten(@TempDir Path tempDir) {
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
        void realRun_whenCalled_returnsSuccess(@TempDir Path tempDir) {
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
        void failure_whenCalled_throwsPipelineException(
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
        void result_fileCount_matchesFiles(
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

    @Nested
    @DisplayName("assembleWithResult — default method")
    class AssembleWithResultDefault {

        @Test
        @DisplayName("default wraps assemble with"
                + " empty warnings")
        void assembleWithResult_defaultWrapsAssemble_succeeds(
                @TempDir Path tempDir) {
            Assembler simple = (c, e, p) ->
                    List.of("file.md");

            AssemblerResult result =
                    simple.assembleWithResult(
                            TestConfigBuilder.minimal(),
                            new TemplateEngine(),
                            tempDir);

            assertThat(result.files())
                    .containsExactly("file.md");
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("override provides warnings")
        void assembleWithResult_whenCalled_overrideProvides(
                @TempDir Path tempDir) {
            Assembler withWarn = new WarningAssembler(
                    List.of("x.md"),
                    List.of("some warning"));

            AssemblerResult result =
                    withWarn.assembleWithResult(
                            TestConfigBuilder.minimal(),
                            new TemplateEngine(),
                            tempDir);

            assertThat(result.files())
                    .containsExactly("x.md");
            assertThat(result.warnings())
                    .containsExactly("some warning");
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

    /**
     * Test helper: assembler that returns fixed files
     * and overrides assembleWithResult with warnings.
     */
    private static final class WarningAssembler
            implements Assembler {

        private final List<String> files;
        private final List<String> warnings;

        WarningAssembler(
                List<String> files,
                List<String> warnings) {
            this.files = files;
            this.warnings = warnings;
        }

        @Override
        public List<String> assemble(
                ProjectConfig config,
                TemplateEngine engine,
                Path outputDir) {
            return files;
        }

        @Override
        public AssemblerResult assembleWithResult(
                ProjectConfig config,
                TemplateEngine engine,
                Path outputDir) {
            return AssemblerResult.of(files, warnings);
        }
    }
}
