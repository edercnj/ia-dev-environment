package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataConfig")
class DataConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with all sub-components")
        void fromMap_allComponents_allSet() {
            var map = Map.<String, Object>of(
                    "database", Map.of("name", "postgresql", "version", "16"),
                    "migration", Map.of("name", "flyway", "version", "10"),
                    "cache", Map.of("name", "redis", "version", "7"));

            var result = DataConfig.fromMap(map);

            assertThat(result.database().name()).isEqualTo("postgresql");
            assertThat(result.database().version()).isEqualTo("16");
            assertThat(result.migration().name()).isEqualTo("flyway");
            assertThat(result.cache().name()).isEqualTo("redis");
        }

        @Test
        @DisplayName("empty map defaults all components to none")
        void fromMap_emptyMap_defaultComponents() {
            var result = DataConfig.fromMap(Map.of());

            assertThat(result.database().name()).isEqualTo("none");
            assertThat(result.database().version()).isEmpty();
            assertThat(result.migration().name()).isEqualTo("none");
            assertThat(result.cache().name()).isEqualTo("none");
        }

        @Test
        @DisplayName("partial map defaults missing components")
        void fromMap_partialMap_defaultsMissing() {
            var map = Map.<String, Object>of(
                    "database", Map.of("name", "mysql"));

            var result = DataConfig.fromMap(map);

            assertThat(result.database().name()).isEqualTo("mysql");
            assertThat(result.migration().name()).isEqualTo("none");
            assertThat(result.cache().name()).isEqualTo("none");
        }
    }
}
