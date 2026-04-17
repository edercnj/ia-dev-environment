package dev.iadev.release.resume;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for {@link ResumeOption#label(String)} rendering
 * (audit finding M-017). The enum is exercised indirectly by
 * {@code SmartResumeOrchestratorTest}, but the label template
 * substitution itself had no direct coverage.
 */
@DisplayName("ResumeOptionTest")
class ResumeOptionTest {

    @Nested
    @DisplayName("label — template substitution")
    class Label {

        @Test
        @DisplayName("resume_interpolatesPhase_returnsResumeFromPhase")
        void resume_interpolatesPhase_returnsResumeFromPhase() {
            String rendered = ResumeOption.RESUME.label(
                    "APPROVAL_PENDING");

            assertThat(rendered)
                    .isEqualTo("Resume from APPROVAL_PENDING");
        }

        @Test
        @DisplayName("abort_interpolatesVersion_returnsAbortReleaseVersion")
        void abort_interpolatesVersion_returnsAbortReleaseVersion() {
            String rendered =
                    ResumeOption.ABORT.label("3.2.0");

            assertThat(rendered).isEqualTo(
                    "Abort release 3.2.0 (cleanup)");
        }

        @Test
        @DisplayName("startNew_ignoresArgument_returnsStaticLabel")
        void startNew_ignoresArgument_returnsStaticLabel() {
            // START_NEW has no %s placeholder — the argument
            // is tolerated and ignored by String.formatted.
            String rendered =
                    ResumeOption.START_NEW.label("3.2.0");

            assertThat(rendered).isEqualTo(
                    "Start new release (discard state)");
        }
    }

    @Nested
    @DisplayName("enum — declared options")
    class Declared {

        @Test
        @DisplayName("values_returnsThreeOptionsInDeclaredOrder")
        void values_returnsThreeOptionsInDeclaredOrder() {
            assertThat(ResumeOption.values())
                    .containsExactly(
                            ResumeOption.RESUME,
                            ResumeOption.ABORT,
                            ResumeOption.START_NEW);
        }
    }
}
