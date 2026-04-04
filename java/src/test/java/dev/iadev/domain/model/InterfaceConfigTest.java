package dev.iadev.domain.model;

import dev.iadev.exception.ConfigValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InterfaceConfig")
class InterfaceConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with all fields")
        void fromMap_allFields_allSet() {
            var map = Map.<String, Object>of(
                    "type", "event-consumer",
                    "spec", "asyncapi-2.6",
                    "broker", "kafka");

            var result = InterfaceConfig.fromMap(map);

            assertThat(result.type()).isEqualTo("event-consumer");
            assertThat(result.spec()).isEqualTo("asyncapi-2.6");
            assertThat(result.broker()).isEqualTo("kafka");
        }

        @Test
        @DisplayName("defaults spec and broker to empty string")
        void fromMap_onlyType_defaultsApplied() {
            var map = Map.<String, Object>of("type", "rest");

            var result = InterfaceConfig.fromMap(map);

            assertThat(result.type()).isEqualTo("rest");
            assertThat(result.spec()).isEmpty();
            assertThat(result.broker()).isEmpty();
        }

        @Test
        @DisplayName("throws when type is missing")
        void fromMap_missingType_throwsException() {
            assertThatThrownBy(
                    () -> InterfaceConfig.fromMap(Map.of()))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("type");
        }
    }
}
