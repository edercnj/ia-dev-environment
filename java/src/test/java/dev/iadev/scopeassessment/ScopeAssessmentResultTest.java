package dev.iadev.scopeassessment;

import dev.iadev.domain.scopeassessment.ScopeAssessmentResult;
import dev.iadev.domain.scopeassessment.ScopeAssessmentTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ScopeAssessmentResult} record.
 */
@DisplayName("ScopeAssessmentResult")
class ScopeAssessmentResultTest {

    @Test
    @DisplayName("record preserves all fields")
    void constructor_allFields_preservesValues() {
        var result = new ScopeAssessmentResult(
                ScopeAssessmentTier.SIMPLE,
                1, 0, false, false, 0,
                "single component",
                List.of("1B", "1C", "1D", "1E"),
                List.of());

        assertThat(result.tier())
                .isEqualTo(ScopeAssessmentTier.SIMPLE);
        assertThat(result.componentCount()).isEqualTo(1);
        assertThat(result.newEndpointCount()).isZero();
        assertThat(result.hasSchemaChanges()).isFalse();
        assertThat(result.hasCompliance()).isFalse();
        assertThat(result.dependentCount()).isZero();
        assertThat(result.rationale())
                .isEqualTo("single component");
        assertThat(result.phasesToSkip())
                .containsExactly("1B", "1C", "1D", "1E");
        assertThat(result.additionalPhases()).isEmpty();
    }

    @Test
    @DisplayName("lists are immutable copies")
    void constructor_mutableLists_createsImmutableCopies() {
        var skip = new java.util.ArrayList<>(
                List.of("1B"));
        var extra = new java.util.ArrayList<>(
                List.of("stakeholder-review"));

        var result = new ScopeAssessmentResult(
                ScopeAssessmentTier.COMPLEX,
                5, 2, true, true, 3,
                "complex story",
                skip, extra);

        skip.add("1C");
        extra.add("extra");

        assertThat(result.phasesToSkip())
                .containsExactly("1B");
        assertThat(result.additionalPhases())
                .containsExactly("stakeholder-review");
    }
}
