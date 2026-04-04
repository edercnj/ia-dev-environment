package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TechComponent")
class TechComponentTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates component with name and version")
        void fromMap_nameAndVersion_bothSet() {
            var map = Map.<String, Object>of(
                    "name", "postgresql",
                    "version", "16");

            var result = TechComponent.fromMap(map);

            assertThat(result.name()).isEqualTo("postgresql");
            assertThat(result.version()).isEqualTo("16");
        }

        @Test
        @DisplayName("creates component with only name, version defaults to empty")
        void fromMap_onlyName_versionDefaultsEmpty() {
            var map = Map.<String, Object>of("name", "redis");

            var result = TechComponent.fromMap(map);

            assertThat(result.name()).isEqualTo("redis");
            assertThat(result.version()).isEmpty();
        }

        @Test
        @DisplayName("empty map defaults to name=none, version=empty")
        void fromMap_emptyMap_defaultValues() {
            var result = TechComponent.fromMap(Map.of());

            assertThat(result.name()).isEqualTo("none");
            assertThat(result.version()).isEmpty();
        }

        @Test
        @DisplayName("null name value defaults to 'none'")
        void fromMap_nullName_defaultsToNone() {
            var map = new HashMap<String, Object>();
            map.put("name", null);

            var result = TechComponent.fromMap(map);

            assertThat(result.name()).isEqualTo("none");
        }

        @Test
        @DisplayName("non-string name value defaults to 'none'")
        void fromMap_nonStringName_defaultsToNone() {
            var map = Map.<String, Object>of("name", 42);

            var result = TechComponent.fromMap(map);

            assertThat(result.name()).isEqualTo("none");
        }

        @Test
        @DisplayName("non-string version value defaults to empty")
        void fromMap_nonStringVersion_defaultsToEmpty() {
            var map = Map.<String, Object>of(
                    "name", "pg", "version", 16);

            var result = TechComponent.fromMap(map);

            assertThat(result.name()).isEqualTo("pg");
            assertThat(result.version()).isEmpty();
        }
    }

    @Test
    @DisplayName("record equality works for same values")
    void equality_sameValues_equal() {
        var a = new TechComponent("redis", "7");
        var b = new TechComponent("redis", "7");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
