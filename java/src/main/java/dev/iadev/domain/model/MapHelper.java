package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Internal helper for extracting typed values from YAML-parsed maps.
 *
 * <p>Provides safe extraction with type checking and required field validation.
 * Used by all model classes in their {@code fromMap()} factory methods.</p>
 */
final class MapHelper {

    private MapHelper() {
        // utility class
    }

    /**
     * Extracts a required field from the map, throwing if absent.
     *
     * @param map the source map
     * @param key the field key
     * @param model the model class name (for error messages)
     * @return the non-null value
     * @throws ConfigValidationException if the field is missing
     */
    static Object requireField(
            Map<String, Object> map,
            String key,
            String model) {
        if (!map.containsKey(key) || map.get(key) == null) {
            throw new ConfigValidationException(key, model);
        }
        return map.get(key);
    }

    /**
     * Extracts a required String field from the map.
     *
     * @param map the source map
     * @param key the field key
     * @param model the model class name (for error messages)
     * @return the string value
     * @throws ConfigValidationException if field is missing or not a String
     */
    static String requireString(
            Map<String, Object> map,
            String key,
            String model) {
        var value = requireField(map, key, model);
        if (value instanceof String s) {
            return s;
        }
        throw new ConfigValidationException(key, "String", model);
    }

    /**
     * Extracts an optional String field with a default value.
     *
     * @param map the source map
     * @param key the field key
     * @param defaultValue the default if absent
     * @return the string value or default
     */
    static String optionalString(
            Map<String, Object> map,
            String key,
            String defaultValue) {
        var value = map.get(key);
        return value instanceof String s ? s : defaultValue;
    }

    /**
     * Extracts an optional boolean field with a default value.
     *
     * @param map the source map
     * @param key the field key
     * @param defaultValue the default if absent
     * @return the boolean value or default
     */
    static boolean optionalBoolean(
            Map<String, Object> map,
            String key,
            boolean defaultValue) {
        var value = map.get(key);
        return value instanceof Boolean b ? b : defaultValue;
    }

    /**
     * Extracts an optional integer field with a default value.
     *
     * @param map the source map
     * @param key the field key
     * @param defaultValue the default if absent
     * @return the integer value or default
     */
    static int optionalInt(
            Map<String, Object> map,
            String key,
            int defaultValue) {
        var value = map.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }

    /**
     * Extracts an optional sub-map with default empty map.
     *
     * @param map the source map
     * @param key the field key
     * @return the sub-map or empty map
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> optionalMap(
            Map<String, Object> map,
            String key) {
        var value = map.get(key);
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    /**
     * Extracts a required sub-map from the map.
     *
     * @param map the source map
     * @param key the field key
     * @param model the model class name (for error messages)
     * @return the sub-map
     * @throws ConfigValidationException if field is missing or not a Map
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> requireMap(
            Map<String, Object> map,
            String key,
            String model) {
        var value = requireField(map, key, model);
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new ConfigValidationException(key, "Map", model);
    }

    /**
     * Extracts an optional list of strings with default empty list.
     *
     * @param map the source map
     * @param key the field key
     * @return the list or empty list (immutable)
     */
    @SuppressWarnings("unchecked")
    static List<String> optionalStringList(
            Map<String, Object> map,
            String key) {
        var value = map.get(key);
        if (value instanceof List<?> list) {
            return List.copyOf((List<String>) list);
        }
        return List.of();
    }

    /**
     * Extracts an optional map of string-to-string with default empty map.
     *
     * @param map the source map
     * @param key the field key
     * @return the string map or empty map (immutable)
     */
    @SuppressWarnings("unchecked")
    static Map<String, String> optionalStringMap(
            Map<String, Object> map,
            String key) {
        var value = map.get(key);
        if (value instanceof Map<?, ?> m) {
            return Map.copyOf((Map<String, String>) m);
        }
        return Map.of();
    }
}
