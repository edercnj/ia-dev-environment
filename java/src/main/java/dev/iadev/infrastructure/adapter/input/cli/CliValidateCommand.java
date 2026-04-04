package dev.iadev.infrastructure.adapter.input.cli;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ValidationResult;
import dev.iadev.domain.port.input.ValidateConfigUseCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * CLI Input Adapter for configuration validation.
 *
 * <p>Wraps the Picocli command to delegate validation logic
 * to the {@link ValidateConfigUseCase} Input Port. This adapter
 * never accesses domain services directly (RULE-003).</p>
 *
 * <p>Responsibilities:</p>
 * <ol>
 *   <li>Parse CLI arguments via Picocli annotations</li>
 *   <li>Check file existence (adapter concern)</li>
 *   <li>Load and parse YAML configuration</li>
 *   <li>Invoke validation via Input Port</li>
 *   <li>Format results for CLI output</li>
 * </ol>
 *
 * @see ValidateConfigUseCase
 */
@Command(
        name = "validate",
        mixinStandardHelpOptions = true,
        description = "Validate a YAML configuration "
                + "file via Input Port."
)
public class CliValidateCommand implements Callable<Integer> {

    /** Successful validation exit code. */
    static final int EXIT_SUCCESS = 0;

    /** Validation failure or I/O error exit code. */
    static final int EXIT_FAILURE = 1;

    private final ValidateConfigUseCase validateUseCase;

    @Option(
            names = {"-c", "--config"},
            description = "Path to the YAML "
                    + "configuration file.",
            required = true
    )
    String configPath;

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output."
    )
    boolean verbose;

    @Spec
    CommandSpec spec;

    /**
     * Creates a new CliValidateCommand.
     *
     * @param validateUseCase the validation use case
     *                        (must not be null)
     * @throws NullPointerException if validateUseCase is null
     */
    public CliValidateCommand(
            ValidateConfigUseCase validateUseCase) {
        this.validateUseCase = Objects.requireNonNull(
                validateUseCase,
                "validateUseCase must not be null");
    }

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();

        if (!fileExists(configPath)) {
            out.println(
                    "Error: Configuration file not found: %s"
                            .formatted(configPath));
            return EXIT_FAILURE;
        }

        return loadAndValidate(out);
    }

    private int loadAndValidate(PrintWriter out) {
        try {
            ProjectConfig config =
                    dev.iadev.config.ConfigLoader
                            .loadConfig(configPath);
            return invokeValidation(config, out);
        } catch (Exception e) {
            out.println("Error: %s"
                    .formatted(e.getMessage()));
            return EXIT_FAILURE;
        }
    }

    private int invokeValidation(
            ProjectConfig config, PrintWriter out) {
        ValidationResult result =
                validateUseCase.validate(config);

        if (result.valid()) {
            if (verbose) {
                out.println(
                        "[PASS] All validations passed");
            }
            out.println("Configuration is valid");
            return EXIT_SUCCESS;
        }

        return handleValidationFailure(result, out);
    }

    private int handleValidationFailure(
            ValidationResult result, PrintWriter out) {
        out.println("Validation failed:");
        for (String error : result.errors()) {
            out.println("- %s".formatted(error));
        }
        return EXIT_FAILURE;
    }

    private boolean fileExists(String path) {
        return Files.exists(Path.of(path));
    }
}
