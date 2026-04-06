package dev.iadev.domain.stack;

import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Pattern selection by architecture style.
 *
 * <p>Maps architecture styles to pattern category names. Used during
 * generation to select which pattern documentation files to include
 * in the output.</p>
 *
 * <p>Zero external framework dependencies (RULE-007).</p>
 */
public final class PatternMapping {

    private PatternMapping() {
        // utility class
    }

    /** Universal patterns included for all known styles. */
    public static final List<String> UNIVERSAL_PATTERNS =
            List.of("architectural", "data");

    /** Architecture style to pattern categories. */
    public static final Map<String, List<String>>
            ARCHITECTURE_PATTERNS = Map.ofEntries(
                    Map.entry("microservice",
                            List.of("microservice",
                                    "resilience",
                                    "integration")),
                    Map.entry("hexagonal",
                            List.of("integration")),
                    Map.entry("ddd",
                            List.of("integration")),
                    Map.entry("cqrs",
                            List.of("integration")),
                    Map.entry("event-driven",
                            List.of("integration",
                                    "resilience")),
                    Map.entry("monolith",
                            List.of("integration")),
                    Map.entry("library",
                            List.of()));

    /** Event-driven architecture patterns. */
    public static final List<String> EVENT_DRIVEN_PATTERNS = List.of(
            "saga-pattern",
            "outbox-pattern",
            "event-sourcing",
            "dead-letter-queue"
    );

    /**
     * Selects pattern category names for a project configuration.
     *
     * <p>Returns sorted, deduplicated list of pattern category names.
     * Returns empty list for unknown styles.</p>
     *
     * @param config the project configuration
     * @return sorted list of pattern category names
     */
    public static List<String> selectPatterns(ProjectConfig config) {
        String style = config.architecture().style();
        List<String> stylePatterns = ARCHITECTURE_PATTERNS.get(style);
        if (stylePatterns == null) {
            return List.of();
        }
        LinkedHashSet<String> categories =
                new LinkedHashSet<>(UNIVERSAL_PATTERNS);
        categories.addAll(stylePatterns);
        if (config.architecture().eventDriven()) {
            categories.addAll(EVENT_DRIVEN_PATTERNS);
        }
        List<String> sorted = new ArrayList<>(categories);
        sorted.sort(String::compareTo);
        return sorted;
    }
}
