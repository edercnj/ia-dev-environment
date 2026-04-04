package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FrameworkConfig")
class FrameworkConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with all fields")
        void fromMap_allFields_allSet() {
            var map = Map.<String, Object>of(
                    "name", "spring-boot",
                    "version", "3.4",
                    "build_tool", "maven",
                    "native_build", true);

            var result = FrameworkConfig.fromMap(map);

            assertThat(result.name()).isEqualTo("spring-boot");
            assertThat(result.version()).isEqualTo("3.4");
            assertThat(result.buildTool()).isEqualTo("maven");
            assertThat(result.nativeBuild()).isTrue();
        }

        @Test
        @DisplayName("defaults buildTool to pip and nativeBuild to false")
        void fromMap_onlyRequired_defaultsApplied() {
            var map = Map.<String, Object>of(
                    "name", "fastapi", "version", "0.104");

            var result = FrameworkConfig.fromMap(map);

            assertThat(result.buildTool()).isEqualTo("pip");
            assertThat(result.nativeBuild()).isFalse();
        }

        @Test
        @DisplayName("throws when name is missing")
        void fromMap_missingName_throwsException() {
            var map = Map.<String, Object>of("version", "3.4");

            assertThatThrownBy(() -> FrameworkConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("throws when version is missing")
        void fromMap_missingVersion_throwsException() {
            var map = Map.<String, Object>of("name", "quarkus");

            assertThatThrownBy(() -> FrameworkConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("version");
        }
    }
}
