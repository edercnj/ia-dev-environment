package dev.iadev.domain.model;

import java.util.Map;

/**
 * Represents the architecture configuration section.
 *
 * <p>The {@code style} field is required and indicates the architecture type
 * (microservice, monolith, library). Boolean flags for DDD and event-driven
 * patterns default to {@code false}.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("style", "microservice", "domain_driven", true);
 * ArchitectureConfig cfg = ArchitectureConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param style the architecture style (required)
 * @param domainDriven whether DDD patterns are enabled (default: false)
 * @param eventDriven whether event-driven patterns are enabled (default: false)
 */
public record ArchitectureConfig(
        String style,
        boolean domainDriven,
        boolean eventDriven) {

    /**
     * Creates an ArchitectureConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new ArchitectureConfig instance
     * @throws ConfigValidationException if style is missing
     */
    public static ArchitectureConfig fromMap(Map<String, Object> map) {
        return new ArchitectureConfig(
                MapHelper.requireString(map, "style", "ArchitectureConfig"),
                MapHelper.optionalBoolean(map, "domain_driven", false),
                MapHelper.optionalBoolean(map, "event_driven", false)
        );
    }
}
