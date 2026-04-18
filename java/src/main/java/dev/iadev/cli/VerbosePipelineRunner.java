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
     * @param config   the project configuration
     * @param destPath the output directory
     * @param ctx      pipeline run context (options +
     *                 filtered/all assembler descriptors)
     * @param out      the print writer for output
     * @return the pipeline result
     */
    static PipelineResult runVerbose(
            ProjectConfig config, Path destPath,
            VerboseRunContext ctx, PrintWriter out) {
        PipelineOptions options = ctx.options();
        List<AssemblerDescriptor> assemblers = ctx.assemblers();
        printFilterHeader(options.platforms(),
                assemblers, ctx.all(), out);
        printIncludedAndSkipped(assemblers, ctx.all(), out);

        long start = System.nanoTime();
        Map<String, AssemblerResult> perAssembler =
                new AssemblerPipeline(assemblers)
                        .runPipelinePerAssembler(
                                config, destPath, options);
        long durationMs =
                (System.nanoTime() - start) / 1_000_000;
        List<String> allFiles = new ArrayList<>();
        List<String> allWarnings = collectResults(
                perAssembler, durationMs, allFiles, out);
        appendDryRunWarning(options,
                assemblers.size(), allWarnings);
        return new PipelineResult(true,
                destPath.toString(),
                allFiles, allWarnings, durationMs);
    }

    private static List<String> collectResults(
            Map<String, AssemblerResult> perAssembler,
            long durationMs,
            List<String> allFiles,
            PrintWriter out) {
        List<String> allWarnings = new ArrayList<>();
        int count = perAssembler.size();
        long avg = count > 0 ? durationMs / count : 0;
        for (Map.Entry<String, AssemblerResult> entry
                : perAssembler.entrySet()) {
            AssemblerResult result = entry.getValue();
            allFiles.addAll(result.files());
            allWarnings.addAll(result.warnings());
            out.println(
                    CliDisplay.formatAssemblerVerbose(
                            entry.getKey(),
                            result.files().size(), avg));
        }
        return allWarnings;
    }

    private static void appendDryRunWarning(
            PipelineOptions options,
            int assemblersSize,
            List<String> allWarnings) {
        if (options.dryRun()) {
            allWarnings.add(
                    PlatformVerboseFormatter
                            .formatDryRunWarning(
                                    options.platforms(),
                                    assemblersSize));
        }
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
