package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Represents the security configuration section.
 *
 * <p>Contains a list of compliance framework names (e.g.,
 * pci-dss, lgpd, sox, hipaa). Defaults to an empty list.
 * The YAML key {@code compliance} is read; the legacy
 * {@code frameworks} key is supported as fallback.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("compliance",
 *     List.of("pci-dss", "lgpd"));
 * SecurityConfig cfg = SecurityConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param frameworks the list of compliance framework
 *     names (default: empty, immutable)
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
     * <p>Reads the {@code compliance} key first; falls
     * back to {@code frameworks} for backward
     * compatibility.</p>
     *
     * @param map the map from YAML deserialization
     * @return a new SecurityConfig instance
     */
    public static SecurityConfig fromMap(
            Map<String, Object> map) {
        List<String> values =
                MapHelper.optionalStringList(
                        map, "compliance");
        if (values.isEmpty()) {
            values = MapHelper.optionalStringList(
                    map, "frameworks");
        }
        return new SecurityConfig(values);
    }
}
