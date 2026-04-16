package dev.iadev.release.dryrun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link InteractiveFlagValidator}.
 *
 * <p>TPP ordering: degenerate (interactive without dry-run) ->
 * happy (both flags) -> boundary (neither, dry-run alone).
 */
@DisplayName("InteractiveFlagValidatorTest")
class InteractiveFlagValidatorTest {

    @Nested
    @DisplayName("degenerate — interactive without dry-run")
    class DegenerateInteractiveWithoutDryRun {

        @Test
        @DisplayName("validate_interactiveWithoutDryRun"
                + "_throwsInteractiveRequiresDryRun")
        void validate_interactiveWithoutDryRun_throwsInteractiveRequiresDryRun() {
            assertThatThrownBy(() ->
                    InteractiveFlagValidator.validate(false, true))
                    .isInstanceOf(InteractiveRequiresDryRunException.class)
                    .hasMessageContaining("INTERACTIVE_REQUIRES_DRYRUN");
        }

        @Test
        @DisplayName("validate_interactiveWithoutDryRun"
                + "_exceptionCarriesExitCode1")
        void validate_interactiveWithoutDryRun_exceptionCarriesExitCode1() {
            InteractiveRequiresDryRunException ex =
                    new InteractiveRequiresDryRunException();
            assertThat(ex.exitCode()).isEqualTo(1);
            assertThat(ex.errorCode())
                    .isEqualTo("INTERACTIVE_REQUIRES_DRYRUN");
        }
    }

    @Nested
    @DisplayName("happy — both flags present")
    class HappyBothFlagsPresent {

        @Test
        @DisplayName("validate_dryRunAndInteractive"
                + "_doesNotThrow")
        void validate_dryRunAndInteractive_doesNotThrow() {
            InteractiveFlagValidator.validate(true, true);
        }
    }

    @Nested
    @DisplayName("boundary — other combinations")
    class BoundaryOtherCombinations {

        @Test
        @DisplayName("validate_dryRunAloneWithoutInteractive"
                + "_doesNotThrow")
        void validate_dryRunAloneWithoutInteractive_doesNotThrow() {
            InteractiveFlagValidator.validate(true, false);
        }

        @Test
        @DisplayName("validate_neitherFlag"
                + "_doesNotThrow")
        void validate_neitherFlag_doesNotThrow() {
            InteractiveFlagValidator.validate(false, false);
        }
    }
}
