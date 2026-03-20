package dev.iadev.domain.stack;

import dev.iadev.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Version-specific validation rules for language-framework
 * compatibility.
 *
 * <p>Extracted from {@link StackValidator} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see StackValidator
 */
public final class StackVersionValidator {

    private StackVersionValidator() {
        // utility class
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
     * Checks Java 17+ requirement for Quarkus 3+ and
     * Spring Boot 3+.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    static List<String> checkJavaFrameworkVersion(
            ProjectConfig config) {
        String fw = config.framework().name();
        String lang = config.language().name();
        if (!"java".equals(lang)
                || (!"quarkus".equals(fw)
                && !"spring-boot".equals(fw))) {
            return List.of();
        }
        Optional<Integer> fwMajor =
                StackValidator.extractMajor(
                        config.framework().version());
        Optional<Integer> langMajor =
                StackValidator.extractMajor(
                        config.language().version());
        if (langMajor.isEmpty()) {
            return List.of();
        }
        return checkJava17Requirement(
                fw, fwMajor, langMajor.get());
    }

    /**
     * Checks Django 5+ requires Python 3.10+.
     *
     * @param config the project configuration
     * @return list of error messages
     */
    static List<String> checkDjangoPythonVersion(
            ProjectConfig config) {
        if (!"django".equals(config.framework().name())) {
            return List.of();
        }
        Optional<Integer> fwMajor =
                StackValidator.extractMajor(
                        config.framework().version());
        if (fwMajor.isEmpty()
                || fwMajor.get()
                < StackValidator.FRAMEWORK_VERSION_5) {
            return List.of();
        }
        Optional<Integer> pyMajor =
                StackValidator.extractMajor(
                        config.language().version());
        Optional<Integer> pyMinor =
                StackValidator.extractMinor(
                        config.language().version());
        if (pyMajor.isEmpty() || pyMinor.isEmpty()) {
            return List.of();
        }
        if (pyMajor.get() < StackValidator.PYTHON_3_MAJOR
                || (pyMajor.get()
                == StackValidator.PYTHON_3_MAJOR
                && pyMinor.get()
                < StackValidator.PYTHON_310_MINOR)) {
            return List.of(
                    "Django 5.x requires Python 3.10+,"
                            + " got Python %s"
                            .formatted(
                                    config.language()
                                            .version()));
        }
        return List.of();
    }

    private static List<String> checkJava17Requirement(
            String fw,
            Optional<Integer> fwMajor,
            int langMajor) {
        boolean needsCheck = fwMajor.isPresent()
                && fwMajor.get()
                >= StackValidator.FRAMEWORK_VERSION_3
                && ("quarkus".equals(fw)
                || "spring-boot".equals(fw));
        if (needsCheck
                && langMajor
                < StackValidator.JAVA_17_MINIMUM) {
            String fwTitle = fw.substring(0, 1).toUpperCase()
                    + fw.substring(1);
            return List.of(
                    "%s %d.x requires Java 17+, got Java %d"
                            .formatted(fwTitle,
                                    fwMajor.get(),
                                    langMajor));
        }
        return List.of();
    }
}
