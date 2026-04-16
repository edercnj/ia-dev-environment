package dev.iadev.release.dryrun;

import java.util.List;
import java.util.Objects;

/**
 * Per-phase record produced by
 * {@link DryRunInteractiveExecutor}.
 *
 * @param phase     phase identifier (e.g. "VALIDATE_DEEP")
 * @param outcome   per-phase outcome
 * @param commands  commands that would have been executed
 */
public record DryRunPhaseResult(
        String phase,
        DryRunPhaseOutcome outcome,
        List<String> commands) {

    public DryRunPhaseResult {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(commands, "commands");
        commands = List.copyOf(commands);
    }
}
