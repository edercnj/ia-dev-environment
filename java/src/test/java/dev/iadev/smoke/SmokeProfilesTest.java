package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SmokeProfiles}.
 */
@DisplayName("SmokeProfiles")
class SmokeProfilesTest {

    @Test
    @DisplayName("profiles() returns 17 bundled profiles")
    void profiles_returns17Profiles() {
        List<String> profiles =
                SmokeProfiles.profiles().toList();

        assertThat(profiles).hasSize(17);
    }

    @Test
    @DisplayName("profiles() contains all expected stacks")
    void profiles_containsExpectedStacks() {
        List<String> profiles =
                SmokeProfiles.profiles().toList();

        assertThat(profiles).containsExactlyInAnyOrder(
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
    }

    @Test
    @DisplayName("profileList() returns unmodifiable list")
    void profileList_returnsUnmodifiableList() {
        List<String> list = SmokeProfiles.profileList();

        assertThat(list)
                .hasSize(17)
                .isUnmodifiable();
    }

    @Test
    @DisplayName("profiles() excludes java-picocli-cli")
    void profiles_excludesPicocliCli() {
        List<String> profiles =
                SmokeProfiles.profiles().toList();

        assertThat(profiles)
                .doesNotContain("java-picocli-cli");
    }

    @Test
    @DisplayName("profiles() is sorted alphabetically")
    void profiles_isSorted() {
        List<String> profiles =
                SmokeProfiles.profiles().toList();

        assertThat(profiles).isSorted();
    }
}
