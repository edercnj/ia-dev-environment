package dev.iadev.cli;

import java.util.List;
import java.util.Map;

/**
 * Static mappings between languages, frameworks, build tools, and versions.
 *
 * <p>Used by {@link InteractivePrompter} to filter frameworks and build tools
 * based on the selected language, and to resolve default versions.</p>
 *
 * <p><b>EPIC-0048 / v4.0.0:</b> the generator is restricted to Java only
 * (see ADR-0048-A). All derived maps include only Java entries.</p>
 */
public final class LanguageFrameworkMapping {

    private LanguageFrameworkMapping() {
        // utility class
    }

    /** Ordered list of supported languages. Java-only since v4.0.0. */
    public static final List<String> LANGUAGES = List.of("java");

    /** Ordered list of architecture styles. */
    public static final List<String> ARCHITECTURE_STYLES = List.of(
            "microservice", "monolith", "library");

    /** Architecture pattern styles for Java. */
    public static final List<String> ARCH_PATTERN_STYLES =
            List.of("layered", "hexagonal", "cqrs",
                    "event-driven", "clean");

    /** Compliance framework options for multi-select. */
    public static final List<String> COMPLIANCE_OPTIONS =
            List.of("none", "pci-dss", "lgpd",
                    "sox", "hipaa");

    /** Languages supporting architecture pattern selection. */
    public static final List<String> ARCH_PATTERN_LANGUAGES =
            List.of("java");

    /** Ordered list of interface types for multi-select. */
    public static final List<String> INTERFACE_TYPES = List.of(
            "rest", "grpc", "graphql", "cli", "events");

    /** Language to compatible frameworks mapping. */
    public static final Map<String, List<String>> FRAMEWORKS = Map.of(
            "java", List.of("spring-boot", "quarkus"));

    /** Language to compatible build tools mapping. */
    public static final Map<String, List<String>> BUILD_TOOLS = Map.of(
            "java", List.of("maven", "gradle"));

    /** Default language versions. */
    public static final Map<String, String> DEFAULT_VERSIONS = Map.of(
            "java", "21");

    /** Default framework versions. */
    public static final Map<String, String> FRAMEWORK_VERSIONS = Map.of(
            "spring-boot", "3.4",
            "quarkus", "3.17");

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
