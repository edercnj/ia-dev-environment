package dev.iadev.telemetry.analyze;

import dev.iadev.telemetry.EventType;
import dev.iadev.telemetry.TelemetryEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Aggregates a stream of {@link TelemetryEvent} into an
 * {@link AnalysisReport} with per-skill, per-phase, and per-tool statistics.
 *
 * <p>The aggregator is a pure, stateless function over the input stream:
 * {@link #aggregate(Stream, List)} drains the stream, computes counts and
 * duration percentiles, and returns an immutable report. Percentiles are
 * Nearest-Rank (ceil) — integer-stable and conservative for small sample
 * sizes, which is desired for workflow telemetry where a single slow
 * invocation genuinely sets the tail.</p>
 *
 * <p>Only events with the relevant {@code *.end} / {@code tool.result} type
 * contribute to statistics: {@code skill.end} for skill stats,
 * {@code phase.end} for phase stats (keyed by {@code skill + "/" + phase}),
 * {@code tool.result} for tool stats. All other event types are counted
 * toward {@link AnalysisReport#totalEvents()} but do not change the
 * aggregates.</p>
 *
 * <p>The timeline is produced from ordered pairs of {@code phase.start} /
 * {@code phase.end}; start events without a matching end produce a
 * zero-duration row so the Gantt still shows the occurrence.</p>
 */
public final class TelemetryAggregator {

    /**
     * Drains {@code events} and returns the aggregated report.
     *
     * @param events the input event stream; caller manages closing
     * @param epics  the epic IDs under analysis (ordered) for the report
     *               metadata
     * @return the immutable analysis report
     */
    public AnalysisReport aggregate(
            Stream<TelemetryEvent> events, List<String> epics) {
        Objects.requireNonNull(events, "events is required");
        Objects.requireNonNull(epics, "epics is required");

        Map<String, List<Long>> skillDurations =
                new LinkedHashMap<>();
        Map<String, List<Long>> phaseDurations =
                new LinkedHashMap<>();
        Map<String, List<Long>> toolDurations =
                new LinkedHashMap<>();
        Map<String, List<String>> skillEpics = new HashMap<>();
        Map<String, List<String>> phaseEpics = new HashMap<>();
        Map<String, List<String>> toolEpics = new HashMap<>();
        Map<String, Instant> phaseStarts = new HashMap<>();
        List<PhaseTimeline> timeline = new ArrayList<>();
        long totalEvents = 0L;
        long totalDuration = 0L;

        for (TelemetryEvent event :
                (Iterable<TelemetryEvent>) events::iterator) {
            totalEvents++;
            EventType type = event.type();
            if (type == EventType.SKILL_END
                    && event.skill() != null
                    && event.durationMs() != null) {
                long d = event.durationMs();
                totalDuration += d;
                skillDurations
                        .computeIfAbsent(event.skill(),
                                k -> new ArrayList<>())
                        .add(d);
                accumulateEpic(skillEpics, event.skill(),
                        event.epicId());
            } else if (type == EventType.PHASE_END
                    && event.skill() != null
                    && event.phase() != null
                    && event.durationMs() != null) {
                long d = event.durationMs();
                String key = event.skill() + "/" + event.phase();
                phaseDurations
                        .computeIfAbsent(key,
                                k -> new ArrayList<>())
                        .add(d);
                accumulateEpic(phaseEpics, key, event.epicId());
                Instant start = phaseStarts.remove(key);
                if (start == null) {
                    start = event.timestamp();
                }
                timeline.add(new PhaseTimeline(
                        event.skill(), event.phase(),
                        start, event.timestamp(), d));
            } else if (type == EventType.PHASE_START
                    && event.skill() != null
                    && event.phase() != null) {
                String key = event.skill() + "/" + event.phase();
                phaseStarts.put(key, event.timestamp());
            } else if (type == EventType.TOOL_RESULT
                    && event.tool() != null
                    && event.durationMs() != null) {
                long d = event.durationMs();
                toolDurations
                        .computeIfAbsent(event.tool(),
                                k -> new ArrayList<>())
                        .add(d);
                accumulateEpic(toolEpics, event.tool(),
                        event.epicId());
            }
        }

        // Unmatched phase.starts → synthesize zero-duration rows so the
        // Gantt shows the occurrence without inflating duration totals.
        for (Map.Entry<String, Instant> pending
                : phaseStarts.entrySet()) {
            String key = pending.getKey();
            int sep = key.indexOf('/');
            String skill = key.substring(0, sep);
            String phase = key.substring(sep + 1);
            Instant ts = pending.getValue();
            timeline.add(new PhaseTimeline(skill, phase, ts, ts, 0L));
        }

        List<Stat> skillStats = toStats(skillDurations, skillEpics);
        List<Stat> phaseStats = toStats(phaseDurations, phaseEpics);
        List<Stat> toolStats = toStats(toolDurations, toolEpics);

        List<PhaseTimeline> sortedTimeline = new ArrayList<>(timeline);
        sortedTimeline.sort(Comparator
                .comparing(PhaseTimeline::startInstant));

        return new AnalysisReport(
                Instant.now(),
                epics,
                totalEvents,
                totalDuration,
                skillStats,
                phaseStats,
                toolStats,
                sortedTimeline,
                List.of());
    }

    private static void accumulateEpic(
            Map<String, List<String>> bucket,
            String key,
            String epicId) {
        if (epicId == null) {
            return;
        }
        List<String> list = bucket
                .computeIfAbsent(key, k -> new ArrayList<>());
        if (!list.contains(epicId)) {
            list.add(epicId);
        }
    }

    private static List<Stat> toStats(
            Map<String, List<Long>> durations,
            Map<String, List<String>> epics) {
        List<Stat> out = new ArrayList<>(durations.size());
        for (Map.Entry<String, List<Long>> entry
                : durations.entrySet()) {
            List<Long> samples = entry.getValue();
            Collections.sort(samples);
            long total = 0L;
            for (Long v : samples) {
                total += v;
            }
            int n = samples.size();
            long avg = total / n;
            long p50 = percentile(samples, 50);
            long p95 = percentile(samples, 95);
            List<String> epicIds = epics.getOrDefault(
                    entry.getKey(), List.of());
            out.add(new Stat(
                    entry.getKey(), n, total, avg, p50, p95, epicIds));
        }
        out.sort(Comparator.comparingLong(Stat::totalMs).reversed());
        return out;
    }

    /**
     * Nearest-Rank percentile (ceil). Input list MUST be sorted ascending.
     */
    static long percentile(List<Long> sortedSamples, int p) {
        int n = sortedSamples.size();
        if (n == 0) {
            return 0L;
        }
        int rank = (int) Math.ceil((p / 100.0) * n);
        if (rank < 1) {
            rank = 1;
        }
        if (rank > n) {
            rank = n;
        }
        return sortedSamples.get(rank - 1);
    }
}
