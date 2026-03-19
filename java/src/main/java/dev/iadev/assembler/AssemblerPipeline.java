package dev.iadev.assembler;

import dev.iadev.exception.PipelineException;
import dev.iadev.model.PipelineResult;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the execution of 23 assemblers in the fixed
 * order defined by RULE-005.
 *
 * <p>Execution flow:
 * <ol>
 *   <li>{@link #buildAssemblers()} — instantiates 23
 *       assemblers in the correct order</li>
 *   <li>{@link #executeAssemblers} — sequential loop invoking
 *       {@code assemble()} on each assembler</li>
 *   <li>{@link #runPipeline} — main entry point that creates
 *       a temp directory, executes assemblers, and handles
 *       dry-run vs. real output</li>
 * </ol>
 *
 * <p>In dry-run mode, files are generated in a temp directory
 * that is cleaned up after execution. In real mode, files are
 * generated in the output directory directly (atomic output
 * is handled externally via RULE-008).</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * AssemblerPipeline pipeline = new AssemblerPipeline(
 *     AssemblerPipeline.buildAssemblers());
 * PipelineResult result = pipeline.runPipeline(
 *     config, outputDir, PipelineOptions.defaults());
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see PipelineOptions
 */
public final class AssemblerPipeline {

    /** Warning appended to dry-run results. */
    public static final String DRY_RUN_WARNING =
            "Dry run -- no files written";

    private final List<AssemblerDescriptor> descriptors;

    /**
     * Creates a pipeline with the given assembler descriptors.
     *
     * @param descriptors the ordered list of assembler
     *                    descriptors
     */
    public AssemblerPipeline(
            List<AssemblerDescriptor> descriptors) {
        this.descriptors = List.copyOf(descriptors);
    }

    /**
     * Builds the ordered list of 23 assemblers per RULE-005.
     *
     * <p>Order: Rules, Skills, Agents, Patterns, Protocols,
     * Hooks, Settings, GithubInstructions, GithubMcp,
     * GithubSkills, GithubAgents, GithubHooks, GithubPrompts,
     * Docs, GrpcDocs, Runbook, CodexAgentsMd, CodexConfig,
     * CodexSkills, DocsAdr, Cicd, EpicReport, Readme</p>
     *
     * <p>Each assembler is a no-op stub that returns an empty
     * list. Concrete implementations are provided by downstream
     * stories (0010-0021).</p>
     *
     * @return immutable ordered list of assembler descriptors
     */
    public static List<AssemblerDescriptor> buildAssemblers() {
        return List.of(
                new AssemblerDescriptor(
                        "RulesAssembler",
                        AssemblerTarget.CLAUDE,
                        new RulesAssembler()),
                new AssemblerDescriptor(
                        "SkillsAssembler",
                        AssemblerTarget.CLAUDE,
                        new SkillsAssembler()),
                new AssemblerDescriptor(
                        "AgentsAssembler",
                        AssemblerTarget.CLAUDE,
                        new AgentsAssembler()),
                new AssemblerDescriptor(
                        "PatternsAssembler",
                        AssemblerTarget.CLAUDE,
                        new PatternsAssembler()),
                new AssemblerDescriptor(
                        "ProtocolsAssembler",
                        AssemblerTarget.CLAUDE,
                        new ProtocolsAssembler()),
                new AssemblerDescriptor(
                        "HooksAssembler",
                        AssemblerTarget.CLAUDE,
                        new HooksAssembler()),
                new AssemblerDescriptor(
                        "SettingsAssembler",
                        AssemblerTarget.CLAUDE,
                        new SettingsAssembler()),
                new AssemblerDescriptor(
                        "GithubInstructionsAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubInstructionsAssembler()),
                new AssemblerDescriptor(
                        "GithubMcpAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubMcpAssembler()),
                stub("GithubSkillsAssembler",
                        AssemblerTarget.GITHUB),
                stub("GithubAgentsAssembler",
                        AssemblerTarget.GITHUB),
                stub("GithubHooksAssembler",
                        AssemblerTarget.GITHUB),
                stub("GithubPromptsAssembler",
                        AssemblerTarget.GITHUB),
                stub("DocsAssembler",
                        AssemblerTarget.DOCS),
                stub("GrpcDocsAssembler",
                        AssemblerTarget.DOCS),
                stub("RunbookAssembler",
                        AssemblerTarget.ROOT),
                stub("CodexAgentsMdAssembler",
                        AssemblerTarget.ROOT),
                stub("CodexConfigAssembler",
                        AssemblerTarget.CODEX),
                stub("CodexSkillsAssembler",
                        AssemblerTarget.CODEX_AGENTS),
                stub("DocsAdrAssembler",
                        AssemblerTarget.ROOT),
                stub("CicdAssembler",
                        AssemblerTarget.ROOT),
                stub("EpicReportAssembler",
                        AssemblerTarget.ROOT),
                stub("ReadmeAssembler",
                        AssemblerTarget.CLAUDE));
    }

    /**
     * Executes assemblers sequentially, aggregating files and
     * warnings.
     *
     * <p>Each assembler receives the resolved target directory
     * based on its {@link AssemblerTarget}. Exceptions from
     * assemblers are wrapped in {@link PipelineException}
     * unless already a PipelineException.</p>
     *
     * @param descriptors the ordered assembler descriptors
     * @param config      the project configuration
     * @param outputDir   the base output directory
     * @param engine      the template engine
     * @return aggregated files and warnings
     * @throws PipelineException if any assembler fails
     */
    public static NormalizedResult executeAssemblers(
            List<AssemblerDescriptor> descriptors,
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine) {
        List<String> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (AssemblerDescriptor desc : descriptors) {
            try {
                Path targetDir =
                        desc.target().resolve(outputDir);
                List<String> result =
                        desc.assembler().assemble(
                                config, engine, targetDir);
                files.addAll(result);
            } catch (PipelineException pe) {
                throw pe;
            } catch (Exception e) {
                throw new PipelineException(
                        "Pipeline failed at "
                                + desc.name()
                                + ": " + e.getMessage(),
                        desc.name(), e);
            }
        }

        return new NormalizedResult(files, warnings);
    }

    /**
     * Runs the full pipeline with dry-run or real output.
     *
     * <p>In dry-run mode, generates files in a temporary
     * directory and returns the result without persisting
     * files to the output directory. In real mode, generates
     * files directly in the output directory.</p>
     *
     * @param config    the project configuration
     * @param outputDir the final output directory
     * @param options   the pipeline execution options
     * @return the pipeline execution result
     * @throws PipelineException if any assembler fails
     */
    public PipelineResult runPipeline(
            ProjectConfig config,
            Path outputDir,
            PipelineOptions options) {
        long start = System.nanoTime();

        TemplateEngine engine = createEngine(options);

        NormalizedResult result;
        if (options.dryRun()) {
            result = runDry(config, engine);
        } else {
            result = runReal(config, outputDir, engine);
        }

        long durationMs =
                (System.nanoTime() - start) / 1_000_000;

        return new PipelineResult(
                true,
                outputDir.toString(),
                result.files(),
                result.warnings(),
                durationMs);
    }

    private NormalizedResult runDry(
            ProjectConfig config,
            TemplateEngine engine) {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(
                    "ia-dev-env-dry-");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to create temp directory", e);
        }

        try {
            NormalizedResult result = executeAssemblers(
                    descriptors, config, tempDir, engine);
            List<String> warnings =
                    new ArrayList<>(result.warnings());
            warnings.add(DRY_RUN_WARNING);
            return new NormalizedResult(
                    result.files(), warnings);
        } finally {
            deleteQuietly(tempDir);
        }
    }

    private NormalizedResult runReal(
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine) {
        return executeAssemblers(
                descriptors, config, outputDir, engine);
    }

    private TemplateEngine createEngine(
            PipelineOptions options) {
        if (options.resourcesDir() != null) {
            return new TemplateEngine(options.resourcesDir());
        }
        return new TemplateEngine();
    }

    private static AssemblerDescriptor stub(
            String name, AssemblerTarget target) {
        return new AssemblerDescriptor(
                name, target, (c, e, p) -> List.of());
    }

    private static void deleteQuietly(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walkFileTree(dir,
                        new java.nio.file.SimpleFileVisitor<>() {
                    @Override
                    public java.nio.file.FileVisitResult
                    visitFile(
                            Path file,
                            java.nio.file.attribute
                                    .BasicFileAttributes attrs)
                            throws IOException {
                        Files.delete(file);
                        return java.nio.file
                                .FileVisitResult.CONTINUE;
                    }

                    @Override
                    public java.nio.file.FileVisitResult
                    postVisitDirectory(
                            Path d, IOException exc)
                            throws IOException {
                        Files.delete(d);
                        return java.nio.file
                                .FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException ignored) {
            // Best-effort cleanup for temp dirs
        }
    }

    /**
     * Aggregated files and warnings from assembler execution.
     *
     * @param files    the list of generated file paths
     * @param warnings the list of non-fatal warnings
     */
    public record NormalizedResult(
            List<String> files,
            List<String> warnings) {

        /**
         * Creates a NormalizedResult with immutable lists.
         */
        public NormalizedResult {
            files = List.copyOf(files);
            warnings = List.copyOf(warnings);
        }
    }
}
