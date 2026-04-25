package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the package-private PlatformParser utility.
 * Coverage-completion tests added in EPIC-0050 to close the
 * branch-coverage gate (see Rule 05 Quality Gates).
 */
@DisplayName("PlatformParser")
class PlatformParserTest {

    @Nested
    @DisplayName("absent field")
    class Absent {

        @Test
        @DisplayName("returns empty set when 'platform'"
                + " key is missing")
        void parse_missingField_returnsEmpty() {
            Set<Platform> result = PlatformParser.parse(
                    Map.of("other", "value"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("string scalar")
    class SingleString {

        @Test
        @DisplayName("returns empty set for 'all'")
        void parse_allString_returnsEmpty() {
            Set<Platform> result = PlatformParser.parse(
                    Map.of("platform", "all"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns singleton for valid"
                + " user-selectable platform")
        void parse_validSingle_returnsSingleton() {
            Set<Platform> result = PlatformParser.parse(
                    Map.of("platform", "claude-code"));

            assertThat(result).containsExactly(
                    Platform.CLAUDE_CODE);
        }

        @Test
        @DisplayName("throws on unknown platform name")
        void parse_unknownPlatform_throws() {
            assertThatThrownBy(() -> PlatformParser.parse(
                    Map.of("platform", "bogus-ide")))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining("Invalid platform"
                            + " value");
        }
    }

    @Nested
    @DisplayName("list")
    class ListValue {

        @Test
        @DisplayName("returns empty set when list contains"
                + " 'all' as any element")
        void parse_listWithAll_returnsEmpty() {
            Set<Platform> result = PlatformParser.parse(
                    Map.of("platform",
                            List.of("all")));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("resolves list of valid platform"
                + " names")
        void parse_validList_returnsResolved() {
            Set<Platform> result = PlatformParser.parse(
                    Map.of("platform",
                            List.of("claude-code")));

            assertThat(result).containsExactly(
                    Platform.CLAUDE_CODE);
        }

        @Test
        @DisplayName("returns empty set for empty list")
        void parse_emptyList_returnsEmpty() {
            Set<Platform> result = PlatformParser.parse(
                    Map.of("platform", List.of()));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("throws when list contains null"
                + " element")
        void parse_listWithNull_throws() {
            java.util.List<Object> listWithNull =
                    new java.util.ArrayList<>();
            listWithNull.add(null);

            assertThatThrownBy(() -> PlatformParser.parse(
                    Map.of("platform", listWithNull)))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining("Null element");
        }

        @Test
        @DisplayName("throws when list contains"
                + " non-string element")
        void parse_listWithNonString_throws() {
            assertThatThrownBy(() -> PlatformParser.parse(
                    Map.of("platform",
                            List.of(42))))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining("Invalid platform"
                            + " list element");
        }

        @Test
        @DisplayName("throws on unknown platform name"
                + " inside list")
        void parse_listWithUnknownName_throws() {
            assertThatThrownBy(() -> PlatformParser.parse(
                    Map.of("platform",
                            List.of("bogus-ide"))))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining("Invalid platform"
                            + " value");
        }
    }

    @Nested
    @DisplayName("invalid types")
    class InvalidType {

        @Test
        @DisplayName("throws when 'platform' is neither"
                + " string nor list (e.g., number)")
        void parse_numberValue_throws() {
            assertThatThrownBy(() -> PlatformParser.parse(
                    Map.of("platform", 42)))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining("Invalid platform"
                            + " value type");
        }

        @Test
        @DisplayName("throws when 'platform' is a map")
        void parse_mapValue_throws() {
            assertThatThrownBy(() -> PlatformParser.parse(
                    Map.of("platform",
                            Map.of("k", "v"))))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining("Invalid platform"
                            + " value type");
        }
    }
}
