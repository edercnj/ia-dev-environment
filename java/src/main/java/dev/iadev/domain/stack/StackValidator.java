package dev.iadev.domain.stack;

import dev.iadev.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validates language-framework compatibility, version
 * constraints, native build support, interface types, and
 * architecture styles.
 *
 * <p>Version-specific checks are delegated to
 * {@link StackVersionValidator}.</p>
 *
 * @see StackVersionValidator
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

    /**
     * Runs all validations and returns aggregated errors.
     *
     * @param config the project configuration to validate
     * @return list of error messages (empty means valid)
     */
    public static List<String> validateStack(
            ProjectConfig config) {
        List<String> errors = new ArrayList<>();
        errors.addAll(validateLanguageFramework(config));
        errors.addAll(validateVersionRequirements(config));
        errors.addAll(validateNativeBuild(config));
        errors.addAll(validateInterfaceTypes(config));
        errors.addAll(validateArchitectureStyle(config));
        return errors;
    }

    /**
     * Validates framework-language compatibility.
     *
     * @param config the project configuration
     * @return list of error messages
     */
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

    /**
     * Delegates to {@link StackVersionValidator}.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    public static List<String> validateVersionRequirements(
            ProjectConfig config) {
        return StackVersionValidator
                .validateVersionRequirements(config);
    }

    /**
     * Validates that native build is only for supported
     * frameworks.
     *
     * @param config the project configuration
     * @return list of error messages
     */
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

    /**
     * Validates that all interface types are known.
     *
     * @param config the project configuration
     * @return list of error messages
     */
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

    /**
     * Validates that the architecture style is known.
     *
     * @param config the project configuration
     * @return list of error messages
     */
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

    /**
     * Extracts the major version number from a version
     * string.
     *
     * @param version the version string
     * @return the major version, or empty
     */
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

    /**
     * Extracts the minor version number from a version
     * string.
     *
     * @param version the version string
     * @return the minor version, or empty
     */
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

    /**
     * Delegates to {@link StackVersionValidator}.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    static List<String> checkJavaFrameworkVersion(
            ProjectConfig config) {
        return StackVersionValidator
                .checkJavaFrameworkVersion(config);
    }

    /**
     * Delegates to {@link StackVersionValidator}.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    static List<String> checkDjangoPythonVersion(
            ProjectConfig config) {
        return StackVersionValidator
                .checkDjangoPythonVersion(config);
    }
}
