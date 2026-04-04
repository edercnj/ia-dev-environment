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
 * <p>When {@code style} is "cqrs", additional fields are
 * relevant:
 * <ul>
 *   <li>{@code eventStore} — the event store type
 *       (eventstoredb, axon, custom). Default:
 *       eventstoredb</li>
 *   <li>{@code eventsPerSnapshot} — number of events
 *       before creating a snapshot. Default: 100</li>
 * </ul>
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
 * @param eventStore the event store type (default:
 *        eventstoredb)
 * @param eventsPerSnapshot events before snapshot
 *        (default: 100)
 */
public record ArchitectureConfig(
        String style,
        boolean domainDriven,
        boolean eventDriven,
        boolean validateWithArchUnit,
        String basePackage,
        String eventStore,
        int eventsPerSnapshot) {

    private static final String DEFAULT_EVENT_STORE =
            "eventstoredb";
    private static final int DEFAULT_EVENTS_PER_SNAPSHOT =
            100;

    /**
     * Creates an ArchitectureConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new ArchitectureConfig instance
     * @throws ConfigValidationException if style is missing
     */
    public static ArchitectureConfig fromMap(
            Map<String, Object> map) {
        Map<String, Object> snapshotPolicy =
                MapHelper.optionalMap(
                        map, "snapshot_policy");
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
                        map, "base_package", ""),
                MapHelper.optionalString(
                        map, "event_store",
                        DEFAULT_EVENT_STORE),
                MapHelper.optionalInt(
                        snapshotPolicy,
                        "events_per_snapshot",
                        DEFAULT_EVENTS_PER_SNAPSHOT));
    }
}
