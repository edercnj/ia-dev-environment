package dev.iadev.cli;

import dev.iadev.assembler.AssemblerDescriptor;
import dev.iadev.assembler.AssemblerPipeline;
import dev.iadev.assembler.PipelineOptions;
import dev.iadev.config.ConfigLoader;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.stack.StackValidator;
import dev.iadev.exception.CliException;
import dev.iadev.exception.ConfigParseException;
import dev.iadev.exception.ConfigValidationException;
import dev.iadev.exception.PipelineException;
import dev.iadev.model.PipelineResult;
import dev.iadev.model.ProjectConfig;
import dev.iadev.util.OverwriteDetector;
import dev.iadev.util.PathUtils;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Picocli subcommand for generating AI development environment
 * boilerplate.
 *
 * <p>Generates {@code .claude/}, {@code .github/}, {@code .codex/},
 * and {@code .agents/} directories based on a YAML configuration
 * file, interactive prompts, or a bundled stack profile.
 *
 * <p>The {@code --config} and {@code --interactive} options are
 * mutually exclusive: only one may be specified at a time.
 * The {@code --stack} option provides an alternative shorthand
 * for bundled profiles.
 *
 * <p>Execution flow:
 * <ol>
 *   <li>Parse options</li>
 *   <li>Load config (--config, --interactive, or --stack)</li>
 *   <li>Validate config via StackValidator</li>
 *   <li>Reject dangerous output paths (RULE-011)</li>
 *   <li>Check overwrite conflicts (RULE-012)</li>
 *   <li>Run pipeline with 23 assemblers (RULE-005)</li>
 *   <li>Display results via CliDisplay</li>
 * </ol>
 *
 * <p>Usage examples:
 * <pre>{@code
 * ia-dev-env generate -c config.yaml
 * ia-dev-env generate -c config.yaml -o output/ --dry-run
 * ia-dev-env generate -s java-quarkus -o output/
 * ia-dev-env generate -i
 * }</pre>
 */
@Command(
        name = "generate",
        mixinStandardHelpOptions = true,
        description = "Generate AI dev environment boilerplate "
                + "from a YAML configuration or interactive "
                + "prompts."
)
public class GenerateCommand implements Callable<Integer> {

    /** Exit code for successful generation. */
    static final int EXIT_SUCCESS = 0;

    /** Exit code for validation/usage errors. */
    static final int EXIT_VALIDATION = 1;

    /** Exit code for execution errors. */
    static final int EXIT_EXECUTION = 2;

    /**
     * Mutually exclusive group for config source selection.
     *
     * <p>Either {@code --config} or {@code --interactive} may be
     * specified, but not both. Both are optional.
     */
    @ArgGroup(exclusive = true, multiplicity = "0..1")
    ConfigSource configSource;

    /**
     * Holds the mutually exclusive config/interactive options.
     */
    static class ConfigSource {

        /** Path to the YAML configuration file. */
        @Option(
                names = {"-c", "--config"},
                description = "Path to the YAML configuration "
                        + "file."
        )
        String configPath;

        /** Enable interactive mode for guided configuration. */
        @Option(
                names = {"-i", "--interactive"},
                description = "Enable interactive mode."
        )
        boolean interactive;
    }

    /** Output directory for generated files. */
    @Option(
            names = {"-o", "--output"},
            description = "Output directory "
                    + "(default: current directory).",
            defaultValue = "."
    )
    String outputDir;

    /** Name of a bundled stack profile to use. */
    @Option(
            names = {"-s", "--stack"},
            description = "Bundled stack profile name."
    )
    String stack;

