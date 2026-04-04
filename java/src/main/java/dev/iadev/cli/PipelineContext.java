package dev.iadev.cli;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.domain.model.ProjectConfig;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Immutable parameter object for verbose pipeline execution.
 *
 * <p>Groups the five parameters needed by
 * {@link GenerateCommand#runVerbosePipeline} into a single
 * cohesive record, reducing parameter count from 5 to 1.</p>
 *
 * @param config     the project configuration
 * @param destPath   the output directory path
 * @param options    the pipeline options
 * @param assemblers the list of assembler descriptors
 * @param out        the output writer for verbose messages
 */
public record PipelineContext(
        ProjectConfig config,
        Path destPath,
        PipelineOptions options,
        List<AssemblerDescriptor> assemblers,
        PrintWriter out) {

    /**
     * Validates that required fields are not null.
     */
    public PipelineContext {
        Objects.requireNonNull(config,
                "config must not be null");
        Objects.requireNonNull(destPath,
                "destPath must not be null");
        Objects.requireNonNull(options,
                "options must not be null");
        Objects.requireNonNull(assemblers,
                "assemblers must not be null");
        Objects.requireNonNull(out,
                "out must not be null");
        assemblers = List.copyOf(assemblers);
    }
}
