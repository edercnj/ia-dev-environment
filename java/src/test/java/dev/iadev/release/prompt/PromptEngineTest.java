package dev.iadev.release.prompt;

import dev.iadev.release.state.NextAction;
import dev.iadev.release.state.ReleaseState;
import dev.iadev.release.state.WaitingFor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PromptEngine}.
 *
 * <p>Follows TPP ordering: degenerate (--no-prompt) ->
 * happy path (continue) -> boundary (exit) ->
 * error (retry, abort).
 */
@DisplayName("PromptEngineTest")
class PromptEngineTest {

    private static final String FIXED_TIME =
            "2026-04-15T10:00:00Z";
    private static final String PR_URL =
            "https://github.com/org/repo/pull/42";
    private static final int PR_NUMBER = 42;

    @Nested
    @DisplayName("--no-prompt degenerate")
    class NoPromptDegenerate {

        @Test
        @DisplayName("resolve_noPromptApprovalGate"
                + "_returnsExitWithoutAskingUser")
        void resolve_noPromptApprovalGate_returnsExitWithoutAskingUser() {
            var askPort = new SpyAskUserQuestionPort();
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            PromptResult result = engine.resolve(
                    HaltPoint.APPROVAL_GATE,
                    approvalPendingState(),
                    true);

            assertThat(result.action())
                    .isEqualTo(PromptAction.EXIT);
            assertThat(askPort.invocationCount).isZero();
        }

        @Test
        @DisplayName("resolve_noPromptApprovalGate"
                + "_persistsWaitingForAndNextActions")
        void resolve_noPromptApprovalGate_persistsWaitingForAndNextActions() {
            var askPort = new SpyAskUserQuestionPort();
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            engine.resolve(
                    HaltPoint.APPROVAL_GATE,
                    approvalPendingState(),
                    true);

            assertThat(statePort.lastUpdatedState).isNotNull();
            assertThat(statePort.lastUpdatedState.waitingFor())
                    .isEqualTo(WaitingFor.PR_MERGE);
            assertThat(statePort.lastUpdatedState.nextActions())
                    .isNotEmpty();
        }

        @Test
        @DisplayName("resolve_noPromptBackmerge"
                + "_returnsExitWithoutAskingUser")
        void resolve_noPromptBackmerge_returnsExitWithoutAskingUser() {
            var askPort = new SpyAskUserQuestionPort();
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            PromptResult result = engine.resolve(
                    HaltPoint.BACKMERGE_MERGE,
                    backmergeState(),
                    true);

            assertThat(result.action())
                    .isEqualTo(PromptAction.EXIT);
            assertThat(askPort.invocationCount).isZero();
        }

        @Test
        @DisplayName("resolve_noPromptRecoverableFailure"
                + "_returnsExitWithoutAskingUser")
        void resolve_noPromptRecoverableFailure_returnsExitWithoutAskingUser() {
            var askPort = new SpyAskUserQuestionPort();
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            PromptResult result = engine.resolve(
                    HaltPoint.RECOVERABLE_FAILURE,
                    recoverableFailureState(),
                    true);

            assertThat(result.action())
                    .isEqualTo(PromptAction.EXIT);
            assertThat(askPort.invocationCount).isZero();
        }
    }

    @Nested
    @DisplayName("happy path — continue")
    class HappyPathContinue {

        @Test
        @DisplayName("resolve_approvalGateContinue"
                + "_returnsContinueResumeAndTag")
        void resolve_approvalGateContinue_returnsContinueResumeAndTag() {
            var askPort = new StubAskUserQuestionPort(
                    "PR mergeado — continuar");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            PromptResult result = engine.resolve(
                    HaltPoint.APPROVAL_GATE,
                    approvalPendingState(),
                    false);

            assertThat(result.action())
                    .isEqualTo(PromptAction.CONTINUE);
            assertThat(askPort.invocationCount).isEqualTo(1);
        }

        @Test
        @DisplayName("resolve_approvalGateContinue"
                + "_updatesLastPromptAnsweredAt")
        void resolve_approvalGateContinue_updatesLastPromptAnsweredAt() {
            var askPort = new StubAskUserQuestionPort(
                    "PR mergeado — continuar");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            engine.resolve(
                    HaltPoint.APPROVAL_GATE,
                    approvalPendingState(),
                    false);

            assertThat(statePort.lastUpdatedState).isNotNull();
            assertThat(statePort.lastUpdatedState
                    .lastPromptAnsweredAt())
                    .isEqualTo(FIXED_TIME);
        }

