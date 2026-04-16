package dev.iadev.release.resume;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SmartResumeOrchestrator}.
 *
 * <p>TPP-ordered: degenerate → happy → boundary → error.
 * Maps to story-0039-0008 §7 Gherkin scenarios.
 */
@DisplayName("SmartResumeOrchestratorTest")
class SmartResumeOrchestratorTest {

    @Nested
    @DisplayName("resolve — decision logic")
    class Resolve {

        @Test
        @DisplayName("noStateFile_returnsAutoDetect"
                + " (degenerate — Gherkin §7.1)")
        void resolve_noStateFile_returnsAutoDetect() {
            SmartResumeOrchestrator orchestrator =
                    new SmartResumeOrchestrator(
                            Optional.empty(),
                            false,
                            true);

            ResumeDecision decision =
                    orchestrator.resolve();

            assertThat(decision.action())
                    .isEqualTo(ResumeAction.AUTO_DETECT);
            assertThat(decision.options()).isEmpty();
        }

        @Test
        @DisplayName("activeState_noPrompt_returnsConflict"
                + " (error — Gherkin §7.5)")
        void resolve_activeState_noPrompt_conflict() {
            DetectedState state = activeState();
            SmartResumeOrchestrator orchestrator =
                    new SmartResumeOrchestrator(
                            Optional.of(state),
                            true,
                            false);

            ResumeDecision decision =
                    orchestrator.resolve();

            assertThat(decision.action())
                    .isEqualTo(ResumeAction.STATE_CONFLICT);
            assertThat(decision.errorCode())
                    .isEqualTo("STATE_CONFLICT");
        }

        @Test
        @DisplayName("activeState_interactive_returnsPrompt"
                + " (happy — Gherkin §7.2)")
        void resolve_activeState_interactive_prompt() {
            DetectedState state = activeState();
            SmartResumeOrchestrator orchestrator =
                    new SmartResumeOrchestrator(
                            Optional.of(state),
                            false,
                            true);

            ResumeDecision decision =
                    orchestrator.resolve();

            assertThat(decision.action())
                    .isEqualTo(ResumeAction.PROMPT_USER);
            assertThat(decision.detectedState())
                    .isPresent();
            assertThat(decision.options())
                    .contains(ResumeOption.RESUME);
            assertThat(decision.options())
                    .contains(ResumeOption.ABORT);
        }

        @Test
        @DisplayName("activeState_withNewCommits"
                + "_offersStartNew (boundary — Gherkin §7.3)")
        void resolve_withNewCommits_offersStartNew() {
            DetectedState state = activeState();
            SmartResumeOrchestrator orchestrator =
                    new SmartResumeOrchestrator(
                            Optional.of(state),
                            false,
                            true);

            ResumeDecision decision =
                    orchestrator.resolve();

            assertThat(decision.options())
                    .contains(ResumeOption.START_NEW);
        }

        @Test
        @DisplayName("activeState_noNewCommits"
                + "_excludesStartNew (boundary — Gherkin §7.4)")
        void resolve_noNewCommits_excludesStartNew() {
            DetectedState state = activeState();
            SmartResumeOrchestrator orchestrator =
                    new SmartResumeOrchestrator(
                            Optional.of(state),
                            false,
                            false);

            ResumeDecision decision =
                    orchestrator.resolve();

            assertThat(decision.options())
                    .doesNotContain(ResumeOption.START_NEW);
            assertThat(decision.options())
                    .containsExactly(
                            ResumeOption.RESUME,
                            ResumeOption.ABORT);
        }

        @Test
        @DisplayName("completedState_returnsAutoDetect"
                + " (boundary — Gherkin §7.6)")
        void resolve_completedState_returnsAutoDetect() {
            // completedState is filtered by detector
            // (returns empty), so orchestrator sees empty
            SmartResumeOrchestrator orchestrator =
                    new SmartResumeOrchestrator(
                            Optional.empty(),
                            false,
                            true);

            ResumeDecision decision =
                    orchestrator.resolve();

            assertThat(decision.action())
                    .isEqualTo(ResumeAction.AUTO_DETECT);
        }
    }

    @Nested
    @DisplayName("buildPromptDisplay — user-facing text")
    class BuildPromptDisplay {

        @Test
        @DisplayName("formatMatchesStorySpec"
                + " (story §5.2)")
        void buildPromptDisplay_matchesSpec() {
            DetectedState state = new DetectedState(
                    "3.2.0",
                    "APPROVAL_PENDING",
                    "v3.1.0",
                    Duration.ofMinutes(134),
                    Path.of("plans/release-state-3.2.0.json"));

            String display =
                    SmartResumeOrchestrator
                            .buildPromptDisplay(
                                    state, true);

            assertThat(display)
                    .contains("3.2.0")
                    .contains("v3.1.0")
                    .contains("APPROVAL_PENDING")
                    .contains("2h 14min");
        }

        @Test
        @DisplayName("displayContainsThreeOptions"
                + " when hasNewCommits")
        void buildPromptDisplay_containsAllOptions() {
            DetectedState state = activeState();

            String display =
                    SmartResumeOrchestrator
                            .buildPromptDisplay(
                                    state, true);

            assertThat(display)
                    .contains("Resume")
                    .contains("Abort")
                    .contains("Start new release")
                    .contains("APPROVAL_PENDING");
        }

        @Test
        @DisplayName("displayHidesStartNew"
                + " when noNewCommits")
        void buildPromptDisplay_hidesStartNew() {
            DetectedState state = activeState();

            String display =
                    SmartResumeOrchestrator
                            .buildPromptDisplay(
                                    state, false);

            assertThat(display)
                    .contains("Resume")
                    .contains("Abort")
                    .doesNotContain("Start new release");
        }
    }

    private static DetectedState activeState() {
        return new DetectedState(
                "3.2.0",
                "APPROVAL_PENDING",
                "v3.1.0",
                Duration.ofHours(2).plusMinutes(14),
                Path.of(
                        "plans/release-state-3.2.0.json"));
    }
}
