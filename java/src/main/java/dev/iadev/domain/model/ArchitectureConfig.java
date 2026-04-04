package dev.iadev.domain.model;

import java.util.Map;

/**
 * Represents the architecture configuration section.
 *
 * <p>The {@code style} field is required and indicates the architecture type
 * (microservice, monolith, library, hexagonal, layered, cqrs, event-driven,
 * clean). Boolean flags for DDD and event-driven patterns default to
 * {@code false}.</p>
 *
 * <p>The {@code validateWithArchUnit} flag controls generation of an ArchUnit
 * test class for hexagonal boundary validation. When enabled,
 * {@code basePackage} must be set.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("style", "hexagonal",
 *     "validate_with_archunit", true,
 *     "base_package", "com.example.myapp");
 * ArchitectureConfig cfg = ArchitectureConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param style the architecture style (required)
 * @param domainDriven whether DDD patterns are enabled
 * @param eventDriven whether event-driven patterns are enabled
 * @param validateWithArchUnit whether to generate ArchUnit tests
 * @param basePackage the base Java package for ArchUnit rules
 */
public record ArchitectureConfig(
        String style,
        boolean domainDriven,
        boolean eventDriven,
        boolean validateWithArchUnit,
        String basePackage) {

    /**
     * Creates an ArchitectureConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new ArchitectureConfig instance
     * @throws ConfigValidationException if style is missing
     */
    public static ArchitectureConfig fromMap(
            Map<String, Object> map) {
        return new ArchitectureConfig(
                MapHelper.requireString(
                        map, "style", "ArchitectureConfig"),
                MapHelper.optionalBoolean(
                        map, "domain_driven", false),
                MapHelper.optionalBoolean(
                        map, "event_driven", false),
                MapHelper.optionalBoolean(
                        map, "validate_with_archunit", false),
                MapHelper.optionalString(
                        map, "base_package", ""));
    }
}
