package dev.iadev.cli;

import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves effective platform set using precedence:
 * CLI > YAML > default(all).
 *
 * <p>Priority order:
 * <ol>
 *   <li>CLI {@code --platform} flag (highest)</li>
 *   <li>YAML {@code platform:} section</li>
 *   <li>Default: empty set (= all platforms)</li>
 * </ol>
 *
 * <p>CLI platforms arrive as typed {@link Platform} values
 * via {@link PlatformConverter}. The special "all" keyword
 * is represented as a {@code null} element in the list
 * (per PlatformConverter contract).</p>
 *
 * @see GenerateCommand
 * @see PlatformConverter
 */
final class PlatformPrecedenceResolver {

    private PlatformPrecedenceResolver() {
        // utility class
    }

    /**
     * Resolves effective platforms from CLI and YAML.
     *
     * @param cliPlatforms CLI values (typed), may be null
     * @param config       project config with YAML platforms
     * @return resolved set (empty = all)
     */
    static Set<Platform> resolve(
            List<Platform> cliPlatforms,
            ProjectConfig config) {
        if (cliPlatforms != null
                && !cliPlatforms.isEmpty()) {
            return buildPlatformSet(cliPlatforms);
        }
        return config.platforms();
    }

    /**
     * Converts the CLI platform list to a Set.
     *
     * <p>Null list or list containing null (from "all"
     * converter result) produces empty set (no filter).
     * Otherwise produces an EnumSet of the specified
     * platforms.</p>
     *
     * @param platformList the parsed platform list
     * @return immutable set of platforms, empty = no filter
     */
    static Set<Platform> buildPlatformSet(
            List<Platform> platformList) {
        if (platformList == null
                || platformList.isEmpty()) {
            return Set.of();
        }
        boolean containsAllMarker = false;
        for (Platform p : platformList) {
            if (p == null) {
                containsAllMarker = true;
                break;
            }
        }
        if (containsAllMarker) {
            return Set.of();
        }
        for (Platform p : platformList) {
            if (!Platform.allUserSelectable().contains(p)) {
                throw new IllegalArgumentException(
                        "Platform '%s' is not user-selectable"
                                .formatted(p.cliName()));
            }
        }
        return Set.copyOf(EnumSet.copyOf(platformList));
    }
}
