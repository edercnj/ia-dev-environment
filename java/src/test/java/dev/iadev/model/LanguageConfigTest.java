package dev.iadev.model;

import dev.iadev.exception.ConfigValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LanguageConfig")
class LanguageConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with name and version")
        void fromMap_nameAndVersion_bothSet() {
            var map = Map.<String, Object>of(
                    "name", "java", "version", "21");

            var result = LanguageConfig.fromMap(map);

            assertThat(result.name()).isEqualTo("java");
            assertThat(result.version()).isEqualTo("21");
        }

        @Test
        @DisplayName("throws when name is missing")
        void fromMap_missingName_throwsException() {
            var map = Map.<String, Object>of("version", "21");

            assertThatThrownBy(() -> LanguageConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("throws when version is missing")
        void fromMap_missingVersion_throwsException() {
            var map = Map.<String, Object>of("name", "java");

            assertThatThrownBy(() -> LanguageConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("version");
        }
    }
}
