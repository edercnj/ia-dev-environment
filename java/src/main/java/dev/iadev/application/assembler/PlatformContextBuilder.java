package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes platform-aware template variables from the
 * active platform set.
 *
 * <p>Produces boolean flags ({@code hasClaude},
 * {@code hasCodex}, {@code isMultiPlatform}) and a
 * {@code platforms} list with CLI-friendly names for use
 * in README/CLAUDE.md template rendering.</p>
 *
 * <p>When the platform set is empty or contains all
 * user-selectable platforms, all flags are true and the
 * platforms list contains all CLI names (RULE-001).</p>
 *
 * @see Platform
 * @see ReadmeAssembler
 */
public final class PlatformContextBuilder {

    private static final int FLAGS_MAP_CAPACITY = 8;

    private PlatformContextBuilder() {
        // utility class
    }

    /**
     * Builds platform flags and platforms list from the
     * active platform set.
     *
     * @param platforms the active platforms; empty or all
     *     user-selectable means "all platforms"
     * @return an ordered map with hasClaude, hasCodex,
     *     isMultiPlatform, and platforms
     */
    public static Map<String, Object> buildPlatformFlags(
            Set<Platform> platforms) {
        Set<Platform> effective =
                resolveEffective(platforms);
        Map<String, Object> flags =
                new LinkedHashMap<>(FLAGS_MAP_CAPACITY);

        boolean claude = effective.contains(
                Platform.CLAUDE_CODE);
        boolean codex = effective.contains(
                Platform.CODEX);

        flags.put("hasClaude", claude);
        flags.put("hasCodex", codex);

        int activeCount = countActive(claude, codex);
        flags.put("isMultiPlatform", activeCount >= 2);

        List<String> cliNames = buildCliNames(effective);
        flags.put("platforms", cliNames);

        return flags;
    }

    private static Set<Platform> resolveEffective(
            Set<Platform> platforms) {
        if (platforms.isEmpty()
                || platforms.containsAll(
                        Platform.allUserSelectable())) {
            return Platform.allUserSelectable();
        }
        return platforms;
    }

    private static int countActive(
            boolean claude,
            boolean codex) {
        int count = 0;
        if (claude) {
            count++;
        }
        if (codex) {
            count++;
        }
        return count;
    }

    private static List<String> buildCliNames(
            Set<Platform> effective) {
        return effective.stream()
                .filter(p -> p != Platform.SHARED)
                .map(Platform::cliName)
                .sorted()
                .toList();
    }
}
