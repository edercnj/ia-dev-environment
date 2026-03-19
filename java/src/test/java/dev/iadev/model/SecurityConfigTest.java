package dev.iadev.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecurityConfig")
class SecurityConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with frameworks list")
        void fromMap_withFrameworks_listPopulated() {
            var map = Map.<String, Object>of(
                    "frameworks",
                    List.of("spring-security", "oauth2"));

            var result = SecurityConfig.fromMap(map);

            assertThat(result.frameworks())
                    .containsExactly("spring-security", "oauth2");
        }

        @Test
        @DisplayName("empty map defaults to empty frameworks list")
        void fromMap_emptyMap_emptyList() {
            var result = SecurityConfig.fromMap(Map.of());

            assertThat(result.frameworks()).isEmpty();
        }
    }

    @Test
    @DisplayName("frameworks list is immutable")
    void frameworks_immutable_throwsOnModification() {
        var mutableList = new ArrayList<>(List.of("keycloak"));
        var config = new SecurityConfig(mutableList);

        assertThatThrownBy(() -> config.frameworks().add("oauth2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensive copy prevents mutation via original list")
    void frameworks_defensiveCopy_originalMutationIgnored() {
        var mutableList = new ArrayList<>(List.of("keycloak"));
        var config = new SecurityConfig(mutableList);
        mutableList.add("oauth2");

        assertThat(config.frameworks()).containsExactly("keycloak");
    }
}
