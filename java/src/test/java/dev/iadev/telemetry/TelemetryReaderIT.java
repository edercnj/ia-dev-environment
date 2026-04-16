package dev.iadev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TelemetryReaderIT {

    @TempDir
    Path tmp;

    @Test
    void count_emptyFile_returnsZero() throws Exception {
        Path file = tmp.resolve("events.ndjson");
        Files.createFile(file);

        long count = TelemetryReader.open(file).count();

        assertThat(count).isZero();
    }

    @Test
    void count_missingFile_returnsZero() {
        Path file = tmp.resolve("missing.ndjson");

        long count = TelemetryReader.open(file).count();

        assertThat(count).isZero();
    }

    @Test
    void count_ignoresBlankLines() throws Exception {
        Path file = tmp.resolve("events.ndjson");
        String line = sample(EventType.SESSION_START)
                .toJsonLine();
        Files.writeString(file,
                line + "\n" + line + "\n",
                StandardCharsets.UTF_8);

        long count = TelemetryReader.open(file).count();

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void stream_roundTripsSingleEvent() throws Exception {
        Path file = tmp.resolve("events.ndjson");
        TelemetryEvent event = sample(EventType.SESSION_START);
        try (TelemetryWriter writer =
                     TelemetryWriter.open(file)) {
            writer.write(event);
        }

        try (Stream<TelemetryEvent> stream =
                     TelemetryReader.open(file).stream()) {
            List<TelemetryEvent> events = stream.toList();
            assertThat(events).containsExactly(event);
        }
    }

    @Test
    void stream_iteratesThousandEventsLazily()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        try (TelemetryWriter writer =
                     TelemetryWriter.open(file)) {
            for (int i = 0; i < 1000; i++) {
                writer.write(sample(EventType.TOOL_CALL));
            }
        }

        TelemetryReader reader = TelemetryReader.open(file);
        long counted;
        try (Stream<TelemetryEvent> stream = reader.stream()) {
            counted = stream.count();
        }
        assertThat(counted).isEqualTo(1000L);
        assertThat(reader.count()).isEqualTo(1000L);
    }

    @Test
    void stream_onMalformedLine_throws() throws Exception {
        Path file = tmp.resolve("events.ndjson");
        Files.writeString(file,
                sample(EventType.SESSION_START).toJsonLine()
                        + "not-json\n",
                StandardCharsets.UTF_8);

        TelemetryReader reader = TelemetryReader.open(file);
        assertThatThrownBy(() -> {
            try (Stream<TelemetryEvent> stream =
                         reader.stream()) {
                stream.toList();
            }
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid NDJSON line");
    }

    @Test
    void streamSkippingInvalid_ignoresMalformedMiddle()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        String valid = sample(EventType.SESSION_START)
                .toJsonLine();
        Files.writeString(file,
                valid + "{\"broken\":" + "\n" + valid
                        + valid,
                StandardCharsets.UTF_8);

        try (Stream<TelemetryEvent> stream =
                     TelemetryReader.open(file)
                             .streamSkippingInvalid()) {
            List<TelemetryEvent> events = stream.toList();
            assertThat(events).hasSize(3);
        }
    }

    @Test
    void stream_missingFile_returnsEmpty() throws Exception {
        Path file = tmp.resolve("missing.ndjson");

        try (Stream<TelemetryEvent> stream =
                     TelemetryReader.open(file).stream()) {
            assertThat(stream.toList()).isEmpty();
        }
    }

    @Test
    void path_returnsConfiguredLocation() {
        Path file = tmp.resolve("events.ndjson");
        assertThat(TelemetryReader.open(file).path())
                .isEqualTo(file);
    }

    @Test
    void open_rejectsNullPath() {
        assertThatThrownBy(() -> TelemetryReader.open(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("path is required");
    }

    private static TelemetryEvent sample(EventType type) {
        return new TelemetryEvent(
                "1.0.0",
                UUID.randomUUID(),
                Instant.parse("2026-04-16T12:34:56.789Z"),
                "claude-sess-abc123",
                null, null, null,
                type,
                null, null, null, null, null, null, null);
    }
}
