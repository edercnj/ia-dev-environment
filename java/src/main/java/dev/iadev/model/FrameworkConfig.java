package dev.iadev.model;

import java.util.Map;

/**
 * Represents the framework configuration.
 *
 * <p>The {@code name} and {@code version} are required. The {@code buildTool}
 * defaults to "pip" and {@code nativeBuild} defaults to {@code false}, matching
 * the TypeScript implementation.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("name", "spring-boot", "version", "3.4", "build_tool", "maven");
 * FrameworkConfig cfg = FrameworkConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param name the framework name (required)
 * @param version the framework version (required)
 * @param buildTool the build tool name (default: "pip")
 * @param nativeBuild whether native image build is supported (default: false)
 */
public record FrameworkConfig(
        String name,
        String version,
        String buildTool,
        boolean nativeBuild) {

    /**
     * Creates a FrameworkConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new FrameworkConfig instance
     * @throws dev.iadev.exception.ConfigValidationException if name or version is missing
     */
    public static FrameworkConfig fromMap(Map<String, Object> map) {
        return new FrameworkConfig(
                MapHelper.requireString(map, "name", "FrameworkConfig"),
                MapHelper.requireString(map, "version", "FrameworkConfig"),
                MapHelper.optionalString(map, "build_tool", "pip"),
                MapHelper.optionalBoolean(map, "native_build", false)
        );
    }
}
