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
            "claude-code, copilot, codex, all";

    private PlatformParser() {
        // utility class
    }

    /**
     * Parses the {@code platform} field from the map.
     *
     * @param map the root YAML map
     * @return immutable set (empty = all platforms)
     */
    @SuppressWarnings("unchecked")
    static Set<Platform> parse(Map<String, Object> map) {
        Object raw = map.get("platform");
        if (raw == null) {
            return Set.of();
        }
        if (raw instanceof String s) {
            return parseSingle(s);
        }
        if (raw instanceof List<?> list) {
            return parseList((List<String>) list);
        }
        return Set.of();
    }

    private static Set<Platform> parseSingle(String value) {
        if ("all".equals(value)) {
            return Set.of();
        }
        Optional<Platform> platform =
                Platform.fromCliName(value);
        if (platform.isEmpty()) {
            throw new ConfigValidationException(
                    ("Invalid platform value: '%s'"
                            + " in YAML config."
                            + " Valid values: %s")
                            .formatted(value,
                                    VALID_VALUES));
        }
        return Set.of(platform.orElseThrow());
    }

    private static Set<Platform> parseList(
            List<String> values) {
        List<Platform> resolved = new ArrayList<>();
        for (String v : values) {
            if ("all".equals(v)) {
                return Set.of();
            }
            Optional<Platform> platform =
                    Platform.fromCliName(v);
            if (platform.isEmpty()) {
                throw new ConfigValidationException(
                        ("Invalid platform value: '%s'"
                                + " in YAML config."
                                + " Valid values: %s")
                                .formatted(v,
                                        VALID_VALUES));
            }
            resolved.add(platform.orElseThrow());
        }
        if (resolved.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(EnumSet.copyOf(resolved));
    }
}
