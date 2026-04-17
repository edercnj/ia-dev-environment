package dev.iadev.release.abort;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke tests for {@link AbortResult} factories, validation,
 * and warnings-list immutability (audit finding M-017).
 */
@DisplayName("AbortResultTest")
class AbortResultTest {

    @Nested
    @DisplayName("factories — exit codes and error codes")
    class Factories {

        @Test
        @DisplayName("success_exitCodeZero_noErrorCode")
        void success_exitCodeZero_noErrorCode() {
            AbortResult result = AbortResult.success(
                    "Abort complete.", List.of("pr-close warned"));

            assertThat(result.exitCode()).isEqualTo(0);
            assertThat(result.output())
                    .isEqualTo("Abort complete.");
            assertThat(result.errorCode()).isNull();
            assertThat(result.warnings())
                    .containsExactly("pr-close warned");
        }

        @Test
        @DisplayName("cancelled_exitCodeTwo_userCancelledCode")
        void cancelled_exitCodeTwo_userCancelledCode() {
            AbortResult result =
                    AbortResult.cancelled("User cancelled.");

            assertThat(result.exitCode()).isEqualTo(2);
            assertThat(result.errorCode())
                    .isEqualTo("ABORT_USER_CANCELLED");
            assertThat(result.output())
                    .isEqualTo("User cancelled.");
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("error_exitCodeOne_errorCodePreserved")
        void error_exitCodeOne_errorCodePreserved() {
            AbortResult result = AbortResult.error(
                    "State file missing.",
                    "ABORT_STATE_NOT_FOUND");

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.errorCode())
                    .isEqualTo("ABORT_STATE_NOT_FOUND");
            assertThat(result.output())
                    .isEqualTo("State file missing.");
            assertThat(result.warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("constructor — validation and defensive copy")
    class Constructor {

        @Test
        @DisplayName("constructor_nullOutput_throws")
        void constructor_nullOutput_throws() {
            assertThatThrownBy(() -> new AbortResult(
                    0, null, null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("output");
        }

        @Test
        @DisplayName("constructor_nullWarnings_throws")
        void constructor_nullWarnings_throws() {
            assertThatThrownBy(() -> new AbortResult(
                    0, "ok", null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("warnings");
        }

        @Test
        @DisplayName("warnings_defensiveCopy_isImmutable")
        void warnings_defensiveCopy_isImmutable() {
            List<String> mutable = new ArrayList<>();
            mutable.add("initial");

            AbortResult result = AbortResult.success(
                    "ok", mutable);

            // Mutation to source does not leak into record.
            mutable.add("leaked");
            assertThat(result.warnings())
                    .containsExactly("initial");
            // Returned list is itself unmodifiable.
            assertThatThrownBy(() ->
                    result.warnings().add("x"))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }
    }
}
