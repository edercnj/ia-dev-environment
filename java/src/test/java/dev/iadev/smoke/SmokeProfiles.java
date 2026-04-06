package dev.iadev.smoke;

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
 * <p>The profiles are an explicitly curated list of the
 * 17 bundled profiles used by golden file and smoke tests.
 * The list excludes {@code java-picocli-cli} (the project's
 * own profile) and is maintained in sync with the golden
 * test resources directory.</p>
 */
public final class SmokeProfiles {

    /**
     * The 17 profiles used by golden file tests and smoke
     * tests. This excludes java-picocli-cli which is the
     * project's own profile.
     */
    private static final List<String> SMOKE_PROFILES =
            List.of(
                    "go-gin",
                    "java-quarkus",
                    "java-spring",
                    "java-spring-clickhouse",
                    "java-spring-cqrs-es",
                    "java-spring-elasticsearch",
                    "java-spring-event-driven",
                    "java-spring-fintech-pci",
                    "java-spring-hexagonal",
                    "java-spring-neo4j",
                    "kotlin-ktor",
                    "python-click-cli",
                    "python-fastapi",
                    "python-fastapi-timescale",
                    "rust-axum",
                    "typescript-commander-cli",
                    "typescript-nestjs");

    private SmokeProfiles() {
        // utility class
    }

    /**
     * Provides the 17 bundled profile names as a stream,
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
     * @return unmodifiable list of 17 profile name strings
     */
    public static List<String> profileList() {
        return SMOKE_PROFILES;
    }
}
