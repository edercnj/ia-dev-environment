package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Represents the security configuration section.
 *
 * <p>Contains a list of security framework names. Defaults to an empty list.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("frameworks", List.of("spring-security", "oauth2"));
 * SecurityConfig cfg = SecurityConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param frameworks the list of security framework names (default: empty, immutable)
 */
public record SecurityConfig(List<String> frameworks) {

    /**
     * Compact constructor enforcing immutability.
     */
    public SecurityConfig {
        frameworks = List.copyOf(frameworks);
    }

    /**
     * Creates a SecurityConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new SecurityConfig instance
     */
    public static SecurityConfig fromMap(Map<String, Object> map) {
        return new SecurityConfig(
                MapHelper.optionalStringList(map, "frameworks")
        );
    }
}
