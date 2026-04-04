package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StackProfile} domain model record.
 */
class StackProfileTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("creates with all fields populated")
        void create_allFields_returnsValidRecord() {
            var props = Map.<String, Object>of("key", "value");
            var profile = new StackProfile(
                    "java-spring", "java", "spring",
                    "maven", props);

            assertThat(profile.name()).isEqualTo("java-spring");
            assertThat(profile.language()).isEqualTo("java");
            assertThat(profile.framework()).isEqualTo("spring");
            assertThat(profile.buildTool()).isEqualTo("maven");
            assertThat(profile.properties()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("defaults null properties to empty map")
        void create_nullProperties_returnsEmptyMap() {
            var profile = new StackProfile(
                    "test", "java", "spring", "maven", null);

            assertThat(profile.properties()).isEmpty();
        }

        @Test
        @DisplayName("creates defensive copy of properties")
        void create_mutableProperties_returnsImmutableCopy() {
            var mutable = new java.util.HashMap<String, Object>();
            mutable.put("key", "value");
            var profile = new StackProfile(
                    "test", "java", "spring", "maven", mutable);

            assertThatThrownBy(
                    () -> profile.properties().put("new", "val"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("rejects null name")
        void create_nullName_throwsException() {
            assertThatThrownBy(
                    () -> new StackProfile(
                            null, "java", "spring",
                            "maven", Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("rejects blank name")
        void create_blankName_throwsException() {
            assertThatThrownBy(
                    () -> new StackProfile(
                            "  ", "java", "spring",
                            "maven", Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }
    }
}
