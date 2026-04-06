package dev.iadev.domain.stack;

import java.util.Map;

/**
 * Database and cache name-to-settings-key mappings.
 * Extracted from StackMapping to keep that class
 * within the 250-line limit.
 */
public final class DatabaseSettingsMapping {

    private DatabaseSettingsMapping() {
        // utility class
    }

    /** Database name to settings key (17 entries). */
    public static final Map<String, String>
            DATABASE_SETTINGS_MAP = Map.ofEntries(
            Map.entry("postgresql", "database-psql"),
            Map.entry("mysql", "database-mysql"),
            Map.entry("oracle", "database-oracle"),
            Map.entry("mongodb", "database-mongodb"),
            Map.entry("cassandra", "database-cassandra"),
            Map.entry("neo4j", "database-neo4j"),
            Map.entry("neptune", "database-neptune"),
            Map.entry("clickhouse", "database-clickhouse"),
            Map.entry("druid", "database-druid"),
            Map.entry("yugabytedb", "database-yugabytedb"),
            Map.entry("cockroachdb",
                    "database-cockroachdb"),
            Map.entry("tidb", "database-tidb"),
            Map.entry("influxdb", "database-influxdb"),
            Map.entry("timescaledb",
                    "database-timescaledb"),
            Map.entry("elasticsearch",
                    "database-elasticsearch"),
            Map.entry("opensearch",
                    "database-opensearch"),
            Map.entry("eventstoredb",
                    "database-eventstoredb"));

    /** Cache name to settings key. */
    public static final Map<String, String>
            CACHE_SETTINGS_MAP = Map.of(
            "redis", "cache-redis",
            "dragonfly", "cache-dragonfly",
            "memcached", "cache-memcached");

    /** Returns database settings key, or empty string. */
    public static String getDatabaseSettingsKey(
            String dbName) {
        return DATABASE_SETTINGS_MAP
                .getOrDefault(dbName, "");
    }

    /** Returns cache settings key, or empty string. */
    public static String getCacheSettingsKey(
            String cacheName) {
        return CACHE_SETTINGS_MAP
                .getOrDefault(cacheName, "");
    }
}
