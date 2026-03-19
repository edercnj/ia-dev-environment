package dev.iadev.exception;

/**
 * Thrown for CLI usage errors such as invalid arguments or conflicting flags.
 *
 * <p>Carries an {@code errorCode} that maps to process exit codes:
 * <ul>
 *   <li>{@code 1} — usage error (invalid arguments, conflicting flags)</li>
 *   <li>{@code 2} — execution error (failure during generation)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new CliException("Invalid argument: --output", 1);
 * }</pre>
 */
public class CliException extends RuntimeException {

    private final int errorCode;

    /**
     * Creates a CLI exception with the given message and exit code.
     *
     * @param message   description of the CLI error
     * @param errorCode process exit code (1 = usage, 2 = execution)
     */
    public CliException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Returns the process exit code associated with this error.
     *
     * @return the exit code (1 = usage error, 2 = execution error)
     */
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "CliException{message='%s', errorCode=%d}"
                .formatted(getMessage(), errorCode);
    }
}
