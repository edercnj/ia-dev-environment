package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewChecklistScore")
class ReviewChecklistScoreTest {

    private static final int BASE_SCORE = 45;
    private static final int EVENT_DRIVEN_SCORE = 8;
    private static final int PCI_DSS_SCORE = 7;
    private static final int LGPD_SCORE = 4;
    private static final double GO_THRESHOLD_PERCENT = 0.84;

    @Nested
    @DisplayName("maxScore()")
    class MaxScore {

        @Test
        @DisplayName("base score without conditionals is 45")
        void maxScore_noConditionals_returns45() {
            var score = ReviewChecklistScore.compute(
                    false, false, false);

            assertThat(score.maxScore()).isEqualTo(BASE_SCORE);
        }

        @Test
        @DisplayName("event-driven adds 8 points")
        void maxScore_eventDriven_returns53() {
            var score = ReviewChecklistScore.compute(
                    true, false, false);

            assertThat(score.maxScore())
                    .isEqualTo(BASE_SCORE + EVENT_DRIVEN_SCORE);
        }

        @Test
        @DisplayName("pci-dss adds 7 points")
        void maxScore_pciDss_returns52() {
            var score = ReviewChecklistScore.compute(
                    false, true, false);

            assertThat(score.maxScore())
                    .isEqualTo(BASE_SCORE + PCI_DSS_SCORE);
        }

        @Test
        @DisplayName("lgpd adds 4 points")
        void maxScore_lgpd_returns49() {
            var score = ReviewChecklistScore.compute(
                    false, false, true);

            assertThat(score.maxScore())
                    .isEqualTo(BASE_SCORE + LGPD_SCORE);
        }

        @Test
        @DisplayName("all conditionals sum to 64")
        void maxScore_allConditionals_returns64() {
            var score = ReviewChecklistScore.compute(
                    true, true, true);

            assertThat(score.maxScore())
                    .isEqualTo(BASE_SCORE + EVENT_DRIVEN_SCORE
                            + PCI_DSS_SCORE + LGPD_SCORE);
        }

        @Test
        @DisplayName("event + pci-dss sum to 60")
        void maxScore_eventAndPciDss_returns60() {
            var score = ReviewChecklistScore.compute(
                    true, true, false);

            assertThat(score.maxScore())
                    .isEqualTo(BASE_SCORE + EVENT_DRIVEN_SCORE
                            + PCI_DSS_SCORE);
        }
    }

    @Nested
    @DisplayName("goThreshold()")
    class GoThreshold {

        @Test
        @DisplayName("base threshold is ceil(45 * 0.84) = 38")
        void goThreshold_base_returns38() {
            var score = ReviewChecklistScore.compute(
                    false, false, false);

            assertThat(score.goThreshold()).isEqualTo(38);
        }

        @Test
        @DisplayName("event threshold is ceil(53 * 0.84) = 45")
        void goThreshold_eventDriven_returns45() {
            var score = ReviewChecklistScore.compute(
                    true, false, false);

            assertThat(score.goThreshold()).isEqualTo(45);
        }

        @Test
        @DisplayName("pci-dss threshold is ceil(52 * 0.84) = 44")
        void goThreshold_pciDss_returns44() {
            var score = ReviewChecklistScore.compute(
                    false, true, false);

            assertThat(score.goThreshold()).isEqualTo(44);
        }

        @Test
        @DisplayName("all conditionals threshold is ceil(64 * 0.84) = 54")
        void goThreshold_allConditionals_returns54() {
            var score = ReviewChecklistScore.compute(
                    true, true, true);

            assertThat(score.goThreshold()).isEqualTo(54);
        }

        @Test
        @DisplayName("threshold rounds up with ceil")
        void goThreshold_rounding_usesCeil() {
            var score = ReviewChecklistScore.compute(
                    true, true, false);
            // 60 * 0.84 = 50.4, ceil = 51
            assertThat(score.goThreshold()).isEqualTo(51);
        }
    }
}
