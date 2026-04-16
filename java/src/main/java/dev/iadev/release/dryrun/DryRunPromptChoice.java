package dev.iadev.release.dryrun;

/**
 * Operator response to the per-phase prompt in an
 * interactive dry-run (story-0039-0013 §3.2).
 */
public enum DryRunPromptChoice {

    /** "Continuar" — advance to next phase. */
    CONTINUE,

    /** "Pular fase" — mark SKIPPED, advance. */
    SKIP,

    /** "Abortar simulação" — stop immediately. */
    ABORT
}
