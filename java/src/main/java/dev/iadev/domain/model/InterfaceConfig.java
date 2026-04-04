package dev.iadev.domain.model;

import java.util.Map;

/**
 * Represents an interface configuration entry (rest, grpc, graphql, cli, etc.).
 *
 * <p>The {@code type} field is required. Optional fields {@code spec} and
 * {@code broker} default to empty strings.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("type", "rest", "spec", "openapi-3.1");
 * InterfaceConfig cfg = InterfaceConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param type the interface type (required: rest, grpc, graphql, cli, etc.)
 * @param spec the API specification version (default: "")
 * @param broker the message broker for event types (default: "")
 */
public record InterfaceConfig(String type, String spec, String broker) {

    /**
     * Creates an InterfaceConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new InterfaceConfig instance
     * @throws ConfigValidationException if type is missing
     */
    public static InterfaceConfig fromMap(Map<String, Object> map) {
        return new InterfaceConfig(
                MapHelper.requireString(map, "type", "InterfaceConfig"),
                MapHelper.optionalString(map, "spec", ""),
                MapHelper.optionalString(map, "broker", "")
        );
    }
}