    /** Enable verbose logging output. */
    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output."
    )
    boolean verbose;

    /** Simulate generation without writing files. */
    @Option(
            names = {"--dry-run"},
            description = "Simulate without writing files."
    )
    boolean dryRun;

    /** Overwrite existing files without prompting. */
    @Option(
            names = {"-f", "--force"},
            description = "Overwrite existing files."
    )
    boolean force;

    @Spec
    CommandSpec spec;

    /**
     * Executes the generate command end-to-end.
     *
     * <p>Flow: loadConfig, validate, rejectDangerousPaths,
     * checkOverwrite, runPipeline, displayResult.
     *
     * @return exit code (0 = success, 1 = validation,
     *         2 = execution)
     */
    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        try {
            return executeGeneration(out);
        } catch (CliException e) {
            out.println("Error: %s".formatted(
                    e.getMessage()));
            return e.getErrorCode();
        } catch (ConfigParseException
                 | ConfigValidationException e) {
            out.println("Error: %s".formatted(
                    e.getMessage()));
            return EXIT_VALIDATION;
        } catch (PipelineException e) {
            out.println("Error: %s".formatted(
                    e.getMessage()));
            return EXIT_EXECUTION;
        } catch (Exception e) {
            out.println("Error: An unexpected error "
                    + "occurred during generation");
            return EXIT_EXECUTION;
        }
    }

    private int executeGeneration(PrintWriter out) {
        Optional<ProjectConfig> configOpt =
                loadConfig(out);
        if (configOpt.isEmpty()) {
            return EXIT_VALIDATION;
        }
        ProjectConfig config = configOpt.orElseThrow();
        int validationResult =
                validateConfig(config, out);
        if (validationResult != EXIT_SUCCESS) {
            return validationResult;
        }
        Path destPath =
                PathUtils.normalizeDirectory(outputDir);
        PathUtils.rejectDangerousPath(destPath);
        int overwriteResult =
                checkOverwrite(destPath, out);
        if (overwriteResult != EXIT_SUCCESS) {
            return overwriteResult;
        }
        PipelineResult result =
                runPipeline(config, destPath, out);
        out.println(CliDisplay.formatResult(
                result, DisplayMode.of(dryRun)));
        return EXIT_SUCCESS;
    }

    private int validateConfig(
            ProjectConfig config, PrintWriter out) {
        List<String> errors =
                StackValidator.validateStack(config);
        if (!errors.isEmpty()) {
            printValidationErrors(errors, out);
            return EXIT_VALIDATION;
        }
        return EXIT_SUCCESS;
    }

    private int checkOverwrite(
            Path destPath, PrintWriter out) {
        if (!dryRun && !force) {
            List<String> conflicts =
                    OverwriteDetector
                            .checkExistingArtifacts(destPath);
            if (!conflicts.isEmpty()) {
                out.println(OverwriteDetector
                        .formatConflictMessage(conflicts));
                return EXIT_VALIDATION;
            }
        }
        if (force && !dryRun) {
            out.println(
                    "Overwriting existing artifacts in "
                            + destPath);
        }
        return EXIT_SUCCESS;
    }

    private Optional<ProjectConfig> loadConfig(
            PrintWriter out) {
        if (stack != null && !stack.isEmpty()) {
            logVerbose(out,
                    "Loading bundled stack profile: "
                            + stack);
            return Optional.of(
                    ConfigProfiles.getStack(stack));
        }
        if (configSource == null) {
            out.println(
                    "Either --config, --interactive, "
                            + "or --stack is required.");
            return Optional.empty();
        }
        if (configSource.interactive) {
            return Optional.of(loadInteractive(out));
        }
        if (configSource.configPath != null) {
            logVerbose(out,
                    "Loading config: "
                            + configSource.configPath);
            return Optional.of(
                    ConfigLoader.loadConfig(
                            configSource.configPath));
        }
        out.println(
                "Either --config, --interactive, "
                        + "or --stack is required.");
        return Optional.empty();
    }

    private ProjectConfig loadInteractive(
            PrintWriter out) {
        logVerbose(out, "Starting interactive mode...");
        try {
            InteractivePrompter prompter =
                    new InteractivePrompter(
                            new JLineTerminalProvider());
            return prompter.prompt();
        } catch (java.io.IOException e) {
            throw new CliException(
                    "Failed to initialize terminal: "
                            + e.getMessage(),
                    EXIT_EXECUTION);
        }
    }

    private void logVerbose(
            PrintWriter out, String message) {
        if (verbose) {
            out.println(message);
        }
    }

    private PipelineResult runPipeline(
            ProjectConfig config,
            Path destPath,
            PrintWriter out) {
        PipelineOptions options = new PipelineOptions(
                dryRun, force, verbose, null);

        List<AssemblerDescriptor> assemblers =
                AssemblerPipeline.buildAssemblers();

        if (verbose) {
            PipelineContext ctx = new PipelineContext(
                    config, destPath, options,
                    assemblers, out);
            return runVerbosePipeline(ctx);
        }

        AssemblerPipeline pipeline =
                new AssemblerPipeline(assemblers);
        return pipeline.runPipeline(
                config, destPath, options);
    }

    private PipelineResult runVerbosePipeline(
            PipelineContext ctx) {
        for (AssemblerDescriptor desc
                : ctx.assemblers()) {
            ctx.out().println("Running %s...".formatted(
                    desc.name()));
        }

        AssemblerPipeline pipeline =
                new AssemblerPipeline(ctx.assemblers());
        PipelineResult result =
                pipeline.runPipeline(
                        ctx.config(), ctx.destPath(),
                        ctx.options());

        int assemblerCount = ctx.assemblers().size();
        long avgDurationPerAssembler =
                assemblerCount > 0
                        ? result.durationMs()
                        / assemblerCount : 0;
        for (AssemblerDescriptor desc
                : ctx.assemblers()) {
            ctx.out().println(
                    CliDisplay.formatAssemblerVerbose(
                            desc.name(),
                            0,
                            avgDurationPerAssembler));
        }

        return result;
    }

    private void printValidationErrors(
            List<String> errors, PrintWriter out) {
        out.println("Validation failed:");
        for (String error : errors) {
            out.println("- %s".formatted(error));
        }
    }
}
