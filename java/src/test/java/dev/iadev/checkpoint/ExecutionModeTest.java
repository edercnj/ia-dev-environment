package dev.iadev.checkpoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExecutionMode} enum.
 */
class ExecutionModeTest {

    @Test
    void values_containsThreeModes() {
        assertThat(ExecutionMode.values()).hasSize(3);
    }

    @Test
    void valueOf_full_returnsFull() {
        assertThat(ExecutionMode.valueOf("FULL"))
                .isEqualTo(ExecutionMode.FULL);
    }

    @Test
    void valueOf_partial_returnsPartial() {
        assertThat(ExecutionMode.valueOf("PARTIAL"))
                .isEqualTo(ExecutionMode.PARTIAL);
    }

    @Test
    void valueOf_dryRun_returnsDryRun() {
        assertThat(ExecutionMode.valueOf("DRY_RUN"))
                .isEqualTo(ExecutionMode.DRY_RUN);
    }
}
