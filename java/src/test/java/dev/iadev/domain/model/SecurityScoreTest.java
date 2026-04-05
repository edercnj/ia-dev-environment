package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SecurityScore} calculation
 * covering all Gherkin acceptance criteria from
 * story-0022-0002.
 */
@DisplayName("SecurityScore")
class SecurityScoreTest {

    @Nested
    @DisplayName("calculate — zero findings")
    class ZeroFindings {

        @Test
        @DisplayName("empty map yields score 100")
        void calculate_emptyMap_returns100() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of());
            assertThat(result.score()).isEqualTo(100);
        }

        @Test
        @DisplayName("empty map yields grade A")
        void calculate_emptyMap_returnsGradeA() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of());
            assertThat(result.grade())
                    .isEqualTo(SecurityGrade.A);
        }

        @Test
        @DisplayName("empty map yields zero totalFindings")
        void calculate_emptyMap_zeroTotal() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of());
            assertThat(result.totalFindings()).isZero();
        }

        @Test
        @DisplayName("all counts are zero")
        void calculate_emptyMap_allCountsZero() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of());
            assertThat(result.criticalCount()).isZero();
            assertThat(result.highCount()).isZero();
            assertThat(result.mediumCount()).isZero();
            assertThat(result.lowCount()).isZero();
            assertThat(result.infoCount()).isZero();
        }
    }

    @Nested
    @DisplayName("calculate — mixed findings")
    class MixedFindings {

        @Test
        @DisplayName(
                "1 CRITICAL + 2 HIGH + 3 MEDIUM = 74")
        void calculate_mixedFindings_returns74() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.CRITICAL, 1,
                            SecuritySeverity.HIGH, 2,
                            SecuritySeverity.MEDIUM, 3));
            assertThat(result.score()).isEqualTo(74);
        }

        @Test
        @DisplayName(
                "1 CRITICAL + 2 HIGH + 3 MEDIUM => C")
        void calculate_mixedFindings_gradeC() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.CRITICAL, 1,
                            SecuritySeverity.HIGH, 2,
                            SecuritySeverity.MEDIUM, 3));
            assertThat(result.grade())
                    .isEqualTo(SecurityGrade.C);
        }

        @Test
        @DisplayName("mixed findings total is 6")
        void calculate_mixedFindings_totalIs6() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.CRITICAL, 1,
                            SecuritySeverity.HIGH, 2,
                            SecuritySeverity.MEDIUM, 3));
            assertThat(result.totalFindings())
                    .isEqualTo(6);
        }

        @Test
        @DisplayName("mixed findings counts are correct")
        void calculate_mixedFindings_countsCorrect() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.CRITICAL, 1,
                            SecuritySeverity.HIGH, 2,
                            SecuritySeverity.MEDIUM, 3));
            assertThat(result.criticalCount())
                    .isEqualTo(1);
            assertThat(result.highCount()).isEqualTo(2);
            assertThat(result.mediumCount()).isEqualTo(3);
            assertThat(result.lowCount()).isZero();
            assertThat(result.infoCount()).isZero();
        }
    }

    @Nested
    @DisplayName("calculate — score floor at zero")
    class ScoreFloor {

        @Test
        @DisplayName("20 CRITICAL yields score 0")
        void calculate_20Critical_returns0() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.CRITICAL, 20));
            assertThat(result.score()).isZero();
        }

        @Test
        @DisplayName("20 CRITICAL yields grade F")
        void calculate_20Critical_gradeF() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.CRITICAL, 20));
            assertThat(result.grade())
                    .isEqualTo(SecurityGrade.F);
        }

        @Test
        @DisplayName("score never goes below zero")
        void calculate_overflow_floorAtZero() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.CRITICAL, 100,
                            SecuritySeverity.HIGH, 100));
            assertThat(result.score()).isZero();
        }
    }

    @Nested
    @DisplayName("calculate — INFO only")
    class InfoOnly {

        @Test
        @DisplayName("50 INFO yields score 100")
        void calculate_50Info_returns100() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.INFO, 50));
            assertThat(result.score()).isEqualTo(100);
        }

        @Test
        @DisplayName("50 INFO yields grade A")
        void calculate_50Info_gradeA() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.INFO, 50));
            assertThat(result.grade())
                    .isEqualTo(SecurityGrade.A);
        }

        @Test
        @DisplayName("50 INFO yields totalFindings 50")
        void calculate_50Info_totalIs50() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.INFO, 50));
            assertThat(result.totalFindings())
                    .isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("calculate — grade boundary")
    class GradeBoundary {

        @Test
        @DisplayName("2 HIGH = score 90 => grade A")
        void calculate_2High_gradeA() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.HIGH, 2));
            assertThat(result.score()).isEqualTo(90);
            assertThat(result.grade())
                    .isEqualTo(SecurityGrade.A);
        }

        @Test
        @DisplayName("1 CRITICAL + 1 HIGH = score 85 => B")
        void calculate_1Critical1High_gradeB() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.CRITICAL, 1,
                            SecuritySeverity.HIGH, 1));
            assertThat(result.score()).isEqualTo(85);
            assertThat(result.grade())
                    .isEqualTo(SecurityGrade.B);
        }

        @Test
        @DisplayName("10 LOW = score 90 => grade A")
        void calculate_10Low_gradeA() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.LOW, 10));
            assertThat(result.score()).isEqualTo(90);
            assertThat(result.grade())
                    .isEqualTo(SecurityGrade.A);
        }

        @Test
        @DisplayName("all severities combined")
        void calculate_allSeverities_correctScore() {
            SecurityScore result =
                    SecurityScore.calculate(Map.of(
                            SecuritySeverity.CRITICAL, 1,
                            SecuritySeverity.HIGH, 1,
                            SecuritySeverity.MEDIUM, 1,
                            SecuritySeverity.LOW, 1,
                            SecuritySeverity.INFO, 1));
            // 10+5+2+1+0 = 18, score = 82
            assertThat(result.score()).isEqualTo(82);
            assertThat(result.grade())
                    .isEqualTo(SecurityGrade.B);
            assertThat(result.totalFindings())
                    .isEqualTo(5);
        }
    }
}
