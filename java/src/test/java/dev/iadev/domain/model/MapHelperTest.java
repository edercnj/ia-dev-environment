package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MapHelper")
class MapHelperTest {

    @Nested
    @DisplayName("requireField()")
    class RequireField {

        @Test
        @DisplayName("returns value when field exists")
        void requireField_fieldExists_returnsValue() {
            var map = Map.<String, Object>of("key", "value");

            var result = MapHelper.requireField(map, "key", "Test");

            assertThat(result).isEqualTo("value");
        }

        @Test
        @DisplayName("throws when field is missing")
        void requireField_fieldMissing_throwsException() {
            assertThatThrownBy(() ->
                    MapHelper.requireField(Map.of(), "key", "Test"))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("key")
                    .hasMessageContaining("Test");
        }

        @Test
        @DisplayName("throws when field is null")
        void requireField_fieldNull_throwsException() {
            var map = new HashMap<String, Object>();
            map.put("key", null);

            assertThatThrownBy(() ->
                    MapHelper.requireField(map, "key", "Test"))
                    .isInstanceOf(ConfigValidationException.class);
        }
    }

    @Nested
    @DisplayName("requireString()")
    class RequireString {

        @Test
        @DisplayName("returns string when field is a String")
        void requireString_validString_returnsValue() {
            var map = Map.<String, Object>of("key", "hello");

            assertThat(MapHelper.requireString(map, "key", "Test"))
                    .isEqualTo("hello");
        }

        @Test
        @DisplayName("throws when field is wrong type")
        void requireString_wrongType_throwsException() {
            var map = Map.<String, Object>of("key", 42);

            assertThatThrownBy(() ->
                    MapHelper.requireString(map, "key", "Test"))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("String");
        }
    }

    @Nested
    @DisplayName("optionalString()")
    class OptionalString {

        @Test
        @DisplayName("returns value when field exists")
        void optionalString_fieldExists_returnsValue() {
            var map = Map.<String, Object>of("key", "val");

            assertThat(MapHelper.optionalString(map, "key", "def"))
                    .isEqualTo("val");
        }

        @Test
        @DisplayName("returns default when field missing")
        void optionalString_fieldMissing_returnsDefault() {
            assertThat(MapHelper.optionalString(Map.of(), "key", "def"))
                    .isEqualTo("def");
        }

        @Test
        @DisplayName("returns default when field is wrong type")
        void optionalString_wrongType_returnsDefault() {
            var map = Map.<String, Object>of("key", 42);

            assertThat(MapHelper.optionalString(map, "key", "def"))
                    .isEqualTo("def");
        }
    }

    @Nested
    @DisplayName("optionalBoolean()")
    class OptionalBoolean {

        @Test
        @DisplayName("returns value when field is boolean")
        void optionalBoolean_booleanValue_returnsValue() {
            var map = Map.<String, Object>of("flag", true);

            assertThat(MapHelper.optionalBoolean(map, "flag", false))
                    .isTrue();
        }

        @Test
        @DisplayName("returns default when field missing")
        void optionalBoolean_missing_returnsDefault() {
            assertThat(MapHelper.optionalBoolean(
                    Map.of(), "flag", true))
                    .isTrue();
        }

