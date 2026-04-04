package dev.iadev.cli;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs the assembler pipeline in verbose mode, printing
 * progress for each assembler before and after execution.
 *
 * <p>Extracted from {@link GenerateCommand} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see GenerateCommand
 * @see AssemblerPipeline
 */
final class VerbosePipelineRunner {

    private VerbosePipelineRunner() {
        // utility class
    }

    /**
     * Runs the pipeline in verbose mode.
     *
     * @param config     the project configuration
     * @param destPath   the output directory
     * @param options    pipeline options
     * @param assemblers the ordered assembler descriptors
     * @param out        the print writer for output
     * @return the pipeline result
     */
    static PipelineResult runVerbose(
            ProjectConfig config,
            Path destPath,
            PipelineOptions options,
            List<AssemblerDescriptor> assemblers,
            PrintWriter out) {
        for (AssemblerDescriptor desc : assemblers) {
            out.println("Running %s...".formatted(
                    desc.name()));
        }

        AssemblerPipeline pipeline =
                new AssemblerPipeline(assemblers);
        PipelineResult result =
                pipeline.runPipeline(
                        config, destPath, options);

        int assemblerCount = assemblers.size();
        long avgDurationPerAssembler =
                assemblerCount > 0
                        ? result.durationMs()
                        / assemblerCount : 0;
        for (AssemblerDescriptor desc : assemblers) {
            out.println(
                    CliDisplay.formatAssemblerVerbose(
                            desc.name(),
                            0,
                            avgDurationPerAssembler));
        }

        return result;
    }
}
