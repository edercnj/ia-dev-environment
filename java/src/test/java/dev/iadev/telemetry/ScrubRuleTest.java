package dev.iadev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ScrubRule} covering the canonical
 * constructor contract, the {@link ScrubRule#of(String,
 * String, String)} factory, and {@link ScrubRule#apply(String)}
 * including the null-pass-through case.
 */
@DisplayName("ScrubRule")
class ScrubRuleTest {

    @Nested
    @DisplayName("canonical constructor")
    class Constructor {

        @Test
        @DisplayName("throws NPE on null category")
        void null_category_throws() {
            assertThatThrownBy(() -> new ScrubRule(
                    null, Pattern.compile("x"), "Y"))
                    .isInstanceOf(
                            NullPointerException.class);
        }

        @Test
        @DisplayName("throws NPE on null pattern")
        void null_pattern_throws() {
            assertThatThrownBy(() -> new ScrubRule(
                    "cat", null, "Y"))
                    .isInstanceOf(
                            NullPointerException.class);
        }

        @Test
        @DisplayName("throws NPE on null replacement")
        void null_replacement_throws() {
            assertThatThrownBy(() -> new ScrubRule(
                    "cat", Pattern.compile("x"), null))
                    .isInstanceOf(
                            NullPointerException.class);
        }

        @Test
        @DisplayName("throws IAE on blank replacement")
        void blank_replacement_throws() {
            assertThatThrownBy(() -> new ScrubRule(
                    "cat", Pattern.compile("x"), "   "))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining(
                            "replacement must not be blank");
        }

        @Test
        @DisplayName("accepts valid arguments")
        void valid_arguments_succeed() {
            ScrubRule rule = new ScrubRule(
                    "cat",
                    Pattern.compile("secret"),
                    "REDACTED");

            assertThat(rule.category()).isEqualTo("cat");
            assertThat(rule.replacement())
                    .isEqualTo("REDACTED");
        }
    }

    @Nested
    @DisplayName("of(regex, replacement) factory")
    class OfFactory {

        @Test
        @DisplayName("compiles the regex into a Pattern")
        void compiles_regex() {
            ScrubRule rule = ScrubRule.of(
                    "cat", "se[c]ret", "REDACTED");

            assertThat(rule.pattern().pattern())
                    .isEqualTo("se[c]ret");
        }

        @Test
        @DisplayName("propagates PatternSyntaxException"
                + " for invalid regex")
        void invalid_regex_propagates() {
            assertThatThrownBy(() -> ScrubRule.of(
                    "cat", "[unclosed", "X"))
                    .isInstanceOf(
                            PatternSyntaxException.class);
        }
    }

    @Nested
    @DisplayName("apply")
    class Apply {

        @Test
        @DisplayName("returns null when input is null")
        void null_input_returnsNull() {
            ScrubRule rule = ScrubRule.of(
                    "cat", "secret", "REDACTED");

            assertThat(rule.apply(null)).isNull();
        }

        @Test
        @DisplayName("replaces every match")
        void multiple_matches_allReplaced() {
            ScrubRule rule = ScrubRule.of(
                    "cat", "secret", "X");

            assertThat(
                    rule.apply("a secret and a secret b"))
                    .isEqualTo("a X and a X b");
        }

        @Test
        @DisplayName("leaves non-matching input unchanged")
        void non_matching_inputUnchanged() {
            ScrubRule rule = ScrubRule.of(
                    "cat", "secret", "X");

            assertThat(rule.apply("nothing to redact"))
                    .isEqualTo("nothing to redact");
        }
    }
}
