package dev.iadev.smoke;

import dev.iadev.release.handoff.GhCliPort;
import dev.iadev.release.handoff.HandoffError;
import dev.iadev.release.handoff.HandoffOrchestrator;
import dev.iadev.release.handoff.HandoffResult;
import dev.iadev.release.handoff.PrNotFoundException;
import dev.iadev.release.handoff.PrReviewDecision;
import dev.iadev.release.handoff.PrReviewState;
import dev.iadev.release.handoff.PrState;
import dev.iadev.release.handoff.SkillInvocationException;
import dev.iadev.release.handoff.SkillInvokerPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test validating the full handoff loop:
 * prompt -> "Rodar /x-pr-fix PR#" -> mock SkillInvoker runs
 * "x-pr-fix" successfully -> mock gh pr view returns MERGED
 * -> next options include "Continuar release" -> finalize.
 *
 * <p>Mocks {@link SkillInvokerPort} and {@link GhCliPort} to
 * avoid invoking the real sibling skill or shell commands in
 * CI (determinism requirement).
 *
 * <p>Introduced by story-0039-0011 TASK-009.
 */
@DisplayName("HandoffLoopSmokeTest")
class HandoffLoopSmokeTest {

    private static final int PR_NUMBER = 297;

    @Test
    @DisplayName("loop_operatorRunsFixCommentsThenPrMerged"
            + "_nextPromptOffersContinueRelease")
    void loop_operatorRunsFixCommentsThenPrMerged_nextPromptOffersContinueRelease() {
        TrackingSkillInvoker skill =
                new TrackingSkillInvoker();
        QueuedGhCli gh = QueuedGhCli.enqueue(
                new PrState(PrReviewState.MERGED,
                        Optional.of(Instant.parse(
                                "2026-04-15T10:30:00Z")),
                        PrReviewDecision.APPROVED));
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(skill, gh);

        HandoffResult result = orchestrator.handoff(PR_NUMBER);

        // Skill tool invoked exactly once with renamed skill
        assertThat(skill.invocations).hasSize(1);
        assertThat(skill.invocations.get(0))
                .isEqualTo("x-pr-fix:297");
        // gh pr view invoked exactly once after the handoff
        assertThat(gh.calls).hasSize(1);
        assertThat(gh.calls.get(0)).isEqualTo(PR_NUMBER);
        // Re-rendered prompt promotes "Continuar release"
        assertThat(result.options().get(0))
                .isEqualTo("Continuar release");
        assertThat(result.error()).isNull();
        assertThat(result.exitCode()).isZero();
    }

    @Test
    @DisplayName("loop_prStillOpenAfterFix"
            + "_offersFixCommentsAgain")
    void loop_prStillOpenAfterFix_offersFixCommentsAgain() {
        TrackingSkillInvoker skill =
                new TrackingSkillInvoker();
        QueuedGhCli gh = QueuedGhCli.enqueue(
                new PrState(PrReviewState.OPEN,
                        Optional.empty(),
                        PrReviewDecision.REVIEW_REQUIRED));
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(skill, gh);

        HandoffResult first = orchestrator.handoff(PR_NUMBER);

        assertThat(first.options())
                .containsExactly(
                        "Rodar fix-comments novamente",
                        "Sair e retomar depois",
                        "Abortar");
        // Simulate operator choosing "Rodar novamente" ->
        // second handoff cycle
        gh.reset(new PrState(PrReviewState.MERGED,
                Optional.of(Instant.parse(
                        "2026-04-15T11:00:00Z")),
                PrReviewDecision.APPROVED));
        HandoffResult second = orchestrator.handoff(PR_NUMBER);
        assertThat(skill.invocations).hasSize(2);
        assertThat(second.options().get(0))
                .isEqualTo("Continuar release");
    }

    @Test
    @DisplayName("loop_prDeletedDuringHandoff"
            + "_exitsOneWithPrNotFound")
    void loop_prDeletedDuringHandoff_exitsOneWithPrNotFound() {
        TrackingSkillInvoker skill =
                new TrackingSkillInvoker();
        GhCliPort deletedPr = pr -> {
            throw new PrNotFoundException(pr);
        };
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(skill, deletedPr);

        HandoffResult result = orchestrator.handoff(PR_NUMBER);

        assertThat(result.error())
                .isEqualTo(HandoffError.HANDOFF_PR_NOT_FOUND);
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.options()).isEmpty();
    }

    @Test
    @DisplayName("loop_fixSkillCrashes"
            + "_warnsHandoffSkillFailedAndOffersRetry")
    void loop_fixSkillCrashes_warnsHandoffSkillFailedAndOffersRetry() {
        SkillInvokerPort failing = (s, a) -> {
            throw new SkillInvocationException(
                    "skill runtime error");
        };
        QueuedGhCli gh = QueuedGhCli.enqueue(null);
        HandoffOrchestrator orchestrator =
                new HandoffOrchestrator(failing, gh);

        HandoffResult result = orchestrator.handoff(PR_NUMBER);

        assertThat(result.error())
                .isEqualTo(HandoffError.HANDOFF_SKILL_FAILED);
        // Exit code stays 0 — operator is prompted to retry
        assertThat(result.exitCode()).isZero();
        // gh is NOT invoked when the skill fails
        assertThat(gh.calls).isEmpty();
    }

    // -- Test doubles --

    private static final class TrackingSkillInvoker
            implements SkillInvokerPort {

        private final List<String> invocations =
                new ArrayList<>();

        @Override
        public void invoke(String skill, String args) {
            invocations.add(skill + ":" + args);
        }
    }

    private static final class QueuedGhCli implements GhCliPort {

        private PrState next;
        private final List<Integer> calls = new ArrayList<>();

        private QueuedGhCli(PrState next) {
            this.next = next;
        }

        static QueuedGhCli enqueue(PrState state) {
            return new QueuedGhCli(state);
        }

        void reset(PrState state) {
            this.next = state;
        }

        @Override
        public PrState viewPr(int prNumber) {
            calls.add(prNumber);
            return next;
        }
    }
}
