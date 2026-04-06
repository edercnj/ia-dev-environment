package dev.iadev.cli;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine.TypeConversionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for PlatformConverter — Picocli type converter
 * for the Platform enum.
 */
@DisplayName("PlatformConverter")
class PlatformConverterTest {

    private final PlatformConverter converter =
            new PlatformConverter();

    @Nested
    @DisplayName("Valid platform names")
    class ValidPlatformNames {

        @ParameterizedTest
        @CsvSource({
                "claude-code, CLAUDE_CODE",
                "copilot,     COPILOT",
                "codex,       CODEX"
        })
        @DisplayName("converts kebab-case to Platform enum")
        void convert_validName_returnsPlatform(
                String input, Platform expected) {
            Platform result = converter.convert(input);

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("All keyword")
    class AllKeyword {

        @Test
        @DisplayName("returns null for 'all' to signal "
                + "no filter")
        void convert_all_returnsNull() {
            Platform result = converter.convert("all");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Invalid platform names")
    class InvalidPlatformNames {

        @ParameterizedTest
        @ValueSource(strings = {
                "invalid", "CLAUDE_CODE",
                "Claude-Code", "shared",
                "unknown", ""
        })
        @DisplayName("throws TypeConversionException "
                + "with clear message")
        void convert_invalidName_throwsWithMessage(
                String input) {
            assertThatThrownBy(
                    () -> converter.convert(input))
                    .isInstanceOf(
                            TypeConversionException.class)
                    .hasMessageContaining(
                            "Invalid platform:")
                    .hasMessageContaining(input)
                    .hasMessageContaining("claude-code")
                    .hasMessageContaining("copilot")
                    .hasMessageContaining("codex")
                    .hasMessageContaining("all");
        }

        @Test
        @DisplayName("rejects 'shared' as not "
                + "user-selectable")
        void convert_shared_throwsException() {
            assertThatThrownBy(
                    () -> converter.convert("shared"))
                    .isInstanceOf(
                            TypeConversionException.class)
                    .hasMessageContaining(
                            "Invalid platform: 'shared'");
        }
    }

    @Nested
    @DisplayName("Error message format")
    class ErrorMessageFormat {

        @Test
        @DisplayName("error message lists all accepted "
                + "values")
        void convert_invalid_messageListsAcceptedValues() {
            assertThatThrownBy(
                    () -> converter.convert("bad"))
                    .isInstanceOf(
                            TypeConversionException.class)
                    .hasMessageContaining(
                            "Valid values: claude-code, "
                                    + "copilot, codex, all");
        }
    }
}
