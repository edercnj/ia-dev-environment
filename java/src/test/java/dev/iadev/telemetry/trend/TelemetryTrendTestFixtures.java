package dev.iadev.telemetry.trend;

import dev.iadev.telemetry.EventStatus;
import dev.iadev.telemetry.EventType;
import dev.iadev.telemetry.TelemetryEvent;
import dev.iadev.telemetry.TelemetryWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Shared test fixtures for the {@code dev.iadev.telemetry.trend} package.
 * Centralizes NDJSON fixture writing and single-event construction so
 * per-test classes are not forced to duplicate boilerplate (QA-15 remediation).
 */
final class TelemetryTrendTestFixtures {

    /** Canonical fixture timestamp — anchors all per-event timestamps. */
    static final Instant FIXTURE_ANCHOR =
            Instant.parse("2026-04-16T12:00:00Z");

    private TelemetryTrendTestFixtures() {
    }

    /**
     * Writes {@code count} {@code skill.end} events for {@code skill} under
     * {@code epicId}, each with a monotonically increasing timestamp and
     * a deterministic duration in the range
     * {@code [baseDurationMs, baseDurationMs + count)}.
     *
     * @param base            the directory that would normally be {@code plans}
     * @param epicId          the epic ID (e.g. {@code EPIC-0040})
     * @param skill           the skill name to stamp into every event
     * @param count           how many events to write
     * @param baseDurationMs  the minimum duration; successive events add 1 ms
     * @return the path to the written NDJSON file
     * @throws Exception if I/O fails (propagated — tests halt)
     */
    static Path writeFixture(
            Path base, String epicId, String skill,
            int count, long baseDurationMs) throws Exception {
        String suffix = epicId.substring("EPIC-".length());
        Path events = base.resolve("epic-" + suffix)
                .resolve("telemetry")
                .resolve("events.ndjson");
        Files.createDirectories(events.getParent());
        try (TelemetryWriter writer = TelemetryWriter.open(events)) {
            for (int i = 0; i < count; i++) {
                writer.write(skillEnd(skill,
                        baseDurationMs + i, epicId,
                        FIXTURE_ANCHOR.plusSeconds(i)));
            }
        }
        return events;
    }

    /**
     * Builds a single {@code skill.end} {@link TelemetryEvent} with the given
     * skill, duration, epic, and timestamp. All other fields are filled with
     * test-safe defaults.
     *
     * @param skill      the skill name
     * @param durationMs the duration in milliseconds (must be &gt;= 0)
     * @param epicId     the epic ID
     * @param ts         the event timestamp
     * @return the populated event
     */
    static TelemetryEvent skillEnd(
            String skill, long durationMs, String epicId,
            Instant ts) {
        return new TelemetryEvent(
                "1.0.0",
                UUID.randomUUID(),
                ts,
                "session-abc",
                epicId,
                null,
                null,
                EventType.SKILL_END,
                skill,
                null,
                null,
                durationMs,
                EventStatus.OK,
                null,
                Map.of());
    }
}
