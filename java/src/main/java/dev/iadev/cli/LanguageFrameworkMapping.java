package dev.iadev.cli;

import java.util.List;
import java.util.Map;

/**
 * Static mappings between languages, frameworks, build tools, and versions.
 *
 * <p>Used by {@link InteractivePrompter} to filter frameworks and build tools
 * based on the selected language, and to resolve default versions.</p>
 */
public final class LanguageFrameworkMapping {

    private LanguageFrameworkMapping() {
        // utility class
    }

    /** Ordered list of supported languages. */
    public static final List<String> LANGUAGES = List.of(
            "java", "python", "go", "kotlin", "typescript", "rust");

    /** Ordered list of architecture styles. */
    public static final List<String> ARCHITECTURE_STYLES = List.of(
            "microservice", "monolith", "library");

    /** Ordered list of interface types for multi-select. */
    public static final List<String> INTERFACE_TYPES = List.of(
            "rest", "grpc", "graphql", "cli", "events");

    /** Language to compatible frameworks mapping. */
    public static final Map<String, List<String>> FRAMEWORKS = Map.of(
            "java", List.of("spring-boot", "quarkus"),
            "python", List.of("fastapi", "click-cli"),
            "go", List.of("gin"),
            "kotlin", List.of("ktor"),
            "typescript", List.of("nestjs"),
            "rust", List.of("axum"));

    /** Language to compatible build tools mapping. */
    public static final Map<String, List<String>> BUILD_TOOLS = Map.of(
            "java", List.of("maven", "gradle"),
            "python", List.of("pip"),
            "go", List.of("go"),
            "kotlin", List.of("gradle"),
            "typescript", List.of("npm"),
            "rust", List.of("cargo"));

    /** Default language versions. */
    public static final Map<String, String> DEFAULT_VERSIONS = Map.of(
            "java", "21",
            "python", "3.12",
            "go", "1.22",
            "kotlin", "2.0",
            "typescript", "5",
            "rust", "1.78");

    /** Default framework versions. */
    public static final Map<String, String> FRAMEWORK_VERSIONS = Map.ofEntries(
            Map.entry("spring-boot", "3.4"),
            Map.entry("quarkus", "3.17"),
            Map.entry("fastapi", "0.115"),
            Map.entry("click-cli", "8.1"),
            Map.entry("gin", "1.10"),
            Map.entry("ktor", "3.0"),
            Map.entry("nestjs", "10"),
            Map.entry("axum", "0.7"));

    /**
     * Returns the frameworks compatible with the given language.
     *
     * @param language the language name
     * @return list of compatible framework names
     */
    public static List<String> frameworksFor(String language) {
        return FRAMEWORKS.getOrDefault(language, List.of());
    }

    /**
     * Returns the build tools compatible with the given language.
     *
     * @param language the language name
     * @return list of compatible build tool names
     */
    public static List<String> buildToolsFor(String language) {
        return BUILD_TOOLS.getOrDefault(language, List.of());
    }

    /**
     * Returns the default version for a language.
     *
     * @param language the language name
     * @return default version string, or empty string if unknown
     */
    public static String defaultVersionFor(String language) {
        return DEFAULT_VERSIONS.getOrDefault(language, "");
    }

    /**
     * Returns the default version for a framework.
     *
     * @param framework the framework name
     * @return default version string, or empty string if unknown
     */
    public static String frameworkVersionFor(String framework) {
        return FRAMEWORK_VERSIONS.getOrDefault(framework, "");
    }
}
