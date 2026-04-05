package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TraceabilityReport")
class TraceabilityReportTest {

    private static final CoverageSummary DEFAULT_SUMMARY =
            new CoverageSummary(
                    "1/1", "4/4 (100%)", 4, 0, 0);

    private static final LocalDate REPORT_DATE =
            LocalDate.of(2026, 4, 3);

    @Nested
    @DisplayName("constructor — validation")
    class Validation {

        @Test
        @DisplayName("throws when targetId is null")
        void constructor_nullTargetId_throwsException() {
            assertThatThrownBy(
                    () -> new TraceabilityReport(
                            null, REPORT_DATE, "abc123",
                            List.of(), List.of(),
                            List.of(), DEFAULT_SUMMARY))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("targetId");
        }

        @Test
        @DisplayName("throws when targetId is blank")
        void constructor_blankTargetId_throwsException() {
            assertThatThrownBy(
                    () -> new TraceabilityReport(
                            "  ", REPORT_DATE, "abc123",
                            List.of(), List.of(),
                            List.of(), DEFAULT_SUMMARY))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("targetId");
        }

        @Test
        @DisplayName("throws when generatedAt is null")
        void constructor_nullGeneratedAt_throwsException() {
            assertThatThrownBy(
                    () -> new TraceabilityReport(
                            "STORY-0007-0003", null,
                            "abc123",
                            List.of(), List.of(),
                            List.of(), DEFAULT_SUMMARY))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("generatedAt");
        }

        @Test
        @DisplayName("throws when buildHash is null")
        void constructor_nullBuildHash_throwsException() {
            assertThatThrownBy(
                    () -> new TraceabilityReport(
                            "STORY-0007-0003", REPORT_DATE,
                            null,
                            List.of(), List.of(),
                            List.of(), DEFAULT_SUMMARY))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("buildHash");
        }

        @Test
        @DisplayName("throws when buildHash is blank")
        void constructor_blankBuildHash_throwsException() {
            assertThatThrownBy(
                    () -> new TraceabilityReport(
                            "STORY-0007-0003", REPORT_DATE,
                            "  ",
                            List.of(), List.of(),
                            List.of(), DEFAULT_SUMMARY))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("buildHash");
        }

        @Test
        @DisplayName("throws when summary is null")
        void constructor_nullSummary_throwsException() {
            assertThatThrownBy(
                    () -> new TraceabilityReport(
                            "STORY-0007-0003", REPORT_DATE,
                            "abc123",
                            List.of(), List.of(),
                            List.of(), null))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("summary");
        }
    }

    @Nested
    @DisplayName("constructor — happy path")
    class HappyPath {

        @Test
        @DisplayName("creates report with all fields")
        void constructor_allFields_allAccessible() {
            var row = new TraceabilityRow(
                    "AT-1", "scenario",
                    "TestClass", "method",
                    ExecutionStatus.PASS, 94);

            var report = new TraceabilityReport(
                    "STORY-0007-0003", REPORT_DATE,
                    "abc123def",
                    List.of(row),
                    List.of("AT-3: unmapped"),
                    List.of("TestClass.extraMethod"),
                    DEFAULT_SUMMARY);

            assertThat(report.targetId())
                    .isEqualTo("STORY-0007-0003");
            assertThat(report.generatedAt())
                    .isEqualTo(REPORT_DATE);
            assertThat(report.buildHash())
                    .isEqualTo("abc123def");
            assertThat(report.entries()).hasSize(1);
            assertThat(report.unmappedRequirements())
                    .containsExactly("AT-3: unmapped");
            assertThat(report.unmappedTests())
                    .containsExactly(
                            "TestClass.extraMethod");
            assertThat(report.summary())
                    .isEqualTo(DEFAULT_SUMMARY);
        }

        @Test
        @DisplayName("creates report with empty lists")
        void constructor_emptyLists_allAccessible() {
            var report = new TraceabilityReport(
                    "EPIC-0007", REPORT_DATE, "def456",
                    List.of(), List.of(), List.of(),
                    DEFAULT_SUMMARY);

            assertThat(report.entries()).isEmpty();
            assertThat(report.unmappedRequirements())
                    .isEmpty();
            assertThat(report.unmappedTests()).isEmpty();
        }
    }

    @Nested
    @DisplayName("immutability")
    class Immutability {

        @Test
        @DisplayName("entries list is immutable")
        void entries_afterConstruction_immutable() {
            var mutableList = new ArrayList<>(
                    List.of(new TraceabilityRow(
                            "AT-1", "scenario",
                            "Test", "method",
                            ExecutionStatus.PASS, 95)));

            var report = new TraceabilityReport(
                    "STORY-1", REPORT_DATE, "hash1",
                    mutableList, List.of(), List.of(),
                    DEFAULT_SUMMARY);

            assertThatThrownBy(
                    () -> report.entries().clear())
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }

        @Test
        @DisplayName("unmappedRequirements is immutable")
        void unmappedRequirements_afterConstruction_immutable() {
            var mutableList =
                    new ArrayList<>(List.of("AT-3"));

            var report = new TraceabilityReport(
                    "STORY-1", REPORT_DATE, "hash1",
                    List.of(), mutableList, List.of(),
                    DEFAULT_SUMMARY);

            assertThatThrownBy(
                    () -> report.unmappedRequirements()
                            .clear())
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }

        @Test
        @DisplayName("unmappedTests is immutable")
        void unmappedTests_afterConstruction_immutable() {
            var mutableList =
                    new ArrayList<>(List.of("Test.m"));

            var report = new TraceabilityReport(
                    "STORY-1", REPORT_DATE, "hash1",
                    List.of(), List.of(), mutableList,
                    DEFAULT_SUMMARY);

            assertThatThrownBy(
                    () -> report.unmappedTests().clear())
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }
    }
}
