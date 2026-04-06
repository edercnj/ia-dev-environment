package dev.iadev.application.assembler;

import dev.iadev.exception.PipelineException;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the execution of 33 assemblers in the fixed
 * order defined by RULE-005.
 *
 * <p>Assembler construction is delegated to
 * {@link AssemblerFactory}.</p>
 *
 * @see Assembler
 * @see AssemblerFactory
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
     * and warnings.
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
        List<String> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (AssemblerDescriptor desc : descriptors) {
            executeSingleAssembler(
                    desc, config, outputDir, engine,
                    files, warnings);
        }

        return AssemblerResult.of(files, warnings);
    }

    private static void executeSingleAssembler(
            AssemblerDescriptor desc,
            ProjectConfig config,
            Path outputDir, TemplateEngine engine,
            List<String> files,
            List<String> warnings) {
        try {
            Path targetDir =
                    desc.target().resolve(outputDir);
            AssemblerResult result =
                    desc.assembler().assembleWithResult(
                            config, engine, targetDir);
            files.addAll(result.files());
            for (String w : result.warnings()) {
                warnings.add("[WARN] %s: %s"
                        .formatted(desc.name(), w));
            }
        } catch (PipelineException pe) {
            throw pe;
        } catch (Exception e) {
            throw new PipelineException(
                    "Pipeline failed at %s: %s"
                            .formatted(desc.name(),
                                    e.getMessage()),
                    desc.name(), e);
        }
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
        long start = System.nanoTime();

        TemplateEngine engine = createEngine(options);

        AssemblerResult result;
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
        Path baseDir = options.dryRun()
                ? createTempDir() : outputDir;
        Map<String, AssemblerResult> results =
                new LinkedHashMap<>();

        try {
            for (AssemblerDescriptor desc : descriptors) {
                Path targetDir =
                        desc.target().resolve(baseDir);
                AssemblerResult result =
                        desc.assembler()
                                .assembleWithResult(
                                        config, engine,
                                        targetDir);
                results.put(desc.name(),
                        relativizeResult(
                                result, baseDir));
            }
        } finally {
            if (options.dryRun()) {
                CopyHelpers.deleteQuietly(baseDir);
            }
        }

        return results;
    }

    private static AssemblerResult relativizeResult(
            AssemblerResult result, Path baseDir) {
        List<String> relative = relativizePaths(
                result.files(), baseDir);
        return AssemblerResult.of(
                relative, result.warnings());
    }

    static List<String> relativizePaths(
            List<String> paths, Path baseDir) {
        return paths.stream()
                .map(p -> {
                    Path filePath = Path.of(p);
                    if (filePath.isAbsolute()
                            && filePath.startsWith(
                                    baseDir)) {
                        return baseDir.relativize(
                                filePath).toString();
                    }
                    return p;
                })
                .toList();
    }

    private AssemblerResult runDry(
            ProjectConfig config,
            TemplateEngine engine) {
        Path tempDir = createTempDir();

        try {
            AssemblerResult result = executeAssemblers(
                    descriptors, config, tempDir, engine);
            List<String> relativePaths =
                    relativizePaths(
                            result.files(), tempDir);
            List<String> warnings =
                    new ArrayList<>(result.warnings());
            warnings.add(DRY_RUN_WARNING);
            return AssemblerResult.of(
                    relativePaths, warnings);
        } finally {
            CopyHelpers.deleteQuietly(tempDir);
        }
    }

    private AssemblerResult runReal(
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine) {
        AssemblerResult result = executeAssemblers(
                descriptors, config, outputDir, engine);
        List<String> relativePaths =
                relativizePaths(
                        result.files(), outputDir);
        return AssemblerResult.of(
                relativePaths, result.warnings());
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory(
                    "ia-dev-env-dry-");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to create temp directory", e);
        }
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
