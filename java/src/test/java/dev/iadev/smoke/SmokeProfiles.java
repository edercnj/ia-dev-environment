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
 * <p>The profiles are the 9 bundled Java profiles used
 * by golden file and smoke tests since EPIC-0048
 * (v4.0.0 Java-only scope, ADR-0048-A). The list
 * excludes {@code java-picocli-cli} (the project's own
 * profile) and is maintained in sync with the golden
 * test resources directory.</p>
 */
public final class SmokeProfiles {

    /**
     * The 9 Java profiles used by golden file tests and
     * smoke tests since EPIC-0048 (non-Java profiles
     * removed). Excludes java-picocli-cli which is the
     * project's own profile.
     */
    private static final List<String> SMOKE_PROFILES =
            List.of(
                    "java-quarkus",
                    "java-spring",
                    "java-spring-clickhouse",
                    "java-spring-cqrs-es",
                    "java-spring-elasticsearch",
                    "java-spring-event-driven",
                    "java-spring-fintech-pci",
                    "java-spring-hexagonal",
                    "java-spring-neo4j");

    private SmokeProfiles() {
        // utility class
    }

    /**
     * Provides the 9 bundled Java profile names as a
     * stream, suitable for {@code @MethodSource}
     * parameterization.
     *
     * @return stream of profile name strings
     */
    public static Stream<String> profiles() {
        return SMOKE_PROFILES.stream();
    }

    /**
     * Returns the list of all smoke-testable profiles.
     *
     * @return unmodifiable list of 9 profile name strings
     */
    public static List<String> profileList() {
        return SMOKE_PROFILES;
    }
}
