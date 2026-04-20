package dev.iadev.domain.lifecycle;

import java.util.Optional;

/**
 * Canonical six-valued lifecycle status for Epic, Story, and
 * Task markdown artifacts. See Rule 22 — lifecycle-integrity
 * for the normative definition. Domain-pure (no I/O, no
 * framework dependencies).
 */
public enum LifecycleStatus {

    PENDENTE("Pendente"),
    PLANEJADA("Planejada"),
    EM_ANDAMENTO("Em Andamento"),
    CONCLUIDA("Concluída"),
    FALHA("Falha"),
    BLOQUEADA("Bloqueada");

    private final String label;

    LifecycleStatus(String label) {
        this.label = label;
    }

    /**
     * Returns the canonical Portuguese label as it appears in
     * the `**Status:**` line of the markdown artifact.
     */
    public String label() {
        return label;
    }

    /**
     * Parses a label back into the enum. Returns
     * {@link Optional#empty()} when the input does not match
     * one of the six canonical values (caller decides whether
     * to throw or treat as absent).
     *
     * @param label the label from the markdown, already
     *     trimmed by the parser
     * @return the matching enum, or empty when unknown
     */
    public static Optional<LifecycleStatus> fromLabel(
            String label) {
        if (label == null) {
            return Optional.empty();
        }
        for (LifecycleStatus s : values()) {
            if (s.label.equals(label)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