        @Test
        @DisplayName("returns default when field is wrong type")
        void optionalBoolean_wrongType_returnsDefault() {
            var map = Map.<String, Object>of("flag", "yes");

            assertThat(MapHelper.optionalBoolean(map, "flag", false))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("optionalInt()")
    class OptionalInt {

        @Test
        @DisplayName("returns value when field is a number")
        void optionalInt_numberValue_returnsValue() {
            var map = Map.<String, Object>of("count", 42);

            assertThat(MapHelper.optionalInt(map, "count", 0))
                    .isEqualTo(42);
        }

        @Test
        @DisplayName("returns default when field missing")
        void optionalInt_missing_returnsDefault() {
            assertThat(MapHelper.optionalInt(Map.of(), "count", 10))
                    .isEqualTo(10);
        }

        @Test
        @DisplayName("handles Double values from YAML")
        void optionalInt_doubleValue_convertsToInt() {
            var map = Map.<String, Object>of("count", 42.0);

            assertThat(MapHelper.optionalInt(map, "count", 0))
                    .isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("optionalMap()")
    class OptionalMap {

        @Test
        @DisplayName("returns sub-map when field is a Map")
        void optionalMap_mapValue_returnsMap() {
            var sub = Map.<String, Object>of("a", "b");
            var map = Map.<String, Object>of("nested", sub);

            assertThat(MapHelper.optionalMap(map, "nested"))
                    .containsEntry("a", "b");
        }

        @Test
        @DisplayName("returns empty map when field missing")
        void optionalMap_missing_returnsEmptyMap() {
            assertThat(MapHelper.optionalMap(Map.of(), "nested"))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns empty map when field is wrong type")
        void optionalMap_wrongType_returnsEmptyMap() {
            var map = Map.<String, Object>of("nested", "string");

            assertThat(MapHelper.optionalMap(map, "nested"))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("requireMap()")
    class RequireMap {

        @Test
        @DisplayName("returns sub-map when field is a Map")
        void requireMap_mapValue_returnsMap() {
            var sub = Map.<String, Object>of("x", "y");
            var map = Map.<String, Object>of("key", sub);

            assertThat(MapHelper.requireMap(map, "key", "Test"))
                    .containsEntry("x", "y");
        }

        @Test
        @DisplayName("throws when field is not a Map")
        void requireMap_wrongType_throwsException() {
            var map = Map.<String, Object>of("key", "not-a-map");

            assertThatThrownBy(() ->
                    MapHelper.requireMap(map, "key", "Test"))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("Map");
        }

        @Test
        @DisplayName("throws when field missing")
        void requireMap_missing_throwsException() {
            assertThatThrownBy(() ->
                    MapHelper.requireMap(Map.of(), "key", "Test"))
                    .isInstanceOf(ConfigValidationException.class);
        }
    }

    @Nested
    @DisplayName("optionalStringList()")
    class OptionalStringList {

        @Test
        @DisplayName("returns list when field is a List")
        void optionalStringList_listValue_returnsList() {
            var map = Map.<String, Object>of(
                    "items", List.of("a", "b"));

            assertThat(MapHelper.optionalStringList(map, "items"))
                    .containsExactly("a", "b");
        }

        @Test
        @DisplayName("returns empty list when field missing")
        void optionalStringList_missing_returnsEmptyList() {
            assertThat(
                    MapHelper.optionalStringList(Map.of(), "items"))
                    .isEmpty();
        }

        @Test
        @DisplayName("returned list is immutable")
        void optionalStringList_whenCalled_immutable() {
            var map = Map.<String, Object>of(
                    "items", List.of("a"));

            var result = MapHelper.optionalStringList(map, "items");

            assertThatThrownBy(() -> result.add("b"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("optionalStringMap()")
    class OptionalStringMap {

        @Test
        @DisplayName("returns map when field is a Map")
        void optionalStringMap_mapValue_returnsMap() {
            var map = Map.<String, Object>of(
                    "env", Map.of("KEY", "val"));

            assertThat(MapHelper.optionalStringMap(map, "env"))
                    .containsEntry("KEY", "val");
        }

        @Test
        @DisplayName("returns empty map when field missing")
        void optionalStringMap_missing_returnsEmptyMap() {
            assertThat(
                    MapHelper.optionalStringMap(Map.of(), "env"))
                    .isEmpty();
        }

        @Test
        @DisplayName("returned map is immutable")
        void optionalStringMap_whenCalled_immutable() {
            var map = Map.<String, Object>of(
                    "env", Map.of("K", "V"));

            var result = MapHelper.optionalStringMap(map, "env");

            assertThatThrownBy(() -> result.put("K2", "V2"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
