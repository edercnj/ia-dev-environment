package dev.iadev.domain.stack;

import dev.iadev.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validates language-framework compatibility, version constraints,
 * native build support, interface types, and architecture styles.
 *
 * <p>All methods are static and the class is stateless.
 * Zero external framework dependencies (RULE-007).</p>
 */
public final class StackValidator {

    /** Minimum Java version for Quarkus 3+ and Spring Boot 3+. */
    public static final int JAVA_17_MINIMUM = 17;

    /** Minimum Python minor version for FastAPI 0.100+ (3.10). */
    public static final int PYTHON_310_MINOR = 10;

    /** Framework major version threshold for Java 17 requirement. */
    public static final int FRAMEWORK_VERSION_3 = 3;

    /** Django major version threshold for Python 3.10 requirement. */
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
    public static List<String> validateStack(ProjectConfig config) {
        List<String> errors = new ArrayList<>();
        errors.addAll(validateLanguageFramework(config));
        errors.addAll(validateVersionRequirements(config));
        errors.addAll(validateNativeBuild(config));
        errors.addAll(validateInterfaceTypes(config));
        errors.addAll(validateArchitectureStyle(config));
        return errors;
    }

    /**
     * Validates that the framework is compatible with the language.
     *
     * @param config the project configuration
     * @return list of error messages (empty if compatible)
     */
    public static List<String> validateLanguageFramework(
            ProjectConfig config) {
        String frameworkName = config.framework().name();
        String languageName = config.language().name();
        List<String> validLanguages =
                StackMapping.FRAMEWORK_LANGUAGE_RULES.get(frameworkName);
        if (validLanguages == null) {
            return List.of();
        }
        if (!validLanguages.contains(languageName)) {
            String expected = String.join(", ", validLanguages);
            return List.of(
                    "Framework '" + frameworkName + "' requires language '"
                            + expected + "', got '" + languageName + "'");
        }
        return List.of();
    }

    /**
     * Validates version requirements across all known rules.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    public static List<String> validateVersionRequirements(
            ProjectConfig config) {
        List<String> errors = new ArrayList<>();
        errors.addAll(checkJavaFrameworkVersion(config));
        errors.addAll(checkDjangoPythonVersion(config));
        return errors;
    }

    /**
     * Checks Java 17+ requirement for Quarkus 3+ and Spring Boot 3+.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    static List<String> checkJavaFrameworkVersion(ProjectConfig config) {
        String fw = config.framework().name();
        String lang = config.language().name();
        if (!"java".equals(lang)
                || (!"quarkus".equals(fw) && !"spring-boot".equals(fw))) {
            return List.of();
        }
        Optional<Integer> fwMajor =
                extractMajor(config.framework().version());
        Optional<Integer> langMajor =
                extractMajor(config.language().version());
        if (langMajor.isEmpty()) {
            return List.of();
        }
        return checkJava17Requirement(fw, fwMajor, langMajor.get());
    }

    /**
     * Checks Django 5+ requires Python 3.10+.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    static List<String> checkDjangoPythonVersion(ProjectConfig config) {
        if (!"django".equals(config.framework().name())) {
            return List.of();
        }
        Optional<Integer> fwMajor =
                extractMajor(config.framework().version());
        if (fwMajor.isEmpty() || fwMajor.get() < FRAMEWORK_VERSION_5) {
            return List.of();
        }
        Optional<Integer> pyMajor =
                extractMajor(config.language().version());
        Optional<Integer> pyMinor =
                extractMinor(config.language().version());
        if (pyMajor.isEmpty() || pyMinor.isEmpty()) {
            return List.of();
        }
        if (pyMajor.get() < PYTHON_3_MAJOR
                || (pyMajor.get() == PYTHON_3_MAJOR
                && pyMinor.get() < PYTHON_310_MINOR)) {
            return List.of(
                    "Django 5.x requires Python 3.10+, got Python "
                            + config.language().version());
        }
        return List.of();
    }

    /**
     * Validates that native build is only enabled for supported frameworks.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    static List<String> validateNativeBuild(ProjectConfig config) {
        if (!config.framework().nativeBuild()) {
            return List.of();
        }
        String fw = config.framework().name();
        if (!StackMapping.NATIVE_SUPPORTED_FRAMEWORKS.contains(fw)) {
            return List.of(
                    "Native build is not supported for framework '" + fw + "'");
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
            if (!StackMapping.VALID_INTERFACE_TYPES.contains(iface.type())) {
                errors.add(
                        "Invalid interface type: '" + iface.type() + "'. "
                                + "Valid: " + String.join(", ",
                                StackMapping.VALID_INTERFACE_TYPES));
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
        if (!StackMapping.VALID_ARCHITECTURE_STYLES.contains(style)) {
            return List.of(
                    "Invalid architecture style: '" + style + "'. "
                            + "Valid: " + String.join(", ",
                            StackMapping.VALID_ARCHITECTURE_STYLES));
        }
        return List.of();
    }

    /**
     * Extracts the major version number from a version string.
     *
     * @param version the version string (e.g. "21", "3.4.1")
     * @return the major version, or empty if unparseable
     */
    public static Optional<Integer> extractMajor(String version) {
        if (version == null || version.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = version.split("\\.");
        try {
            return Optional.of(Integer.parseInt(parts[0]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Extracts the minor version number from a version string.
     *
     * @param version the version string (e.g. "3.10.1")
     * @return the minor version, or empty if unparseable
     */
    public static Optional<Integer> extractMinor(String version) {
        if (version == null || version.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = version.split("\\.");
        if (parts.length < 2) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(parts[1]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static List<String> checkJava17Requirement(
            String fw,
            Optional<Integer> fwMajor,
            int langMajor) {
        boolean needsCheck = fwMajor.isPresent()
                && fwMajor.get() >= FRAMEWORK_VERSION_3
                && ("quarkus".equals(fw) || "spring-boot".equals(fw));
        if (needsCheck && langMajor < JAVA_17_MINIMUM) {
            String fwTitle = fw.substring(0, 1).toUpperCase()
                    + fw.substring(1);
            return List.of(
                    fwTitle + " " + fwMajor.get()
                            + ".x requires Java 17+, got Java " + langMajor);
        }
        return List.of();
    }
}
