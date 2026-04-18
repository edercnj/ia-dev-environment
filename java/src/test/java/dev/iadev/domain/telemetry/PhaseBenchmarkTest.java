package dev.iadev.domain.telemetry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the {@link PhaseBenchmark} record's validation
 * branches.
 */
@DisplayName("PhaseBenchmark")
class PhaseBenchmarkTest {

    @Test
    @DisplayName("constructor_blankPhase_throws")
    void constructor_blankPhase_throws() {
        assertThatThrownBy(
                () -> new PhaseBenchmark(
                        "  ", 10L, 5L, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phase");
    }

    @Test
    @DisplayName("constructor_negativeDuration_throws")
    void constructor_negativeDuration_throws() {
        assertThatThrownBy(
                () -> new PhaseBenchmark(
                        "p", -1L, 5L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationSec");
    }

    @Test
    @DisplayName("constructor_negativeMean_throws")
    void constructor_negativeMean_throws() {
        assertThatThrownBy(
                () -> new PhaseBenchmark(
                        "p", 10L, -1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("meanSec");
    }

    @Test
    @DisplayName("constructor_validValues_stored")
    void constructor_validValues_stored() {
        PhaseBenchmark pb = new PhaseBenchmark(
                "VALIDATE", 10L, 5L, 100);

        assertThat(pb.phase()).isEqualTo("VALIDATE");
        assertThat(pb.durationSec()).isEqualTo(10L);
        assertThat(pb.meanSec()).isEqualTo(5L);
        assertThat(pb.deltaPercent()).isEqualTo(100);
    }
}
