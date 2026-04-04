package dev.iadev.domain.model;

import java.util.Map;

/**
 * Represents the project identity containing name and purpose.
 *
 * <p>Both fields are required. A {@link dev.iadev.exception.ConfigValidationException}
 * is thrown if either is missing or has an invalid type.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("name", "my-project", "purpose", "A CLI tool");
 * ProjectIdentity id = ProjectIdentity.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param name the project name (required, kebab-case)
 * @param purpose a one-line description of the project (required)
 */
public record ProjectIdentity(String name, String purpose) {

    /**
     * Creates a ProjectIdentity from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new ProjectIdentity instance
     * @throws dev.iadev.exception.ConfigValidationException if required fields are missing or have wrong type
     */
    public static ProjectIdentity fromMap(Map<String, Object> map) {
        return new ProjectIdentity(
                MapHelper.requireString(map, "name", "ProjectIdentity"),
                MapHelper.requireString(map, "purpose", "ProjectIdentity")
        );
    }
}
