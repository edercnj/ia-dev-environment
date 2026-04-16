package dev.iadev.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidBumpException — error vocabulary")
class InvalidBumpExceptionTest {

    @Test
    @DisplayName("new_exposesCodeAndMessage")
    void new_exposesCodeAndMessage() {
        InvalidBumpException ex = new InvalidBumpException(
                InvalidBumpException.Code.VERSION_NO_BUMP_SIGNAL, "no feat/fix/perf");

        assertThat(ex.code()).isEqualTo(InvalidBumpException.Code.VERSION_NO_BUMP_SIGNAL);
        assertThat(ex.getMessage()).contains("no feat/fix/perf");
    }

    @Test
    @DisplayName("new_nullCode_rejected")
    void new_nullCode_rejected() {
        assertThatThrownBy(() -> new InvalidBumpException(null, "msg"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("code_enumContainsAllDocumentedCodes")
    void code_enumContainsAllDocumentedCodes() {
        assertThat(InvalidBumpException.Code.values())
                .containsExactlyInAnyOrder(
                        InvalidBumpException.Code.VERSION_INVALID_FORMAT,
                        InvalidBumpException.Code.VERSION_NO_BUMP_SIGNAL,
                        InvalidBumpException.Code.VERSION_NO_COMMITS,
                        InvalidBumpException.Code.INVALID_BUMP_COMBINATION);
    }
}
