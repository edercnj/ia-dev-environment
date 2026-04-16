package dev.iadev.release.abort;

/**
 * Thrown by {@link CleanupPort} operations when an individual
 * cleanup step fails. The {@link AbortOrchestrator} catches
 * these per-step and logs a warning (warn-only policy per
 * story-0039-0010 §3.2).
 */
public final class CleanupException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public CleanupException(
            String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CleanupException(
            String errorCode,
            String message,
            Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
