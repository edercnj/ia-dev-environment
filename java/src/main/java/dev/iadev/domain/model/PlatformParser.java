package dev.iadev.domain.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses the optional {@code platform} field from a
 * YAML-parsed map into a set of {@link Platform} values.
 *
 * <p>Supports three YAML formats:
 * <ul>
 *   <li>Absent: returns empty set (= all)</li>
 *   <li>String: single value or "all"</li>
 *   <li>List: multiple platform names</li>
 * </ul>
 *
 * @see ProjectConfig
 * @see Platform
 */
final class PlatformParser {

    private static final String VALID_VALUES =
            "claude-code, codex, all";

    private PlatformParser() {
        // utility class
    }

    /**
     * Parses the {@code platform} field from the map.
     *
     * @param map the root YAML map
     * @return immutable set (empty = all platforms)
     */
    static Set<Platform> parse(Map<String, Object> map) {
        Object raw = map.get("platform");
        if (raw == null) {
            return Set.of();
        }
        if (raw instanceof String s) {
            return parseSingle(s);
        }
        if (raw instanceof List<?> list) {
            return parseList(validateListElements(list));
        }
        throw new ConfigValidationException(
                ("Invalid platform value type: '%s'"
                        + " in YAML config."
                        + " Expected a string, a list,"
                        + " or the field to be absent.")
                        .formatted(
                                raw.getClass()
                                        .getSimpleName()));
    }

    private static Set<Platform> parseSingle(String value) {
        if ("all".equals(value)) {
            return Set.of();
        }
        Platform platform = Platform.fromCliName(value)
                .orElseThrow(() ->
                        new ConfigValidationException(
                                ("Invalid platform value:"
                                        + " '%s' in YAML"
                                        + " config. Valid"
                                        + " values: %s")
                                        .formatted(value,
                                                VALID_VALUES)));
        rejectNonSelectable(platform, value);
        return Set.of(platform);
    }

    private static void rejectNonSelectable(
            Platform platform, String rawValue) {
        if (!Platform.allUserSelectable()
                .contains(platform)) {
            throw new ConfigValidationException(
                    ("Platform '%s' is not"
                            + " user-selectable in YAML"
                            + " config. Valid values: %s")
                            .formatted(rawValue,
                                    VALID_VALUES));
        }
    }

    private static List<String> validateListElements(
            List<?> list) {
        List<String> result = new ArrayList<>();
        for (Object element : list) {
            if (element == null) {
                throw new ConfigValidationException(
                        "Null element in platform list"
                                + " in YAML config."
                                + " Expected strings."
                                + " Valid values: "
                                + VALID_VALUES);
            }
            if (!(element instanceof String s)) {
                throw new ConfigValidationException(
                        ("Invalid platform list element:"
                                + " '%s' (type: %s)."
                                + " Expected strings."
                                + " Valid values: %s")
                                .formatted(
                                        element,
                                        element.getClass()
                                                .getSimpleName(),
                                        VALID_VALUES));
            }
            result.add(s);
        }
        return result;
    }

    private static Set<Platform> parseList(
            List<String> values) {
        List<Platform> resolved = new ArrayList<>();
        for (String v : values) {
            if ("all".equals(v)) {
                return Set.of();
            }
            Platform platform = Platform.fromCliName(v)
                    .orElseThrow(() ->
                            new ConfigValidationException(
                                    ("Invalid platform"
                                            + " value: '%s'"
                                            + " in YAML"
                                            + " config."
                                            + " Valid"
                                            + " values: %s")
                                            .formatted(v,
                                                    VALID_VALUES)));
            rejectNonSelectable(platform, v);
            resolved.add(platform);
        }
        if (resolved.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(EnumSet.copyOf(resolved));
    }
}
