package dev.iadev.cli;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.AssemblerResult;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs the assembler pipeline in verbose mode, printing
 * progress for each assembler before and after execution.
 *
 * <p>Shows platform filter header with INCLUDED/SKIPPED
 * assemblers per RULE-008.</p>
 *
 * @see GenerateCommand
 * @see AssemblerPipeline
 * @see PlatformVerboseFormatter
 */
final class VerbosePipelineRunner {

    private VerbosePipelineRunner() {
        // utility class
    }

    /**
     * Runs the pipeline in verbose mode with platform
     * filter awareness.
     *
     * @param config     the project configuration
     * @param destPath   the output directory
     * @param options    pipeline options
     * @param assemblers the filtered assembler descriptors
     * @param all        the complete assembler descriptors
     * @param out        the print writer for output
     * @return the pipeline result
     */
    static PipelineResult runVerbose(
            ProjectConfig config,
            Path destPath,
            PipelineOptions options,
            List<AssemblerDescriptor> assemblers,
            List<AssemblerDescriptor> all,
            PrintWriter out) {
        printFilterHeader(options.platforms(),
                assemblers, all, out);
        printIncludedAndSkipped(assemblers, all, out);

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
            String dryRunWarning =
                    PlatformVerboseFormatter
                            .formatDryRunWarning(
                                    options.platforms(),
                                    assemblers.size());
            allWarnings.add(dryRunWarning);
        }

        return new PipelineResult(
                true,
                destPath.toString(),
                allFiles,
                allWarnings,
                durationMs);
    }

    private static void printFilterHeader(
            Set<Platform> platforms,
            List<AssemblerDescriptor> filtered,
            List<AssemblerDescriptor> all,
            PrintWriter out) {
        out.println(
                PlatformVerboseFormatter
                        .formatFilterHeader(
                                platforms, filtered, all));
    }

    private static void printIncludedAndSkipped(
            List<AssemblerDescriptor> filtered,
            List<AssemblerDescriptor> all,
            PrintWriter out) {
        for (AssemblerDescriptor desc : filtered) {
            out.println(
                    PlatformVerboseFormatter
                            .formatIncluded(desc));
        }
        List<AssemblerDescriptor> skipped =
                PlatformVerboseFormatter
                        .computeSkipped(filtered, all);
        for (AssemblerDescriptor desc : skipped) {
            out.println(
                    PlatformVerboseFormatter
                            .formatSkipped(desc));
        }
    }
}