        @Test
        @DisplayName("resolve_approvalGateFixComments"
                + "_returnsHandoff")
        void resolve_approvalGateFixComments_returnsHandoff() {
            var askPort = new StubAskUserQuestionPort(
                    "Rodar /x-pr-fix PR#");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            PromptResult result = engine.resolve(
                    HaltPoint.APPROVAL_GATE,
                    approvalPendingState(),
                    false);

            assertThat(result.action())
                    .isEqualTo(PromptAction.HANDOFF);
        }

        @Test
        @DisplayName("resolve_backmergeContinue"
                + "_returnsContinue")
        void resolve_backmergeContinue_returnsContinue() {
            var askPort = new StubAskUserQuestionPort(
                    "PR mergeado — continuar");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            PromptResult result = engine.resolve(
                    HaltPoint.BACKMERGE_MERGE,
                    backmergeState(),
                    false);

            assertThat(result.action())
                    .isEqualTo(PromptAction.CONTINUE);
        }
    }

    @Nested
    @DisplayName("boundary — exit and resume later")
    class BoundaryExit {

        @Test
        @DisplayName("resolve_approvalGateExit"
                + "_returnsExitAndPreservesState")
        void resolve_approvalGateExit_returnsExitAndPreservesState() {
            var askPort = new StubAskUserQuestionPort(
                    "Sair e retomar depois");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            PromptResult result = engine.resolve(
                    HaltPoint.APPROVAL_GATE,
                    approvalPendingState(),
                    false);

            assertThat(result.action())
                    .isEqualTo(PromptAction.EXIT);
            assertThat(statePort.lastUpdatedState.waitingFor())
                    .isEqualTo(WaitingFor.PR_MERGE);
        }
    }

    @Nested
    @DisplayName("recoverable failure branches")
    class RecoverableFailure {

        @Test
        @DisplayName("resolve_recoverableRetry"
                + "_returnsRetry")
        void resolve_recoverableRetry_returnsRetry() {
            var askPort = new StubAskUserQuestionPort(
                    "Tentar novamente");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            PromptResult result = engine.resolve(
                    HaltPoint.RECOVERABLE_FAILURE,
                    recoverableFailureState(),
                    false);

            assertThat(result.action())
                    .isEqualTo(PromptAction.RETRY);
        }

        @Test
        @DisplayName("resolve_recoverableSkip"
                + "_returnsSkip")
        void resolve_recoverableSkip_returnsSkip() {
            var askPort = new StubAskUserQuestionPort(
                    "Pular esta etapa");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            PromptResult result = engine.resolve(
                    HaltPoint.RECOVERABLE_FAILURE,
                    recoverableFailureState(),
                    false);

            assertThat(result.action())
                    .isEqualTo(PromptAction.SKIP);
        }

        @Test
        @DisplayName("resolve_recoverableAbort"
                + "_returnsAbortWithExitCode2")
        void resolve_recoverableAbort_returnsAbortWithExitCode2() {
            var askPort = new StubAskUserQuestionPort(
                    "Abortar");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            PromptResult result = engine.resolve(
                    HaltPoint.RECOVERABLE_FAILURE,
                    recoverableFailureState(),
                    false);

            assertThat(result.action())
                    .isEqualTo(PromptAction.ABORT);
            assertThat(result.exitCode()).isEqualTo(2);
            assertThat(result.errorCode())
                    .isEqualTo("PROMPT_USER_ABORT");
        }

        @Test
        @DisplayName("resolve_recoverableAbort"
                + "_preservesStateForInspection")
        void resolve_recoverableAbort_preservesStateForInspection() {
            var askPort = new StubAskUserQuestionPort(
                    "Abortar");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            engine.resolve(
                    HaltPoint.RECOVERABLE_FAILURE,
                    recoverableFailureState(),
                    false);

            assertThat(statePort.lastUpdatedState).isNotNull();
            assertThat(statePort.lastUpdatedState
                    .lastPromptAnsweredAt())
                    .isEqualTo(FIXED_TIME);
        }
    }

    @Nested
    @DisplayName("input validation")
    class InputValidation {

        @Test
        @DisplayName("resolve_unexpectedResponse"
                + "_throwsWithPromptInvalidResponse")
        void resolve_unexpectedResponse_throwsWithPromptInvalidResponse() {
            var askPort = new StubAskUserQuestionPort(
                    "some garbage input");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            assertThatThrownBy(() -> engine.resolve(
                    HaltPoint.APPROVAL_GATE,
                    approvalPendingState(),
                    false))
                    .isInstanceOf(PromptInvalidResponseException.class)
                    .hasMessageContaining(
                            "PROMPT_INVALID_RESPONSE");
        }

