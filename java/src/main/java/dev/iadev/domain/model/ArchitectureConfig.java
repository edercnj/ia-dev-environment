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
 * <p>CQRS-related fields are grouped into {@link CqrsConfig}.</p>
 *
 * @param style the architecture style (required)
 * @param domainDriven whether DDD patterns are enabled
 * @param eventDriven whether event-driven patterns are enabled
 * @param validateWithArchUnit whether to generate ArchUnit tests
 * @param basePackage the base Java package for ArchUnit rules
 * @param cqrs CQRS-related configuration
 * @param dddEnabled whether DDD strategic KP is explicitly
 *        enabled (default: false)
 */
public record ArchitectureConfig(
        String style,
        boolean domainDriven,
        boolean eventDriven,
        boolean validateWithArchUnit,
        String basePackage,
        CqrsConfig cqrs,
        boolean dddEnabled) {

    /**
     * CQRS-related configuration fields.
     *
     * @param eventStore the event store type
     *        (eventstoredb, axon, custom)
     * @param eventsPerSnapshot events before snapshot
     *        (default: 100)
     * @param schemaRegistry schema registry type
     *        (default: "")
     * @param outboxPattern whether outbox pattern is
     *        enabled (default: false)
     * @param deadLetterStrategy dead letter queue strategy
     *        (default: "")
     */
    public record CqrsConfig(
            String eventStore,
            int eventsPerSnapshot,
            String schemaRegistry,
            boolean outboxPattern,
            String deadLetterStrategy) {

        private static final String DEFAULT_EVENT_STORE =
                "eventstoredb";

        /** Default number of events before snapshot. */
        public static final int
                DEFAULT_EVENTS_PER_SNAPSHOT = 100;

        /**
         * Creates a CqrsConfig from a YAML-parsed map.
         *
         * @param map the map from YAML deserialization
         * @return a new CqrsConfig instance
         */
        public static CqrsConfig fromMap(
                Map<String, Object> map) {
            Map<String, Object> snapshotPolicy =
                    MapHelper.optionalMap(
                            map, "snapshot_policy");
            return new CqrsConfig(
                    MapHelper.optionalString(
                            map, "event_store",
                            DEFAULT_EVENT_STORE),
                    MapHelper.optionalInt(
                            snapshotPolicy,
                            "events_per_snapshot",
                            DEFAULT_EVENTS_PER_SNAPSHOT),
                    MapHelper.optionalString(
                            map, "schema_registry", ""),
                    MapHelper.optionalBoolean(
                            map, "outbox_pattern", false),
                    MapHelper.optionalString(
                            map, "dead_letter_strategy",
                            ""));
        }
    }

    /** Default number of events before creating a snapshot. */
    public static final int DEFAULT_EVENTS_PER_SNAPSHOT =
            CqrsConfig.DEFAULT_EVENTS_PER_SNAPSHOT;

    /** Convenience accessor for event store. */
    public String eventStore() {
        return cqrs.eventStore();
    }

    /** Convenience accessor for events per snapshot. */
    public int eventsPerSnapshot() {
        return cqrs.eventsPerSnapshot();
    }

    /** Convenience accessor for schema registry. */
    public String schemaRegistry() {
        return cqrs.schemaRegistry();
    }

    /** Convenience accessor for outbox pattern. */
    public boolean outboxPattern() {
        return cqrs.outboxPattern();
    }

    /** Convenience accessor for dead letter strategy. */
    public String deadLetterStrategy() {
        return cqrs.deadLetterStrategy();
    }

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
                        map, "validate_with_archunit",
                        false),
                MapHelper.optionalString(
                        map, "base_package", ""),
                CqrsConfig.fromMap(map),
                MapHelper.optionalBoolean(
                        map, "ddd_enabled", false));
    }
}
