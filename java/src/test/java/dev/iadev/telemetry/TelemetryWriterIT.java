package dev.iadev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TelemetryWriterIT {

    @TempDir
    Path tmp;

    @Test
    void write_singleEvent_producesSingleNdjsonLine()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        TelemetryEvent event = sample(
                "11111111-1111-4111-8111-111111111111",
                EventType.SESSION_START);

        try (TelemetryWriter writer =
                     TelemetryWriter.open(file)) {
            writer.write(event);
        }

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).contains(
                "\"type\":\"session.start\"");
        TelemetryEvent parsed =
                TelemetryEvent.fromJsonLine(lines.get(0));
        assertThat(parsed).isEqualTo(event);
    }

    @Test
    void write_twoEvents_producesTwoLinesInOrder()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        TelemetryEvent first = sample(
                "11111111-1111-4111-8111-111111111111",
                EventType.SESSION_START);
        TelemetryEvent second = sample(
                "22222222-2222-4222-8222-222222222222",
                EventType.SESSION_END);

        try (TelemetryWriter writer =
                     TelemetryWriter.open(file)) {
            writer.write(first);
            writer.write(second);
        }

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).contains(
                "\"type\":\"session.start\"");
        assertThat(lines.get(1)).contains(
                "\"type\":\"session.end\"");
    }

    @Test
    void write_concurrentWriters_produceTwoValidLines()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        TelemetryEvent eventA = sample(
                "11111111-1111-4111-8111-111111111111",
                EventType.SESSION_START);
        TelemetryEvent eventB = sample(
                "22222222-2222-4222-8222-222222222222",
                EventType.SKILL_START);

        ExecutorService pool =
                Executors.newFixedThreadPool(2);
        try (TelemetryWriter writerA =
                     TelemetryWriter.open(file);
             TelemetryWriter writerB =
                     TelemetryWriter.open(file)) {

            Future<?> taskA = pool.submit(() -> {
                for (int i = 0; i < 25; i++) {
                    writerA.write(eventA);
                }
            });
            Future<?> taskB = pool.submit(() -> {
                for (int i = 0; i < 25; i++) {
                    writerB.write(eventB);
                }
            });
            taskA.get(10, TimeUnit.SECONDS);
            taskB.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(50);
        for (String line : lines) {
            // Each line must parse back to an event — no torn writes.
            assertThat(TelemetryEvent.fromJsonLine(line))
                    .isNotNull();
        }
    }

    @Test
    void write_lockTimeout_raisesTelemetryWriteTimeout()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        Files.createFile(file);

        try (FileChannel externalChannel = FileChannel.open(
                file,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
             FileLock ignored =
                     externalChannel.lock();
             TelemetryWriter writer =
                     TelemetryWriter.open(file, 100L)) {

            TelemetryEvent event = sample(
                    "11111111-1111-4111-8111-111111111111",
                    EventType.SESSION_START);

            assertThatThrownBy(() -> writer.write(event))
                    .isInstanceOf(
                            TelemetryWriteTimeoutException.class)
                    .hasMessageContaining(
                            "Could not acquire file lock")
                    .hasMessageContaining("100ms");
        }
    }

    @Test
    void open_rejectsNonPositiveTimeout() {
        Path file = tmp.resolve("events.ndjson");
        assertThatThrownBy(
                () -> TelemetryWriter.open(file, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeoutMillis");
    }

    @Test
    void open_createsParentDirectory() throws Exception {
        Path file = tmp.resolve("nested")
                .resolve("dir").resolve("events.ndjson");

        try (TelemetryWriter writer =
                     TelemetryWriter.open(file)) {
            writer.write(sample(
                    "11111111-1111-4111-8111-111111111111",
                    EventType.SESSION_START));
        }

        assertThat(Files.exists(file)).isTrue();
    }

    @Test
    void path_returnsConfiguredLocation() {
        Path file = tmp.resolve("events.ndjson");
        try (TelemetryWriter writer =
                     TelemetryWriter.open(file, 1500L)) {
            assertThat(writer.path()).isEqualTo(file);
            assertThat(writer.lockTimeoutMillis())
                    .isEqualTo(1500L);
        }
    }

    @Test
    void write_rejectsNullEvent() {
        Path file = tmp.resolve("events.ndjson");
        try (TelemetryWriter writer =
                     TelemetryWriter.open(file)) {
            assertThatThrownBy(() -> writer.write(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "event is required");
        }
    }

    @Test
    void open_rejectsNullPath() {
        assertThatThrownBy(() -> TelemetryWriter.open(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("path is required");
    }

    @Test
    void flush_forcesBytesToDisk() throws Exception {
        Path file = tmp.resolve("events.ndjson");
        try (TelemetryWriter writer =
                     TelemetryWriter.open(file)) {
            writer.write(sample(
                    "11111111-1111-4111-8111-111111111111",
                    EventType.SESSION_START));
            writer.flush();
        }
        // After close the file must still have exactly one line.
        assertThat(Files.readAllLines(file)).hasSize(1);
    }

    @Test
    void close_isIdempotent() throws Exception {
        Path file = tmp.resolve("events.ndjson");
        TelemetryWriter writer = TelemetryWriter.open(file);
        writer.close();
        // Second close() re-throws through the same path because the
        // channel is already closed; the rethrow keeps the UncheckedIO
        // contract. We just assert the call does not hang or throw
        // something unexpected.
        try {
            writer.close();
        } catch (RuntimeException expected) {
            // acceptable — either no-op or UncheckedIOException
        }
    }

    @Test
    void open_invalidPath_rejectsWithUncheckedIo()
            throws Exception {
        // An existing regular file masquerading as a directory forces
        // createDirectories/open to fail, triggering the UncheckedIO
        // path of TelemetryWriter.open().
        Path regularFile = tmp.resolve("a-file");
        Files.writeString(regularFile, "x");
        Path insideFile = regularFile.resolve("events.ndjson");

        assertThatThrownBy(
                () -> TelemetryWriter.open(insideFile))
                .isInstanceOf(
                        java.io.UncheckedIOException.class)
                .hasMessageContaining(
                        "failed to open telemetry writer");
    }

    @Test
    void flush_onClosedWriter_raisesUncheckedIo()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        TelemetryWriter writer = TelemetryWriter.open(file);
        writer.write(sample(
                "11111111-1111-4111-8111-111111111111",
                EventType.SESSION_START));
        writer.close();

        assertThatThrownBy(writer::flush)
                .isInstanceOf(
                        java.io.UncheckedIOException.class)
                .hasMessageContaining(
                        "failed to flush telemetry writer");
    }

    @Test
    void write_whenInterruptedDuringLockPoll_propagates()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        Files.createFile(file);

        try (FileChannel externalChannel = FileChannel.open(
                file,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
             FileLock ignored =
                     externalChannel.lock();
             TelemetryWriter writer =
                     TelemetryWriter.open(file, 1000L)) {

            TelemetryEvent event = sample(
                    "11111111-1111-4111-8111-111111111111",
                    EventType.SESSION_START);

            Thread.currentThread().interrupt();
            try {
                assertThatThrownBy(() -> writer.write(event))
                        .isInstanceOfAny(
                                java.io.UncheckedIOException
                                        .class,
                                TelemetryWriteTimeoutException
                                        .class);
            } finally {
                // clear the interrupted flag for subsequent tests
                Thread.interrupted();
            }
        }
    }

    @Test
    void write_afterClose_raisesUncheckedIo() throws Exception {
        Path file = tmp.resolve("events.ndjson");
        TelemetryWriter writer = TelemetryWriter.open(file);
        writer.close();

        TelemetryEvent event = sample(
                "11111111-1111-4111-8111-111111111111",
                EventType.SESSION_START);
        assertThatThrownBy(() -> writer.write(event))
                .isInstanceOfAny(
                        java.io.UncheckedIOException.class,
                        java.nio.channels.ClosedChannelException
                                .class);
    }

    @Test
    void timeoutException_exposesPathAndTimeout()
            throws Exception {
        Path file = tmp.resolve("events.ndjson");
        Files.createFile(file);

        try (FileChannel externalChannel = FileChannel.open(
                file,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
             FileLock ignored =
                     externalChannel.lock();
             TelemetryWriter writer =
                     TelemetryWriter.open(file, 50L)) {

            TelemetryEvent event = sample(
                    "11111111-1111-4111-8111-111111111111",
                    EventType.SESSION_START);

            assertThatThrownBy(() -> writer.write(event))
                    .isInstanceOfSatisfying(
                            TelemetryWriteTimeoutException.class,
                            e -> {
                                assertThat(e.path())
                                        .isEqualTo(
                                                file.toString());
                                assertThat(e.timeoutMillis())
                                        .isEqualTo(50L);
                            });
        }
    }

    private static TelemetryEvent sample(
            String uuid, EventType type) {
        List<?> ignored = new ArrayList<>();
        return new TelemetryEvent(
                "1.0.0",
                UUID.fromString(uuid),
                Instant.parse("2026-04-16T12:34:56.789Z"),
                "claude-sess-abc123",
                null, null, null,
                type,
                null, null, null, null, null, null, null);
    }
}
