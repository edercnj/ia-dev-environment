package dev.iadev.cli;

import java.util.List;
import java.util.Objects;

/**
 * Immutable parameter object capturing the results of
 * interactive project prompts.
 *
 * <p>Groups the values collected from the user into a
 * single cohesive record, reducing parameter count in
 * {@link InteractivePrompter#displaySummary} and
 * {@link InteractivePrompter#buildConfig}.</p>
 *
 * @param name             the project name in kebab-case
 * @param purpose          the project purpose description
 * @param archStyle        the architecture style
 * @param language         the programming language
 * @param framework        the framework name
 * @param buildTool        the build tool name
 * @param interfaces       the selected interface types
 * @param database         the database name or empty
 * @param cache            the cache name or empty string
 * @param archPatternStyle the architecture pattern
 *                         (empty if not applicable)
 * @param validateArchUnit whether to validate with ArchUnit
 * @param compliance       the selected compliance frameworks
 */
public record ProjectSummary(
        String name,
        String purpose,
        String archStyle,
        String language,
        String framework,
        String buildTool,
        List<String> interfaces,
        String database,
        String cache,
        String archPatternStyle,
        boolean validateArchUnit,
        List<String> compliance) {

    /**
     * Validates that required fields are not null.
     */
    public ProjectSummary {
        Objects.requireNonNull(name,
                "name must not be null");
        Objects.requireNonNull(purpose,
                "purpose must not be null");
        Objects.requireNonNull(archStyle,
                "archStyle must not be null");
        Objects.requireNonNull(language,
                "language must not be null");
        Objects.requireNonNull(framework,
                "framework must not be null");
        Objects.requireNonNull(buildTool,
                "buildTool must not be null");
        Objects.requireNonNull(interfaces,
                "interfaces must not be null");
        Objects.requireNonNull(database,
                "database must not be null");
        Objects.requireNonNull(cache,
                "cache must not be null");
        Objects.requireNonNull(archPatternStyle,
                "archPatternStyle must not be null");
        Objects.requireNonNull(compliance,
                "compliance must not be null");
        interfaces = List.copyOf(interfaces);
        compliance = List.copyOf(compliance);
    }

    /**
     * Creates a ProjectSummary with the original 9 fields
     * plus defaults for the new fields. Maintains backward
     * compatibility with existing callers.
     */
    public ProjectSummary(
            String name, String purpose,
            String archStyle, String language,
            String framework, String buildTool,
            List<String> interfaces,
            String database, String cache) {
        this(name, purpose, archStyle, language,
                framework, buildTool, interfaces,
                database, cache, "", false, List.of());
    }
}
