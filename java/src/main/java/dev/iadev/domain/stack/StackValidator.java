package dev.iadev.domain.stack;

import dev.iadev.domain.model.BranchingModel;
import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validates stack compatibility. Delegates version checks
 * to {@link StackVersionValidator} and CQRS validation
 * to {@link StackCqrsValidator}.
 */
public final class StackValidator {

    /** Minimum Java version for Quarkus 3+. */
    public static final int JAVA_17_MINIMUM = 17;

    /** Minimum Python minor version for FastAPI. */
    public static final int PYTHON_310_MINOR = 10;

    /** Framework major version threshold. */
    public static final int FRAMEWORK_VERSION_3 = 3;

    /** Django major version threshold. */
    public static final int FRAMEWORK_VERSION_5 = 5;

    /** Python major version constant. */
    public static final int PYTHON_3_MAJOR = 3;

    private StackValidator() {
        // utility class
    }

    /** Runs all validations and returns aggregated errors. */
    public static List<String> validateStack(
            ProjectConfig config) {
        List<String> errors = new ArrayList<>();
        errors.addAll(validateLanguageFramework(config));
        errors.addAll(validateVersionRequirements(config));
        errors.addAll(validateNativeBuild(config));
        errors.addAll(validateInterfaceTypes(config));
        errors.addAll(validateArchitectureStyle(config));
        errors.addAll(validateEventStore(config));
        errors.addAll(validateSchemaRegistry(config));
        errors.addAll(validateDeadLetterStrategy(config));
        errors.addAll(validatePlatforms(config));
        errors.addAll(validateBranchingModel(config));
        return errors;
    }

    /** Validates framework-language compatibility. */
    public static List<String> validateLanguageFramework(
            ProjectConfig config) {
        String frameworkName = config.framework().name();
        String languageName = config.language().name();
        List<String> validLanguages =
                StackMapping.FRAMEWORK_LANGUAGE_RULES
                        .get(frameworkName);
        if (validLanguages == null) {
            return List.of();
        }
        if (!validLanguages.contains(languageName)) {
            String expected = String.join(
                    ", ", validLanguages);
            return List.of(
                    ("Framework '%s' requires language"
                            + " '%s', got '%s'")
                            .formatted(frameworkName,
                                    expected,
                                    languageName));
        }
        return List.of();
    }

    /** Delegates to {@link StackVersionValidator}. */
    public static List<String> validateVersionRequirements(
            ProjectConfig config) {
        return StackVersionValidator
                .validateVersionRequirements(config);
    }

    /** Validates native build framework support. */
    static List<String> validateNativeBuild(
            ProjectConfig config) {
        if (!config.framework().nativeBuild()) {
            return List.of();
        }
        String fw = config.framework().name();
        if (!StackMapping.NATIVE_SUPPORTED_FRAMEWORKS
                .contains(fw)) {
            return List.of(
                    ("Native build is not supported"
                            + " for framework '%s'")
                            .formatted(fw));
        }
        return List.of();
    }

    /** Validates that all interface types are known. */
    public static List<String> validateInterfaceTypes(
            ProjectConfig config) {
        List<String> errors = new ArrayList<>();
        for (var iface : config.interfaces()) {
            if (!StackMapping.VALID_INTERFACE_TYPES
                    .contains(iface.type())) {
                errors.add(
                        ("Invalid interface type: '%s'."
                                + " Valid: %s")
                                .formatted(iface.type(),
                                        String.join(", ",
                                                StackMapping
                                                        .VALID_INTERFACE_TYPES)));
            }
        }
        return errors;
    }

    /** Validates that the architecture style is known. */
    public static List<String> validateArchitectureStyle(
            ProjectConfig config) {
        String style = config.architecture().style();
        if (!StackMapping.VALID_ARCHITECTURE_STYLES
                .contains(style)) {
            return List.of(
                    ("Invalid architecture style: '%s'."
                            + " Valid: %s")
                            .formatted(style,
                                    String.join(", ",
                                            StackMapping
                                                    .VALID_ARCHITECTURE_STYLES)));
        }
        return List.of();
    }

    /** Delegates to {@link StackCqrsValidator}. */
    public static List<String> validateEventStore(
            ProjectConfig config) {
        return StackCqrsValidator
                .validateEventStore(config);
    }

    /** Delegates to {@link StackCqrsValidator}. */
    public static List<String> validateSchemaRegistry(
            ProjectConfig config) {
        return StackCqrsValidator
                .validateSchemaRegistry(config);
    }

    /** Delegates to {@link StackCqrsValidator}. */
    public static List<String> validateDeadLetterStrategy(
            ProjectConfig config) {
        return StackCqrsValidator
                .validateDeadLetterStrategy(config);
    }

    /**
     * Validates that configured platforms are
     * user-selectable (not SHARED).
     */
    public static List<String> validatePlatforms(
            ProjectConfig config) {
        Set<Platform> userSelectable =
                Platform.allUserSelectable();
        List<String> errors = new ArrayList<>();
        for (Platform p : config.platforms()) {
            if (!userSelectable.contains(p)) {
                errors.add(
                        ("Invalid platform value:"
                                + " '%s' in YAML config."
                                + " Valid values:"
                                + " claude-code, all")
                                .formatted(p.cliName()));
            }
        }
        return errors;
    }

    /**
     * Validates the branching model is not null.
     *
     * <p>Since {@link ProjectConfig} defaults to GITFLOW in
     * the compact constructor, this validation catches only
     * programmatic misuse where the field is somehow null
     * after construction.</p>
     */
    public static List<String> validateBranchingModel(
            ProjectConfig config) {
        if (config.branchingModel() == null) {
            return List.of(
                    "Invalid branching-model: 'null'."
                            + " Accepted values:"
                            + " gitflow, trunk");
        }
        return List.of();
    }

    /** Extracts the major version from a version string. */
    public static Optional<Integer> extractMajor(
            String version) {
        if (version == null || version.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = version.split("\\.");
        try {
            return Optional.of(
                    Integer.parseInt(parts[0]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Extracts the minor version from a version string. */
    public static Optional<Integer> extractMinor(
            String version) {
        if (version == null || version.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = version.split("\\.");
        if (parts.length < 2) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    Integer.parseInt(parts[1]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Delegates to {@link StackVersionValidator}. */
    static List<String> checkJavaFrameworkVersion(
            ProjectConfig config) {
        return StackVersionValidator
                .checkJavaFrameworkVersion(config);
    }

    /** Delegates to {@link StackVersionValidator}. */
    static List<String> checkDjangoPythonVersion(
            ProjectConfig config) {
        return StackVersionValidator
                .checkDjangoPythonVersion(config);
    }
}
