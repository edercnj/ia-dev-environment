package dev.iadev.release.handoff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link HandoffOrchestrator} covering the
 * full decision table in story-0039-0011 §5.3 and the error
 * paths in §5.4.
 *
 * <p>Scenarios (TPP order):
 * <ul>
 *   <li>Happy path (TASK-001): PR OPEN + fix-comments invoked +
 *       options re-rendered</li>
 *   <li>Boundary (TASK-003): PR mergeado during handoff -> main
 *       option becomes "Continuar release"</li>
 *   <li>Degenerate (TASK-005a): PR CLOSED -> reopen/new/abort
 *       option set</li>
 *   <li>Error (TASK-005b): skill invoker fails -> HANDOFF_SKILL_FAILED
 *       warn + retry/continue/abort options</li>
 *   <li>Error (TASK-005c): gh pr view 404 -> exit 1
 *       HANDOFF_PR_NOT_FOUND</li>
 * </ul>
 */
@DisplayName("HandoffOrchestratorTest")
class HandoffOrchestratorTest {

    private static final int PR_NUMBER = 297;

    @Test
    @DisplayName("handoff_happyPath_prOpenAfterFix"
            + "_invokesSkillAndReRendersPrompt")
    void handoff_happyPath_prOpenAfterFix_invokesSkillAndReRendersPrompt() {
        RecordingSkillInvoker skill = new RecordingSkillInvoker();
        StubGhCli gh = StubGhCli.returning(
                new PrState(PrReviewState.OPEN,
                        Optional.empty(),
                        PrReviewDecision.REVIEW_REQUIRED));
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(skill, gh);

        HandoffResult result = orchestrator.handoff(PR_NUMBER);

        assertThat(skill.invocations).hasSize(1);
        assertThat(skill.invocations.get(0).skill())
                .isEqualTo("x-pr-fix");
        assertThat(skill.invocations.get(0).args())
                .isEqualTo("297");
        assertThat(gh.callCount).isEqualTo(1);
        assertThat(result.error()).isNull();
        assertThat(result.options())
                .containsExactly(
                        "Rodar fix-comments novamente",
                        "Sair e retomar depois",
                        "Abortar");
    }

    @Test
    @DisplayName("handoff_boundaryPrMergedDuringHandoff"
            + "_mainOptionIsContinueRelease")
    void handoff_boundaryPrMergedDuringHandoff_mainOptionIsContinueRelease() {
        RecordingSkillInvoker skill = new RecordingSkillInvoker();
        StubGhCli gh = StubGhCli.returning(
                new PrState(PrReviewState.MERGED,
                        Optional.of(Instant.parse(
                                "2026-04-15T10:00:00Z")),
                        PrReviewDecision.APPROVED));
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(skill, gh);

        HandoffResult result = orchestrator.handoff(PR_NUMBER);

        assertThat(result.options().get(0))
                .isEqualTo("Continuar release");
        assertThat(result.error()).isNull();
    }

    @Test
    @DisplayName("handoff_openAndApproved"
            + "_mainOptionIsMergeOnGithub")
    void handoff_openAndApproved_mainOptionIsMergeOnGithub() {
        RecordingSkillInvoker skill = new RecordingSkillInvoker();
        StubGhCli gh = StubGhCli.returning(
                new PrState(PrReviewState.OPEN,
                        Optional.empty(),
                        PrReviewDecision.APPROVED));
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(skill, gh);

        HandoffResult result = orchestrator.handoff(PR_NUMBER);

        assertThat(result.options().get(0))
                .isEqualTo("Mergear no GitHub e voltar");
    }

    @Test
    @DisplayName("handoff_degeneratePrClosedDuringHandoff"
            + "_offersReopenNewAbortOptions")
    void handoff_degeneratePrClosedDuringHandoff_offersReopenNewAbortOptions() {
        RecordingSkillInvoker skill = new RecordingSkillInvoker();
        StubGhCli gh = StubGhCli.returning(
                new PrState(PrReviewState.CLOSED,
                        Optional.empty(),
                        PrReviewDecision.REVIEW_REQUIRED));
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(skill, gh);

        HandoffResult result = orchestrator.handoff(PR_NUMBER);

        assertThat(result.options())
                .containsExactly(
                        "Reabrir PR",
                        "Iniciar novo release",
                        "Abortar");
        assertThat(result.error()).isNull();
    }

    @Test
    @DisplayName("handoff_skillInvocationFails"
            + "_warnsHandoffSkillFailedAndOffersRetry")
    void handoff_skillInvocationFails_warnsHandoffSkillFailedAndOffersRetry() {
        SkillInvokerPort failing = (skill, args) -> {
            throw new SkillInvocationException(
                    "skill runtime error");
        };
        StubGhCli gh = StubGhCli.returning(
                new PrState(PrReviewState.OPEN,
                        Optional.empty(),
                        PrReviewDecision.REVIEW_REQUIRED));
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(failing, gh);

        HandoffResult result = orchestrator.handoff(PR_NUMBER);

        assertThat(result.error())
                .isEqualTo(HandoffError.HANDOFF_SKILL_FAILED);
        assertThat(result.exitCode()).isZero();
        assertThat(result.options())
                .containsExactly(
                        "Tentar novamente",
                        "Continuar mesmo assim",
                        "Abortar");
        // gh pr view is NOT called when skill fails
        assertThat(gh.callCount).isZero();
    }

