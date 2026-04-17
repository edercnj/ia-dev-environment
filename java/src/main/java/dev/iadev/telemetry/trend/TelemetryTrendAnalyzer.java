package dev.iadev.telemetry.trend;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Application-layer orchestrator that turns a raw {@link TelemetryIndex} into
 * a {@link TrendReport}.
 *
 * <p>Wires together {@link RegressionDetector} and
 * {@link SlowestSkillsAggregator} and restricts the input series to the most
 * recent {@code lastN} epics (natural ordering by epic ID — epic IDs are
 * zero-padded decimal strings like {@code epic-0001}, so lexicographic
 * {@link String} ordering is equivalent to numeric ordering).</p>
 *
 * <p>Pure and stateless: no I/O, no hidden state. The caller feeds the index
 * in and receives the report.</p>
 */
public final class TelemetryTrendAnalyzer {

    private static final int TOP_N = 10;

    /**
     * Returns the trend report for the given index.
     *
     * @param index        the source index (produced by
     *                     {@link TelemetryIndexBuilder})
     * @param lastN        the window size; the most-recent {@code lastN}
     *                     epics are considered
     * @param thresholdPct the regression threshold percentage (must be &gt;= 0)
     * @param strategy     the baseline aggregation strategy
     * @return the trend report
     * @throws IllegalArgumentException if {@code lastN &lt; 1} or
     *                                  {@code thresholdPct &lt; 0}
     */
    public TrendReport analyze(
            TelemetryIndex index,
            int lastN,
            double thresholdPct,
            BaselineStrategy strategy) {
        Objects.requireNonNull(index, "index is required");
        Objects.requireNonNull(strategy, "strategy is required");
        if (lastN < 1) {
            throw new IllegalArgumentException(
                    "--last must be >= 1, got " + lastN);
        }
        if (thresholdPct < 0) {
            throw new IllegalArgumentException(
                    "--threshold-pct must be >= 0");
        }

        List<String> allEpics = collectEpicsInOrder(index);
        List<String> window = tail(allEpics, lastN);
        List<EpicSkillP95> windowed = filterByEpics(
                index.series(), window);

        RegressionDetector detector = new RegressionDetector();
        List<Regression> regressions = detector.detect(
                windowed, window, strategy, thresholdPct);

        SlowestSkillsAggregator slowestAgg =
                new SlowestSkillsAggregator();
        List<SlowSkill> slowest = slowestAgg.rank(windowed, TOP_N);

        // Cap regressions at top-N too.
        List<Regression> topRegressions = regressions.size() > TOP_N
                ? regressions.subList(0, TOP_N)
                : regressions;

        return new TrendReport(
                Instant.now(),
                window,
                thresholdPct,
                strategy.name(),
                topRegressions,
                slowest);
    }

    private static List<String> collectEpicsInOrder(
            TelemetryIndex index) {
        Set<String> seen = new LinkedHashSet<>();
        for (EpicSkillP95 row : index.series()) {
            seen.add(row.epicId());
        }
        List<String> out = new ArrayList<>(seen);
        out.sort(Comparator.naturalOrder());
        return out;
    }

    private static List<String> tail(List<String> source, int n) {
        if (source.size() <= n) {
            return source;
        }
        return new ArrayList<>(
                source.subList(source.size() - n, source.size()));
    }

    private static List<EpicSkillP95> filterByEpics(
            List<EpicSkillP95> series, List<String> window) {
        if (window.size() == 0) {
            return List.of();
        }
        Set<String> allowed = new LinkedHashSet<>(window);
        List<EpicSkillP95> out = new ArrayList<>();
        for (EpicSkillP95 row : series) {
            if (allowed.contains(row.epicId())) {
                out.add(row);
            }
        }
        return out;
    }

    /**
     * @return the natural epic count of the given {@link TelemetryIndex}
     *         (distinct {@code epicId}s in {@link TelemetryIndex#series()})
     */
    public int epicCount(TelemetryIndex index) {
        Objects.requireNonNull(index, "index is required");
        return collectEpicsInOrder(index).size();
    }
}
