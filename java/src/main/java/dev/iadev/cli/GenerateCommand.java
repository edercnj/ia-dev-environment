package dev.iadev.cli;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Picocli subcommand for generating AI development environment boilerplate.
 *
 * <p>Generates {@code .claude/}, {@code .github/}, {@code .codex/}, and
 * {@code .agents/} directories based on a YAML configuration file or
 * interactive prompts.
 *
 * <p>The {@code --config} and {@code --interactive} options are mutually
 * exclusive: only one may be specified at a time.
 *
 * <p>Usage examples:
 * <pre>{@code
 * ia-dev-env generate -c config.yaml
 * ia-dev-env generate -c config.yaml -o output/ --dry-run
 * ia-dev-env generate -i
 * }</pre>
 */
@Command(
        name = "generate",
        mixinStandardHelpOptions = true,
        description = "Generate AI dev environment boilerplate "
                + "from a YAML configuration or interactive prompts."
)
public class GenerateCommand implements Callable<Integer> {

    /**
     * Mutually exclusive group for config source selection.
     *
     * <p>Either {@code --config} or {@code --interactive} may be specified,
     * but not both. Both are optional — if neither is provided, the command
     * runs as a no-op placeholder.
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
                description = "Path to the YAML configuration file."
        )
        String configPath;

        /** Enable interactive mode for guided configuration. */
        @Option(
                names = {"-i", "--interactive"},
                description = "Enable interactive mode."
        )
        boolean interactive;
    }

    /** Output directory for generated files (defaults to current dir). */
    @Option(
            names = {"-o", "--output"},
            description = "Output directory (default: current directory).",
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

    /**
     * Executes the generate command.
     *
     * <p>Currently a placeholder that returns exit code 0.
     * Full implementation will be added in story-0006-0027.
     *
     * @return exit code (0 = success, 1 = usage error, 2 = execution error)
     */
    @Override
    public Integer call() {
        return 0;
    }
}
