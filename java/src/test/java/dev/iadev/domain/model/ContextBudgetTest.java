package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContextBudget}.
 */
@DisplayName("ContextBudget")
class ContextBudgetTest {

    @Nested
    @DisplayName("fromLineCount")
    class FromLineCount {

        @ParameterizedTest
        @CsvSource({
                "0, light",
                "1, light",
                "100, light",
                "199, light"
        })
        @DisplayName("lines below 200 classify as light")
        void fromLineCount_belowThreshold_returnsLight(
                int lines, String expected) {
            ContextBudget budget =
                    ContextBudget.fromLineCount(lines);

            assertThat(budget.value()).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "200, medium",
                "350, medium",
                "500, medium"
        })
        @DisplayName("lines 200-500 classify as medium")
        void fromLineCount_midRange_returnsMedium(
                int lines, String expected) {
            ContextBudget budget =
                    ContextBudget.fromLineCount(lines);

            assertThat(budget.value()).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "501, heavy",
                "1000, heavy",
                "1610, heavy"
        })
        @DisplayName("lines above 500 classify as heavy")
        void fromLineCount_aboveThreshold_returnsHeavy(
                int lines, String expected) {
            ContextBudget budget =
                    ContextBudget.fromLineCount(lines);

            assertThat(budget.value()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("value")
    class Value {

        @Test
        @DisplayName("LIGHT returns light")
        void light_value_returnsLight() {
            assertThat(ContextBudget.LIGHT.value())
                    .isEqualTo("light");
        }

        @Test
        @DisplayName("MEDIUM returns medium")
        void medium_value_returnsMedium() {
            assertThat(ContextBudget.MEDIUM.value())
                    .isEqualTo("medium");
        }

        @Test
        @DisplayName("HEAVY returns heavy")
        void heavy_value_returnsHeavy() {
            assertThat(ContextBudget.HEAVY.value())
                    .isEqualTo("heavy");
        }
    }
}
