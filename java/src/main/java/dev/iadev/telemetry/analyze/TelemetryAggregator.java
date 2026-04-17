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

        AggregationState state = new AggregationState();
        for (TelemetryEvent event :
                (Iterable<TelemetryEvent>) events::iterator) {
            state.totalEvents++;
            dispatchEvent(event, state);
        }
        synthesizePendingTimeline(state);
        return buildReport(state, epics);
    }

    private static AnalysisReport buildReport(
            AggregationState state, List<String> epics) {
        List<PhaseTimeline> sortedTimeline =
                new ArrayList<>(state.timeline);
        sortedTimeline.sort(Comparator
                .comparing(PhaseTimeline::startInstant));
        return new AnalysisReport(
                Instant.now(),
                epics,
                state.totalEvents,
                state.totalDuration,
                toStats(state.skillDurations, state.skillEpics),
                toStats(state.phaseDurations, state.phaseEpics),
                toStats(state.toolDurations, state.toolEpics),
                sortedTimeline,
                List.of());
    }

    private static void dispatchEvent(
            TelemetryEvent event, AggregationState state) {
        EventType type = event.type();
        if (type == EventType.SKILL_END) {
            accumulateSkill(event, state);
        } else if (type == EventType.PHASE_END) {
            accumulatePhaseEnd(event, state);
        } else if (type == EventType.PHASE_START) {
            accumulatePhaseStart(event, state);
        } else if (type == EventType.TOOL_RESULT) {
            accumulateTool(event, state);
        }
    }

    private static void accumulateSkill(
            TelemetryEvent event, AggregationState state) {
        if (event.skill() == null || event.durationMs() == null) {
            return;
        }
        long d = event.durationMs();
        state.totalDuration += d;
        state.skillDurations
                .computeIfAbsent(event.skill(),
                        k -> new ArrayList<>())
                .add(d);
        accumulateEpic(state.skillEpics, event.skill(),
                event.epicId());
    }

    private static void accumulatePhaseEnd(
            TelemetryEvent event, AggregationState state) {
        if (event.skill() == null || event.phase() == null
                || event.durationMs() == null) {
            return;
        }
        long d = event.durationMs();
        String key = event.skill() + "/" + event.phase();
        state.phaseDurations
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(d);
        accumulateEpic(state.phaseEpics, key, event.epicId());
        Instant start = state.phaseStarts.remove(key);
        if (start == null) {
            start = event.timestamp();
        }
        state.timeline.add(new PhaseTimeline(
                event.skill(), event.phase(),
                start, event.timestamp(), d));
    }

    private static void accumulatePhaseStart(
            TelemetryEvent event, AggregationState state) {
        if (event.skill() == null || event.phase() == null) {
            return;
        }
        String key = event.skill() + "/" + event.phase();
        state.phaseStarts.put(key, event.timestamp());
    }

    private static void accumulateTool(
            TelemetryEvent event, AggregationState state) {
        if (event.tool() == null || event.durationMs() == null) {
            return;
        }
        long d = event.durationMs();
        state.toolDurations
                .computeIfAbsent(event.tool(),
                        k -> new ArrayList<>())
                .add(d);
        accumulateEpic(state.toolEpics, event.tool(),
                event.epicId());
    }

    /**
     * Synthesizes zero-duration timeline rows for
     * {@code phase.start} events that had no matching
     * {@code phase.end} — the Gantt still shows the
     * occurrence without inflating totals.
     */
    private static void synthesizePendingTimeline(
            AggregationState state) {
        for (Map.Entry<String, Instant> pending
                : state.phaseStarts.entrySet()) {
            String key = pending.getKey();
            int sep = key.indexOf('/');
            String skill = key.substring(0, sep);
            String phase = key.substring(sep + 1);
            Instant ts = pending.getValue();
            state.timeline.add(new PhaseTimeline(
                    skill, phase, ts, ts, 0L));
        }
    }

    /** Mutable per-aggregation state. */
    private static final class AggregationState {
        final Map<String, List<Long>> skillDurations =
                new LinkedHashMap<>();
        final Map<String, List<Long>> phaseDurations =
                new LinkedHashMap<>();
        final Map<String, List<Long>> toolDurations =
                new LinkedHashMap<>();
        final Map<String, List<String>> skillEpics =
                new HashMap<>();
        final Map<String, List<String>> phaseEpics =
                new HashMap<>();
        final Map<String, List<String>> toolEpics =
                new HashMap<>();
        final Map<String, Instant> phaseStarts =
                new HashMap<>();
        final List<PhaseTimeline> timeline = new ArrayList<>();
        long totalEvents;
        long totalDuration;
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
