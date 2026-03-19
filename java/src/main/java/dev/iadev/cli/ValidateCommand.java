package dev.iadev.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Picocli subcommand for validating a YAML configuration file.
 *
 * <p>Checks that the configuration file is well-formed, contains all
 * required sections, and satisfies cross-field validation rules.
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

    /**
     * Executes the validate command.
     *
     * <p>Currently a placeholder that returns exit code 0.
     * Full implementation will be added in story-0006-0022.
     *
     * @return exit code (0 = success, 1 = usage error, 2 = execution error)
     */
    @Override
    public Integer call() {
        return 0;
    }
}
