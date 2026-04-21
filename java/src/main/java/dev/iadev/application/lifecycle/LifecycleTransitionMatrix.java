package dev.iadev.application.lifecycle;

import dev.iadev.domain.lifecycle.LifecycleStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable transition matrix for {@link LifecycleStatus}.
 * Normative source: Rule 22 — lifecycle-integrity. Consumed
 * by every skill / helper that performs a status write to
 * validate the proposed transition BEFORE applying the
 * change.
 *
 * <p>The matrix is thread-safe (all state is immutable after
 * static initialisation).</p>
 */
public final class LifecycleTransitionMatrix {

    private static final Map<LifecycleStatus,
            Set<LifecycleStatus>> ALLOWED;

    static {
        EnumMap<LifecycleStatus, Set<LifecycleStatus>> m =
                new EnumMap<>(LifecycleStatus.class);
        m.put(LifecycleStatus.PENDENTE, EnumSet.of(
                LifecycleStatus.PLANEJADA,
                LifecycleStatus.EM_ANDAMENTO,
                LifecycleStatus.FALHA,
                LifecycleStatus.BLOQUEADA));
        m.put(LifecycleStatus.PLANEJADA, EnumSet.of(
                LifecycleStatus.EM_ANDAMENTO,
                LifecycleStatus.FALHA,
                LifecycleStatus.BLOQUEADA));
        m.put(LifecycleStatus.EM_ANDAMENTO, EnumSet.of(
                LifecycleStatus.CONCLUIDA,
                LifecycleStatus.FALHA,
                LifecycleStatus.BLOQUEADA));
        m.put(LifecycleStatus.CONCLUIDA, EnumSet.of(
                LifecycleStatus.EM_ANDAMENTO));
        m.put(LifecycleStatus.FALHA, EnumSet.of(
                LifecycleStatus.PENDENTE));
        m.put(LifecycleStatus.BLOQUEADA, EnumSet.of(
                LifecycleStatus.PENDENTE,
                LifecycleStatus.PLANEJADA,
                LifecycleStatus.EM_ANDAMENTO,
                LifecycleStatus.FALHA));
        ALLOWED = Map.copyOf(m);
    }

    private LifecycleTransitionMatrix() {
        // Utility class — not instantiable.
    }

    /**
     * Returns {@code true} when the transition is permitted
     * by the matrix. Transitions to the same state are
     * considered forbidden (no-op writes are redundant and
     * indicate caller confusion).
     */
    public static boolean isAllowed(LifecycleStatus from,
            LifecycleStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<LifecycleStatus> allowed = ALLOWED.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * Validates the transition or throws
     * {@link StatusTransitionInvalidException} with diagnostic
     * context when forbidden. Null inputs are forbidden.
     */
    public static void validateOrThrow(
            LifecycleStatus from, LifecycleStatus to) {
        if (from == null || to == null) {
            throw new StatusTransitionInvalidException(
                    from, to);
        }
        if (!isAllowed(from, to)) {
            throw new StatusTransitionInvalidException(
                    from, to);
        }
    }
}
