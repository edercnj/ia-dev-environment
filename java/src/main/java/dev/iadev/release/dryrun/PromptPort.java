package dev.iadev.release.dryrun;

/**
 * Port abstracting the per-phase prompt used by
 * {@link DryRunInteractiveExecutor}.
 *
 * <p>Decouples the use case from any real TTY / UI so that
 * tests (and CI smoke runs) may inject scripted responders
 * (RULE-004 non-interactive equivalent).
 */
@FunctionalInterface
public interface PromptPort {

    /**
     * Prompts the operator for the action at a given phase.
     *
     * @param phase    phase about to be simulated
     * @param position 1-based index within the catalog
     * @param total    total number of phases in the catalog
     * @return operator choice
     */
    DryRunPromptChoice promptForPhase(PhaseDescriptor phase,
                                      int position,
                                      int total);
}
