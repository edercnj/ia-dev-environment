package dev.iadev.release.state;

/**
 * Thrown by {@link StateFileValidator} when a release state
 * instance violates the {@code schemaVersion: 2} contract.
 *
 * <p>The {@link #errorCode()} maps to the exit-code catalog
 * declared in story-0039-0002 §5.3:
 * <ul>
 *   <li>{@code STATE_SCHEMA_VERSION} — {@code schemaVersion != 2}</li>
 *   <li>{@code STATE_INVALID_ENUM} — unknown {@code waitingFor}
 *       value (reserved for future JSON-level validation)</li>
 *   <li>{@code STATE_INVALID_ACTION} — {@code nextActions[].command}
 *       violates {@code ^/[a-z\-]+}</li>
 * </ul>
 */
public final class StateFileValidationException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public StateFileValidationException(
            String errorCode, String message) {
        super(formatMessage(errorCode, message));
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }

    private static String formatMessage(
            String errorCode, String message) {
        return errorCode + ": " + message;
    }
}
