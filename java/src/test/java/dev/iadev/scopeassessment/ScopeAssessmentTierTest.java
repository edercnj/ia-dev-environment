package dev.iadev.scopeassessment;

import dev.iadev.domain.scopeassessment.ScopeAssessmentTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ScopeAssessmentTier} enum.
 */
@DisplayName("ScopeAssessmentTier")
class ScopeAssessmentTierTest {

    @Test
    @DisplayName("enum has exactly three values")
    void values_always_returnsThreeValues() {
        assertThat(ScopeAssessmentTier.values())
                .hasSize(3)
                .containsExactly(
                        ScopeAssessmentTier.SIMPLE,
                        ScopeAssessmentTier.STANDARD,
                        ScopeAssessmentTier.COMPLEX);
    }

    @Test
    @DisplayName("valueOf resolves SIMPLE")
    void valueOf_simple_resolves() {
        assertThat(ScopeAssessmentTier.valueOf("SIMPLE"))
                .isEqualTo(ScopeAssessmentTier.SIMPLE);
    }

    @Test
    @DisplayName("valueOf resolves STANDARD")
    void valueOf_standard_resolves() {
        assertThat(ScopeAssessmentTier.valueOf("STANDARD"))
                .isEqualTo(ScopeAssessmentTier.STANDARD);
    }

    @Test
    @DisplayName("valueOf resolves COMPLEX")
    void valueOf_complex_resolves() {
        assertThat(ScopeAssessmentTier.valueOf("COMPLEX"))
                .isEqualTo(ScopeAssessmentTier.COMPLEX);
    }
}
