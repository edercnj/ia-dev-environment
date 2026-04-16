package dev.iadev.telemetry;

/**
 * Raised by {@link TelemetryWriter} when it cannot acquire the
 * {@code FileChannel} lock on the NDJSON file within the configured timeout.
 *
 * <p>This is a checked-like condition surfaced as a runtime exception because
 * telemetry emission is opt-in and best-effort — callers should catch and
 * log, not treat a transient lock contention as a structural failure. The
 * exception carries the target path and observed timeout so diagnostics do
 * not need to inspect logs to reconstruct context.</p>
 */
public class TelemetryWriteTimeoutException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String path;
    private final long timeoutMillis;

    /**
     * @param path          the NDJSON file the writer was targeting
     * @param timeoutMillis the timeout budget that elapsed without acquiring
     *                      the lock
     */
    public TelemetryWriteTimeoutException(
            String path, long timeoutMillis) {
        super("Could not acquire file lock in "
                + timeoutMillis + "ms: " + path);
        this.path = path;
        this.timeoutMillis = timeoutMillis;
    }

    /** @return the target NDJSON file path */
    public String path() {
        return path;
    }

    /** @return the timeout budget in milliseconds */
    public long timeoutMillis() {
        return timeoutMillis;
    }
}
