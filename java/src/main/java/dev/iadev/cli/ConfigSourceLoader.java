package dev.iadev.cli;

import dev.iadev.config.ConfigLoader;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.exception.CliException;
import dev.iadev.domain.model.ProjectConfig;

import java.io.PrintWriter;
import java.util.Optional;
import java.util.function.Consumer;

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

    /**
     * Debug sink that discards all messages. Callers that
     * do not want verbose output pass this (or any other
     * no-op {@link Consumer}) instead of the prior
     * {@code boolean verbose = false} argument.
     */
    static final Consumer<String> SILENT_DEBUG = s -> { };

    private ConfigSourceLoader() {
        // utility class
    }

    /**
     * Loads config based on command options.
     *
     * @param stack        the stack profile name
     * @param configSource the config source options
     * @param debugSink    receives verbose debug messages;
     *                     pass {@link #SILENT_DEBUG} to
     *                     suppress them
     * @param out          the print writer for user-facing
     *                     errors
     * @return the loaded config, or empty if missing
     */
    static Optional<ProjectConfig> loadConfig(
            String stack,
            GenerateCommand.ConfigSource configSource,
            Consumer<String> debugSink,
            PrintWriter out) {
        if (stack != null && !stack.isEmpty()) {
            debugSink.accept(
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
                    loadInteractive(debugSink));
        }
        if (configSource.configPath != null) {
            debugSink.accept(
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
            Consumer<String> debugSink) {
        debugSink.accept("Starting interactive mode...");
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
}
