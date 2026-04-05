package dev.iadev.scopeassessment;

import dev.iadev.domain.scopeassessment.LifecyclePhaseConfig;
import dev.iadev.domain.scopeassessment.ScopeAssessmentTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LifecyclePhaseConfig} record.
 */
@DisplayName("LifecyclePhaseConfig")
class LifecyclePhaseConfigTest {

    @Test
    @DisplayName("record preserves all fields")
    void constructor_allFields_preservesValues() {
        var config = new LifecyclePhaseConfig(
                List.of("1A", "2", "4", "5", "6"),
                List.of("1B", "1C", "1D", "1E"),
                List.of(),
                ScopeAssessmentTier.SIMPLE,
                false);

        assertThat(config.activePhases())
                .containsExactly("1A", "2", "4", "5", "6");
        assertThat(config.skippedPhases())
                .containsExactly("1B", "1C", "1D", "1E");
        assertThat(config.additionalPhases()).isEmpty();
        assertThat(config.tier())
                .isEqualTo(ScopeAssessmentTier.SIMPLE);
        assertThat(config.overrideActive()).isFalse();
    }

    @Test
    @DisplayName("lists are immutable copies")
    void constructor_mutableLists_createsImmutableCopies() {
        var active = new java.util.ArrayList<>(
                List.of("1A"));
        var skipped = new java.util.ArrayList<>(
                List.of("1B"));
        var extra = new java.util.ArrayList<>(
                List.of("stakeholder-review"));

        var config = new LifecyclePhaseConfig(
                active, skipped, extra,
                ScopeAssessmentTier.COMPLEX, true);

        active.add("2");
        skipped.add("1C");
        extra.add("extra");

        assertThat(config.activePhases())
                .containsExactly("1A");
        assertThat(config.skippedPhases())
                .containsExactly("1B");
        assertThat(config.additionalPhases())
                .containsExactly("stakeholder-review");
    }
}
