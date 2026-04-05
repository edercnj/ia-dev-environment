package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CoverageSummary")
class CoverageSummaryTest {

    @Nested
    @DisplayName("constructor — validation")
    class Validation {

        @Test
        @DisplayName("throws when storiesCovered is null")
        void constructor_nullStoriesCovered_throwsException() {
            assertThatThrownBy(() -> new CoverageSummary(
                    null, "10/12 (83%)", 8, 1, 1))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining(
                            "storiesCovered");
        }

        @Test
        @DisplayName("throws when storiesCovered is "
                + "blank")
        void constructor_blankStoriesCovered_throwsException() {
            assertThatThrownBy(() -> new CoverageSummary(
                    "  ", "10/12 (83%)", 8, 1, 1))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining(
                            "storiesCovered");
        }

        @Test
        @DisplayName("throws when scenariosCovered is "
                + "null")
        void constructor_nullScenariosCovered_throwsException() {
            assertThatThrownBy(() -> new CoverageSummary(
                    "3/4", null, 8, 1, 1))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining(
                            "scenariosCovered");
        }

        @Test
        @DisplayName("throws when passCount is negative")
        void constructor_negativePassCount_throwsException() {
            assertThatThrownBy(() -> new CoverageSummary(
                    "3/4", "10/12 (83%)", -1, 0, 0))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("throws when skipCount is negative")
        void constructor_negativeSkipCount_throwsException() {
            assertThatThrownBy(() -> new CoverageSummary(
                    "3/4", "10/12 (83%)", 0, -1, 0))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("throws when failCount is negative")
        void constructor_negativeFailCount_throwsException() {
            assertThatThrownBy(() -> new CoverageSummary(
                    "3/4", "10/12 (83%)", 0, 0, -1))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }
    }

    @Nested
    @DisplayName("constructor — happy path")
    class HappyPath {

        @Test
        @DisplayName("creates summary with all fields")
        void constructor_allFields_allAccessible() {
            var summary = new CoverageSummary(
                    "3/4", "10/12 (83%)", 8, 1, 1);

            assertThat(summary.storiesCovered())
                    .isEqualTo("3/4");
            assertThat(summary.scenariosCovered())
                    .isEqualTo("10/12 (83%)");
            assertThat(summary.passCount()).isEqualTo(8);
            assertThat(summary.skipCount()).isEqualTo(1);
            assertThat(summary.failCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("allows zero counts")
        void constructor_zeroCounts_createsInstance() {
            var summary = new CoverageSummary(
                    "1/1", "4/4 (100%)", 0, 0, 0);

            assertThat(summary.passCount()).isZero();
            assertThat(summary.skipCount()).isZero();
            assertThat(summary.failCount()).isZero();
        }
    }
}
