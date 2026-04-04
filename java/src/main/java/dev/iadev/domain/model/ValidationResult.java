package dev.iadev.domain.model;

import java.util.List;

/**
 * Immutable result of configuration validation.
 *
 * <p>Returned by
 * {@link dev.iadev.domain.port.input.ValidateConfigUseCase}
 * after validating a project configuration.</p>
 *
 * @param valid whether the configuration is valid
 * @param errors list of validation error messages (immutable)
 */
public record ValidationResult(
        boolean valid,
        List<String> errors) {

    /**
     * Compact constructor enforcing immutability.
     */
    public ValidationResult {
        errors = List.copyOf(errors);
    }
}
