package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Platform enum.
 */
@DisplayName("Platform")
class PlatformTest {

    @Nested
    @DisplayName("enum values")
    class EnumValues {

        @Test
        @DisplayName("has exactly 3 values")
        void values_whenCalled_returnsExactlyThree() {
            assertThat(Platform.values()).hasSize(3);
        }

        @Test
        @DisplayName("contains CLAUDE_CODE, CODEX, SHARED")
        void values_whenCalled_containsAllExpected() {
            assertThat(Platform.values())
                    .containsExactly(
                            Platform.CLAUDE_CODE,
                            Platform.CODEX,
                            Platform.SHARED);
        }
    }

    @Nested
    @DisplayName("cliName")
    class CliName {

        @ParameterizedTest
        @CsvSource({
                "CLAUDE_CODE, claude-code",
                "CODEX,       codex",
                "SHARED,      shared"
        })
        @DisplayName("returns kebab-case name")
        void cliName_whenCalled_returnsKebabCase(
                Platform platform, String expected) {
            assertThat(platform.cliName())
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("fromCliName")
    class FromCliName {

        @ParameterizedTest
        @CsvSource({
                "claude-code, CLAUDE_CODE",
                "codex,       CODEX",
                "shared,      SHARED"
        })
        @DisplayName("returns matching platform for "
                + "valid CLI name")
        void fromCliName_validName_returnsPresent(
                String cliName, Platform expected) {
            Optional<Platform> result =
                    Platform.fromCliName(cliName);

            assertThat(result)
                    .isPresent()
                    .contains(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "invalid", "copilot", "CLAUDE_CODE",
                "Claude-Code", "unknown-platform"
        })
        @DisplayName("returns empty for invalid CLI name")
        void fromCliName_invalidName_returnsEmpty(
                String cliName) {
            assertThat(Platform.fromCliName(cliName))
                    .isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("returns empty for null or empty "
                + "input")
        void fromCliName_nullOrEmpty_returnsEmpty(
                String cliName) {
            assertThat(Platform.fromCliName(cliName))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("allUserSelectable")
    class AllUserSelectable {

        @Test
        @DisplayName("returns exactly 2 platforms")
        void allUserSelectable_returnsTwo() {
            EnumSet<Platform> selectable =
                    Platform.allUserSelectable();

            assertThat(selectable).hasSize(2);
        }

        @Test
        @DisplayName("contains CLAUDE_CODE, CODEX")
        void allUserSelectable_containsExpected() {
            EnumSet<Platform> selectable =
                    Platform.allUserSelectable();

            assertThat(selectable).containsExactlyInAnyOrder(
                    Platform.CLAUDE_CODE,
                    Platform.CODEX);
        }

        @Test
        @DisplayName("excludes SHARED")
        void allUserSelectable_excludesShared() {
            EnumSet<Platform> selectable =
                    Platform.allUserSelectable();

            assertThat(selectable)
                    .doesNotContain(Platform.SHARED);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString returns enum constant name")
        void toString_returnsEnumName() {
            assertThat(Platform.CLAUDE_CODE.toString())
                    .isEqualTo("CLAUDE_CODE");
        }
    }
}
