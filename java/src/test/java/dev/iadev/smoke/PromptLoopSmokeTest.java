package dev.iadev.smoke;

import dev.iadev.release.prompt.AskUserQuestionPort;
import dev.iadev.release.prompt.ClockPort;
import dev.iadev.release.prompt.HaltPoint;
import dev.iadev.release.prompt.PromptAction;
import dev.iadev.release.prompt.PromptEngine;
import dev.iadev.release.prompt.PromptResult;
import dev.iadev.release.prompt.StatePort;
import dev.iadev.release.state.NextAction;
import dev.iadev.release.state.ReleaseState;
import dev.iadev.release.state.WaitingFor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test validating the full interactive prompt loop:
 * prompt -> "Sair" -> reinvoke -> prompt -> "Continuar"
 * -> finalize. State file consistency at each step.
 *
 * <p>Introduced by story-0039-0007 TASK-004.
 */
@DisplayName("PromptLoopSmokeTest")
class PromptLoopSmokeTest {

    private static final String FIXED_TIME =
            "2026-04-15T10:00:00Z";

    @Test
    @DisplayName("fullLoop_exitThenContinue"
            + "_stateConsistentAtEachStep")
    void fullLoop_exitThenContinue_stateConsistentAtEachStep() {
        var answers = new SequentialAskPort(
                "Sair e retomar depois",
                "PR mergeado — continuar");
        var statePort = new CapturingStatePort();
        var clockPort = fixedClock();
        var engine = new PromptEngine(
                statePort, clockPort, answers);

        // Step 1: prompt -> operator chooses "Sair"
        PromptResult exitResult = engine.resolve(
                HaltPoint.APPROVAL_GATE,
                approvalPendingState(),
                false);

        assertThat(exitResult.action())
                .isEqualTo(PromptAction.EXIT);
        assertThat(statePort.lastState().waitingFor())
                .isEqualTo(WaitingFor.PR_MERGE);
        assertThat(statePort.lastState().nextActions())
                .isNotEmpty();

        // Step 2: reinvoke -> operator chooses "Continuar"
        PromptResult continueResult = engine.resolve(
                HaltPoint.APPROVAL_GATE,
                statePort.lastState(),
                false);

        assertThat(continueResult.action())
                .isEqualTo(PromptAction.CONTINUE);
        assertThat(statePort.lastState()
                .lastPromptAnsweredAt())
                .isEqualTo(FIXED_TIME);
    }

    @Test
    @DisplayName("fullLoop_recoverableRetryThenContinue"
            + "_stateConsistentAtEachStep")
    void fullLoop_recoverableRetryThenContinue_stateConsistentAtEachStep() {
        var answers = new SequentialAskPort(
                "Tentar novamente",
                "PR mergeado — continuar");
        var statePort = new CapturingStatePort();
        var clockPort = fixedClock();
        var engine = new PromptEngine(
                statePort, clockPort, answers);

        // Step 1: recoverable failure -> retry
        PromptResult retryResult = engine.resolve(
                HaltPoint.RECOVERABLE_FAILURE,
                recoverableState(),
                false);

        assertThat(retryResult.action())
                .isEqualTo(PromptAction.RETRY);
        assertThat(statePort.lastState().waitingFor())
                .isEqualTo(WaitingFor.USER_CONFIRMATION);

        // Step 2: after retry succeeds, approval gate
        PromptResult continueResult = engine.resolve(
                HaltPoint.APPROVAL_GATE,
                approvalPendingState(),
                false);

        assertThat(continueResult.action())
                .isEqualTo(PromptAction.CONTINUE);
    }

    @Test
    @DisplayName("fullLoop_noOrphanState"
            + "_afterFinalStep")
    void fullLoop_noOrphanState_afterFinalStep() {
        var answers = new SequentialAskPort(
                "PR mergeado — continuar");
        var statePort = new CapturingStatePort();
        var clockPort = fixedClock();
        var engine = new PromptEngine(
                statePort, clockPort, answers);

        engine.resolve(
                HaltPoint.APPROVAL_GATE,
                approvalPendingState(),
                false);

        // After continue, lastPromptAnsweredAt is set
        assertThat(statePort.lastState()
                .lastPromptAnsweredAt())
                .isNotNull();
        // waitingFor is still set (caller is responsible
        // for clearing it after phase completion)
        assertThat(statePort.lastState().waitingFor())
                .isEqualTo(WaitingFor.PR_MERGE);
    }

    // -- Helpers --

    private static ClockPort fixedClock() {
        return () -> Instant.parse(FIXED_TIME);
    }

    private static ReleaseState approvalPendingState() {
        return new ReleaseState(
                2, "3.2.0", "APPROVAL_PENDING",
                "release/3.2.0", "develop",
                false, false, false, true,
                "2026-04-15T08:00:00Z",
                "2026-04-15T09:00:00Z",
                List.of("INITIALIZED"),
                "3.2.0", "3.1.0", "minor",
                42, "https://github.com/org/repo/pull/42",
                null, null, null, null,
                List.of(), null, Map.of(), null, null);
    }

    private static ReleaseState recoverableState() {
        return new ReleaseState(
                2, "3.2.0", "VALIDATE_DEEP",
                "release/3.2.0", "develop",
                false, false, false, true,
                "2026-04-15T08:00:00Z",
                "2026-04-15T09:00:00Z",
                List.of("INITIALIZED"),
                "3.2.0", "3.1.0", "minor",
                null, null, null, null, null, null,
                List.of(), null, Map.of(), null, null);
    }

    /** Returns answers sequentially from a queue. */
    private static final class SequentialAskPort
            implements AskUserQuestionPort {

        private final Deque<String> answers;

        SequentialAskPort(String... responses) {
            this.answers = new ArrayDeque<>(
                    List.of(responses));
        }

        @Override
        public String ask(String question,
                          List<String> options) {
            return answers.poll();
        }
    }

    /** Captures the last persisted state. */
    private static final class CapturingStatePort
            implements StatePort {

        private ReleaseState last;

        @Override
        public void update(ReleaseState state) {
            last = state;
        }

        ReleaseState lastState() {
            return last;
        }
    }
}
