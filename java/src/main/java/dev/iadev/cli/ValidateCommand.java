package dev.iadev.cli;

import dev.iadev.config.ConfigLoader;
import dev.iadev.domain.stack.StackValidator;
import dev.iadev.exception.ConfigParseException;
import dev.iadev.exception.ConfigValidationException;
import dev.iadev.model.ProjectConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Picocli subcommand for validating a YAML configuration file.
 *
 * <p>Checks that the configuration file is well-formed, contains all
 * required sections, and satisfies cross-field validation rules
 * (language-framework compatibility, version requirements,
 * architecture style, interface types).
 *
 * <p>Returns exit code 0 for valid configuration, 1 for invalid
 * configuration or I/O errors. Never prints stack traces unless
 * verbose mode is enabled.
 *
 * <p>Usage examples:
 * <pre>{@code
 * ia-dev-env validate -c config.yaml
 * ia-dev-env validate -c config.yaml --verbose
 * }</pre>
 */
@Command(
        name = "validate",
        mixinStandardHelpOptions = true,
        description = "Validate a YAML configuration file."
)
public class ValidateCommand implements Callable<Integer> {

    /** Successful validation exit code. */
    static final int EXIT_SUCCESS = 0;

    /** Validation failure or I/O error exit code. */
    static final int EXIT_FAILURE = 1;

    /** Path to the YAML configuration file (required). */
    @Option(
            names = {"-c", "--config"},
            description = "Path to the YAML configuration file.",
            required = true
    )
    String configPath;

    /** Enable verbose logging output. */
    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output."
    )
    boolean verbose;

    @Spec
    CommandSpec spec;

    /**
     * Executes the validate command.
     *
     * <p>Flow: check file exists, load YAML via ConfigLoader,
     * run StackValidator validations, report results.
     *
     * @return exit code (0 = valid, 1 = invalid or error)
     */
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
        ProjectConfig config;
        try {
            config = ConfigLoader.loadConfig(configPath);
        } catch (ConfigParseException e) {
            out.println("Error: Invalid YAML: %s"
                    .formatted(e.getMessage()));
            return EXIT_FAILURE;
        } catch (ConfigValidationException e) {
            return handleValidationException(e, out);
        } catch (Exception e) {
            out.println("Error: An unexpected error "
                    + "occurred during validation");
            return EXIT_FAILURE;
        }
        return runValidations(config, out);
    }

    private int handleValidationException(
            ConfigValidationException e, PrintWriter out) {
        List<String> missing = e.getMissingSections();
        if (!missing.isEmpty()) {
            List<String> errors = new ArrayList<>();
            for (String section : missing) {
                errors.add(
                        "Missing required section: %s"
                                .formatted(section));
            }
            if (verbose) {
                out.println(
                        "[FAIL] Mandatory sections present: %s"
                                .formatted(
                                        String.join(", ",
                                                missing)));
            }
            printValidationFailed(errors, out);
            return EXIT_FAILURE;
        }
        out.println("Error: %s".formatted(e.getMessage()));
        return EXIT_FAILURE;
    }

    private int runValidations(
            ProjectConfig config, PrintWriter out) {
        List<String> allErrors =
                collectAllErrors(config, out);

        if (allErrors.isEmpty()) {
            out.println("Configuration is valid");
            return EXIT_SUCCESS;
        }

        printValidationFailed(allErrors, out);
        return EXIT_FAILURE;
    }

    private List<String> collectAllErrors(
            ProjectConfig config, PrintWriter out) {
        List<String> allErrors = new ArrayList<>();
        allErrors.addAll(runCategory(
                "Mandatory sections present",
                c -> List.of(), config, out));
        allErrors.addAll(runCategory(
                "Language-framework compatibility",
                StackValidator::validateLanguageFramework,
                config, out));
        allErrors.addAll(runCategory(
                "Version requirements",
                StackValidator::validateVersionRequirements,
                config, out));
        allErrors.addAll(runCategory(
                "Architecture style",
                StackValidator::validateArchitectureStyle,
                config, out));
        allErrors.addAll(runCategory(
                "Interface types",
                StackValidator::validateInterfaceTypes,
                config, out));
        return allErrors;
    }

    private List<String> runCategory(
            String name,
            Function<ProjectConfig, List<String>> validator,
            ProjectConfig config,
            PrintWriter out) {
        List<String> errors = validator.apply(config);
        if (verbose) {
            if (errors.isEmpty()) {
                out.println("[PASS] %s".formatted(name));
            } else {
                out.println("[FAIL] %s: %s".formatted(
                        name, String.join("; ", errors)));
            }
        }
        return errors;
    }

    private void printValidationFailed(
            List<String> errors, PrintWriter out) {
        out.println("Validation failed:");
        for (String error : errors) {
            out.println("- %s".formatted(error));
        }
    }

    private boolean fileExists(String path) {
        return Files.exists(Path.of(path));
    }
}
