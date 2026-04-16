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

    @Test
    void streamIterator_afterExhaustion_nextThrows()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        Files.writeString(file,
                sample(EventType.SESSION_START).toJsonLine(),
                StandardCharsets.UTF_8);

        try (Stream<TelemetryEvent> stream =
                     TelemetryReader.open(file).stream()) {
            java.util.Iterator<TelemetryEvent> it =
                    stream.iterator();
            assertThat(it.hasNext()).isTrue();
            it.next();
            // Second hasNext() call goes through the exhausted
            // branch (nextEvent == null).
            assertThat(it.hasNext()).isFalse();
            // And idempotent repeat still returns false.
            assertThat(it.hasNext()).isFalse();
            assertThatThrownBy(it::next)
                    .isInstanceOf(
                            java.util.NoSuchElementException
                                    .class);
        }
    }

    @Test
    void streamIterator_hasNextCachesLookahead()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        String line = sample(EventType.SESSION_START)
                .toJsonLine();
        Files.writeString(file,
                line + line,
                StandardCharsets.UTF_8);

        try (Stream<TelemetryEvent> stream =
                     TelemetryReader.open(file).stream()) {
            java.util.Iterator<TelemetryEvent> it =
                    stream.iterator();
            // Two hasNext() calls without next() — second hits the
            // "nextEvent != null" branch.
            assertThat(it.hasNext()).isTrue();
            assertThat(it.hasNext()).isTrue();
            it.next();
            assertThat(it.hasNext()).isTrue();
            it.next();
            assertThat(it.hasNext()).isFalse();
        }
    }

    @Test
    void count_onInvalidPath_raisesUncheckedIo()
            throws Exception {
        // A path whose parent is a regular file triggers
        // Files.lines() to fail (NoSuchFileException) — but we pass
        // Files.exists() first so we need an existing-but-unreadable
        // file. On POSIX we flip permissions; on Windows we skip.
        Path file = tmp.resolve("unreadable.ndjson");
        Files.writeString(file, "ignored");
        boolean posix = file.getFileSystem()
                .supportedFileAttributeViews()
                .contains("posix");
        if (!posix) {
            return;
        }
        Files.setPosixFilePermissions(file,
                java.util.EnumSet.noneOf(
                        java.nio.file.attribute
                                .PosixFilePermission.class));
        try {
            assertThatThrownBy(
                    () -> TelemetryReader.open(file).count())
                    .isInstanceOf(
                            java.io.UncheckedIOException.class)
                    .hasMessageContaining(
                            "failed to count telemetry events");
        } finally {
            Files.setPosixFilePermissions(file,
                    java.util.EnumSet.of(
                            java.nio.file.attribute
                                    .PosixFilePermission
                                    .OWNER_READ,
                            java.nio.file.attribute
                                    .PosixFilePermission
                                    .OWNER_WRITE));
        }
    }

    @Test
    void stream_skipsBlankLinesInMiddle() throws Exception {
        Path file = tmp.resolve("events.ndjson");
        String line = sample(EventType.SESSION_START)
                .toJsonLine();
        Files.writeString(file,
                line + "\n\n  \n" + line,
                StandardCharsets.UTF_8);

        try (Stream<TelemetryEvent> stream =
                     TelemetryReader.open(file).stream()) {
            assertThat(stream.count()).isEqualTo(2L);
        }
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
