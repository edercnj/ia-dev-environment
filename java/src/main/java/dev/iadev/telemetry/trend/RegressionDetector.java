package dev.iadev.telemetry.trend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Detects per-skill P95 regressions across a window of epics.
 *
 * <p>Pure, stateless domain component: {@link #detect(List, List, BaselineStrategy,
 * double)} consumes the per-skill per-epic P95 series (produced by
 * {@link TelemetryIndexBuilder}) and returns the subset whose most-recent
 * epic P95 exceeds the historical baseline (mean or median of the preceding
 * samples) by at least the configured threshold percentage.</p>
 *
 * <p>Ordering contract: the returned list is sorted by {@code deltaPct}
 * descending — the worst regression appears first.</p>
 *
 * <p>Skills with fewer than 2 samples in the window are silently skipped:
 * a regression cannot be defined without at least one historical sample
 * plus the current sample.</p>
 */
public final class RegressionDetector {

    /**
     * Returns all regressions whose delta percentage meets or exceeds
     * {@code thresholdPct}, sorted by delta descending.
     *
     * @param series       the per-skill per-epic P95 samples (flat list;
     *                     rows are regrouped internally)
     * @param epicOrder    the epic IDs in analysis order (oldest first);
     *                     the last entry is the "current" epic
     * @param strategy     the baseline aggregation strategy
     * @param thresholdPct the minimum delta percentage to qualify as a
     *                     regression (inclusive)
     * @return the regression list, deltaPct-desc ordered
     */
    public List<Regression> detect(
            List<EpicSkillP95> series,
            List<String> epicOrder,
            BaselineStrategy strategy,
            double thresholdPct) {
        Objects.requireNonNull(series, "series is required");
        Objects.requireNonNull(epicOrder, "epicOrder is required");
        Objects.requireNonNull(strategy, "strategy is required");

        if (epicOrder.size() < 2) {
            return List.of();
        }
        String currentEpic = epicOrder.get(epicOrder.size() - 1);
        Map<String, Map<String, Long>> skillToEpicP95 =
                groupBySkill(series);

        List<Regression> out = new ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> entry
                : skillToEpicP95.entrySet()) {
            addIfRegression(out, entry, epicOrder,
                    currentEpic, strategy, thresholdPct);
        }
        out.sort(Comparator.comparingDouble(Regression::deltaPct)
                .reversed());
        return out;
    }

    private static void addIfRegression(
            List<Regression> out,
            Map.Entry<String, Map<String, Long>> entry,
            List<String> epicOrder,
            String currentEpic,
            BaselineStrategy strategy,
            double thresholdPct) {
        Map<String, Long> byEpic = entry.getValue();
        Long current = byEpic.get(currentEpic);
        if (current == null) {
            return; // skill absent in current epic — skip
        }
        List<Long> historical = collectHistorical(
                epicOrder, byEpic, currentEpic);
        if (historical.isEmpty()) {
            return;
        }
        long baseline = aggregate(historical, strategy);
        if (baseline <= 0L) {
            return; // divide-by-zero guard
        }
        double delta = ((double) (current - baseline)
                / (double) baseline) * 100.0;
        if (delta >= thresholdPct) {
            out.add(new Regression(
                    entry.getKey(), baseline, current,
                    round2(delta), List.copyOf(epicOrder)));
        }
    }

    private static Map<String, Map<String, Long>> groupBySkill(
            List<EpicSkillP95> series) {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        for (EpicSkillP95 row : series) {
            out.computeIfAbsent(row.skill(),
                    k -> new LinkedHashMap<>())
                    .put(row.epicId(), row.p95Ms());
        }
        return out;
    }

    private static List<Long> collectHistorical(
            List<String> epicOrder,
            Map<String, Long> byEpic,
            String currentEpic) {
        List<Long> out = new ArrayList<>();
        for (String epic : epicOrder) {
            if (epic.equals(currentEpic)) {
                continue;
            }
            Long v = byEpic.get(epic);
            if (v != null) {
                out.add(v);
            }
        }
        return out;
    }

    static long aggregate(
            List<Long> values, BaselineStrategy strategy) {
        return switch (strategy) {
            case MEAN -> mean(values);
            case MEDIAN -> median(values);
        };
    }

    private static long mean(List<Long> values) {
        long total = 0L;
        for (Long v : values) {
            total += v;
        }
        return total / values.size();
    }

    private static long median(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if ((n & 1) == 1) {
            return sorted.get(n / 2);
        }
        long low = sorted.get(n / 2 - 1);
        long high = sorted.get(n / 2);
        return (low + high) / 2L;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
