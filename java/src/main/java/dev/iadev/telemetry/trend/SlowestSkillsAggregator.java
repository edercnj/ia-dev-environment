package dev.iadev.telemetry.trend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ranks skills by mean-of-P95 across the analyzed epic window.
 *
 * <p>Pure, stateless domain component: {@link #rank(List, int)} consumes the
 * per-skill per-epic P95 series produced by {@link TelemetryIndexBuilder} and
 * returns up to {@code topN} entries sorted by {@link SlowSkill#avgP95Ms()}
 * descending.</p>
 */
public final class SlowestSkillsAggregator {

    /**
     * Computes the top-N slowest skills.
     *
     * @param series  the per-skill per-epic P95 samples
     * @param topN    the maximum number of rows returned (0 or negative
     *                values are treated as "no limit")
     * @return the ranked {@link SlowSkill} list, avgP95 descending
     */
    public List<SlowSkill> rank(List<EpicSkillP95> series, int topN) {
        Objects.requireNonNull(series, "series is required");

        Map<String, SkillAccumulator> bySkill = new LinkedHashMap<>();
        for (EpicSkillP95 row : series) {
            bySkill.computeIfAbsent(row.skill(),
                    k -> new SkillAccumulator())
                    .add(row.p95Ms(), row.invocations());
        }

        List<SlowSkill> out = new ArrayList<>(bySkill.size());
        for (Map.Entry<String, SkillAccumulator> entry
                : bySkill.entrySet()) {
            SkillAccumulator acc = entry.getValue();
            long avg = acc.sampleCount == 0
                    ? 0L
                    : acc.totalP95 / acc.sampleCount;
            out.add(new SlowSkill(
                    entry.getKey(), avg, acc.invocations));
        }
        out.sort(Comparator.comparingLong(SlowSkill::avgP95Ms)
                .reversed());
        if (topN > 0 && out.size() > topN) {
            return out.subList(0, topN);
        }
        return out;
    }

    private static final class SkillAccumulator {
        long totalP95;
        int sampleCount;
        long invocations;

        void add(long p95, long inv) {
            totalP95 += p95;
            sampleCount++;
            invocations += inv;
        }
    }
}
