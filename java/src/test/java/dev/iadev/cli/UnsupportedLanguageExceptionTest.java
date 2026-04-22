package dev.iadev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UnsupportedLanguageExceptionTest {

    @Test
    void getMessage_withPython_returnsRuleMessage() {
        UnsupportedLanguageException ex =
                new UnsupportedLanguageException("python");

        assertThat(ex.getMessage()).isEqualTo(
                "Language 'python' is not supported."
                        + " Only 'java' is available"
                        + " (see CHANGELOG v4.0.0"
                        + " / EPIC-0048).");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "python", "go", "kotlin", "typescript",
            "rust", "csharp", "foo", "JAVA"})
    void getMessage_withAnyAttemptedValue_embedsValue(
            String value) {
        UnsupportedLanguageException ex =
                new UnsupportedLanguageException(value);

        assertThat(ex.getMessage())
                .startsWith("Language '" + value + "'")
                .contains(
                        "Only 'java' is available"
                                + " (see CHANGELOG v4.0.0"
                                + " / EPIC-0048).");
    }

    @Test
    void attemptedLanguage_returnsOriginalValue() {
        UnsupportedLanguageException ex =
                new UnsupportedLanguageException("rust");

        assertThat(ex.attemptedLanguage()).isEqualTo("rust");
    }

    @Test
    void attemptedLanguage_withEmptyString_returnsEmpty() {
        UnsupportedLanguageException ex =
                new UnsupportedLanguageException("");

        assertThat(ex.attemptedLanguage()).isEqualTo("");
        assertThat(ex.getMessage()).startsWith("Language ''");
    }

    @Test
    void attemptedLanguage_withNull_normalizesToEmpty() {
        UnsupportedLanguageException ex =
                new UnsupportedLanguageException(null);

        assertThat(ex.attemptedLanguage()).isEqualTo("");
        assertThat(ex.getMessage()).startsWith("Language ''");
    }

    @Test
    void isInstanceOfIllegalArgumentException() {
        UnsupportedLanguageException ex =
                new UnsupportedLanguageException("python");

        assertThat(ex).isInstanceOf(
                IllegalArgumentException.class);
    }
}
