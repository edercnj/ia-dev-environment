package dev.iadev.domain.scopeassessment;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeAssessmentEngineTest {

    private static final List<String> PARALLEL_PLANNING_PHASES =
            List.of("1B", "1C", "1D", "1E");

    private static final String STAKEHOLDER_REVIEW =
            "stakeholder-review";

    // --- TPP: degenerate cases ---

    @Nested
    class DegenrateCases {

        @Test
        void classify_allZeros_returnsSimple() {
            var metrics = new ScopeMetrics(
                    0, 0, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.SIMPLE);
        }
    }

    // --- GK-1: empty story classifies as SIMPLE ---

    @Nested
    class GK1_EmptyStory {

        @Test
        void classify_zeroComponents_returnsSimple() {
            var metrics = new ScopeMetrics(
                    0, 0, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.SIMPLE);
            assertThat(result.rationale())
                    .containsIgnoringCase("no components detected");
        }
    }

    // --- GK-2: 1 component, 0 endpoints = SIMPLE ---

    @Nested
    class GK2_SingleComponent {

        @Test
        void classify_oneComponent_returnsSimple() {
            var metrics = new ScopeMetrics(
                    1, 0, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.SIMPLE);
            assertThat(result.phasesToSkip())
                    .containsExactlyElementsOf(
                            PARALLEL_PLANNING_PHASES);
        }

        @Test
        void classify_oneComponent_rationaleExplains() {
            var metrics = new ScopeMetrics(
                    1, 0, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.rationale())
                    .contains("single component change")
                    .contains("no new endpoints");
        }

        @Test
        void classify_oneComponent_noAdditionalPhases() {
            var metrics = new ScopeMetrics(
                    1, 0, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.additionalPhases()).isEmpty();
        }
    }

    // --- GK-3: 3 components + 1 endpoint = STANDARD ---

    @Nested
    class GK3_Standard {

        @Test
        void classify_threeComponents_returnsStandard() {
            var metrics = new ScopeMetrics(
                    3, 1, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.STANDARD);
            assertThat(result.phasesToSkip()).isEmpty();
            assertThat(result.additionalPhases()).isEmpty();
        }

        @Test
        void classify_twoComponents_returnsStandard() {
            var metrics = new ScopeMetrics(
                    2, 0, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.STANDARD);
        }

        @Test
        void classify_oneEndpoint_returnsStandard() {
            var metrics = new ScopeMetrics(
                    0, 1, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.STANDARD);
        }

        @Test
        void classify_twoEndpoints_returnsStandard() {
            var metrics = new ScopeMetrics(
                    0, 2, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.STANDARD);
        }
    }

    // --- GK-4: compliance always elevates to COMPLEX ---

    @Nested
    class GK4_ComplianceElevation {

        @Test
        void classify_complianceWithOneComponent_returnsComplex() {
            var metrics = new ScopeMetrics(
                    1, 0, false, true, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.COMPLEX);
            assertThat(result.rationale())
                    .containsIgnoringCase(
                            "compliance requirement detected");
            assertThat(result.additionalPhases())
                    .contains(STAKEHOLDER_REVIEW);
        }

        @Test
        void classify_complianceWithZeroComponents_complex() {
            var metrics = new ScopeMetrics(
                    0, 0, false, true, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.COMPLEX);
        }
    }

    // --- GK-5: schema changes elevate to COMPLEX ---

    @Nested
    class GK5_SchemaChanges {

        @Test
        void classify_schemaChangesTwoComponents_complex() {
            var metrics = new ScopeMetrics(
                    2, 0, true, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.COMPLEX);
            assertThat(result.rationale())
                    .containsIgnoringCase("schema changes detected");
        }

        @Test
        void classify_schemaChangesZeroComponents_complex() {
            var metrics = new ScopeMetrics(
                    0, 0, true, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.COMPLEX);
        }

        @Test
        void classify_schemaChanges_hasStakeholderReview() {
            var metrics = new ScopeMetrics(
                    2, 0, true, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.additionalPhases())
                    .contains(STAKEHOLDER_REVIEW);
        }
    }

    // --- GK-6: 4+ components = COMPLEX ---

    @Nested
    class GK6_ManyComponents {

        @Test
        void classify_fourComponents_returnsComplex() {
            var metrics = new ScopeMetrics(
                    4, 0, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.COMPLEX);
        }

        @Test
        void classify_fiveComponents_rationaleIncludesCount() {
            var metrics = new ScopeMetrics(
                    5, 0, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.COMPLEX);
            assertThat(result.rationale())
                    .contains("5 components affected");
        }
    }

    // --- Elevation rules ---

    @Nested
    class ElevationRules {

        @Test
        void classify_complianceOverridesSimple_complex() {
            var metrics = new ScopeMetrics(
                    1, 0, false, true, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.COMPLEX);
        }

        @Test
        void classify_schemaAndCompliance_complex() {
            var metrics = new ScopeMetrics(
                    1, 0, true, true, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.COMPLEX);
        }

        @Test
        void classify_twoStandardCriteria_stayStandard() {
            var metrics = new ScopeMetrics(
                    3, 2, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.STANDARD);
        }

        @Test
        void classify_threeEndpoints_noElevation_standard() {
            var metrics = new ScopeMetrics(
                    0, 3, false, false, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier())
                    .isEqualTo(StoryComplexityTier.STANDARD);
        }
    }

    // --- Parameterized boundary tests ---

    @Nested
    class BoundaryTests {

        @ParameterizedTest
        @CsvSource({
                "0, 0, false, false, SIMPLE",
                "1, 0, false, false, SIMPLE",
                "2, 0, false, false, STANDARD",
                "3, 0, false, false, STANDARD",
                "4, 0, false, false, COMPLEX",
                "5, 0, false, false, COMPLEX",
                "0, 1, false, false, STANDARD",
                "0, 2, false, false, STANDARD",
                "0, 3, false, false, STANDARD",
                "1, 1, false, false, STANDARD",
                "0, 0, true, false, COMPLEX",
                "0, 0, false, true, COMPLEX",
                "1, 0, true, true, COMPLEX"
        })
        void classify_parameterized_returnsExpectedTier(
                int components,
                int endpoints,
                boolean schema,
                boolean compliance,
                StoryComplexityTier expectedTier) {
            var metrics = new ScopeMetrics(
                    components, endpoints, schema, compliance, 0);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.tier()).isEqualTo(expectedTier);
        }
    }

    // --- Metrics passthrough ---

    @Nested
    class MetricsPassthrough {

        @Test
        void classify_metricsArePreservedInResult() {
            var metrics = new ScopeMetrics(
                    3, 2, true, false, 5);

            var result = ScopeAssessmentEngine.classify(metrics);

            assertThat(result.metrics()).isEqualTo(metrics);
        }
    }
}
