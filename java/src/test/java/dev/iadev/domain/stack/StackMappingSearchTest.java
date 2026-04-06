package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for search engine entries in StackMapping.
 */
@DisplayName("StackMapping — Search Engines")
class StackMappingSearchTest {

    @Nested
    @DisplayName("DATABASE_SETTINGS_MAP — search entries")
    class DatabaseSettingsMapSearch {

        @Test
        @DisplayName("contains elasticsearch entry")
        void databaseSettingsMap_elasticsearch_present() {
            assertThat(StackMapping.DATABASE_SETTINGS_MAP)
                    .containsKey("elasticsearch");
        }

        @Test
        @DisplayName("contains opensearch entry")
        void databaseSettingsMap_opensearch_present() {
            assertThat(StackMapping.DATABASE_SETTINGS_MAP)
                    .containsKey("opensearch");
        }

        @ParameterizedTest
        @CsvSource({
                "elasticsearch, database-elasticsearch",
                "opensearch, database-opensearch"
        })
        @DisplayName("maps {0} to {1}")
        void databaseSettingsMap_searchEngine_correctKey(
                String dbName, String expected) {
            assertThat(StackMapping.getDatabaseSettingsKey(
                    dbName)).isEqualTo(expected);
        }

        @Test
        @DisplayName("total entries is 7 (5 existing + 2 search)")
        void databaseSettingsMap_totalSize_seven() {
            assertThat(StackMapping.DATABASE_SETTINGS_MAP)
                    .hasSize(7);
        }
    }
}
