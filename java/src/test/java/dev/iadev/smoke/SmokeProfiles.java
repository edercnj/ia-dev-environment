package dev.iadev.smoke;

import dev.iadev.config.ConfigProfiles;

import java.util.List;
import java.util.stream.Stream;

/**
 * Reusable profile providers for smoke tests.
 *
 * <p>Provides {@code @MethodSource}-compatible methods
 * that supply the bundled profile names. All smoke test
 * classes should reference these providers instead of
 * duplicating the profile list.</p>
 *
 * <p>The profiles are sourced from
 * {@link ConfigProfiles#getAvailableStacks()} to ensure
 * consistency with the production code.</p>
 *
 * @see ConfigProfiles
 */
public final class SmokeProfiles {

    /**
     * The 8 profiles used by golden file tests and smoke
     * tests. This excludes java-picocli-cli which is the
     * project's own profile.
     */
    private static final List<String> SMOKE_PROFILES =
            List.of(
                    "go-gin",
                    "java-quarkus",
                    "java-spring",
                    "kotlin-ktor",
                    "python-click-cli",
                    "python-fastapi",
                    "rust-axum",
                    "typescript-nestjs");

    private SmokeProfiles() {
        // utility class
    }

    /**
     * Provides the 8 bundled profile names as a stream,
     * suitable for {@code @MethodSource} parameterization.
     *
     * @return stream of profile name strings
     */
    public static Stream<String> profiles() {
        return SMOKE_PROFILES.stream();
    }

    /**
     * Returns the list of all smoke-testable profiles.
     *
     * @return unmodifiable list of 8 profile name strings
     */
    public static List<String> profileList() {
        return SMOKE_PROFILES;
    }
}
