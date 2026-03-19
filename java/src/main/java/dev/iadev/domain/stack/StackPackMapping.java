package dev.iadev.domain.stack;

import java.util.Map;

/**
 * Framework to knowledge pack mapping.
 *
 * <p>Maps framework names to their corresponding knowledge pack
 * directory names. Used during generation to select framework-specific
 * documentation and patterns.</p>
 *
 * <p>Zero external framework dependencies (RULE-007).</p>
 */
public final class StackPackMapping {

    private StackPackMapping() {
        // utility class
    }

    /** Framework name to knowledge pack directory name (11 entries). */
    public static final Map<String, String> FRAMEWORK_STACK_PACK = Map.ofEntries(
            Map.entry("quarkus", "quarkus-patterns"),
            Map.entry("spring-boot", "spring-patterns"),
            Map.entry("nestjs", "nestjs-patterns"),
            Map.entry("express", "express-patterns"),
            Map.entry("fastapi", "fastapi-patterns"),
            Map.entry("django", "django-patterns"),
            Map.entry("gin", "gin-patterns"),
            Map.entry("ktor", "ktor-patterns"),
            Map.entry("axum", "axum-patterns"),
            Map.entry("dotnet", "dotnet-patterns"),
            Map.entry("click", "click-cli-patterns")
    );

    /**
     * Returns the knowledge pack directory name for a framework.
     *
     * @param framework the framework name
     * @return the pack name, or empty string if not found
     */
    public static String getStackPackName(String framework) {
        return FRAMEWORK_STACK_PACK.getOrDefault(framework, "");
    }
}
