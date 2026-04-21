package dev.iadev.application.lifecycle;

import dev.iadev.domain.lifecycle.LifecycleStatus;

/**
 * Thrown when a caller attempts a lifecycle transition that
 * is not permitted by the matrix in Rule 22 — for example
 * {@code Concluída → Pendente}. Carries the {@code from} and
 * {@code to} values for diagnostics.
 */
public class StatusTransitionInvalidException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final LifecycleStatus from;
    private final LifecycleStatus to;

    public StatusTransitionInvalidException(
            LifecycleStatus from, LifecycleStatus to) {
        super("Invalid lifecycle transition: "
                + labelOf(from) + " -> " + labelOf(to)
                + " (see Rule 22).");
        this.from = from;
        this.to = to;
    }

    private static String labelOf(LifecycleStatus s) {
        return s == null ? "null" : s.label();
    }

    public LifecycleStatus from() {
        return from;
    }

    public LifecycleStatus to() {
        return to;
    }
}
