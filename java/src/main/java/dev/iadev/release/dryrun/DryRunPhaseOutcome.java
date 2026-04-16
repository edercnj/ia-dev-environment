package dev.iadev.release.dryrun;

/**
 * Outcome assigned to each phase during an interactive
 * dry-run simulation (story-0039-0013 §3.2).
 */
public enum DryRunPhaseOutcome {

    /** Operator chose "Continuar" — phase simulated. */
    SIMULATED,

    /** Operator chose "Pular fase" — phase skipped. */
    SKIPPED,

    /** Operator chose "Abortar" — simulation aborted here. */
    ABORTED,

    /** Phase never reached (simulation aborted earlier). */
    NOT_REACHED
}
