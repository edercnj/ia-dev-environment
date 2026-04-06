package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DatabaseSettingsMapping")
class DatabaseSettingsMappingTest {

    @Nested
    @DisplayName("DATABASE_SETTINGS_MAP")
    class DatabaseSettingsMapTests {

        @Test
        @DisplayName("contains exactly 17 entries")
        void databaseSettingsMap_size_seventeen() {
            assertThat(DatabaseSettingsMapping
                    .DATABASE_SETTINGS_MAP)
                    .hasSize(17);
        }

        @ParameterizedTest
        @CsvSource({
                "postgresql, database-psql",
                "mysql, database-mysql",
                "oracle, database-oracle",
                "mongodb, database-mongodb",
                "cassandra, database-cassandra",
                "neo4j, database-neo4j",
                "neptune, database-neptune",
                "clickhouse, database-clickhouse",
                "druid, database-druid",
                "yugabytedb, database-yugabytedb",
                "cockroachdb, database-cockroachdb",
                "tidb, database-tidb",
                "influxdb, database-influxdb",
                "timescaledb, database-timescaledb",
                "elasticsearch, database-elasticsearch",
                "opensearch, database-opensearch",
                "eventstoredb, database-eventstoredb"
        })
        @DisplayName("database {0} maps to {1}")
        void databaseSettingsMap_entry(
                String db, String expected) {
            assertThat(DatabaseSettingsMapping
                    .DATABASE_SETTINGS_MAP)
                    .containsEntry(db, expected);
        }
    }

    @Nested
    @DisplayName("CACHE_SETTINGS_MAP")
    class CacheSettingsMapTests {

        @Test
        @DisplayName("contains exactly 3 entries")
        void cacheSettingsMap_size_three() {
            assertThat(DatabaseSettingsMapping
                    .CACHE_SETTINGS_MAP)
                    .hasSize(3);
        }

        @ParameterizedTest
        @CsvSource({
                "redis, cache-redis",
                "dragonfly, cache-dragonfly",
                "memcached, cache-memcached"
        })
        @DisplayName("cache {0} maps to {1}")
        void cacheSettingsMap_entry(
                String cache, String expected) {
            assertThat(DatabaseSettingsMapping
                    .CACHE_SETTINGS_MAP)
                    .containsEntry(cache, expected);
        }
    }

    @Nested
    @DisplayName("getDatabaseSettingsKey")
    class GetDatabaseSettingsKey {

        @Test
        @DisplayName("postgresql returns database-psql")
        void getDatabaseSettingsKey_postgresql_correct() {
            assertThat(DatabaseSettingsMapping
                    .getDatabaseSettingsKey("postgresql"))
                    .isEqualTo("database-psql");
        }

        @Test
        @DisplayName("unknown database returns empty")
        void getDatabaseSettingsKey_unknown_empty() {
            assertThat(DatabaseSettingsMapping
                    .getDatabaseSettingsKey("unknowndb"))
                    .isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "couchdb", "mariadb", "sqlite",
                "", "POSTGRESQL"
        })
        @DisplayName("invalid name '{0}' returns empty")
        void getDatabaseSettingsKey_invalid_empty(
                String name) {
            assertThat(DatabaseSettingsMapping
                    .getDatabaseSettingsKey(name))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("getCacheSettingsKey")
    class GetCacheSettingsKey {

        @Test
        @DisplayName("redis returns cache-redis")
        void getCacheSettingsKey_redis_correct() {
            assertThat(DatabaseSettingsMapping
                    .getCacheSettingsKey("redis"))
                    .isEqualTo("cache-redis");
        }

        @Test
        @DisplayName("unknown cache returns empty")
        void getCacheSettingsKey_unknown_empty() {
            assertThat(DatabaseSettingsMapping
                    .getCacheSettingsKey("hazelcast"))
                    .isEmpty();
        }
    }
}
