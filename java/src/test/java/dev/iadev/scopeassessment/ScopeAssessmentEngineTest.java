package dev.iadev.scopeassessment;

import dev.iadev.domain.scopeassessment.LifecyclePhaseConfig;
import dev.iadev.domain.scopeassessment.ScopeAssessmentEngine;
import dev.iadev.domain.scopeassessment.ScopeAssessmentResult;
import dev.iadev.domain.scopeassessment.ScopeAssessmentTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ScopeAssessmentEngine}.
 * Organized by TPP: degenerate -> simple -> standard ->
 * complex -> override -> edge cases.
 */
@DisplayName("ScopeAssessmentEngine")
class ScopeAssessmentEngineTest {

    private static final List<String> SIMPLE_SKIP =
            List.of("1B", "1C", "1D", "1E");
    private static final List<String> COMPLEX_EXTRA =
            List.of("stakeholder-review");

    @Nested
    @DisplayName("assess — degenerate cases")
    class AssessDegenerate {

        @Test
        @DisplayName("empty story classifies as SIMPLE")
        void assess_emptyContent_returnsSimple() {
            var result = ScopeAssessmentEngine
                    .assess("", 0);

            assertThat(result.tier())
                    .isEqualTo(ScopeAssessmentTier.SIMPLE);
            assertThat(result.componentCount()).isZero();
            assertThat(result.rationale())
                    .contains("no components detected");
        }

        @Test
        @DisplayName("blank story classifies as SIMPLE")
        void assess_blankContent_returnsSimple() {
            var result = ScopeAssessmentEngine
                    .assess("   \n  \n  ", 0);

            assertThat(result.tier())
                    .isEqualTo(ScopeAssessmentTier.SIMPLE);
        }
    }

    @Nested
    @DisplayName("assess — SIMPLE tier")
    class AssessSimple {

        @Test
        @DisplayName("1 component, 0 endpoints = SIMPLE")
        void assess_oneComponent_returnsSimple() {
            String story = """
                    ## 3. Description
                    Modify UserService.java to add logging.
                    """;
            var result = ScopeAssessmentEngine
                    .assess(story, 0);

            assertThat(result.tier())
                    .isEqualTo(ScopeAssessmentTier.SIMPLE);
            assertThat(result.componentCount())
                    .isEqualTo(1);
            assertThat(result.newEndpointCount()).isZero();
            assertThat(result.phasesToSkip())
                    .isEqualTo(SIMPLE_SKIP);
            assertThat(result.rationale()).contains(
                    "single component change",
                    "no new endpoints");
        }

        @Test
        @DisplayName("SIMPLE skips phases 1B-1E")
        void assess_simple_skipsPlanningPhases() {
            String story = """
                    Modify Config.java only.
                    """;
            var result = ScopeAssessmentEngine
                    .assess(story, 0);

            assertThat(result.phasesToSkip())
                    .containsExactly("1B", "1C", "1D", "1E");
            assertThat(result.additionalPhases()).isEmpty();
        }
    }

    @Nested
    @DisplayName("assess — STANDARD tier")
    class AssessStandard {

        @Test
        @DisplayName("3 components = STANDARD")
        void assess_threeComponents_returnsStandard() {
            String story = """
                    ## 3. Description
                    Modify Controller.java, Service.java,
                    and Repository.java for the new feature.
                    | POST /payments | Create payment |
                    """;
            var result = ScopeAssessmentEngine
                    .assess(story, 0);

            assertThat(result.tier())
                    .isEqualTo(ScopeAssessmentTier.STANDARD);
            assertThat(result.componentCount())
                    .isGreaterThanOrEqualTo(2);
            assertThat(result.phasesToSkip()).isEmpty();
            assertThat(result.additionalPhases()).isEmpty();
        }

