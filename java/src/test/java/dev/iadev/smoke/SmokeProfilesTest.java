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
    @DisplayName("profiles() returns 13 bundled profiles")
    void profiles_returns13Profiles() {
        List<String> profiles =
                SmokeProfiles.profiles().toList();

        assertThat(profiles).hasSize(13);
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
                "java-spring-cqrs-es",
                "java-spring-event-driven",
                "java-spring-fintech-pci",
                "java-spring-hexagonal",
                "kotlin-ktor",
                "python-click-cli",
                "python-fastapi",
                "rust-axum",
                "typescript-commander-cli",
                "typescript-nestjs");
    }

    @Test
    @DisplayName("profileList() returns unmodifiable list")
    void profileList_returnsUnmodifiableList() {
        List<String> list = SmokeProfiles.profileList();

        assertThat(list)
                .hasSize(13)
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
