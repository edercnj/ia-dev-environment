package dev.iadev.release.prompt;

import dev.iadev.release.state.NextAction;
import dev.iadev.release.state.ReleaseState;
import dev.iadev.release.state.WaitingFor;

import java.util.List;
import java.util.Objects;

/**
 * Engine that resolves halt-point prompts in the
 * {@code x-release} interactive flow.
 *
 * <p>At each halt point, the engine either:
 * <ul>
 *   <li>In {@code --no-prompt} mode: persists state and
 *       returns {@link PromptAction#EXIT} without invoking
 *       {@link AskUserQuestionPort}</li>
 *   <li>In interactive mode: asks the operator via
 *       {@link AskUserQuestionPort} and dispatches the
 *       selected option to the appropriate action</li>
 * </ul>
 *
 * <p>Introduced by story-0039-0007.
 */
public final class PromptEngine {

    private final StatePort statePort;
    private final ClockPort clockPort;
    private final AskUserQuestionPort askPort;

    public PromptEngine(StatePort statePort,
                        ClockPort clockPort,
                        AskUserQuestionPort askPort) {
        this.statePort = statePort;
        this.clockPort = clockPort;
        this.askPort = askPort;
    }

    /**
     * Resolves a halt point into an actionable result.
     *
     * @param haltPoint the current halt point
     * @param state     current release state
     * @param noPrompt  true to skip interactive prompts
     * @return resolved prompt result with action
     */
    public PromptResult resolve(HaltPoint haltPoint,
                                ReleaseState state,
                                boolean noPrompt) {
        WaitingFor waiting = mapWaitingFor(haltPoint);
        List<NextAction> actions = buildNextActions(haltPoint);
        ReleaseState updated = withHaltState(
                state, waiting, actions,
                state.lastPromptAnsweredAt());
        statePort.update(updated);

        if (noPrompt) {
            return PromptResult.of(PromptAction.EXIT);
        }

        String answer = askPort.ask(
                buildQuestion(haltPoint, state),
                haltPoint.options());
        String timestamp = clockPort.now().toString();
        ReleaseState answered = withHaltState(
                state, waiting, actions, timestamp);
        statePort.update(answered);

        return dispatch(haltPoint, answer);
    }

    private PromptResult dispatch(HaltPoint haltPoint,
                                  String answer) {
        return switch (haltPoint) {
            case APPROVAL_GATE, BACKMERGE_MERGE ->
                    dispatchPrHalt(haltPoint, answer);
            case RECOVERABLE_FAILURE ->
                    dispatchRecoverable(haltPoint, answer);
        };
    }

    private PromptResult dispatchPrHalt(
            HaltPoint haltPoint, String answer) {
        validateAnswer(answer, haltPoint);
        List<String> opts = haltPoint.options();
        if (answer.equals(opts.get(0))) {
            return PromptResult.of(PromptAction.CONTINUE);
        }
        if (answer.equals(opts.get(1))) {
            return PromptResult.of(PromptAction.HANDOFF);
        }
        if (answer.equals(opts.get(2))) {
            return PromptResult.of(PromptAction.EXIT);
        }
        throw new PromptInvalidResponseException(answer);
    }

    private PromptResult dispatchRecoverable(
            HaltPoint haltPoint, String answer) {
        validateAnswer(answer, haltPoint);
        List<String> opts = haltPoint.options();
        if (answer.equals(opts.get(0))) {
            return PromptResult.of(PromptAction.RETRY);
        }
        if (answer.equals(opts.get(1))) {
            return PromptResult.of(PromptAction.SKIP);
        }
        if (answer.equals(opts.get(2))) {
            return PromptResult.abort(
                    2, "PROMPT_USER_ABORT");
        }
        throw new PromptInvalidResponseException(answer);
    }

    private static void validateAnswer(
            String answer, HaltPoint haltPoint) {
        if (answer == null || answer.isBlank()) {
            throw new PromptInvalidResponseException(
                    Objects.toString(answer, "<null>"));
        }
        if (!haltPoint.options().contains(answer)) {
            throw new PromptInvalidResponseException(answer);
        }
    }

    private static WaitingFor mapWaitingFor(
            HaltPoint haltPoint) {
        return switch (haltPoint) {
            case APPROVAL_GATE -> WaitingFor.PR_MERGE;
            case BACKMERGE_MERGE ->
                    WaitingFor.BACKMERGE_MERGE;
            case RECOVERABLE_FAILURE ->
                    WaitingFor.USER_CONFIRMATION;
        };
    }

    private static List<NextAction> buildNextActions(
            HaltPoint haltPoint) {
        return haltPoint.options().stream()
                .map(label -> new NextAction(
                        label, mapCommand(haltPoint, label)))
                .toList();
    }

    private static String mapCommand(
            HaltPoint haltPoint,
            String label) {
        if (label.startsWith("Rodar")) {
            return "/x-pr-fix";
        }
        return "/x-release";
    }

    private static String buildQuestion(
            HaltPoint haltPoint,
            ReleaseState state) {
        String version = state.version();
        return switch (haltPoint) {
            case APPROVAL_GATE -> {
                Integer prNum = state.prNumber();
                String prLabel = prNum != null
                        ? "#" + prNum : "unknown";
                yield String.format(
                        "Release v%s — PR %s opened."
                                + " Choose an action:",
                        version, prLabel);
            }
            case BACKMERGE_MERGE -> String.format(
                    "Release v%s — back-merge PR opened."
                            + " Choose an action:",
                    version);
            case RECOVERABLE_FAILURE -> String.format(
                    "Release v%s — recoverable failure."
                            + " Choose an action:",
                    version);
        };
    }

    private static ReleaseState withHaltState(
            ReleaseState state,
            WaitingFor waiting,
            List<NextAction> actions,
            String lastPromptAnsweredAt) {
        return new ReleaseState(
                state.schemaVersion(),
                state.version(),
                state.phase(),
                state.branch(),
                state.baseBranch(),
                state.hotfix(),
                state.dryRun(),
                state.signedTag(),
                state.interactive(),
                state.noWaitCi(),
                state.startedAt(),
                state.lastPhaseCompletedAt(),
                state.phasesCompleted(),
                state.targetVersion(),
                state.previousVersion(),
                state.bumpType(),
                state.prNumber(),
                state.prUrl(),
                state.prTitle(),
                state.changelogEntry(),
                state.tagMessage(),
                state.worktreePath(),
                actions,
                waiting,
                state.phaseDurations(),
                state.ciCheckedAt(),
                state.ciStatus(),
                lastPromptAnsweredAt,
                state.githubReleaseUrl());
    }
}
