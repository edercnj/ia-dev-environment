package dev.iadev.exception;

/**
 * Thrown when checkpoint state is invalid (missing fields, invalid enum values).
 *
 * <p>Carries the name of the invalid field for precise error reporting.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * if (status == null) {
 *     throw new CheckpointValidationException(
 *         "Missing required field 'status'", "status");
 * }
 * }</pre>
 */
public class CheckpointValidationException extends RuntimeException {

    private final String field;

    /**
     * Creates a checkpoint validation exception for an invalid field.
     *
     * @param message description of the validation failure
     * @param field   the field name that is invalid
     */
    public CheckpointValidationException(
            String message, String field) {
        super(message);
        this.field = field;
    }

    /**
     * Returns the name of the invalid checkpoint field.
     *
     * @return the field name
     */
    public String getField() {
        return field;
    }

    @Override
    public String toString() {
        return "CheckpointValidationException{message='%s', field='%s'}"
                .formatted(getMessage(), field);
    }
}
