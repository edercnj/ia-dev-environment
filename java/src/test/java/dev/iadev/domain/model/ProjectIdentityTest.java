package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProjectIdentity")
class ProjectIdentityTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates identity with name and purpose")
        void fromMap_nameAndPurpose_bothSet() {
            var map = Map.<String, Object>of(
                    "name", "my-project",
                    "purpose", "A CLI tool for developers");

            var result = ProjectIdentity.fromMap(map);

            assertThat(result.name()).isEqualTo("my-project");
            assertThat(result.purpose())
                    .isEqualTo("A CLI tool for developers");
        }

        @Test
        @DisplayName("throws when name is missing")
        void fromMap_missingName_throwsException() {
            var map = Map.<String, Object>of("purpose", "Some purpose");

            assertThatThrownBy(() -> ProjectIdentity.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("name")
                    .hasMessageContaining("ProjectIdentity");
        }

        @Test
        @DisplayName("throws when purpose is missing")
        void fromMap_missingPurpose_throwsException() {
            var map = Map.<String, Object>of("name", "my-project");

            assertThatThrownBy(() -> ProjectIdentity.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("purpose")
                    .hasMessageContaining("ProjectIdentity");
        }

        @Test
        @DisplayName("throws when name is null")
        void fromMap_nullName_throwsException() {
            var map = new HashMap<String, Object>();
            map.put("name", null);
            map.put("purpose", "Some purpose");

            assertThatThrownBy(() -> ProjectIdentity.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("throws when name is wrong type")
        void fromMap_wrongTypeName_throwsException() {
            var map = Map.<String, Object>of(
                    "name", 123,
                    "purpose", "Some purpose");

            assertThatThrownBy(() -> ProjectIdentity.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("name")
                    .hasMessageContaining("String");
        }

        @Test
        @DisplayName("throws when both fields missing (empty map)")
        void fromMap_emptyMap_throwsException() {
            assertThatThrownBy(() -> ProjectIdentity.fromMap(Map.of()))
                    .isInstanceOf(ConfigValidationException.class);
        }
    }
}
