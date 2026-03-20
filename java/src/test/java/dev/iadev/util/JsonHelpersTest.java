package dev.iadev.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link JsonHelpers} — centralized JSON escape
 * and indentation utilities implementing RFC 8259 Section 7.
 */
@DisplayName("JsonHelpers")
class JsonHelpersTest {

    @Nested
    @DisplayName("escapeJson — degenerate cases")
    class EscapeJsonDegenerate {

        @Test
        @DisplayName("empty string returns empty string")
        void escapeJson_emptyString_returnsEmpty() {
            assertThat(JsonHelpers.escapeJson(""))
                    .isEmpty();
        }

        @Test
        @DisplayName("null returns empty string")
        void escapeJson_null_returnsEmpty() {
            assertThat(JsonHelpers.escapeJson(null))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("escapeJson — individual RFC 8259 escapes")
    class EscapeJsonIndividual {

        @Test
        @DisplayName("escapes double quote")
        void escapeJson_doubleQuote_escaped() {
            assertThat(JsonHelpers.escapeJson("a\"b"))
                    .isEqualTo("a\\\"b");
        }

        @Test
        @DisplayName("escapes backslash")
        void escapeJson_backslash_escaped() {
            assertThat(JsonHelpers.escapeJson("a\\b"))
                    .isEqualTo("a\\\\b");
        }

        @Test
        @DisplayName("escapes newline")
        void escapeJson_newline_escaped() {
            assertThat(JsonHelpers.escapeJson("a\nb"))
                    .isEqualTo("a\\nb");
        }

        @Test
        @DisplayName("escapes carriage return")
        void escapeJson_carriageReturn_escaped() {
            assertThat(JsonHelpers.escapeJson("a\rb"))
                    .isEqualTo("a\\rb");
        }

        @Test
        @DisplayName("escapes tab")
        void escapeJson_tab_escaped() {
            assertThat(JsonHelpers.escapeJson("a\tb"))
                    .isEqualTo("a\\tb");
        }

        @Test
        @DisplayName("escapes backspace")
        void escapeJson_backspace_escaped() {
            assertThat(JsonHelpers.escapeJson("a\bb"))
                    .isEqualTo("a\\bb");
        }

        @Test
        @DisplayName("escapes form feed")
        void escapeJson_formFeed_escaped() {
            assertThat(JsonHelpers.escapeJson("a\fb"))
                    .isEqualTo("a\\fb");
        }
    }

    @Nested
    @DisplayName("escapeJson — all RFC 8259 escapes combined")
    class EscapeJsonAllCombined {

        @Test
        @DisplayName("escapes all special characters"
                + " in one string")
        void escapeJson_allSpecialChars_allEscaped() {
            String input =
                    "\"\\\n\r\t\b\f";
            String expected =
                    "\\\"\\\\\\n\\r\\t\\b\\f";

            assertThat(JsonHelpers.escapeJson(input))
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("escapeJson — Unicode control characters")
    class EscapeJsonUnicode {

        @Test
        @DisplayName("escapes U+0000 (NUL)")
        void escapeJson_nul_escapedAsUnicode() {
            assertThat(JsonHelpers.escapeJson("\u0000"))
                    .isEqualTo("\\u0000");
        }

        @Test
        @DisplayName("escapes U+0001 (SOH)")
        void escapeJson_soh_escapedAsUnicode() {
            assertThat(JsonHelpers.escapeJson("\u0001"))
                    .isEqualTo("\\u0001");
        }

        @Test
        @DisplayName("escapes U+001F (US)")
        void escapeJson_us_escapedAsUnicode() {
            assertThat(JsonHelpers.escapeJson("\u001F"))
                    .isEqualTo("\\u001f");
        }

        @Test
        @DisplayName("does not escape U+0020 (space)")
        void escapeJson_space_notEscaped() {
            assertThat(JsonHelpers.escapeJson(" "))
                    .isEqualTo(" ");
        }

        @ParameterizedTest(name = "U+{0} is escaped")
        @ValueSource(ints = {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05,
            0x06, 0x07, 0x0e, 0x0f, 0x10, 0x11,
            0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
            0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d,
            0x1e, 0x1f})
        @DisplayName("escapes control char")
        void escapeJson_controlChars_allEscaped(
                int codePoint) {
            String input = String.valueOf(
                    (char) codePoint);
            String result =
                    JsonHelpers.escapeJson(input);

            assertThat(result)
                    .startsWith("\\u00")
                    .hasSize(6);
        }
    }

    @Nested
    @DisplayName("escapeJson — passthrough")
    class EscapeJsonPassthrough {

        @Test
        @DisplayName("plain ASCII unchanged")
        void escapeJson_plainAscii_unchanged() {
            assertThat(JsonHelpers.escapeJson("hello"))
                    .isEqualTo("hello");
        }

        @Test
        @DisplayName("non-ASCII Unicode unchanged")
        void escapeJson_nonAsciiUnicode_unchanged() {
            assertThat(
                    JsonHelpers.escapeJson("\u00e9\u00f1"))
                    .isEqualTo("\u00e9\u00f1");
        }
    }

    @Nested
    @DisplayName("indent — level 0")
    class IndentZero {

        @Test
        @DisplayName("returns empty string")
        void indent_zero_returnsEmpty() {
            assertThat(JsonHelpers.indent(0)).isEmpty();
        }
    }

    @Nested
    @DisplayName("indent — positive levels")
    class IndentPositive {

        @Test
        @DisplayName("level 1 returns 2 spaces")
        void indent_one_returns2Spaces() {
            assertThat(JsonHelpers.indent(1))
                    .isEqualTo("  ");
        }

        @Test
        @DisplayName("level 5 returns 10 spaces")
        void indent_five_returns10Spaces() {
            assertThat(JsonHelpers.indent(5))
                    .isEqualTo("          ");
        }

        @ParameterizedTest(name = "level {0} -> {1} spaces")
        @CsvSource({"1,2", "2,4", "3,6", "4,8", "10,20"})
        @DisplayName("returns level * 2 spaces")
        void indent_variousLevels_correctSpaces(
                int level, int expectedSpaces) {
            String result = JsonHelpers.indent(level);

            assertThat(result).hasSize(expectedSpaces);
            assertThat(result).matches("^ *$");
        }
    }

    @Nested
    @DisplayName("indent — negative level")
    class IndentNegative {

        @Test
        @DisplayName("throws IllegalArgumentException")
        void indent_negative_throwsException() {
            assertThatThrownBy(
                    () -> JsonHelpers.indent(-1))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("-1");
        }

        @Test
        @DisplayName("exception message contains"
                + " the negative value")
        void indent_negativeValue_messageContainsValue() {
            assertThatThrownBy(
                    () -> JsonHelpers.indent(-5))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("-5");
        }
    }

    @Nested
    @DisplayName("utility class contract")
    class UtilityClassContract {

        @Test
        @DisplayName("class is final")
        void utility_whenCalled_classFinal() {
            assertThat(
                    java.lang.reflect.Modifier.isFinal(
                            JsonHelpers.class
                                    .getModifiers()))
                    .isTrue();
        }
    }
}
