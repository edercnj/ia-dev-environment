package dev.iadev.exception;

import java.util.List;

/**
 * Infrastructure-layer configuration validation exception.
 *
 * <p>This class extends the domain-layer
 * {@link dev.iadev.domain.model.ConfigValidationException} to
 * maintain backward compatibility with existing infrastructure
 * and adapter code that catches or throws this exception.</p>
 *
 * <p>New domain code should use
 * {@link dev.iadev.domain.model.ConfigValidationException}
 * directly. This subclass exists solely as a migration bridge
 * (RULE-001: domain isolation).</p>
 *
 * @see dev.iadev.domain.model.ConfigValidationException
 */
public class ConfigValidationException
        extends dev.iadev.domain.model.ConfigValidationException {

    /**
     * Creates an exception for a missing required field.
     *
     * @param field the missing field name
     * @param model the model class name
     */
    public ConfigValidationException(
            String field, String model) {
        super(field, model);
    }

    /**
     * Creates an exception for an invalid field type.
     *
     * @param field        the field name
     * @param expectedType the expected type name
     * @param model        the model class name
     */
    public ConfigValidationException(
            String field,
            String expectedType,
            String model) {
        super(field, expectedType, model);
    }

    /**
     * Creates an exception with a custom message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public ConfigValidationException(
            String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception for missing configuration sections.
     *
     * @param message         description of the validation failure
     * @param missingSections list of missing section names
     */
    public ConfigValidationException(
            String message, List<String> missingSections) {
        super(message, missingSections);
    }
}