        @Test
        @DisplayName("1-2 new endpoints = STANDARD")
        void assess_twoEndpoints_returnsStandard() {
            String story = """
                    ## Data Contract
                    | POST /api/users | Create user |
                    | GET /api/users/{id} | Get user |
                    """;
            var result = ScopeAssessmentEngine
                    .assess(story, 0);

            assertThat(result.tier())
                    .isEqualTo(ScopeAssessmentTier.STANDARD);
            assertThat(result.newEndpointCount())
                    .isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("assess — COMPLEX tier")
    class AssessComplex {

        @Test
        @DisplayName("compliance always elevates to COMPLEX")
        void assess_compliance_alwaysComplex() {
            String story = """
                    ## 3. Description
                    Minor text change in Config.java.
                    compliance: pci-dss
                    """;
            var result = ScopeAssessmentEngine
                    .assess(story, 0);

            assertThat(result.tier())
                    .isEqualTo(ScopeAssessmentTier.COMPLEX);
            assertThat(result.hasCompliance()).isTrue();
            assertThat(result.rationale())
                    .contains("compliance requirement detected");
            assertThat(result.additionalPhases())
                    .isEqualTo(COMPLEX_EXTRA);
        }

        @Test
        @DisplayName("schema changes elevate to COMPLEX")
        void assess_schemaChanges_returnsComplex() {
            String story = """
                    ## 3. Description
                    Update User.java and UserDto.java.
                    Add migration script for ALTER TABLE users.
                    """;
            var result = ScopeAssessmentEngine
                    .assess(story, 0);

            assertThat(result.tier())
                    .isEqualTo(ScopeAssessmentTier.COMPLEX);
            assertThat(result.hasSchemaChanges()).isTrue();
            assertThat(result.rationale())
                    .contains("schema changes detected");
        }

        @Test
        @DisplayName("4+ components = COMPLEX")
        void assess_fiveComponents_returnsComplex() {
            String story = """
                    ## 3. Description
                    Modify Controller.java, Service.java,
                    Repository.java, Mapper.java, and
                    Config.java for the new feature.
                    """;
            var result = ScopeAssessmentEngine
                    .assess(story, 0);

            assertThat(result.tier())
                    .isEqualTo(ScopeAssessmentTier.COMPLEX);
            assertThat(result.componentCount())
                    .isGreaterThanOrEqualTo(4);
            assertThat(result.rationale())
                    .contains("components affected");
        }

        @Test
        @DisplayName("COMPLEX adds stakeholder-review phase")
        void assess_complex_addsStakeholderReview() {
            String story = """
                    compliance: pci-dss
                    """;
            var result = ScopeAssessmentEngine
                    .assess(story, 0);

            assertThat(result.additionalPhases())
                    .containsExactly("stakeholder-review");
            assertThat(result.phasesToSkip()).isEmpty();
        }
    }

    @Nested
    @DisplayName("assess — schema change patterns")
    class AssessSchemaPatterns {

        @ParameterizedTest
        @ValueSource(strings = {
                "migration script",
                "ALTER TABLE",
                "CREATE TABLE",
                "DROP TABLE",
                "ADD COLUMN"
        })
        @DisplayName("detects schema change keywords")
        void assess_schemaKeyword_detectsSchemaChange(
                String keyword) {
            String story = "Description with " + keyword
                    + " mentioned.";
            var result = ScopeAssessmentEngine
                    .assess(story, 0);

            assertThat(result.hasSchemaChanges()).isTrue();
        }
    }

    @Nested
    @DisplayName("assess — endpoint detection")
    class AssessEndpoints {

        @ParameterizedTest
        @ValueSource(strings = {
                "POST /api/users",
                "GET /api/users/{id}",
                "PUT /api/users/{id}",
                "DELETE /api/users/{id}",
                "PATCH /api/users/{id}"
        })
        @DisplayName("detects HTTP method + path patterns")
        void assess_httpEndpoint_countsEndpoint(
                String endpoint) {
            String story = "| " + endpoint + " | desc |";
            var result = ScopeAssessmentEngine
                    .assess(story, 0);

            assertThat(result.newEndpointCount())
                    .isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("buildPhaseConfig")
    class BuildPhaseConfig {

        @Test
        @DisplayName("SIMPLE result produces skip config")
        void buildPhaseConfig_simple_skipsPhases() {
            var result = new ScopeAssessmentResult(
                    ScopeAssessmentTier.SIMPLE,
                    1, 0, false, false, 0,
                    "simple",
                    SIMPLE_SKIP, List.of());

            LifecyclePhaseConfig config =
                    ScopeAssessmentEngine
                            .buildPhaseConfig(result, false);

            assertThat(config.tier())
                    .isEqualTo(ScopeAssessmentTier.SIMPLE);
            assertThat(config.skippedPhases())
                    .isEqualTo(SIMPLE_SKIP);
            assertThat(config.overrideActive()).isFalse();
            assertThat(config.activePhases())
                    .contains("1A", "2", "4", "5", "6")
                    .doesNotContain("1B", "1C", "1D", "1E");
        }

        @Test
        @DisplayName("STANDARD result produces full config")
        void buildPhaseConfig_standard_allPhases() {
            var result = new ScopeAssessmentResult(
                    ScopeAssessmentTier.STANDARD,
                    3, 1, false, false, 0,
                    "standard",
                    List.of(), List.of());

            LifecyclePhaseConfig config =
                    ScopeAssessmentEngine
                            .buildPhaseConfig(result, false);

            assertThat(config.tier())
                    .isEqualTo(ScopeAssessmentTier.STANDARD);
            assertThat(config.skippedPhases()).isEmpty();
            assertThat(config.additionalPhases()).isEmpty();
            assertThat(config.activePhases())
                    .contains("1A", "1B", "1C", "1D", "1E",
                            "2", "3", "4", "5", "6");
        }

        @Test
        @DisplayName("COMPLEX result adds stakeholder review")
        void buildPhaseConfig_complex_addsStakeholder() {
            var result = new ScopeAssessmentResult(
                    ScopeAssessmentTier.COMPLEX,
                    5, 2, true, true, 3,
                    "complex",
                    List.of(), COMPLEX_EXTRA);

            LifecyclePhaseConfig config =
                    ScopeAssessmentEngine
                            .buildPhaseConfig(result, false);

            assertThat(config.tier())
                    .isEqualTo(ScopeAssessmentTier.COMPLEX);
            assertThat(config.additionalPhases())
                    .containsExactly("stakeholder-review");
        }

        @Test
        @DisplayName("--full-lifecycle overrides SIMPLE to "
                + "full execution")
        void buildPhaseConfig_fullOverride_allPhases() {
            var result = new ScopeAssessmentResult(
                    ScopeAssessmentTier.SIMPLE,
                    1, 0, false, false, 0,
                    "simple",
                    SIMPLE_SKIP, List.of());

            LifecyclePhaseConfig config =
                    ScopeAssessmentEngine
                            .buildPhaseConfig(result, true);

            assertThat(config.overrideActive()).isTrue();
            assertThat(config.skippedPhases()).isEmpty();
            assertThat(config.activePhases())
                    .contains("1A", "1B", "1C", "1D", "1E",
                            "2", "3", "4", "5", "6");
        }
    }
}
