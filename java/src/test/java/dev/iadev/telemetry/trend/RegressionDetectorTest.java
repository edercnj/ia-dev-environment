package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegressionDetectorTest {

    private final RegressionDetector detector =
            new RegressionDetector();

    @Test
    void detect_singleEpicWindow_returnsEmpty() {
        List<Regression> out = detector.detect(
                List.of(new EpicSkillP95(
                        "EPIC-0001", "foo", 100L, 10L)),
                List.of("EPIC-0001"),
                BaselineStrategy.MEDIAN, 20.0);
        assertThat(out).isEmpty();
    }

    @Test
    void detect_stableSeries_returnsNoRegressions() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0002", "foo", 105L, 5L),
                new EpicSkillP95("EPIC-0003", "foo", 110L, 5L));
        List<Regression> out = detector.detect(
                series,
                List.of("EPIC-0001", "EPIC-0002", "EPIC-0003"),
                BaselineStrategy.MEDIAN, 20.0);
        assertThat(out).isEmpty();
    }

    @Test
    void detect_regressionAboveThreshold_isReturned() {
        // baseline (median of 100, 100, 100, 100) = 100; current = 140; delta = 40%
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0002", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0003", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0004", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0005", "foo", 140L, 5L));
        List<Regression> out = detector.detect(series,
                List.of("EPIC-0001", "EPIC-0002", "EPIC-0003",
                        "EPIC-0004", "EPIC-0005"),
                BaselineStrategy.MEDIAN, 20.0);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).skill()).isEqualTo("foo");
        assertThat(out.get(0).baselineP95Ms()).isEqualTo(100L);
        assertThat(out.get(0).currentP95Ms()).isEqualTo(140L);
        assertThat(out.get(0).deltaPct()).isEqualTo(40.0);
    }

    @Test
    void detect_medianStableAgainstOutlier_notReported() {
        // Historical samples: [100, 100, 100, 5000] → median = 100; current = 110 → delta 10%
        // Mean would be 1325 → current 110 appears as *negative* delta.
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0002", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0003", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0004", "foo", 5000L, 5L),
                new EpicSkillP95("EPIC-0005", "foo", 110L, 5L));
        List<Regression> out = detector.detect(series,
                List.of("EPIC-0001", "EPIC-0002", "EPIC-0003",
                        "EPIC-0004", "EPIC-0005"),
                BaselineStrategy.MEDIAN, 20.0);
        assertThat(out).isEmpty();
    }

    @Test
    void detect_meanStrategy_detectsRegression() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0002", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0003", "foo", 130L, 5L));
        List<Regression> out = detector.detect(series,
                List.of("EPIC-0001", "EPIC-0002", "EPIC-0003"),
                BaselineStrategy.MEAN, 20.0);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).baselineP95Ms()).isEqualTo(100L);
        assertThat(out.get(0).deltaPct()).isEqualTo(30.0);
    }

    @Test
    void detect_multipleRegressions_sortedByDeltaDesc() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "a", 100L, 5L),
                new EpicSkillP95("EPIC-0002", "a", 100L, 5L),
                new EpicSkillP95("EPIC-0003", "a", 130L, 5L),
                new EpicSkillP95("EPIC-0001", "b", 100L, 5L),
                new EpicSkillP95("EPIC-0002", "b", 100L, 5L),
                new EpicSkillP95("EPIC-0003", "b", 200L, 5L));
        List<Regression> out = detector.detect(series,
                List.of("EPIC-0001", "EPIC-0002", "EPIC-0003"),
                BaselineStrategy.MEAN, 20.0);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).skill()).isEqualTo("b");
        assertThat(out.get(1).skill()).isEqualTo("a");
    }

    @Test
    void detect_skillAbsentInCurrentEpic_isSkipped() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "foo", 100L, 5L),
                new EpicSkillP95("EPIC-0002", "foo", 100L, 5L));
        List<Regression> out = detector.detect(series,
                List.of("EPIC-0001", "EPIC-0002", "EPIC-0003"),
                BaselineStrategy.MEAN, 20.0);
        assertThat(out).isEmpty();
    }

    @Test
    void detect_zeroBaseline_isSkipped() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "foo", 0L, 5L),
                new EpicSkillP95("EPIC-0002", "foo", 50L, 5L));
        List<Regression> out = detector.detect(series,
                List.of("EPIC-0001", "EPIC-0002"),
                BaselineStrategy.MEAN, 20.0);
        assertThat(out).isEmpty();
    }

    @Test
    void aggregate_mean_returnsArithmeticAverage() {
        assertThat(RegressionDetector.aggregate(
                List.of(100L, 200L, 300L), BaselineStrategy.MEAN))
                .isEqualTo(200L);
    }

    @Test
    void aggregate_medianEven_returnsAverageOfTwoMiddles() {
        assertThat(RegressionDetector.aggregate(
                List.of(100L, 200L, 300L, 400L),
                BaselineStrategy.MEDIAN))
                .isEqualTo(250L);
    }

    @Test
    void aggregate_medianOdd_returnsMiddle() {
        assertThat(RegressionDetector.aggregate(
                List.of(100L, 200L, 300L),
                BaselineStrategy.MEDIAN))
                .isEqualTo(200L);
    }
}
