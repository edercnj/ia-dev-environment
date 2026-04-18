package dev.iadev.release.resume;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for {@link ResumeDecision} package-private
 * factories (audit finding M-017). The factories are exercised
 * transitively by {@code SmartResumeOrchestratorTest}, but the
 * invariants (error code present only for STATE_CONFLICT,
 * detectedState empty for non-prompt paths) had no direct
 * coverage.
 */
@DisplayName("ResumeDecisionTest")
class ResumeDecisionTest {

    @Nested
    @DisplayName("autoDetect — degenerate path (no state)")
    class AutoDetect {

        @Test
        @DisplayName("autoDetect_noErrorCode_emptyOptions")
        void autoDetect_noErrorCode_emptyOptions() {
            ResumeDecision decision =
                    ResumeDecision.autoDetect();

            assertThat(decision.action())
                    .isEqualTo(ResumeAction.AUTO_DETECT);
            assertThat(decision.errorCode()).isNull();
            assertThat(decision.options()).isEmpty();
            assertThat(decision.detectedState()).isEmpty();
        }
    }

    @Nested
    @DisplayName("stateConflict — --no-prompt abort path")
    class StateConflict {

        @Test
        @DisplayName("stateConflict_errorCodeStateConflict_emptyOptions")
        void stateConflict_errorCodeStateConflict_emptyOptions() {
            ResumeDecision decision =
                    ResumeDecision.stateConflict();

            assertThat(decision.action())
                    .isEqualTo(ResumeAction.STATE_CONFLICT);
            assertThat(decision.errorCode())
                    .isEqualTo("STATE_CONFLICT");
            assertThat(decision.options()).isEmpty();
            assertThat(decision.detectedState()).isEmpty();
        }
    }

    @Nested
    @DisplayName("promptUser — active state with options")
    class PromptUser {

        @Test
        @DisplayName("promptUser_detectedStatePresent_optionsCopied")
        void promptUser_detectedStatePresent_optionsCopied() {
            DetectedState state = new DetectedState(
                    "3.2.0", "APPROVAL_PENDING", "v3.1.0",
                    Duration.ofHours(2),
                    Path.of("release-state-3.2.0.json"));
            List<ResumeOption> opts = List.of(
                    ResumeOption.RESUME, ResumeOption.ABORT);

            ResumeDecision decision =
                    ResumeDecision.promptUser(state, opts);

            assertThat(decision.action())
                    .isEqualTo(ResumeAction.PROMPT_USER);
            assertThat(decision.errorCode()).isNull();
            assertThat(decision.options())
                    .containsExactly(
                            ResumeOption.RESUME,
                            ResumeOption.ABORT);
            assertThat(decision.detectedState())
                    .contains(state);
        }
    }
}
