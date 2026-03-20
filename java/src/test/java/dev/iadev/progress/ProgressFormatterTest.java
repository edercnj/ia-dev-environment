package dev.iadev.progress;

import dev.iadev.checkpoint.ExecutionMetrics;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProgressFormatter}.
 */
class ProgressFormatterTest {

    @Nested
    class FormatProgressBar {

        @Test
        void formatProgressBar_zeroOfTen_allEmpty() {
            var result = ProgressFormatter
                    .formatProgressBar(0, 10);

            assertThat(result).isEqualTo(
                    "[\u2591\u2591\u2591\u2591\u2591"
                    + "\u2591\u2591\u2591\u2591\u2591"
                    + "\u2591\u2591\u2591\u2591\u2591"
                    + "\u2591\u2591\u2591\u2591\u2591"
                    + "] 0/10 (0%)");
        }

        @Test
        void formatProgressBar_fiveOfTen_halfFilled() {
            var result = ProgressFormatter
                    .formatProgressBar(5, 10);

            assertThat(result).isEqualTo(
                    "[\u2588\u2588\u2588\u2588\u2588"
                    + "\u2588\u2588\u2588\u2588\u2588"
                    + "\u2591\u2591\u2591\u2591\u2591"
                    + "\u2591\u2591\u2591\u2591\u2591"
                    + "] 5/10 (50%)");
        }

        @Test
        void formatProgressBar_tenOfTen_allFilled() {
            var result = ProgressFormatter
                    .formatProgressBar(10, 10);

            assertThat(result).isEqualTo(
                    "[\u2588\u2588\u2588\u2588\u2588"
                    + "\u2588\u2588\u2588\u2588\u2588"
                    + "\u2588\u2588\u2588\u2588\u2588"
                    + "\u2588\u2588\u2588\u2588\u2588"
                    + "] 10/10 (100%)");
        }

        @Test
        void formatProgressBar_zeroTotal_emptyBar() {
            var result = ProgressFormatter
                    .formatProgressBar(0, 0);

            assertThat(result).isEqualTo(
                    "[\u2591\u2591\u2591\u2591\u2591"
                    + "\u2591\u2591\u2591\u2591\u2591"
                    + "\u2591\u2591\u2591\u2591\u2591"
                    + "\u2591\u2591\u2591\u2591\u2591"
                    + "] 0/0 (0%)");
        }

        @Test
        void formatProgressBar_threeOfTen_roundsDown() {
            var result = ProgressFormatter
                    .formatProgressBar(3, 10);

            // 3/10 = 30% => 6 filled, 14 empty
            assertThat(result).isEqualTo(
                    "[\u2588\u2588\u2588\u2588\u2588"
                    + "\u2588"
                    + "\u2591\u2591\u2591\u2591\u2591"
                    + "\u2591\u2591\u2591\u2591\u2591"
                    + "\u2591\u2591\u2591\u2591"
                    + "] 3/10 (30%)");
        }

        @Test
        void formatProgressBar_whenCalled_barHasExactly20Characters() {
            var result = ProgressFormatter
                    .formatProgressBar(7, 10);

            // Extract just the bar content between [ and ]
            int openBracket = result.indexOf('[');
            int closeBracket = result.indexOf(']');
            var barContent = result.substring(
                    openBracket + 1, closeBracket);

            assertThat(barContent).hasSize(20);
        }
    }

    @Nested
    class FormatStatusSummary {

        @Test
        void formatStatusSummary_mixedStatuses_formatsAll() {
            var metrics = metricsOf(3, 10, 1, 2);

            var result = ProgressFormatter
                    .formatStatusSummary(metrics);

            assertThat(result).isEqualTo(
                    "SUCCESS: 3 | FAILED: 1"
                    + " | BLOCKED: 2 | PENDING: 4");
        }

