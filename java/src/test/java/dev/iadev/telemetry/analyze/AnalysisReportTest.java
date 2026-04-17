package dev.iadev.telemetry.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisReportTest {

    @Test
    void constructor_nullEpics_defaultsToEmpty() {
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                null, 0L, 0L, null, null, null, null, null);

        assertThat(report.epics()).isEmpty();
        assertThat(report.skills()).isEmpty();
        assertThat(report.phases()).isEmpty();
        assertThat(report.tools()).isEmpty();
        assertThat(report.timeline()).isEmpty();
        assertThat(report.observations()).isEmpty();
    }

    @Test
    void constructor_nullGeneratedAt_throws() {
        assertThatThrownBy(() -> new AnalysisReport(
                null, List.of(), 0L, 0L,
                List.of(), List.of(), List.of(),
                List.of(), List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_negativeTotalEvents_throws() {
        assertThatThrownBy(() -> new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of(), -1L, 0L,
                List.of(), List.of(), List.of(),
                List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalEvents");
    }

    @Test
    void constructor_negativeTotalDurationMs_throws() {
        assertThatThrownBy(() -> new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of(), 0L, -1L,
                List.of(), List.of(), List.of(),
                List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalDurationMs");
    }

    @Test
    void constructor_mutableLists_defensivelyCopied() {
        List<String> mutable = new ArrayList<>();
        mutable.add("EPIC-0040");
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                mutable, 0L, 0L,
                List.of(), List.of(), List.of(),
                List.of(), List.of());
        mutable.add("EPIC-0041");

        assertThat(report.epics()).containsExactly("EPIC-0040");
    }
}
