package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutionStatus")
class ExecutionStatusTest {

    @Test
    @DisplayName("has exactly four values")
    void values_always_returnsFourStatuses() {
        assertThat(ExecutionStatus.values())
                .hasSize(4)
                .containsExactly(
                        ExecutionStatus.PASS,
                        ExecutionStatus.FAIL,
                        ExecutionStatus.SKIP,
                        ExecutionStatus.UNMAPPED);
    }

    @Test
    @DisplayName("valueOf PASS returns correct enum")
    void valueOf_pass_returnsPass() {
        assertThat(ExecutionStatus.valueOf("PASS"))
                .isEqualTo(ExecutionStatus.PASS);
    }

    @Test
    @DisplayName("valueOf FAIL returns correct enum")
    void valueOf_fail_returnsFail() {
        assertThat(ExecutionStatus.valueOf("FAIL"))
                .isEqualTo(ExecutionStatus.FAIL);
    }

    @Test
    @DisplayName("valueOf SKIP returns correct enum")
    void valueOf_skip_returnsSkip() {
        assertThat(ExecutionStatus.valueOf("SKIP"))
                .isEqualTo(ExecutionStatus.SKIP);
    }

    @Test
    @DisplayName("valueOf UNMAPPED returns correct enum")
    void valueOf_unmapped_returnsUnmapped() {
        assertThat(ExecutionStatus.valueOf("UNMAPPED"))
                .isEqualTo(ExecutionStatus.UNMAPPED);
    }
}
