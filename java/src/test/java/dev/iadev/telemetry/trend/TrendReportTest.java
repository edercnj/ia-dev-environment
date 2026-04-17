package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrendReportTest {

    @Test
    void construct_validValues_succeeds() {
        TrendReport r = new TrendReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0001"),
                20.0,
                "MEAN",
                List.of(),
                List.of());
        assertThat(r.baseline()).isEqualTo("MEAN");
        assertThat(r.thresholdPct()).isEqualTo(20.0);
    }

    @Test
    void construct_blankBaseline_throws() {
        assertThatThrownBy(() -> new TrendReport(
                Instant.now(), List.of(), 20.0, "",
                List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_negativeThreshold_throws() {
        assertThatThrownBy(() -> new TrendReport(
                Instant.now(), List.of(), -1.0, "MEAN",
                List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_nullCollections_areEmpty() {
        TrendReport r = new TrendReport(
                Instant.now(), null, 0.0, "MEAN", null, null);
        assertThat(r.epicsAnalyzed()).isEmpty();
        assertThat(r.regressions()).isEmpty();
        assertThat(r.slowest()).isEmpty();
    }
}
