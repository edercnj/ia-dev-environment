package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservabilityConfig")
class ObservabilityConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with all fields")
        void fromMap_allFields_allSet() {
            var map = Map.<String, Object>of(
                    "tool", "prometheus",
                    "metrics", "micrometer",
                    "tracing", "jaeger");

            var result = ObservabilityConfig.fromMap(map);

            assertThat(result.tool()).isEqualTo("prometheus");
            assertThat(result.metrics()).isEqualTo("micrometer");
            assertThat(result.tracing()).isEqualTo("jaeger");
        }

        @Test
        @DisplayName("empty map defaults all fields to 'none'")
        void fromMap_emptyMap_allDefaultsNone() {
            var result = ObservabilityConfig.fromMap(Map.of());

            assertThat(result.tool()).isEqualTo("none");
            assertThat(result.metrics()).isEqualTo("none");
            assertThat(result.tracing()).isEqualTo("none");
        }

        @Test
        @DisplayName("partial map defaults missing fields to 'none'")
        void fromMap_partialMap_missingDefaultsNone() {
            var map = Map.<String, Object>of("tool", "grafana");

            var result = ObservabilityConfig.fromMap(map);

            assertThat(result.tool()).isEqualTo("grafana");
            assertThat(result.metrics()).isEqualTo("none");
            assertThat(result.tracing()).isEqualTo("none");
        }
    }
}
