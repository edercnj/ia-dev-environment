package dev.iadev.exception;

/**
 * Thrown when configuration validation fails during YAML deserialization.
 *
 * <p>Carries context about which field failed validation, the expected type,
 * and the model class where the validation occurred.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new ConfigValidationException("name", "String", "ProjectIdentity");
 * }</pre>
 * </p>
 */
public class ConfigValidationException extends RuntimeException {

    /**
     * Creates an exception for a missing required field.
     *
     * @param field the missing field name
     * @param model the model class name
     */
    public ConfigValidationException(String field, String model) {
        super("Missing required field '%s' in %s".formatted(field, model));
    }

    /**
     * Creates an exception for an invalid field type.
     *
     * @param field the field name
     * @param expectedType the expected type name
     * @param model the model class name
     */
    public ConfigValidationException(
            String field,
            String expectedType,
            String model) {
        super("Invalid type for field '%s' in %s: expected %s"
                .formatted(field, model, expectedType));
    }

    /**
     * Creates an exception with a custom message.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
