package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SecurityGrade} score-to-grade
 * mapping.
 */
@DisplayName("SecurityGrade")
class SecurityGradeTest {

    @Nested
    @DisplayName("fromScore — grade boundaries")
    class FromScore {

        @ParameterizedTest(name = "score {0} => grade {1}")
        @CsvSource({
            "100, A",
            "90,  A",
            "89,  B",
            "80,  B",
            "79,  C",
            "70,  C",
            "69,  D",
            "60,  D",
            "59,  F",
            "0,   F"
        })
        @DisplayName("maps score to correct grade")
        void fromScore_boundary_returnsCorrectGrade(
                int score, SecurityGrade expected) {
            assertThat(SecurityGrade.fromScore(score))
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("minimumScore")
    class MinimumScore {

        @Test
        @DisplayName("A minimum is 90")
        void minimumScore_gradeA_is90() {
            assertThat(SecurityGrade.A.minimumScore())
                    .isEqualTo(90);
        }

        @Test
        @DisplayName("B minimum is 80")
        void minimumScore_gradeB_is80() {
            assertThat(SecurityGrade.B.minimumScore())
                    .isEqualTo(80);
        }

        @Test
        @DisplayName("C minimum is 70")
        void minimumScore_gradeC_is70() {
            assertThat(SecurityGrade.C.minimumScore())
                    .isEqualTo(70);
        }

        @Test
        @DisplayName("D minimum is 60")
        void minimumScore_gradeD_is60() {
            assertThat(SecurityGrade.D.minimumScore())
                    .isEqualTo(60);
        }

        @Test
        @DisplayName("F minimum is 0")
        void minimumScore_gradeF_is0() {
            assertThat(SecurityGrade.F.minimumScore())
                    .isEqualTo(0);
        }
    }

    @Test
    @DisplayName("enum has exactly 5 grades")
    void values_count_isFive() {
        assertThat(SecurityGrade.values()).hasSize(5);
    }
}
