package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Filters assembler descriptors by target platform(s).
 *
 * <p>Given a set of requested platforms, this filter
 * retains only descriptors whose platforms have a
 * non-empty intersection with the effective set
 * (requested + SHARED). Preserves original order
 * (RULE-002).</p>
 *
 * <p>Rules enforced:
 * <ul>
 *   <li>RULE-001: Empty set or all user-selectable
 *       platforms = no filter (backward compatible)</li>
 *   <li>RULE-002: Original descriptor order preserved</li>
 *   <li>RULE-003: SHARED assemblers always included</li>
 *   <li>RULE-009: Platforms compose via union</li>
 * </ul>
 *
 * @see AssemblerDescriptor
 * @see Platform
 */
public final class PlatformFilter {

    private PlatformFilter() {
        // utility class
    }

    /**
     * Filters the given descriptors by the requested
     * platforms.
     *
     * <p>If {@code platforms} is empty or contains all
     * user-selectable platforms, returns the original
     * list unchanged. Otherwise, adds {@link
     * Platform#SHARED} to the effective set and keeps
     * only descriptors with at least one matching
     * platform.</p>
     *
     * @param descriptors the ordered list of assembler
     *                    descriptors (never null)
     * @param platforms   the requested platforms
     *                    (never null, empty = no filter)
     * @return filtered list preserving original order
     */
    public static List<AssemblerDescriptor> filter(
            List<AssemblerDescriptor> descriptors,
            Set<Platform> platforms) {
        if (shouldSkipFilter(platforms)) {
            return descriptors;
        }
        Set<Platform> effective =
                buildEffectiveSet(platforms);
        return descriptors.stream()
                .filter(d -> hasIntersection(
                        d.platforms(), effective))
                .toList();
    }

    private static boolean shouldSkipFilter(
            Set<Platform> platforms) {
        return platforms.isEmpty()
                || platforms.containsAll(
                        Platform.allUserSelectable());
    }

    private static Set<Platform> buildEffectiveSet(
            Set<Platform> platforms) {
        EnumSet<Platform> effective =
                EnumSet.copyOf(platforms);
        effective.add(Platform.SHARED);
        return Collections.unmodifiableSet(effective);
    }

    private static boolean hasIntersection(
            Set<Platform> descriptorPlatforms,
            Set<Platform> effectivePlatforms) {
        return !Collections.disjoint(
                descriptorPlatforms, effectivePlatforms);
    }
}
