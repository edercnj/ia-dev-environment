package dev.iadev.domain.qualitygate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReportFormatter}.
 */
class ReportFormatterTest {

    @Nested
    class HeaderFormatting {

        @Test
        void format_passedResult_containsPASSED() {
            var result = new QualityGateResult(
                    85, 70, true,
                    List.of(), 10, 10, 10, 15, 10,
                    List.of());
            var report = ReportFormatter.format(
                    "story-0016-0001", result);
            assertThat(report).contains(
                    "=== Spec Quality Gate"
                            + " — story-0016-0001 ===");
            assertThat(report).contains(
                    "85/100 (threshold: 70) — PASSED");
        }

        @Test
        void format_rejectedResult_containsREJECTED() {
            var result = new QualityGateResult(
                    45, 70, false,
                    List.of(), 0, 0, 10, 0, 10,
                    List.of("Fix something"));
            var report = ReportFormatter.format(
                    "story-0016-0002", result);
            assertThat(report).contains(
                    "45/100 (threshold: 70) — REJECTED");
        }
    }

    @Nested
    class BreakdownFormatting {

        @Test
        void format_withScenarios_showsBreakdown() {
            var scenario = new ScenarioScore(
                    "@GK-1", 5, 5, 0,
                    List.of("@GK-1 Then: 'resultado "
                            + "esperado' is non-verifiable"
                            + " — specify exact HTTP "
                            + "status, field name, "
                            + "or value"));
            var result = new QualityGateResult(
                    45, 70, false,
                    List.of(scenario),
                    0, 0, 0, 0, 10,
                    List.of());
            var report = ReportFormatter.format(
                    "story-0016-0003", result);
            assertThat(report).contains("Breakdown:");
            assertThat(report).contains("Scenarios (1):");
            assertThat(report).contains("@GK-1:");
            assertThat(report).contains("Data Contract:");
            assertThat(report).contains("Types:");
            assertThat(report).contains("Vagueness:");
            assertThat(report).contains("Dependencies:");
        }
    }

    @Nested
    class ActionItemsFormatting {

        @Test
        void format_withActionItems_showsNumberedList() {
            var result = new QualityGateResult(
                    30, 70, false,
                    List.of(), 0, 0, 0, 0, 0,
                    List.of("Fix item A", "Fix item B"));
            var report = ReportFormatter.format(
                    "story-test", result);
            assertThat(report).contains("Action Items:");
            assertThat(report).contains("1. Fix item A");
            assertThat(report).contains("2. Fix item B");
        }

        @Test
        void format_noActionItems_noSection() {
            var result = new QualityGateResult(
                    100, 70, true,
                    List.of(), 10, 10, 10, 15, 10,
                    List.of());
            var report = ReportFormatter.format(
                    "story-test", result);
            assertThat(report)
                    .doesNotContain("Action Items:");
        }
    }

    @Nested
    class StoryIdFormatting {

        @Test
        void format_customThreshold_showsInReport() {
            var result = new QualityGateResult(
                    78, 85, false,
                    List.of(), 10, 10, 10, 15, 10,
                    List.of());
            var report = ReportFormatter.format(
                    "story-0016-0006", result);
            assertThat(report).contains(
                    "78/100 (threshold: 85) — REJECTED");
        }
    }
}