        @Test
        void formatStatusSummary_allSuccess_noPending() {
            var metrics = metricsOf(5, 5, 0, 0);

            var result = ProgressFormatter
                    .formatStatusSummary(metrics);

            assertThat(result).isEqualTo(
                    "SUCCESS: 5 | FAILED: 0"
                    + " | BLOCKED: 0 | PENDING: 0");
        }

        @Test
        void formatStatusSummary_noneCompleted_allPending() {
            var metrics = metricsOf(0, 5, 0, 0);

            var result = ProgressFormatter
                    .formatStatusSummary(metrics);

            assertThat(result).isEqualTo(
                    "SUCCESS: 0 | FAILED: 0"
                    + " | BLOCKED: 0 | PENDING: 5");
        }
    }

    @Nested
    class FormatEta {

        @Test
        void formatEta_positiveValue_formatsMinutes() {
            var result = ProgressFormatter.formatEta(10.5);

            assertThat(result).isEqualTo(
                    "Estimated remaining: 10.5 min");
        }

        @Test
        void formatEta_negativeOne_returnsUnknown() {
            var result = ProgressFormatter.formatEta(-1);

            assertThat(result).isEqualTo(
                    "Estimated remaining: unknown");
        }

        @Test
        void formatEta_zero_returnsZeroMin() {
            var result = ProgressFormatter.formatEta(0.0);

            assertThat(result).isEqualTo(
                    "Estimated remaining: 0.0 min");
        }

        @Test
        void formatEta_wholeNumber_formatsWithOneDecimal() {
            var result = ProgressFormatter.formatEta(5.0);

            assertThat(result).isEqualTo(
                    "Estimated remaining: 5.0 min");
        }
    }

    @Nested
    class FormatPhaseProgress {

        @Test
        void formatPhaseProgress_middlePhase_formatsCorrectly() {
            var result = ProgressFormatter
                    .formatPhaseProgress(2, 5);

            assertThat(result).isEqualTo(
                    "Phase 2/5 in progress");
        }

        @Test
        void formatPhaseProgress_firstPhase_formatsCorrectly() {
            var result = ProgressFormatter
                    .formatPhaseProgress(1, 3);

            assertThat(result).isEqualTo(
                    "Phase 1/3 in progress");
        }
    }

    @Nested
    class FormatThroughput {

        @Test
        void formatThroughput_positiveValue_formatsMinPerStory() {
            // 120000 ms = 2.0 min
            var result = ProgressFormatter
                    .formatThroughput(120_000.0);

            assertThat(result).isEqualTo(
                    "Average: 2.0 min/story");
        }

        @Test
        void formatThroughput_zero_returnsZero() {
            var result = ProgressFormatter.formatThroughput(0.0);

            assertThat(result).isEqualTo(
                    "Average: 0.0 min/story");
        }

        @Test
        void formatThroughput_fractionalMinutes_formats() {
            // 90000 ms = 1.5 min
            var result = ProgressFormatter
                    .formatThroughput(90_000.0);

            assertThat(result).isEqualTo(
                    "Average: 1.5 min/story");
        }
    }

    @Nested
    class Format {

        @Test
        void format_mixedMetrics_containsAllSections() {
            var metrics = new ExecutionMetrics(
                    5, 10, 1, 2, 10.5, 600_000L,
                    120_000.0,
                    Map.of("s1", 60_000L),
                    Map.of(0, 140_000L)
            );

            var result = ProgressFormatter.format(metrics, 2, 5);

            assertThat(result)
                    .contains("5/10 (50%)")
                    .contains("SUCCESS: 5 | FAILED: 1"
                            + " | BLOCKED: 2 | PENDING: 2")
                    .contains("Phase 2/5 in progress")
                    .contains("Estimated remaining: 10.5 min")
                    .contains("Average: 2.0 min/story");
        }
    }

    private static ExecutionMetrics metricsOf(
            int completed, int total, int failed, int blocked) {
        return new ExecutionMetrics(
                completed, total, failed, blocked,
                0.0, 0L, 0.0, Map.of(), Map.of()
        );
    }
}
