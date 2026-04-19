package dev.iadev.parallelism;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Catalog of high-contention "hotspot" paths extracted from
 * the {@code parallelism-heuristics} knowledge pack
 * (RULE-004).
 *
 * <p>Two units that both touch a hotspot path MUST be
 * serialized regardless of their per-unit write sets. The
 * catalog is kept intentionally small and is the single
 * source of truth used by {@link CollisionDetector}.</p>
 *
 * <p>Paths ending in {@code /**} are treated as glob
 * prefixes: a path matches the pattern iff it starts with
 * the prefix before the {@code /**} marker.</p>
 */
public final class HotspotCatalog {

    private static final List<String> SEED = List.of(
            "java/src/main/java/dev/iadev/application/"
                    + "assembler/SettingsAssembler.java",
            "java/src/main/java/dev/iadev/application/"
                    + "assembler/HooksAssembler.java",
            "java/src/main/java/dev/iadev/application/"
                    + "assembler/SkillsAssembler.java",
            "CLAUDE.md",
            ".gitignore",
            "CHANGELOG.md",
            "pom.xml",
            "java/src/test/resources/golden/**",
            ".claude/templates/**"
    );

    private final List<String> patterns;

    public HotspotCatalog() {
        this(SEED);
    }

    /**
     * Constructs a catalog from an arbitrary pattern list
     * (primarily for tests).
     *
     * @param patterns ordered, non-null list of hotspot
     *                 patterns (glob {@code /**} suffix
     *                 permitted)
     */
    public HotspotCatalog(List<String> patterns) {
        Objects.requireNonNull(patterns, "patterns");
        this.patterns = List.copyOf(patterns);
    }

    /**
     * Returns the first hotspot pattern that matches the
     * given path, or {@link Optional#empty()} if none match.
     *
     * @param path repository-relative path to test
     * @return matching pattern, or empty
     */
    public Optional<String> matchHotspot(String path) {
        if (path == null) {
            return Optional.empty();
        }
        for (String pattern : patterns) {
            if (matches(pattern, path)) {
                return Optional.of(pattern);
            }
        }
        return Optional.empty();
    }

    private static boolean matches(
            String pattern, String path) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern
                    .substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return pattern.equals(path);
    }

    /**
     * @return the immutable list of configured hotspot
     *         patterns (for diagnostics).
     */
    public List<String> patterns() {
        return patterns;
    }
}
