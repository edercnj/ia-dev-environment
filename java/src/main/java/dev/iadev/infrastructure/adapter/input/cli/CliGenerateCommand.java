package dev.iadev.infrastructure.adapter.input.cli;

import dev.iadev.domain.model.GenerationContext;
import dev.iadev.domain.model.GenerationResult;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ValidationResult;
import dev.iadev.domain.port.input.GenerateEnvironmentUseCase;
import dev.iadev.domain.port.input.ValidateConfigUseCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * CLI Input Adapter for environment generation.
 *
 * <p>Wraps the Picocli command to delegate all business logic
 * to Input Ports ({@link GenerateEnvironmentUseCase} and
 * {@link ValidateConfigUseCase}). This adapter never accesses
 * domain services or engines directly (RULE-003).</p>
 *
 * <p>Responsibilities:</p>
 * <ol>
 *   <li>Parse CLI arguments via Picocli annotations</li>
 *   <li>Build domain objects from CLI inputs</li>
 *   <li>Invoke use cases via Input Ports</li>
 *   <li>Format results for CLI output</li>
 * </ol>
 *
 * @see GenerateEnvironmentUseCase
 * @see ValidateConfigUseCase
 */
@Command(
        name = "generate",
        mixinStandardHelpOptions = true,
        description = "Generate AI dev environment "
                + "boilerplate via Input Port."
)
public class CliGenerateCommand implements Callable<Integer> {

    /** Exit code for successful generation. */
    static final int EXIT_SUCCESS = 0;

    /** Exit code for validation/usage errors. */
    static final int EXIT_VALIDATION = 1;

    /** Exit code for execution errors. */
    static final int EXIT_EXECUTION = 2;

    private final GenerateEnvironmentUseCase generateUseCase;
    private final ValidateConfigUseCase validateUseCase;

    @Option(names = {"-c", "--config"},
            description = "Path to the YAML config file.",
            required = true)
    String configPath;

    @Option(names = {"-o", "--output"},
            description = "Output directory.",
            defaultValue = ".")
    String outputDir;

    @Option(names = {"-v", "--verbose"},
            description = "Enable verbose output.")
    boolean verbose;

    @Spec
    CommandSpec spec;

    /**
     * Creates a new CliGenerateCommand.
     *
     * @param generateUseCase the generation use case
     *                        (must not be null)
     * @param validateUseCase the validation use case
     *                        (must not be null)
     * @throws NullPointerException if any parameter is null
     */
    public CliGenerateCommand(
            GenerateEnvironmentUseCase generateUseCase,
            ValidateConfigUseCase validateUseCase) {
        this.generateUseCase = Objects.requireNonNull(
                generateUseCase,
                "generateUseCase must not be null");
        this.validateUseCase = Objects.requireNonNull(
                validateUseCase,
                "validateUseCase must not be null");
    }

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        try {
            return executeGeneration(out);
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
        ProjectConfig config = loadConfig();
        ValidationResult validationResult =
                validateUseCase.validate(config);

        if (!validationResult.valid()) {
            return handleValidationFailure(
                    validationResult, out);
        }

        return invokeGeneration(config, out);
    }

    private int handleValidationFailure(
            ValidationResult result, PrintWriter out) {
        out.println("Validation failed:");
        for (String error : result.errors()) {
            out.println("- %s".formatted(error));
        }
        return EXIT_VALIDATION;
    }

    private int invokeGeneration(
            ProjectConfig config, PrintWriter out) {
        Path destPath = Path.of(outputDir)
                .toAbsolutePath().normalize();
        GenerationContext context =
                new GenerationContext(
                        config, destPath, verbose);

        GenerationResult result =
                generateUseCase.generate(context);

        return formatResult(result, out);
    }

    private int formatResult(
            GenerationResult result, PrintWriter out) {
        if (result.success()) {
            int fileCount =
                    result.filesGenerated().size();
            out.println("Generation completed: "
                    + "%d files generated".formatted(
                    fileCount));
            return EXIT_SUCCESS;
        }

        out.println("Generation failed");
        for (String warning : result.warnings()) {
            out.println("Warning: %s"
                    .formatted(warning));
        }
        return EXIT_EXECUTION;
    }

    private ProjectConfig loadConfig() {
        return dev.iadev.config.ConfigLoader
                .loadConfig(configPath);
    }
}
