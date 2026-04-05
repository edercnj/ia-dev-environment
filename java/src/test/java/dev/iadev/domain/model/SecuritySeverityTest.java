package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SecuritySeverity} enum weights.
 */
@DisplayName("SecuritySeverity")
class SecuritySeverityTest {

    @Test
    @DisplayName("CRITICAL weight is 10")
    void weight_critical_returns10() {
        assertThat(SecuritySeverity.CRITICAL.weight())
                .isEqualTo(10);
    }

    @Test
    @DisplayName("HIGH weight is 5")
    void weight_high_returns5() {
        assertThat(SecuritySeverity.HIGH.weight())
                .isEqualTo(5);
    }

    @Test
    @DisplayName("MEDIUM weight is 2")
    void weight_medium_returns2() {
        assertThat(SecuritySeverity.MEDIUM.weight())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("LOW weight is 1")
    void weight_low_returns1() {
        assertThat(SecuritySeverity.LOW.weight())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("INFO weight is 0")
    void weight_info_returns0() {
        assertThat(SecuritySeverity.INFO.weight())
                .isEqualTo(0);
    }

    @Test
    @DisplayName("enum has exactly 5 values")
    void values_count_isFive() {
        assertThat(SecuritySeverity.values())
                .hasSize(5);
    }
}
