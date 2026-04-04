package dev.iadev.infrastructure.adapter.input.cli;

import dev.iadev.domain.model.StackProfile;
import dev.iadev.domain.port.input.ListStackProfilesUseCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * CLI Input Adapter for listing available stack profiles.
 *
 * <p>Wraps the Picocli command to delegate listing logic
 * to the {@link ListStackProfilesUseCase} Input Port. This
 * adapter never accesses domain services directly
 * (RULE-003).</p>
 *
 * @see ListStackProfilesUseCase
 */
@Command(
        name = "list-profiles",
        mixinStandardHelpOptions = true,
        description = "List available stack profiles "
                + "via Input Port."
)
public class CliListStackProfilesCommand
        implements Callable<Integer> {

    /** Successful execution exit code. */
    static final int EXIT_SUCCESS = 0;

    private final ListStackProfilesUseCase
            listStackProfilesUseCase;

    @Spec
    CommandSpec spec;

    /**
     * Creates a new CliListStackProfilesCommand.
     *
     * @param listStackProfilesUseCase the use case for
     *        listing profiles (must not be null)
     * @throws NullPointerException if parameter is null
     */
    public CliListStackProfilesCommand(
            ListStackProfilesUseCase
                    listStackProfilesUseCase) {
        this.listStackProfilesUseCase =
                Objects.requireNonNull(
                        listStackProfilesUseCase,
                        "listStackProfilesUseCase "
                                + "must not be null");
    }

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        List<StackProfile> profiles =
                listStackProfilesUseCase.listProfiles();

        if (profiles.isEmpty()) {
            out.println("No stack profiles available");
            return EXIT_SUCCESS;
        }

        formatProfiles(profiles, out);
        return EXIT_SUCCESS;
    }

    private void formatProfiles(
            List<StackProfile> profiles,
            PrintWriter out) {
        out.println("Available stack profiles:");
        out.println();
        for (StackProfile profile : profiles) {
            out.println("  %s".formatted(
                    profile.name()));
            out.println("    Language:   %s".formatted(
                    profile.language()));
            out.println("    Framework:  %s".formatted(
                    profile.framework()));
            out.println("    Build Tool: %s".formatted(
                    profile.buildTool()));
            out.println();
        }
    }
}
