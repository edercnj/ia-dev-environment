package dev.iadev.model;

import java.util.Map;

/**
 * Represents the observability configuration section.
 *
 * <p>All fields default to "none" when absent in the YAML config.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("tool", "prometheus", "metrics", "micrometer");
 * ObservabilityConfig cfg = ObservabilityConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param tool the observability tool (default: "none")
 * @param metrics the metrics backend (default: "none")
 * @param tracing the tracing backend (default: "none")
 */
public record ObservabilityConfig(
        String tool,
        String metrics,
        String tracing) {

    /**
     * Creates an ObservabilityConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new ObservabilityConfig instance with defaults for missing values
     */
    public static ObservabilityConfig fromMap(Map<String, Object> map) {
        return new ObservabilityConfig(
                MapHelper.optionalString(map, "tool", "none"),
                MapHelper.optionalString(map, "metrics", "none"),
                MapHelper.optionalString(map, "tracing", "none")
        );
    }
}
