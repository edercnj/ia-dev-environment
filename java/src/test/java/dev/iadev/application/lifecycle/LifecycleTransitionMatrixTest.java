package dev.iadev.application.lifecycle;

import dev.iadev.domain.lifecycle.LifecycleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exhaustive matrix coverage for
 * {@link LifecycleTransitionMatrix}. Verifies every allowed
 * cell and a representative sample of forbidden cells.
 * Story-0046-0001 / TASK-0046-0001-003.
 */
@DisplayName("LifecycleTransitionMatrix — Rule 22 matrix")
class LifecycleTransitionMatrixTest {

    @Test
    @DisplayName("isAllowed returns true for every allowed "
            + "transition in the matrix")
    void isAllowed_allowedTransitions_returnTrue() {
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.PENDENTE,
                LifecycleStatus.PLANEJADA)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.PENDENTE,
                LifecycleStatus.EM_ANDAMENTO)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.PENDENTE,
                LifecycleStatus.FALHA)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.PENDENTE,
                LifecycleStatus.BLOQUEADA)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.PLANEJADA,
                LifecycleStatus.EM_ANDAMENTO)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.PLANEJADA,
                LifecycleStatus.FALHA)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.PLANEJADA,
                LifecycleStatus.BLOQUEADA)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.EM_ANDAMENTO,
                LifecycleStatus.CONCLUIDA)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.EM_ANDAMENTO,
                LifecycleStatus.FALHA)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.EM_ANDAMENTO,
                LifecycleStatus.BLOQUEADA)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.CONCLUIDA,
                LifecycleStatus.EM_ANDAMENTO)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.FALHA,
                LifecycleStatus.PENDENTE)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.BLOQUEADA,
                LifecycleStatus.PENDENTE)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.BLOQUEADA,
                LifecycleStatus.PLANEJADA)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.BLOQUEADA,
                LifecycleStatus.EM_ANDAMENTO)).isTrue();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.BLOQUEADA,
                LifecycleStatus.FALHA)).isTrue();
    }

    @Test
    @DisplayName("isAllowed returns false for forbidden "
            + "transitions (data-loss guard)")
    void isAllowed_forbiddenTransitions_returnFalse() {
        // Data-loss guards:
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.CONCLUIDA,
                LifecycleStatus.PENDENTE)).isFalse();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.CONCLUIDA,
                LifecycleStatus.PLANEJADA)).isFalse();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.CONCLUIDA,
                LifecycleStatus.FALHA)).isFalse();
        // Skip-ahead guards:
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.PENDENTE,
                LifecycleStatus.CONCLUIDA)).isFalse();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.PLANEJADA,
                LifecycleStatus.PENDENTE)).isFalse();
        // Self-transition:
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.EM_ANDAMENTO,
                LifecycleStatus.EM_ANDAMENTO)).isFalse();
    }

    @Test
    @DisplayName("isAllowed returns false when either side "
            + "is null")
    void isAllowed_nullInputs_returnFalse() {
        assertThat(LifecycleTransitionMatrix.isAllowed(
                null, LifecycleStatus.PENDENTE)).isFalse();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                LifecycleStatus.PENDENTE, null)).isFalse();
        assertThat(LifecycleTransitionMatrix.isAllowed(
                null, null)).isFalse();
    }

    @Test
    @DisplayName("validateOrThrow passes for allowed "
            + "transition")
    void validateOrThrow_allowed_doesNotThrow() {
        LifecycleTransitionMatrix.validateOrThrow(
                LifecycleStatus.PENDENTE,
                LifecycleStatus.PLANEJADA);
    }

    @Test
    @DisplayName("validateOrThrow throws "
            + "StatusTransitionInvalidException with context")
    void validateOrThrow_forbidden_throwsWithContext() {
        assertThatThrownBy(() ->
                LifecycleTransitionMatrix.validateOrThrow(
                        LifecycleStatus.CONCLUIDA,
                        LifecycleStatus.PENDENTE))
                .isInstanceOf(
                        StatusTransitionInvalidException.class)
                .hasMessageContaining("Concluída")
                .hasMessageContaining("Pendente")
                .hasMessageContaining("Rule 22");
    }

    @Test
    @DisplayName("validateOrThrow throws when inputs are null")
    void validateOrThrow_nullInputs_throws() {
        assertThatThrownBy(() ->
                LifecycleTransitionMatrix.validateOrThrow(
                        null, LifecycleStatus.PENDENTE))
                .isInstanceOf(
                        StatusTransitionInvalidException.class);
        assertThatThrownBy(() ->
                LifecycleTransitionMatrix.validateOrThrow(
                        LifecycleStatus.PENDENTE, null))
                .isInstanceOf(
                        StatusTransitionInvalidException.class);
    }

    @Test
    @DisplayName("fromLabel round-trips every enum value")
    void fromLabel_allValues_roundTrip() {
        for (LifecycleStatus s : LifecycleStatus.values()) {
            assertThat(LifecycleStatus.fromLabel(s.label()))
                    .contains(s);
        }
    }

    @Test
    @DisplayName("fromLabel returns empty for unknown labels "
            + "and null")
    void fromLabel_unknown_returnsEmpty() {
        assertThat(LifecycleStatus.fromLabel("Unknown"))
                .isEmpty();
        assertThat(LifecycleStatus.fromLabel(""))
                .isEmpty();
        assertThat(LifecycleStatus.fromLabel(null))
                .isEmpty();
    }

    @Test
    @DisplayName("StatusTransitionInvalidException exposes "
            + "from and to via accessors")
    void exception_accessors_returnProvidedValues() {
        StatusTransitionInvalidException ex =
                new StatusTransitionInvalidException(
                        LifecycleStatus.CONCLUIDA,
                        LifecycleStatus.PENDENTE);
        assertThat(ex.from())
                .isEqualTo(LifecycleStatus.CONCLUIDA);
        assertThat(ex.to())
                .isEqualTo(LifecycleStatus.PENDENTE);
    }
}
