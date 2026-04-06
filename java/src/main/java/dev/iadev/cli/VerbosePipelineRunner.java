package dev.iadev.cli;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.AssemblerResult;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the assembler pipeline in verbose mode, printing
 * progress for each assembler before and after execution.
 *
 * <p>Uses {@link AssemblerPipeline#runPipelinePerAssembler}
 * to track per-assembler file counts and durations.</p>
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

        long start = System.nanoTime();

        AssemblerPipeline pipeline =
                new AssemblerPipeline(assemblers);
        Map<String, AssemblerResult> perAssembler =
                pipeline.runPipelinePerAssembler(
                        config, destPath, options);

        long durationMs =
                (System.nanoTime() - start) / 1_000_000;

        List<String> allFiles = new ArrayList<>();
        List<String> allWarnings = new ArrayList<>();
        int assemblerCount = perAssembler.size();
        long avgDuration = assemblerCount > 0
                ? durationMs / assemblerCount : 0;

        for (Map.Entry<String, AssemblerResult> entry
                : perAssembler.entrySet()) {
            AssemblerResult result = entry.getValue();
            allFiles.addAll(result.files());
            allWarnings.addAll(result.warnings());

            out.println(
                    CliDisplay.formatAssemblerVerbose(
                            entry.getKey(),
                            result.files().size(),
                            avgDuration));
        }

        if (options.dryRun()) {
            allWarnings.add(
                    AssemblerPipeline.DRY_RUN_WARNING);
        }

        return new PipelineResult(
                true,
                destPath.toString(),
                allFiles,
                allWarnings,
                durationMs);
    }
}
