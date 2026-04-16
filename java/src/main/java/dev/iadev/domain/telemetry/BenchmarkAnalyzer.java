package dev.iadev.domain.telemetry;

import dev.iadev.domain.model.PhaseMetric;
import dev.iadev.domain.model.PhaseOutcome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Pure domain service that computes top-3 slowest phases
 * for the current release vs. the mean of the last N
 * historical releases (story-0039-0012 §3.3).
 *
 * <p>Receives {@link PhaseMetric} records via a
 * {@link Stream} (DIP — the adapter layer is responsible
 * for loading the JSONL). Zero framework imports, zero
 * File I/O — enforces Rule 04 (domain purity).
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Group metrics by {@code releaseVersion},
 *       preserving the encounter order of the stream
 *       (required so the "last N" window is
 *       deterministic).</li>
 *   <li>If fewer than {@link #MIN_HISTORY} historical
 *       releases exist, return
 *       {@link BenchmarkResult.InsufficientHistory}.</li>
 *   <li>Restrict the historical window to the
 *       <strong>last {@link #MIN_HISTORY}</strong>
 *       historical releases (by encounter order of the
 *       input stream) — older releases are discarded to
 *       keep the moving window bounded.</li>
 *   <li>For each phase in the current release, compute
 *       the mean duration across the windowed historical
 *       releases that contain the same phase.</li>
 *   <li>Return the top-3 phases ranked by this release's
 *       {@code durationSec} descending, with the delta
 *       percentage vs. the historical mean.</li>
 * </ol>
 *
 * <p>Only phases with outcome {@link PhaseOutcome#SUCCESS}
 * contribute to the mean — FAILED and SKIPPED are
 * ignored to prevent outliers from biasing the
 * benchmark.
 */
public final class BenchmarkAnalyzer {

    /** Minimum history size required for a benchmark. */
    public static final int MIN_HISTORY = 5;

    private static final int TOP_N = 3;

    /**
     * Analyzes the supplied metrics.
     *
     * @param metrics all metrics (current + historical);
     *                must not be {@code null}
     * @param currentReleaseVersion semver of the current
     *                              release; must not be
     *                              {@code null} or blank
     * @return either top-3 benchmark entries or an
     *         insufficient-history sentinel
     */
    public BenchmarkResult analyze(
            Stream<PhaseMetric> metrics,
            String currentReleaseVersion) {
        validate(metrics, currentReleaseVersion);

        Map<String, List<PhaseMetric>> byRelease =
                groupByRelease(metrics);
        List<PhaseMetric> currentMetrics = byRelease.remove(
                currentReleaseVersion);
        if (currentMetrics == null) {
            currentMetrics = List.of();
        }

        Set<String> historicalReleases = byRelease.keySet();
        if (historicalReleases.size() < MIN_HISTORY) {
            return new BenchmarkResult.InsufficientHistory(
                    historicalReleases.size());
        }

        Collection<List<PhaseMetric>> window =
                lastNReleases(byRelease, MIN_HISTORY);
        Map<String, Double> phaseMeans = computePhaseMeans(
                window);
        List<PhaseBenchmark> ranked = rankTop(
                currentMetrics, phaseMeans);
        return new BenchmarkResult.TopPhases(ranked);
    }

    private void validate(
            Stream<PhaseMetric> metrics,
            String currentReleaseVersion) {
        if (metrics == null) {
            throw new IllegalArgumentException(
                    "metrics must not be null");
        }
        if (currentReleaseVersion == null
                || currentReleaseVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "currentReleaseVersion must not be "
                            + "null or blank");
        }
    }

    private Map<String, List<PhaseMetric>> groupByRelease(
            Stream<PhaseMetric> metrics) {
        Map<String, List<PhaseMetric>> byRelease =
                new LinkedHashMap<>();
        metrics.forEach(m -> byRelease
                .computeIfAbsent(
                        m.releaseVersion(),
                        k -> new ArrayList<>())
                .add(m));
        return byRelease;
    }

    private Collection<List<PhaseMetric>> lastNReleases(
            Map<String, List<PhaseMetric>> byRelease,
            int n) {
        List<List<PhaseMetric>> releases =
                new ArrayList<>(byRelease.values());
        int size = releases.size();
        if (size <= n) {
            return releases;
        }
        return releases.subList(size - n, size);
    }

    private Map<String, Double> computePhaseMeans(
            Collection<List<PhaseMetric>> historical) {
        Map<String, Long> sum = new HashMap<>();
        Map<String, Integer> count = new HashMap<>();
        for (List<PhaseMetric> release : historical) {
            Set<String> seen = new HashSet<>();
            for (PhaseMetric m : release) {
                if (m.outcome() != PhaseOutcome.SUCCESS) {
                    continue;
                }
                if (!seen.add(m.phase())) {
                    continue;
                }
                sum.merge(
                        m.phase(),
                        m.durationSec(),
                        Long::sum);
                count.merge(m.phase(), 1, Integer::sum);
            }
        }
        Map<String, Double> means = new HashMap<>();
        for (Map.Entry<String, Long> e : sum.entrySet()) {
            int c = count.get(e.getKey());
            means.put(e.getKey(),
                    (double) e.getValue() / c);
        }
        return means;
    }

    private List<PhaseBenchmark> rankTop(
            List<PhaseMetric> current,
            Map<String, Double> means) {
        List<PhaseBenchmark> entries = new ArrayList<>();
        for (PhaseMetric m : current) {
            if (m.outcome() != PhaseOutcome.SUCCESS) {
                continue;
            }
            long mean = Math.round(
                    means.getOrDefault(m.phase(), 0.0));
            int delta = computeDelta(
                    m.durationSec(), mean);
            entries.add(new PhaseBenchmark(
                    m.phase(),
                    m.durationSec(),
                    mean,
                    delta));
        }
        entries.sort(Comparator
                .comparingLong(PhaseBenchmark::durationSec)
                .reversed());
        return entries.stream()
                .limit(TOP_N)
                .toList();
    }

    private int computeDelta(long actual, long mean) {
        if (mean == 0) {
            return 0;
        }
        double raw = ((double) actual - mean) / mean * 100.0;
        return (int) Math.round(raw);
    }
}
