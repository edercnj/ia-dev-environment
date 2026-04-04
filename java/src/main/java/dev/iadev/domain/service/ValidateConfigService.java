package dev.iadev.domain.service;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ValidationResult;
import dev.iadev.domain.port.input.ValidateConfigUseCase;
import dev.iadev.domain.port.output.StackProfileRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Domain service that validates project configuration.
 *
 * <p>Implements {@link ValidateConfigUseCase} by applying
 * domain validation rules to a {@link ProjectConfig}. Checks
 * required fields, cross-field consistency, and structural
 * integrity of the configuration.</p>
 *
 * <p>Dependencies are exclusively Output Ports injected via
 * constructor. Contains no infrastructure dependencies.</p>
 *
 * @see ValidateConfigUseCase
 * @see StackProfileRepository
 */
public final class ValidateConfigService
        implements ValidateConfigUseCase {

    private final StackProfileRepository profileRepository;

    /**
     * Creates a new ValidateConfigService.
     *
     * @param profileRepository the repository for validating
     *                          stack profiles (must not be null)
     * @throws NullPointerException if profileRepository is null
     */
    public ValidateConfigService(
            StackProfileRepository profileRepository) {
        this.profileRepository = Objects.requireNonNull(
                profileRepository,
                "profileRepository must not be null");
    }

    /**
     * Validates the given project configuration.
     *
     * <p>Performs structural validation checking for required
     * fields: project identity, language, framework, and at
     * least one interface definition.</p>
     *
     * @param config the project configuration to validate
     *               (must not be null)
     * @return the validation result with validity flag and errors
     * @throws NullPointerException if config is null
     */
    @Override
    public ValidationResult validate(ProjectConfig config) {
        Objects.requireNonNull(config,
                "config must not be null");

        List<String> errors = new ArrayList<>();
        validateRequiredSections(config, errors);

        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, errors);
    }

    private void validateRequiredSections(
            ProjectConfig config, List<String> errors) {
        if (config.project() == null) {
            errors.add(
                    "Missing required section: project");
        }
        if (config.language() == null) {
            errors.add(
                    "Missing required section: language");
        }
        if (config.framework() == null) {
            errors.add(
                    "Missing required section: framework");
        }
        if (config.architecture() == null) {
            errors.add(
                    "Missing required section: architecture");
        } else {
            validateArchitectureCrossFields(
                    config.architecture(), errors);
        }
        if (config.interfaces() == null
                || config.interfaces().isEmpty()) {
            errors.add("At least one interface definition "
                    + "is required");
        }
    }

    private void validateArchitectureCrossFields(
            dev.iadev.domain.model.ArchitectureConfig arch,
            List<String> errors) {
        if (arch.validateWithArchUnit()
                && (arch.basePackage() == null
                    || arch.basePackage().isBlank())) {
            errors.add("architecture.basePackage is required"
                    + " when validateWithArchUnit is true");
        }
    }
}
