package dev.iadev.telemetry.analyze;

import dev.iadev.telemetry.TelemetryEvent;
import dev.iadev.telemetry.TelemetryReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reads and aggregates NDJSON telemetry for a list of epics.
 *
 * <p>Extracted from {@link TelemetryAnalyzeCli} (audit finding M-004) so
 * the CLI can focus on argument parsing and output destination while this
 * class owns the filesystem access + streaming + aggregation pipeline.</p>
 *
 * <p>The aggregator streams events per epic so the full event set is never
 * buffered in memory. Streams are explicitly closed in a {@code finally}
 * block; fail-open semantics are applied to any stream-close failure so a
 * broken reader never prevents the successful aggregation from being
 * returned.</p>
 */
public final class EpicTelemetryAggregator {

    /**
     * Streams events for each epic, unions them lazily, and returns the
     * aggregated {@link AnalysisReport}.
     *
     * @param epicIds       epic identifiers (e.g., {@code EPIC-0040})
     * @param base          base directory containing {@code epic-XXXX/}
     *                      children
     * @param sinceInstant  optional lower-bound filter on event timestamp
     * @return the aggregated report across all supplied epics
     * @throws NoTelemetryException   when any epic directory lacks
     *                                 {@code events.ndjson}
     * @throws IllegalArgumentException on NDJSON parse failure (propagated
     *                                   from {@link TelemetryReader})
     */
    public AnalysisReport aggregate(
            List<String> epicIds, Path base,
            Optional<Instant> sinceInstant) {
        TelemetryAggregator aggregator = new TelemetryAggregator();
        List<Stream<TelemetryEvent>> opened =
                new ArrayList<>(epicIds.size());
        try {
            Stream<TelemetryEvent> unified = Stream.empty();
            for (String epicId : epicIds) {
                Path events = eventsPath(base, epicId);
                if (!Files.exists(events)) {
                    throw new NoTelemetryException(
                            "Epic " + epicId
                                    + " has no telemetry data at "
                                    + events);
                }
                Stream<TelemetryEvent> perEpic =
                        TelemetryReader.open(events)
                                .streamSkippingInvalid()
                                .filter(e -> sinceInstant.isEmpty()
                                        || !e.timestamp()
                                                .isBefore(
                                                        sinceInstant
                                                                .get()));
                opened.add(perEpic);
                unified = Stream.concat(unified, perEpic);
            }
            return aggregator.aggregate(unified, epicIds);
        } finally {
            closeAll(opened);
        }
    }

    /**
     * Returns the canonical {@code events.ndjson} path for a given epic.
     *
     * @param base    base plans directory
     * @param epicId  epic identifier (e.g., {@code EPIC-0040})
     * @return the resolved NDJSON path
     */
    public static Path eventsPath(Path base, String epicId) {
        return base
                .resolve("epic-" + extractEpicSuffix(epicId))
                .resolve("telemetry")
                .resolve("events.ndjson");
    }

    /**
     * Extracts the numeric suffix of an epic identifier.
     *
     * @param epicId  identifier such as {@code EPIC-0040}
     * @return the suffix ({@code 0040}) or the original string when no
     *         {@code EPIC-} prefix is present
     */
    public static String extractEpicSuffix(String epicId) {
        String prefix = "EPIC-";
        if (epicId.startsWith(prefix)) {
            return epicId.substring(prefix.length());
        }
        return epicId;
    }

    private static void closeAll(List<Stream<TelemetryEvent>> streams) {
        for (Stream<TelemetryEvent> s : streams) {
            try {
                s.close();
            } catch (RuntimeException ignored) {
                // Fail-open: closing a telemetry stream must never abort
                // the caller.
            }
        }
    }

    /**
     * Thrown when a requested epic has no NDJSON file on disk.
     */
    public static final class NoTelemetryException
            extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public NoTelemetryException(String message) {
            super(message);
        }
    }
}
