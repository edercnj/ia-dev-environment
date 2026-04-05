package dev.iadev.domain.stack;

import dev.iadev.domain.model.ProjectConfig;

import java.util.List;

/**
 * CQRS-related validation rules for event store, schema
 * registry, and dead letter strategy.
 *
 * <p>Extracted from {@link StackValidator} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see StackValidator
 */
public final class StackCqrsValidator {

    private StackCqrsValidator() {
        // utility class
    }

    /**
     * Validates eventStore enum value if set.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    public static List<String> validateEventStore(
            ProjectConfig config) {
        String value = config.architecture().eventStore();
        if (value.isEmpty()) {
            return List.of();
        }
        if (!StackMapping.VALID_EVENT_STORES
                .contains(value)) {
            return List.of(
                    ("Invalid architecture.event_store:"
                            + " '%s'. Valid: %s")
                            .formatted(value,
                                    String.join(", ",
                                            StackMapping
                                                    .VALID_EVENT_STORES)));
        }
        return List.of();
    }

    /**
     * Validates schemaRegistry enum value if set.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    public static List<String> validateSchemaRegistry(
            ProjectConfig config) {
        String value =
                config.architecture().schemaRegistry();
        if (value.isEmpty()) {
            return List.of();
        }
        if (!StackMapping.VALID_SCHEMA_REGISTRIES
                .contains(value)) {
            return List.of(
                    ("Invalid architecture.schema_registry:"
                            + " '%s'. Valid: %s")
                            .formatted(value,
                                    String.join(", ",
                                            StackMapping
                                                    .VALID_SCHEMA_REGISTRIES)));
        }
        return List.of();
    }

    /**
     * Validates deadLetterStrategy enum value if set.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    public static List<String> validateDeadLetterStrategy(
            ProjectConfig config) {
        String value =
                config.architecture().deadLetterStrategy();
        if (value.isEmpty()) {
            return List.of();
        }
        if (!StackMapping.VALID_DEAD_LETTER_STRATEGIES
                .contains(value)) {
            return List.of(
                    ("Invalid architecture.dead_letter_strategy:"
                            + " '%s'. Valid: %s")
                            .formatted(value,
                                    String.join(", ",
                                            StackMapping
                                                    .VALID_DEAD_LETTER_STRATEGIES)));
        }
        return List.of();
    }
}
