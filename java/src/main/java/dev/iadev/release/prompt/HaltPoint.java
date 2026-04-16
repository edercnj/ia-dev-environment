package dev.iadev.release.prompt;

import java.util.List;

/**
 * Halt points in the {@code x-release} flow where the
 * operator is prompted for a decision.
 *
 * <p>Each halt point declares its available options as
 * human-readable labels matching story-0039-0007 §3.1.
 */
public enum HaltPoint {

    /**
     * Phase 8 — release PR created, waiting for merge.
     */
    APPROVAL_GATE(List.of(
            "PR mergeado — continuar",
            "Rodar /x-pr-fix PR#",
            "Sair e retomar depois")),

    /**
     * Phase 10 — back-merge PR created, waiting for merge.
     */
    BACKMERGE_MERGE(List.of(
            "PR mergeado — continuar",
            "Rodar /x-pr-fix PR#",
            "Sair e retomar depois")),

    /**
     * Recoverable failure (e.g. push rejected by race).
     */
    RECOVERABLE_FAILURE(List.of(
            "Tentar novamente",
            "Pular esta etapa",
            "Abortar"));

    private final List<String> options;

    HaltPoint(List<String> options) {
        this.options = List.copyOf(options);
    }

    /**
     * Returns the fixed set of options for this halt point.
     *
     * @return unmodifiable list of option labels
     */
    public List<String> options() {
        return options;
    }
}
