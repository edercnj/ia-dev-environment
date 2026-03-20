package dev.iadev.cli;

import dev.iadev.config.ConfigLoader;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.exception.CliException;
import dev.iadev.model.ProjectConfig;

import java.io.PrintWriter;
import java.util.Optional;

/**
 * Loads project configuration from the specified source
 * (stack profile, interactive mode, or YAML file).
 *
 * <p>Extracted from {@link GenerateCommand} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see GenerateCommand
 */
final class ConfigSourceLoader {

    private ConfigSourceLoader() {
        // utility class
    }

    /**
     * Loads config based on command options.
     *
     * @param stack        the stack profile name
     * @param configSource the config source options
     * @param verbose      whether to log verbose output
     * @param out          the print writer
     * @return the loaded config, or empty if missing
     */
    static Optional<ProjectConfig> loadConfig(
            String stack,
            GenerateCommand.ConfigSource configSource,
            boolean verbose,
            PrintWriter out) {
        if (stack != null && !stack.isEmpty()) {
            logVerbose(verbose, out,
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
            return Optional.of(
                    loadInteractive(verbose, out));
        }
        if (configSource.configPath != null) {
            logVerbose(verbose, out,
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

    private static ProjectConfig loadInteractive(
            boolean verbose, PrintWriter out) {
        logVerbose(verbose, out,
                "Starting interactive mode...");
        try {
            InteractivePrompter prompter =
                    new InteractivePrompter(
                            new JLineTerminalProvider());
            return prompter.prompt();
        } catch (java.io.IOException e) {
            throw new CliException(
                    "Failed to initialize terminal: "
                            + e.getMessage(),
                    GenerateCommand.EXIT_EXECUTION);
        }
    }

    private static void logVerbose(
            boolean verbose,
            PrintWriter out, String message) {
        if (verbose) {
            out.println(message);
        }
    }
}
