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
 * <p>Produces the {@code hasClaude} boolean flag, the
 * {@code isMultiPlatform} flag (retained for template
 * compatibility and always {@code false} after EPIC-0034
 * reduced the generator to a single user-selectable
 * platform), and a {@code platforms} list with CLI-friendly
 * names for use in README and CLAUDE.md template
 * rendering.</p>
 *
 * <p>The {@code platforms} parameter is retained so
 * callers can express intent, but the resolution always
 * converges on {@link Platform#CLAUDE_CODE} because it is
 * the only user-selectable platform.</p>
 *
 * @see Platform
 * @see ReadmeAssembler
 */
public final class PlatformContextBuilder {

    private static final int FLAGS_MAP_CAPACITY = 4;

    private PlatformContextBuilder() {
        // utility class
    }

    /**
     * Builds platform flags and platforms list from the
     * active platform set.
     *
     * @param platforms the active platforms; empty or all
     *     user-selectable means "all platforms"
     * @return an ordered map with hasClaude,
     *     isMultiPlatform, and platforms
     */
    public static Map<String, Object> buildPlatformFlags(
            Set<Platform> platforms) {
        Set<Platform> effective =
                resolveEffective(platforms);
        Map<String, Object> flags =
                new LinkedHashMap<>(FLAGS_MAP_CAPACITY);

        flags.put("hasClaude",
                effective.contains(Platform.CLAUDE_CODE));
        flags.put("isMultiPlatform", false);
        flags.put("platforms", buildCliNames(effective));

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

    private static List<String> buildCliNames(
            Set<Platform> effective) {
        return effective.stream()
                .filter(p -> p != Platform.SHARED)
                .map(Platform::cliName)
                .sorted()
                .toList();
    }
}