    @Test
    @DisplayName("handoff_ghPrViewReturns404"
            + "_exitsOneWithHandoffPrNotFound")
    void handoff_ghPrViewReturns404_exitsOneWithHandoffPrNotFound() {
        RecordingSkillInvoker skill = new RecordingSkillInvoker();
        GhCliPort notFoundGh = pr -> {
            throw new PrNotFoundException(pr);
        };
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(skill, notFoundGh);

        HandoffResult result = orchestrator.handoff(PR_NUMBER);

        assertThat(result.error())
                .isEqualTo(HandoffError.HANDOFF_PR_NOT_FOUND);
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.options()).isEmpty();
    }

    @Test
    @DisplayName("handoff_negativePrNumber"
            + "_rejectsWithIllegalArgument")
    void handoff_negativePrNumber_rejectsWithIllegalArgument() {
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(
                        new RecordingSkillInvoker(),
                        StubGhCli.returning(null));

        assertThatThrownBy(() -> orchestrator.handoff(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
    }

    @Test
    @DisplayName("handoff_zeroPrNumber"
            + "_rejectsWithIllegalArgument")
    void handoff_zeroPrNumber_rejectsWithIllegalArgument() {
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(
                        new RecordingSkillInvoker(),
                        StubGhCli.returning(null));

        assertThatThrownBy(() -> orchestrator.handoff(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0");
    }

    // -- Value-object normalization tests --

    @Test
    @DisplayName("prState_nullMergedAt_isNormalizedToEmptyOptional")
    void prState_nullMergedAt_isNormalizedToEmptyOptional() {
        PrState state = new PrState(
                PrReviewState.OPEN, null,
                PrReviewDecision.APPROVED);

        assertThat(state.mergedAt()).isEmpty();
    }

    @Test
    @DisplayName("prState_nullReviewDecision"
            + "_isNormalizedToReviewRequired")
    void prState_nullReviewDecision_isNormalizedToReviewRequired() {
        PrState state = new PrState(
                PrReviewState.OPEN,
                Optional.empty(),
                null);

        assertThat(state.reviewDecision())
                .isEqualTo(PrReviewDecision.REVIEW_REQUIRED);
    }

    @Test
    @DisplayName("prState_nullState_throwsIllegalArgument")
    void prState_nullState_throwsIllegalArgument() {
        assertThatThrownBy(() -> new PrState(
                null, Optional.empty(),
                PrReviewDecision.APPROVED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("state");
    }

    @Test
    @DisplayName("handoffResult_nullOptions"
            + "_isNormalizedToEmptyList")
    void handoffResult_nullOptions_isNormalizedToEmptyList() {
        HandoffResult result = new HandoffResult(
                null, null, 0);

        assertThat(result.options()).isEmpty();
    }

    @Test
    @DisplayName("skillInvocationException_withCause"
            + "_preservesCauseForDiagnosis")
    void skillInvocationException_withCause_preservesCauseForDiagnosis() {
        RuntimeException cause = new RuntimeException("root");
        SkillInvocationException e =
                new SkillInvocationException(
                        "wrapper", cause);

        assertThat(e.getCause()).isSameAs(cause);
        assertThat(e).hasMessage("wrapper");
    }

    @Test
    @DisplayName("prNotFoundException_exposesPrNumber")
    void prNotFoundException_exposesPrNumber() {
        PrNotFoundException e = new PrNotFoundException(42);

        assertThat(e.prNumber()).isEqualTo(42);
        assertThat(e).hasMessageContaining("42");
    }

    @Test
    @DisplayName("handoffOrchestrator_nullSkillInvoker"
            + "_rejectsAtConstruction")
    void handoffOrchestrator_nullSkillInvoker_rejectsAtConstruction() {
        assertThatThrownBy(() -> new HandoffOrchestrator(
                null, StubGhCli.returning(null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("skillInvoker");
    }

    @Test
    @DisplayName("handoffOrchestrator_nullGhCli"
            + "_rejectsAtConstruction")
    void handoffOrchestrator_nullGhCli_rejectsAtConstruction() {
        assertThatThrownBy(() -> new HandoffOrchestrator(
                new RecordingSkillInvoker(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ghCli");
    }

    @Test
    @DisplayName("resolveOptions_nullState_rejectedByRequireNonNull")
    void resolveOptions_nullState_rejectedByRequireNonNull() {
        assertThatThrownBy(
                () -> HandoffOrchestrator.resolveOptions(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("state");
    }

    @Test
    @DisplayName("openWithChangesRequested"
            + "_offersFixCommentsAgain")
    void openWithChangesRequested_offersFixCommentsAgain() {
        RecordingSkillInvoker skill = new RecordingSkillInvoker();
        StubGhCli gh = StubGhCli.returning(
                new PrState(PrReviewState.OPEN,
                        Optional.empty(),
                        PrReviewDecision.CHANGES_REQUESTED));
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(skill, gh);

        HandoffResult result = orchestrator.handoff(PR_NUMBER);

        assertThat(result.options().get(0))
                .isEqualTo("Rodar fix-comments novamente");
    }

    // -- Test doubles --

    private static final class RecordingSkillInvoker
            implements SkillInvokerPort {

        private final java.util.List<Invocation> invocations =
                new java.util.ArrayList<>();

        @Override
        public void invoke(String skill, String args) {
            invocations.add(new Invocation(skill, args));
        }

        private record Invocation(String skill, String args) { }
    }

    private static final class StubGhCli implements GhCliPort {

        private final PrState state;
        private int callCount;

        private StubGhCli(PrState state) {
            this.state = state;
        }

        static StubGhCli returning(PrState state) {
            return new StubGhCli(state);
        }

        @Override
        public PrState viewPr(int prNumber) {
            callCount++;
            return state;
        }
    }
}
