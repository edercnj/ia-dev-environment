package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProfileArtifacts}.
 *
 * <p>Validates construction, accessors, immutability,
 * and category count retrieval.</p>
 */
@DisplayName("ProfileArtifacts")
class ProfileArtifactsTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("creates instance with valid data")
        void constructor_validData_createsInstance() {
            var dirs = List.of(".claude", "docs");
            var cats = Map.of("rules", 6, "skills", 14);

            var profile = new ProfileArtifacts(
                    42, dirs, cats);

            assertThat(profile.totalFiles()).isEqualTo(42);
            assertThat(profile.directories())
                    .containsExactly(".claude", "docs");
            assertThat(profile.categories())
                    .containsEntry("rules", 6)
                    .containsEntry("skills", 14);
        }

        @Test
        @DisplayName("enforces immutable directories list")
        void constructor_directories_returnsImmutableList() {
            var profile = new ProfileArtifacts(
                    10, List.of("a"), Map.of("x", 1));

            assertThatThrownBy(
                    () -> profile.directories().add("b"))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("enforces immutable categories map")
        void constructor_categories_returnsImmutableMap() {
            var profile = new ProfileArtifacts(
                    10, List.of("a"), Map.of("x", 1));

            assertThatThrownBy(
                    () -> profile.categories().put("y", 2))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("getCategoryCount")
    class GetCategoryCount {

        @Test
        @DisplayName("returns count for existing category")
        void getCategoryCount_existing_returnsCount() {
            var profile = new ProfileArtifacts(
                    10, List.of(), Map.of("rules", 6));

            assertThat(profile.getCategoryCount("rules"))
                    .isEqualTo(6);
        }

        @Test
        @DisplayName("returns zero for missing category")
        void getCategoryCount_missing_returnsZero() {
            var profile = new ProfileArtifacts(
                    10, List.of(), Map.of("rules", 6));

            assertThat(profile.getCategoryCount("agents"))
                    .isZero();
        }
    }
}
