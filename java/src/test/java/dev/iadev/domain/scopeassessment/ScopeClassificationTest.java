package dev.iadev.domain.scopeassessment;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeClassificationTest {

    @Nested
    class Construction {

        @Test
        void create_simpleClassification_storesAllFields() {
            var metrics = new ScopeMetrics(1, 0, false, false, 0);
            var result = new ScopeClassification(
                    StoryComplexityTier.SIMPLE,
                    metrics,
                    "single component",
                    List.of("1B", "1C", "1D", "1E"),
                    List.of());

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.SIMPLE);
            assertThat(result.metrics()).isEqualTo(metrics);
            assertThat(result.rationale())
                    .isEqualTo("single component");
            assertThat(result.phasesToSkip())
                    .containsExactly("1B", "1C", "1D", "1E");
            assertThat(result.additionalPhases()).isEmpty();
        }

        @Test
        void create_complexClassification_hasAdditionalPhases() {
            var metrics = new ScopeMetrics(5, 0, false, true, 2);
            var result = new ScopeClassification(
                    StoryComplexityTier.COMPLEX,
                    metrics,
                    "compliance requirement",
                    List.of(),
                    List.of("stakeholder-review"));

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.COMPLEX);
            assertThat(result.additionalPhases())
                    .containsExactly("stakeholder-review");
            assertThat(result.phasesToSkip()).isEmpty();
        }
    }

    @Nested
    class Immutability {

        @Test
        void phasesToSkip_isDefensivelyCopied() {
            var mutableList = new java.util.ArrayList<>(
                    List.of("1B", "1C"));
            var metrics = new ScopeMetrics(1, 0, false, false, 0);
            var result = new ScopeClassification(
                    StoryComplexityTier.SIMPLE,
                    metrics,
                    "test",
                    mutableList,
                    List.of());

            mutableList.add("1D");

            assertThat(result.phasesToSkip())
                    .containsExactly("1B", "1C");
        }

        @Test
        void additionalPhases_isDefensivelyCopied() {
            var mutableList = new java.util.ArrayList<>(
                    List.of("stakeholder-review"));
            var metrics = new ScopeMetrics(5, 0, true, false, 0);
            var result = new ScopeClassification(
                    StoryComplexityTier.COMPLEX,
                    metrics,
                    "test",
                    List.of(),
                    mutableList);

            mutableList.add("extra-phase");

            assertThat(result.additionalPhases())
                    .containsExactly("stakeholder-review");
        }
    }
}