        @Test
        @DisplayName("resolve_nullAnswer"
                + "_throwsWithPromptInvalidResponse")
        void resolve_nullAnswer_throwsWithPromptInvalidResponse() {
            var askPort = new NullAskUserQuestionPort();
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            assertThatThrownBy(() -> engine.resolve(
                    HaltPoint.APPROVAL_GATE,
                    approvalPendingState(),
                    false))
                    .isInstanceOf(PromptInvalidResponseException.class)
                    .hasMessageContaining(
                            "PROMPT_INVALID_RESPONSE");
        }
    }

    @Nested
    @DisplayName("nextActions command mapping")
    class NextActionsCommandMapping {

        @Test
        @DisplayName("resolve_approvalGate"
                + "_handoffOptionMapsToXPrFix")
        void resolve_approvalGate_handoffOptionMapsToXPrFix() {
            var askPort = new StubAskUserQuestionPort(
                    "PR mergeado — continuar");
            var statePort = new SpyStatePort();
            var clockPort = fixedClock();
            var engine = new PromptEngine(
                    statePort, clockPort, askPort);

            engine.resolve(
                    HaltPoint.APPROVAL_GATE,
                    approvalPendingState(),
                    false);

            assertThat(statePort.lastUpdatedState.nextActions())
                    .anyMatch(a -> a.label().startsWith("Rodar")
                            && a.command().equals("/x-pr-fix"));
            assertThat(statePort.lastUpdatedState.nextActions())
                    .filteredOn(a -> !a.label().startsWith("Rodar"))
                    .allMatch(a -> a.command()
                            .equals("/x-release"));
        }
    }

    // -- Test doubles --

    private static ClockPort fixedClock() {
        return () -> Instant.parse(FIXED_TIME);
    }

    private static ReleaseState approvalPendingState() {
        return new ReleaseState(
                2, "3.2.0", "APPROVAL_PENDING",
                "release/3.2.0", "develop",
                false, false, false, true,
                false,
                "2026-04-15T08:00:00Z",
                "2026-04-15T09:00:00Z",
                List.of("INITIALIZED"),
                "3.2.0", "3.1.0", "minor",
                PR_NUMBER, PR_URL, null, null, null,
                null,
                List.of(), null, Map.of(), null, null,
                null, null);
    }

    private static ReleaseState backmergeState() {
        return new ReleaseState(
                2, "3.2.0", "BACK_MERGE_DEVELOP",
                "release/3.2.0", "develop",
                false, false, false, true,
                false,
                "2026-04-15T08:00:00Z",
                "2026-04-15T09:00:00Z",
                List.of("INITIALIZED"),
                "3.2.0", "3.1.0", "minor",
                PR_NUMBER, PR_URL, null, null, null,
                null,
                List.of(), null, Map.of(), null, null,
                null, null);
    }

    private static ReleaseState recoverableFailureState() {
        return new ReleaseState(
                2, "3.2.0", "VALIDATE_DEEP",
                "release/3.2.0", "develop",
                false, false, false, true,
                false,
                "2026-04-15T08:00:00Z",
                "2026-04-15T09:00:00Z",
                List.of("INITIALIZED"),
                "3.2.0", "3.1.0", "minor",
                null, null, null, null, null,
                null,
                List.of(), null, Map.of(), null, null,
                null, null);
    }

    /** Spy that counts invocations but returns no answer. */
    private static final class SpyAskUserQuestionPort
            implements AskUserQuestionPort {

        int invocationCount;

        @Override
        public String ask(String question,
                          List<String> options) {
            invocationCount++;
            return options.get(0);
        }
    }

    /** Stub that returns a predetermined answer. */
    private static final class StubAskUserQuestionPort
            implements AskUserQuestionPort {

        private final String answer;
        int invocationCount;

        StubAskUserQuestionPort(String answer) {
            this.answer = answer;
        }

        @Override
        public String ask(String question,
                          List<String> options) {
            invocationCount++;
            return answer;
        }
    }

    /** Stub that returns null to simulate port returning null. */
    private static final class NullAskUserQuestionPort
            implements AskUserQuestionPort {

        @Override
        public String ask(String question,
                          List<String> options) {
            return null;
        }
    }

    /** Spy that captures the last updated state. */
    private static final class SpyStatePort
            implements StatePort {

        ReleaseState lastUpdatedState;

        @Override
        public void update(ReleaseState state) {
            lastUpdatedState = state;
        }
    }
}
