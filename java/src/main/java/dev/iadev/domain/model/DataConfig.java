package dev.iadev.domain.model;

import java.util.Map;

/**
 * Represents the data layer configuration (database, migration tool, cache).
 *
 * <p>All sub-components are optional and default to empty TechComponents
 * (name="none", version="").</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("database", Map.of("name", "postgresql", "version", "16"));
 * DataConfig cfg = DataConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param database the database component (default: TechComponent("none", ""))
 * @param migration the migration tool component (default: TechComponent("none", ""))
 * @param cache the cache component (default: TechComponent("none", ""))
 */
public record DataConfig(
        TechComponent database,
        TechComponent migration,
        TechComponent cache) {

    /**
     * Creates a DataConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new DataConfig instance with defaults for missing components
     */
    public static DataConfig fromMap(Map<String, Object> map) {
        return new DataConfig(
                TechComponent.fromMap(MapHelper.optionalMap(map, "database")),
                TechComponent.fromMap(MapHelper.optionalMap(map, "migration")),
                TechComponent.fromMap(MapHelper.optionalMap(map, "cache"))
        );
    }
}
