package dev.iadev.model;

import dev.iadev.exception.ConfigValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ArchitectureConfig")
class ArchitectureConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with all fields")
        void fromMap_allFields_allSet() {
            var map = Map.<String, Object>of(
                    "style", "microservice",
                    "domain_driven", true,
                    "event_driven", true);

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.style()).isEqualTo("microservice");
            assertThat(result.domainDriven()).isTrue();
            assertThat(result.eventDriven()).isTrue();
        }

        @Test
        @DisplayName("defaults domainDriven and eventDriven to false")
        void fromMap_onlyStyle_booleansDefaultFalse() {
            var map = Map.<String, Object>of("style", "library");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.style()).isEqualTo("library");
            assertThat(result.domainDriven()).isFalse();
            assertThat(result.eventDriven()).isFalse();
        }

        @Test
        @DisplayName("throws when style is missing")
        void fromMap_missingStyle_throwsException() {
            var map = Map.<String, Object>of(
                    "domain_driven", true);

            assertThatThrownBy(() -> ArchitectureConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("style");
        }

        @Test
        @DisplayName("non-boolean domain_driven defaults to false")
        void fromMap_nonBooleanDomainDriven_defaultsFalse() {
            var map = Map.<String, Object>of(
                    "style", "monolith",
                    "domain_driven", "yes");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.domainDriven()).isFalse();
        }
    }
}
