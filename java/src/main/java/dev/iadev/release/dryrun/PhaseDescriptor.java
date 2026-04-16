package dev.iadev.release.dryrun;

import java.util.List;
import java.util.Objects;

/**
 * Descriptor of a single release phase used by
 * {@link DryRunInteractiveExecutor}.
 *
 * @param name      phase identifier (e.g. "VALIDATE_DEEP")
 * @param commands  commands that would run in this phase
 */
public record PhaseDescriptor(String name,
                              List<String> commands) {

    public PhaseDescriptor {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(commands, "commands");
        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "name must not be blank");
        }
        commands = List.copyOf(commands);
    }
}
