package dev.iadev.application.assembler;

import dev.iadev.exception.PipelineException;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the execution of 34 assemblers in the fixed
 * order defined by RULE-005.
 *
 * <p>Per-descriptor filtering, execution, and output
 * shaping are delegated to {@link
 * AssemblerFilterStrategy}; elapsed-time measurement is
 * delegated to {@link PipelineTimer}. Assembler
 * construction is delegated to {@link AssemblerFactory}.</p>
 *
 * @see Assembler
 * @see AssemblerFactory
 * @see AssemblerFilterStrategy
 * @see PipelineTimer
 * @see PipelineOptions
 */
public final class AssemblerPipeline {

    /** Warning appended to dry-run results. */
    public static final String DRY_RUN_WARNING =
            "Dry run -- no files written";

    private final List<AssemblerDescriptor> descriptors;

    /**
     * Creates a pipeline with the given assembler
     * descriptors.
     *
     * @param descriptors the ordered list of assembler
     *                    descriptors
     */
    public AssemblerPipeline(
            List<AssemblerDescriptor> descriptors) {
        this.descriptors = List.copyOf(descriptors);
    }

    /**
     * Delegates to {@link AssemblerFactory#buildAssemblers}.
     *
     * @return immutable ordered list of assembler descriptors
     */
    public static List<AssemblerDescriptor>
            buildAssemblers() {
        return AssemblerFactory.buildAssemblers();
    }

    /**
     * Executes assemblers sequentially, aggregating files
     * and warnings. Retained as a static entry point for
     * call-sites that do not hold a pipeline instance.
     *
     * @param descriptors the ordered assembler descriptors
     * @param config      the project configuration
     * @param outputDir   the base output directory
     * @param engine      the template engine
     * @return aggregated files and warnings
     * @throws PipelineException if any assembler fails
     */
    public static AssemblerResult executeAssemblers(
            List<AssemblerDescriptor> descriptors,
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine) {
        return new AssemblerFilterStrategy(
                        config, PipelineOptions.defaults())
                .executeAll(descriptors, outputDir, engine);
    }

    /**
     * Runs the full pipeline with dry-run or real output.
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
        TemplateEngine engine = createEngine(options);
        AssemblerFilterStrategy strategy =
                new AssemblerFilterStrategy(
                        config, options);
        PipelineTimer timer = new PipelineTimer();
        long start = timer.start();
        AssemblerResult result = options.dryRun()
                ? strategy.runDry(descriptors, engine)
                : strategy.runReal(
                        descriptors, outputDir, engine);
        long durationMs = timer.stop(start);

        return new PipelineResult(
                true,
                outputDir.toString(),
                result.files(),
                result.warnings(),
                durationMs);
    }

    /**
     * Executes assemblers individually and returns a map of
     * assembler name to its result.
     *
     * @param config    the project configuration
     * @param outputDir the base output directory
     * @param options   the pipeline execution options
     * @return ordered map of assembler name to result
     * @throws PipelineException if any assembler fails
     */
    public Map<String, AssemblerResult>
            runPipelinePerAssembler(
                    ProjectConfig config,
                    Path outputDir,
                    PipelineOptions options) {
        TemplateEngine engine = createEngine(options);
        AssemblerFilterStrategy strategy =
                new AssemblerFilterStrategy(
                        config, options);
        Path baseDir = options.dryRun()
                ? AssemblerFilterStrategy.createTempDir()
                : outputDir;
        Map<String, AssemblerResult> results =
                new LinkedHashMap<>();

        try {
            for (AssemblerDescriptor desc : descriptors) {
                if (!strategy.shouldRun(desc)) {
                    continue;
                }
                results.put(desc.name(),
                        strategy.executeAndRelativize(
                                desc, baseDir, engine));
            }
        } finally {
            if (options.dryRun()) {
                CopyHelpers.deleteQuietly(baseDir);
            }
        }
        return results;
    }

    static List<String> relativizePaths(
            List<String> paths, Path baseDir) {
        return AssemblerFilterStrategy.relativizePaths(
                paths, baseDir);
    }

    private TemplateEngine createEngine(
            PipelineOptions options) {
        if (options.resourcesDir() != null) {
            return new TemplateEngine(
                    options.resourcesDir());
        }
        return new TemplateEngine();
    }
}
