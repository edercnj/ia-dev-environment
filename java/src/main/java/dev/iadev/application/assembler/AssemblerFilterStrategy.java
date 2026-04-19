package dev.iadev.application.assembler;

import dev.iadev.exception.PipelineException;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Decides which assemblers run and executes them
 * sequentially against a per-descriptor target directory.
 *
 * <p>Owns the per-descriptor selection and execution
 * responsibility previously handled inline by {@link
 * AssemblerPipeline}, together with the dry-run / real
 * output shaping (temp-dir allocation, path
 * relativization). The strategy accepts a {@link
 * ProjectConfig} and {@link PipelineOptions} as
 * collaborators so future feature flags (e.g., disable a
 * given assembler based on stack) can be added here
 * without touching the pipeline orchestrator.</p>
 *
 * <p>Current behaviour: {@link
 * #shouldRun(AssemblerDescriptor)} returns {@code true}
 * unconditionally — every descriptor passed to the
 * pipeline runs. The method exists as the seam for future
 * filters.</p>
 *
 * @see AssemblerPipeline
 * @see PipelineOptions
 */
public final class AssemblerFilterStrategy {

    private final ProjectConfig config;
    private final PipelineOptions options;

    /**
     * Creates a strategy bound to a project configuration
     * and pipeline options.
     *
     * @param config  the project configuration (never null)
     * @param options the pipeline execution options
     *                (never null)
     */
    public AssemblerFilterStrategy(
            ProjectConfig config,
            PipelineOptions options) {
        this.config = Objects.requireNonNull(
                config, "config");
        this.options = Objects.requireNonNull(
                options, "options");
    }

    /**
     * Decides whether the given assembler descriptor
     * should run in the current pipeline context.
     *
     * @param descriptor the assembler descriptor (never
     *                   null)
     * @return {@code true} if the descriptor should run
     */
    public boolean shouldRun(
            AssemblerDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        return true;
    }

    /**
     * Runs every descriptor that passes {@link
     * #shouldRun(AssemblerDescriptor)} sequentially,
     * aggregating files and warnings against the given
     * output directory.
     *
     * @param descriptors the ordered assembler descriptors
     * @param outputDir   the base output directory
     * @param engine      the template engine
     * @return aggregated files and warnings
     * @throws PipelineException if any assembler fails
     */
    public AssemblerResult executeAll(
            List<AssemblerDescriptor> descriptors,
            Path outputDir,
            TemplateEngine engine) {
        List<String> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (AssemblerDescriptor desc : descriptors) {
            if (!shouldRun(desc)) {
                continue;
            }
            executeOne(
                    desc, outputDir, engine,
                    files, warnings);
        }
        return AssemblerResult.of(files, warnings);
    }

    /**
     * Executes one descriptor against the given base
     * directory and returns its result with paths
     * relativised to {@code baseDir}.
     *
     * @param desc     the descriptor to execute
     * @param baseDir  the base output directory
     * @param engine   the template engine
     * @return the descriptor's result with relativised
     *         paths
     */
    AssemblerResult executeAndRelativize(
            AssemblerDescriptor desc,
            Path baseDir,
            TemplateEngine engine) {
        Path targetDir = desc.target().resolve(baseDir);
        AssemblerResult result =
                desc.assembler().assembleWithResult(
                        config, engine, targetDir);
        return AssemblerResult.of(
                relativizePaths(result.files(), baseDir),
                result.warnings());
    }

    /**
     * Runs the configured descriptors in dry-run mode,
     * cleaning up the temporary directory afterwards.
     *
     * @param descriptors the ordered assembler descriptors
     * @param engine      the template engine
     * @return dry-run result (paths relativised +
     *         DRY_RUN_WARNING appended)
     */
    AssemblerResult runDry(
            List<AssemblerDescriptor> descriptors,
            TemplateEngine engine) {
        Path tempDir = createTempDir();
        try {
            AssemblerResult result = executeAll(
                    descriptors, tempDir, engine);
            List<String> warnings =
                    new ArrayList<>(result.warnings());
            warnings.add(AssemblerPipeline.DRY_RUN_WARNING);
            return AssemblerResult.of(
                    relativizePaths(
                            result.files(), tempDir),
                    warnings);
        } finally {
            CopyHelpers.deleteQuietly(tempDir);
        }
    }

    /**
     * Runs the configured descriptors against the real
     * output directory, relativising the reported paths.
     *
     * @param descriptors the ordered assembler descriptors
     * @param outputDir   the real output directory
     * @param engine      the template engine
     * @return real-run result with paths relativised
     */
    AssemblerResult runReal(
            List<AssemblerDescriptor> descriptors,
            Path outputDir,
            TemplateEngine engine) {
        AssemblerResult result = executeAll(
                descriptors, outputDir, engine);
        return AssemblerResult.of(
                relativizePaths(
                        result.files(), outputDir),
                result.warnings());
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

    static Path createTempDir() {
        try {
            return Files.createTempDirectory(
                    "ia-dev-env-dry-");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to create temp directory", e);
        }
    }

    private void executeOne(
            AssemblerDescriptor desc,
            Path outputDir,
            TemplateEngine engine,
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
     * @return the bound project configuration
     */
    public ProjectConfig config() {
        return config;
    }

    /**
     * @return the bound pipeline options
     */
    public PipelineOptions options() {
        return options;
    }
}
