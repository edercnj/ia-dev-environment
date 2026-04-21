package dev.iadev.application.lifecycle;

import java.nio.file.Path;

/**
 * Thrown by {@link StatusFieldParser} when a read or write
 * to the {@code **Status:**} field fails irrecoverably —
 * missing file, I/O error, or a partial write that cannot be
 * atomically renamed into place. Carries a fixed error code
 * {@code STATUS_SYNC_FAILED} plus the target path so
 * operators can correlate logs with the affected artifact.
 */
public class StatusSyncException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String CODE = "STATUS_SYNC_FAILED";

    private final Path file;

    public StatusSyncException(Path file, String detail,
            Throwable cause) {
        super(CODE + ": " + detail + " (path="
                + (file == null ? "null" : file.toString())
                + ")", cause);
        this.file = file;
    }

    public StatusSyncException(Path file, String detail) {
        this(file, detail, null);
    }

    public Path file() {
        return file;
    }

    public String code() {
        return CODE;
    }
}
