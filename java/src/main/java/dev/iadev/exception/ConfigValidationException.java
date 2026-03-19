package dev.iadev.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when configuration validation fails during YAML deserialization.
 *
 * <p>Carries context about which field failed validation, the expected type,
 * the model class where the validation occurred, or a list of missing
 * configuration sections.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Missing field in a model
 * throw new ConfigValidationException("name", "ProjectIdentity");
 *
 * // Missing YAML sections
 * throw new ConfigValidationException(
 *     "Missing required sections",
 *     List.of("language", "framework"));
 * }</pre>
 */
public class ConfigValidationException extends RuntimeException {

    private final List<String> missingSections;

    /**
     * Creates an exception for a missing required field.
     *
     * @param field the missing field name
     * @param model the model class name
     */
    public ConfigValidationException(String field, String model) {
        super("Missing required field '%s' in %s"
                .formatted(field, model));
        this.missingSections = List.of();
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
        super("Invalid type for field '%s' in %s: expected %s"
                .formatted(field, model, expectedType));
        this.missingSections = List.of();
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
        this.missingSections = List.of();
    }

    /**
     * Creates an exception for missing configuration sections.
     *
     * <p>The provided list is defensively copied and stored as an
     * unmodifiable list.</p>
     *
     * @param message         description of the validation failure
     * @param missingSections list of missing section names
     */
    public ConfigValidationException(
            String message, List<String> missingSections) {
        super(message);
        this.missingSections = Collections.unmodifiableList(
                List.copyOf(missingSections));
    }

    /**
     * Returns the list of missing configuration sections.
     *
     * <p>Returns an empty list if this exception was not created
     * with the missing sections constructor.</p>
     *
     * @return unmodifiable list of missing section names
     */
    public List<String> getMissingSections() {
        return missingSections;
    }

    @Override
    public String toString() {
        if (missingSections.isEmpty()) {
            return "ConfigValidationException{message='%s'}"
                    .formatted(getMessage());
        }
        return ("ConfigValidationException{message='%s'"
                + ", missingSections=%s}")
                        .formatted(getMessage(), missingSections);
    }
}
