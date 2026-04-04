package dev.iadev.domain.model;

import java.util.Map;

/**
 * Represents a technology component with a name and optional version.
 *
 * <p>Used for database, cache, migration tool, and other technology references
 * within the project configuration.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("name", "postgresql", "version", "16");
 * TechComponent tc = TechComponent.fromMap(map);
 * // tc.name() == "postgresql", tc.version() == "16"
 * }</pre>
 * </p>
 *
 * @param name the component name (defaults to "none" if absent)
 * @param version the component version (defaults to empty string if absent)
 */
public record TechComponent(String name, String version) {

    /**
     * Creates a TechComponent from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new TechComponent instance with defaults for missing values
     */
    public static TechComponent fromMap(Map<String, Object> map) {
        var name = map.get("name");
        var version = map.get("version");
        return new TechComponent(
                name instanceof String s ? s : "none",
                version instanceof String s ? s : ""
        );
    }
}
