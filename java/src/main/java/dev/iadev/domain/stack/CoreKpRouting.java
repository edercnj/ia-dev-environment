package dev.iadev.domain.stack;

import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Core rule to knowledge pack routing.
 *
 * <p>Defines static and conditional mappings from core rule source files
 * to knowledge pack destinations. Used during generation to route
 * documentation files to the correct knowledge pack directories.</p>
 *
 * <p>Zero external framework dependencies (RULE-007).</p>
 */
public final class CoreKpRouting {

    private CoreKpRouting() {
        // utility class
    }

    /**
     * A route from a core rule source file to a knowledge pack.
     *
     * @param sourceFile the source rule file name
     * @param kpName the target knowledge pack name
     * @param destFile the destination file name within the knowledge pack
     */
    public record CoreKpRoute(
            String sourceFile, String kpName, String destFile) {
    }

    /**
     * A conditional route that is excluded for a specific architecture style.
     *
     * @param sourceFile the source rule file name
     * @param kpName the target knowledge pack name
     * @param destFile the destination file name within the knowledge pack
     * @param conditionField the config field to check
     * @param conditionExclude the value that excludes this route
     */
    public record ConditionalCoreKpRoute(
            String sourceFile,
            String kpName,
            String destFile,
            String conditionField,
            String conditionExclude) {
    }

    /** 12 static routes from core rules to knowledge packs. */
    public static final List<CoreKpRoute> CORE_TO_KP_MAPPING = List.of(
            new CoreKpRoute("01-clean-code.md",
                    "coding-standards", "clean-code.md"),
            new CoreKpRoute("02-solid-principles.md",
                    "coding-standards", "solid-principles.md"),
            new CoreKpRoute("03-testing-philosophy.md",
                    "testing", "testing-philosophy.md"),
            new CoreKpRoute("05-architecture-principles.md",
                    "architecture", "architecture-principles.md"),
            new CoreKpRoute("06-api-design-principles.md",
                    "api-design", "api-design-principles.md"),
            new CoreKpRoute("07-security-principles.md",
                    "security", "security-principles.md"),
            new CoreKpRoute("08-observability-principles.md",
                    "observability", "observability-principles.md"),
            new CoreKpRoute("09-resilience-principles.md",
                    "resilience", "resilience-principles.md"),
            new CoreKpRoute("10-infrastructure-principles.md",
                    "infrastructure", "infrastructure-principles.md"),
            new CoreKpRoute("11-database-principles.md",
                    "database-patterns", "database-principles.md"),
            new CoreKpRoute("13-story-decomposition.md",
                    "story-planning", "story-decomposition.md"),
            new CoreKpRoute("14-refactoring-guidelines.md",
                    "coding-standards", "refactoring-guidelines.md")
    );

    /** 1 conditional route: cloud-native excluded for library style. */
    public static final List<ConditionalCoreKpRoute> CONDITIONAL_CORE_KP =
            List.of(
                    new ConditionalCoreKpRoute(
                            "12-cloud-native-principles.md",
                            "infrastructure",
                            "cloud-native-principles.md",
                            "architecture_style",
                            "library")
            );

    /**
     * Returns all routes whose conditions are met for the given config.
     *
     * <p>Includes all static routes plus any conditional routes
     * whose exclusion condition does not match the config.</p>
     *
     * @param config the project configuration
     * @return list of active routes
     */
    public static List<CoreKpRoute> getActiveRoutes(ProjectConfig config) {
        List<CoreKpRoute> routes = new ArrayList<>(CORE_TO_KP_MAPPING);
        for (ConditionalCoreKpRoute route : CONDITIONAL_CORE_KP) {
            String configValue =
                    resolveConditionValue(config, route.conditionField());
            if (!configValue.equals(route.conditionExclude())) {
                routes.add(new CoreKpRoute(
                        route.sourceFile(),
                        route.kpName(),
                        route.destFile()));
            }
        }
        return routes;
    }

    /**
     * Resolves a condition field name to a config value.
     *
     * @param config the project configuration
     * @param field the condition field name
     * @return the resolved value
     */
    static String resolveConditionValue(
            ProjectConfig config, String field) {
        if ("architecture_style".equals(field)) {
            return config.architecture().style();
        }
        return "";
    }
}
