package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TelemetryTrendAnalyzerTest {

    private final TelemetryTrendAnalyzer analyzer =
            new TelemetryTrendAnalyzer();

    @Test
    void analyze_invalidLast_throws() {
        TelemetryIndex empty = new TelemetryIndex(
                "1.0.0", Instant.now(), Map.of(), List.of());
        assertThatThrownBy(() -> analyzer.analyze(
                empty, 0, 20.0, BaselineStrategy.MEAN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void analyze_negativeThreshold_throws() {
        TelemetryIndex empty = new TelemetryIndex(
                "1.0.0", Instant.now(), Map.of(), List.of());
        assertThatThrownBy(() -> analyzer.analyze(
                empty, 5, -1.0, BaselineStrategy.MEAN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void analyze_restrictsToLastN() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0002", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0003", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0004", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0005", "foo", 100L, 5L));
        TelemetryIndex idx = new TelemetryIndex(
                "1.0.0", Instant.now(), Map.of(), series);
        TrendReport out = analyzer.analyze(
                idx, 3, 20.0, BaselineStrategy.MEAN);
        assertThat(out.epicsAnalyzed()).containsExactly(
                "EPIC-0003", "EPIC-0004", "EPIC-0005");
    }

    @Test
    void analyze_detectsRegressionOnSyntheticFixture() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "foo", 1000L, 5L),
                new EpicSkillP95("EPIC-0002", "foo", 1000L, 5L),
                new EpicSkillP95("EPIC-0003", "foo", 1000L, 5L),
                new EpicSkillP95("EPIC-0004", "foo", 1000L, 5L),
                new EpicSkillP95("EPIC-0005", "foo", 1400L, 5L));
        TelemetryIndex idx = new TelemetryIndex(
                "1.0.0", Instant.now(), Map.of(), series);
        TrendReport out = analyzer.analyze(
                idx, 5, 20.0, BaselineStrategy.MEAN);
        assertThat(out.regressions()).hasSize(1);
        assertThat(out.regressions().get(0).skill()).isEqualTo("foo");
        assertThat(out.regressions().get(0).deltaPct())
                .isEqualTo(40.0);
    }

    @Test
    void analyze_emitsStrategyLabel() {
        TelemetryIndex idx = new TelemetryIndex(
                "1.0.0", Instant.now(), Map.of(),
                List.of(
                    new EpicSkillP95("EPIC-0001", "foo", 100L, 5L),
                    new EpicSkillP95("EPIC-0002", "foo", 100L, 5L)));
        TrendReport out = analyzer.analyze(
                idx, 5, 20.0, BaselineStrategy.MEDIAN);
        assertThat(out.baseline()).isEqualTo("MEDIAN");
    }

    @Test
    void analyze_slowestRanked() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "fast", 100L, 5L),
                new EpicSkillP95("EPIC-0002", "fast", 100L, 5L),
                new EpicSkillP95("EPIC-0001", "slow", 900L, 5L),
                new EpicSkillP95("EPIC-0002", "slow", 900L, 5L));
        TelemetryIndex idx = new TelemetryIndex(
                "1.0.0", Instant.now(), Map.of(), series);
        TrendReport out = analyzer.analyze(
                idx, 5, 20.0, BaselineStrategy.MEDIAN);
        assertThat(out.slowest()).hasSize(2);
        assertThat(out.slowest().get(0).skill()).isEqualTo("slow");
    }

    @Test
    void epicCount_returnsDistinctEpicIds() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "a", 1L, 1L),
                new EpicSkillP95("EPIC-0001", "b", 1L, 1L),
                new EpicSkillP95("EPIC-0002", "a", 1L, 1L));
        TelemetryIndex idx = new TelemetryIndex(
                "1.0.0", Instant.now(), Map.of(), series);
        assertThat(analyzer.epicCount(idx)).isEqualTo(2);
    }
}
