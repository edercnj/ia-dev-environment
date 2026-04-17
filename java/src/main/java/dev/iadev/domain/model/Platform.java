package dev.iadev.domain.model;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Target AI platform for assembler output generation.
 *
 * <p>Each platform maps to a specific tooling ecosystem:
 * <ul>
 *   <li>{@link #CLAUDE_CODE} — Anthropic Claude Code
 *       ({@code .claude/})</li>
 *   <li>{@link #SHARED} — Platform-agnostic artifacts
 *       (docs, CI/CD, constitution)</li>
 *   <li>{@link #ALL} — Sentinel value produced ONLY by
 *       {@code PlatformConverter} when the user types
 *       {@code --platform all}. Never user-selectable via
 *       {@link #fromCliName(String)} and excluded from
 *       {@link #allUserSelectable()}.</li>
 * </ul>
 *
 * <p>{@link #SHARED} is never user-selectable via CLI;
 * shared assemblers are always included automatically
 * (RULE-003).</p>
 *
 * @see AssemblerDescriptor
 */
public enum Platform {

    /** Anthropic Claude Code platform. */
    CLAUDE_CODE("claude-code"),

    /** Platform-agnostic shared artifacts. */
    SHARED("shared"),

    /**
     * Sentinel meaning "all platforms — no filter". Produced
     * by the CLI type converter when the user types "all".
     * Rule 03 (never return null) forbids the previous
     * {@code null} representation of this intent.
     */
    ALL("__all__");

    private final String cliName;

    Platform(String cliName) {
        this.cliName = cliName;
    }

    /**
     * Returns the kebab-case CLI name for this platform.
     *
     * @return the CLI-friendly name (e.g., "claude-code")
     */
    public String cliName() {
        return cliName;
    }

    /**
     * Resolves a platform from its CLI name.
     *
     * <p>The {@link #ALL} sentinel is never resolvable via
     * this method — its synthetic cliName ({@code __all__})
     * is an internal marker, and the {@code "all"} keyword is
     * handled exclusively by {@code PlatformConverter}.</p>
     *
     * @param cliName the kebab-case CLI name, may be null
     * @return the matching platform, or empty if not found
     */
    public static Optional<Platform> fromCliName(
            String cliName) {
        if (cliName == null || cliName.isEmpty()) {
            return Optional.empty();
        }
        for (Platform p : values()) {
            if (p == ALL) {
                continue;
            }
            if (p.cliName.equals(cliName)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all platforms selectable by the user via CLI.
     *
     * <p>{@link #SHARED} is excluded because shared
     * assemblers are always included automatically
     * (RULE-003).</p>
     *
     * @return an {@link EnumSet} containing all platforms
     *         except {@link #SHARED}
     */
    public static EnumSet<Platform> allUserSelectable() {
        return EnumSet.of(CLAUDE_CODE);
    }
}
