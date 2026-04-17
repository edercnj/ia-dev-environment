package dev.iadev.cli;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerFactory;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.domain.model.Platform;
import dev.iadev.domain.stack.StackValidator;
import dev.iadev.exception.CliException;
import dev.iadev.exception.ConfigParseException;
import dev.iadev.exception.PipelineException;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
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
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Picocli subcommand for generating AI development
 * environment boilerplate.
 *
 * @see ConfigSourceLoader
 * @see VerbosePipelineRunner
 */
@Command(
        name = "generate",
        mixinStandardHelpOptions = true,
        description = "Generate AI dev environment "
                + "boilerplate from a YAML configuration "
                + "or interactive prompts."
)
public class GenerateCommand implements Callable<Integer> {

    /** Exit code for successful generation. */
    static final int EXIT_SUCCESS = 0;

    /** Exit code for validation/usage errors. */
    static final int EXIT_VALIDATION = 1;

    /** Exit code for execution errors. */
    static final int EXIT_EXECUTION = 2;

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    ConfigSource configSource;

    /** Mutually exclusive config/interactive options. */
    static class ConfigSource {
        @Option(names = {"-c", "--config"},
                description = "Path to the YAML "
                        + "configuration file.")
        String configPath;

        @Option(names = {"-i", "--interactive"},
                description = "Enable interactive mode.")
        boolean interactive;
    }

    @Option(names = {"-o", "--output"},
            description = "Output directory.",
            defaultValue = ".")
    String outputDir;

    @Option(names = {"-s", "--stack"},
            description = "Bundled stack profile name.")
    String stack;

    @Option(names = {"-v", "--verbose"},
            description = "Enable verbose output.")
    boolean verbose;

    @Option(names = {"--dry-run"},
            description = "Simulate without writing files.")
    boolean dryRun;

    @Option(names = {"-f", "--force"},
            description = "Overwrite existing files.")
    boolean force;

    @Option(names = {"--overwrite-constitution"},
            description = "Regenerate CONSTITUTION.md "
                    + "even if it already exists.")
    boolean overwriteConstitution;

    @Option(names = {"-p", "--platform"},
            description = "Target AI platform(s) for "
                    + "artifact generation. Values: "
                    + "claude-code, all. "
                    + "Multiple values separated by comma. "
                    + "Default: all (generate for all "
                    + "platforms).",
            split = "\\s*,\\s*",
            converter = PlatformConverter.class)
    List<Platform> platforms;

    @Spec
    CommandSpec spec;

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
                 | dev.iadev.domain.model
                         .ConfigValidationException e) {
            out.println("Error: %s".formatted(
                    e.getMessage()));
            return EXIT_VALIDATION;
        } catch (PipelineException e) {
            out.println("Error: %s".formatted(
                    e.getMessage()));
            return EXIT_EXECUTION;
        } catch (IllegalArgumentException e) {
            out.println("Error: %s".formatted(
                    e.getMessage()));
            return EXIT_VALIDATION;
        } catch (Exception e) {
            out.println("Error: An unexpected error "
                    + "occurred during generation");
            return EXIT_EXECUTION;
        }
    }

    private int executeGeneration(PrintWriter out) {
        Optional<ProjectConfig> configOpt =
                ConfigSourceLoader.loadConfig(
                        stack,
                        configSource,
                        verbose
                                ? out::println
                                : ConfigSourceLoader
                                        .SILENT_DEBUG,
                        out);
        if (configOpt.isEmpty()) {
            return EXIT_VALIDATION;
        }
        ProjectConfig config = configOpt.orElseThrow();
        int validationResult =
                validateConfig(config, out);
        if (validationResult != EXIT_SUCCESS) {
            return validationResult;
        }
        return generateOutput(config, out);
    }

    private int validateConfig(
            ProjectConfig config, PrintWriter out) {
        List<String> errors =
                StackValidator.validateStack(config);
        if (!errors.isEmpty()) {
            out.println("Validation failed:");
            for (String error : errors) {
                out.println("- %s".formatted(error));
            }
            return EXIT_VALIDATION;
        }
        return EXIT_SUCCESS;
    }

    private int generateOutput(
            ProjectConfig config, PrintWriter out) {
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

    private int checkOverwrite(
            Path destPath, PrintWriter out) {
        if (!dryRun && !force) {
            List<String> conflicts =
                    OverwriteDetector
                            .checkExistingArtifacts(
                                    destPath);
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

    private PipelineResult runPipeline(
            ProjectConfig config,
            Path destPath,
            PrintWriter out) {
        Set<Platform> platformSet =
                PlatformPrecedenceResolver.resolve(
                        platforms, config);
        PipelineOptions options = new PipelineOptions(
                dryRun, force, verbose,
                overwriteConstitution, null,
                platformSet);
        List<AssemblerDescriptor> assemblers =
                AssemblerFactory.buildAssemblers(options);
        if (verbose) {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAllAssemblers(
                            options);
            return VerbosePipelineRunner.runVerbose(
                    config, destPath, options,
                    assemblers, all, out);
        }
        AssemblerPipeline pipeline =
                new AssemblerPipeline(assemblers);
        return pipeline.runPipeline(
                config, destPath, options);
    }
}
