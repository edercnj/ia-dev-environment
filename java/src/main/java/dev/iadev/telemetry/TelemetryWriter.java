package dev.iadev.telemetry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Append-only NDJSON writer for {@link TelemetryEvent} streams, safe to share
 * across threads and across OS processes.
 *
 * <p>Each call to {@link #write(TelemetryEvent)} acquires an exclusive
 * {@link FileLock} on the target file via {@link FileChannel#tryLock()} with
 * bounded polling. If the lock cannot be acquired within
 * {@link #lockTimeoutMillis()} (default {@value #DEFAULT_LOCK_TIMEOUT_MILLIS}
 * ms) the writer raises {@link TelemetryWriteTimeoutException} rather than
 * blocking indefinitely — telemetry emission is opt-in and MUST NOT stall the
 * producer (Rule 04 of EPIC-0040: hooks fail-open).</p>
 *
 * <p>The underlying {@link FileChannel} is created with {@code CREATE} +
 * {@code WRITE} + {@code APPEND} so the OS guarantees that writes land at
 * end-of-file even when multiple processes contend (RULE-002: NDJSON
 * append-only). Callers SHOULD use try-with-resources so the channel is
 * released promptly.</p>
 */
public final class TelemetryWriter implements AutoCloseable {

    /** Default lock acquisition budget (2s), per story contract. */
    public static final long DEFAULT_LOCK_TIMEOUT_MILLIS = 2000L;

    private static final long POLL_INTERVAL_MILLIS = 25L;

    private final Path path;
    private final FileChannel channel;
    private final long lockTimeoutMillis;
    private final Object writeMonitor = new Object();

    private TelemetryWriter(
            Path path, FileChannel channel,
            long lockTimeoutMillis) {
        this.path = path;
        this.channel = channel;
        this.lockTimeoutMillis = lockTimeoutMillis;
    }

    /**
     * Opens a writer with the default lock timeout
     * ({@value #DEFAULT_LOCK_TIMEOUT_MILLIS} ms).
     *
     * @param path the target NDJSON file (parent dirs created on demand)
     * @return a fresh, open writer
     * @throws UncheckedIOException if the file cannot be opened
     */
    public static TelemetryWriter open(Path path) {
        return open(path, DEFAULT_LOCK_TIMEOUT_MILLIS);
    }

    /**
     * Opens a writer with an explicit lock timeout.
     *
     * @param path          the target NDJSON file
     * @param timeoutMillis the lock acquisition budget, must be positive
     * @return a fresh, open writer
     */
    public static TelemetryWriter open(
            Path path, long timeoutMillis) {
        Objects.requireNonNull(path, "path is required");
        if (timeoutMillis <= 0L) {
            throw new IllegalArgumentException(
                    "timeoutMillis must be > 0: "
                            + timeoutMillis);
        }
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            FileChannel channel = FileChannel.open(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND);
            return new TelemetryWriter(
                    path, channel, timeoutMillis);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to open telemetry writer: " + path,
                    e);
        }
    }

    /**
     * Serializes the event and appends a single NDJSON line to the file.
     *
     * @param event the event to persist; must not be null
     * @throws TelemetryWriteTimeoutException if the file lock cannot be
     *                                        acquired within the budget
     * @throws UncheckedIOException           if the underlying write fails
     */
    public void write(TelemetryEvent event) {
        Objects.requireNonNull(event, "event is required");
        byte[] payload = event.toJsonLine()
                .getBytes(StandardCharsets.UTF_8);
        synchronized (writeMonitor) {
            try (FileLock lock = acquireLock()) {
                ByteBuffer buffer = ByteBuffer.wrap(payload);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                // Explicit release so the compiler does not warn about the
                // lock being unused; try-with-resources will no-op. We do
                // NOT fsync on every write — the story contract requires
                // write p99 < 5ms per event (EPIC-0040 DoD) and fsync
                // adds ~3ms per call on typical APFS/ext4. Close() will
                // flush, and hooks write ndjson atomically via
                // FileChannel APPEND + line-sized buffers (partial lines
                // cannot occur).
                lock.release();
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "failed to write telemetry event to "
                                + path, e);
            }
        }
    }

    /**
     * Forces any buffered bytes to disk via {@link FileChannel#force(boolean)}.
     *
     * <p>Callers who need durability guarantees after a specific event
     * (e.g. pre-shutdown flush) can invoke this explicitly. The per-write
     * path intentionally skips {@code force} to keep p99 latency under
     * the 5ms budget declared by EPIC-0040.</p>
     */
    public void flush() {
        try {
            channel.force(false);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to flush telemetry writer: "
                            + path, e);
        }
    }

    /** @return the configured lock timeout in milliseconds */
    public long lockTimeoutMillis() {
        return lockTimeoutMillis;
    }

    /** @return the target NDJSON file path */
    public Path path() {
        return path;
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to close telemetry writer: " + path,
                    e);
        }
    }

    private FileLock acquireLock() throws IOException {
        long deadline = System.currentTimeMillis()
                + lockTimeoutMillis;
        while (true) {
            FileLock lock = tryAcquireOnce();
            if (lock != null) {
                return lock;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new TelemetryWriteTimeoutException(
                        path.toString(), lockTimeoutMillis);
            }
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(
                        "interrupted while waiting for "
                                + "telemetry lock on " + path,
                        e);
            }
        }
    }

    private FileLock tryAcquireOnce() throws IOException {
        try {
            return channel.tryLock();
        } catch (OverlappingFileLockException e) {
            // Another thread/channel in THIS JVM already holds the lock on
            // the same region — treat as contended and retry.
            return null;
        } catch (ClosedChannelException e) {
            throw e;
        }
    }
}
