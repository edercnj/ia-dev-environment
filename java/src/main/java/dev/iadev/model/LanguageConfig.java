package dev.iadev.model;

import java.util.Map;

/**
 * Represents the programming language configuration.
 *
 * <p>Both {@code name} and {@code version} are required fields.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("name", "java", "version", "21");
 * LanguageConfig cfg = LanguageConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param name the language name (required: java, python, go, kotlin, typescript, rust)
 * @param version the language version string (required)
 */
public record LanguageConfig(String name, String version) {

    /**
     * Creates a LanguageConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new LanguageConfig instance
     * @throws dev.iadev.exception.ConfigValidationException if name or version is missing
     */
    public static LanguageConfig fromMap(Map<String, Object> map) {
        return new LanguageConfig(
                MapHelper.requireString(map, "name", "LanguageConfig"),
                MapHelper.requireString(map, "version", "LanguageConfig")
        );
    }
}
