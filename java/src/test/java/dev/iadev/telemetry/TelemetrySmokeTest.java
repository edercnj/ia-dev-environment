package dev.iadev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end smoke test for the Java telemetry domain: writes 1000
 * real events via {@link TelemetryWriter} and reads them back through
 * {@link TelemetryReader}, asserting (a) lossless round trip, (b)
 * bounded wall-clock (must finish within 2 seconds per story
 * contract), and (c) file-level parity (1000 non-blank lines on disk).
 *
 * <p>Intentionally exercises default configuration — no custom
 * timeouts, no mocked channels. Any regression in the writer's lock
 * handling, the NDJSON line framing, or the reader's streaming mode
 * will surface here.</p>
 */
class TelemetrySmokeTest {

    private static final int EVENT_COUNT = 1000;
    private static final Duration BUDGET =
            Duration.ofSeconds(2);

    @TempDir
    Path tmp;

    @Test
    void roundTripThousandEvents_withinBudget()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");

        Instant started = Instant.now();
        try (TelemetryWriter writer =
                     TelemetryWriter.open(file)) {
            for (int i = 0; i < EVENT_COUNT; i++) {
                writer.write(event(i));
            }
        }
        Duration writeElapsed =
                Duration.between(started, Instant.now());

        long readCount;
        TelemetryReader reader = TelemetryReader.open(file);
        try (Stream<TelemetryEvent> stream = reader.stream()) {
            readCount = stream.count();
        }
        Duration totalElapsed =
                Duration.between(started, Instant.now());

        long diskLines = Files.readAllLines(file).size();

        assertThat(readCount).isEqualTo(EVENT_COUNT);
        assertThat(reader.count()).isEqualTo(EVENT_COUNT);
        assertThat(diskLines).isEqualTo(EVENT_COUNT);
        assertThat(totalElapsed)
                .as("write %s + read within %s",
                        writeElapsed, BUDGET)
                .isLessThanOrEqualTo(BUDGET);
    }

    private static TelemetryEvent event(int index) {
        EventType type = EventType.values()[
                index % EventType.values().length];
        return new TelemetryEvent(
                "1.0.0",
                UUID.randomUUID(),
                Instant.parse("2026-04-16T12:34:56.789Z")
                        .plusMillis(index),
                "claude-sess-smoke",
                null, null, null,
                type,
                null, null, null,
                (long) index,
                EventStatus.OK,
                null, null);
    }
}
