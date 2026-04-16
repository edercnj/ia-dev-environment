package dev.iadev.release.resume;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Decides the smart resume action based on detected state,
 * prompt mode, and new-commits availability.
 *
 * <p>Decision matrix (story-0039-0008 §3):
 * <ul>
 *   <li>No state found → AUTO_DETECT</li>
 *   <li>State found + {@code --no-prompt} → STATE_CONFLICT
 *       (legacy CI behavior preserved)</li>
 *   <li>State found + interactive → PROMPT_USER with
 *       options [RESUME, ABORT, START_NEW?]</li>
 * </ul>
 *
 * <p>{@code START_NEW} is offered only when
 * {@code hasNewCommits} is {@code true} (§3.2).
 */
public final class SmartResumeOrchestrator {

    private final Optional<DetectedState> detectedState;
    private final boolean noPrompt;
    private final boolean hasNewCommits;

    /**
     * @param detectedState result from StateFileDetector
     * @param noPrompt      true if --no-prompt is set
     * @param hasNewCommits true if versionable commits exist
     *                      since the base tag
     */
    public SmartResumeOrchestrator(
            Optional<DetectedState> detectedState,
            boolean noPrompt,
            boolean hasNewCommits) {
        this.detectedState = detectedState;
        this.noPrompt = noPrompt;
        this.hasNewCommits = hasNewCommits;
    }

    /**
     * Resolves the resume decision.
     *
     * @return immutable decision with action and options
     */
    public ResumeDecision resolve() {
        if (detectedState.isEmpty()) {
            return ResumeDecision.autoDetect();
        }
        if (noPrompt) {
            return ResumeDecision.stateConflict();
        }
        return buildPromptDecision(detectedState.get());
    }

    /**
     * Builds a user-facing display string matching the
     * format specified in story-0039-0008 §5.2.
     *
     * @param state the detected in-flight state
     * @return formatted display string
     */
    public static String buildPromptDisplay(
            DetectedState state) {
        String age = StateFileDetector
                .formatAge(state.staleDuration());
        return """
                Release in progress detected:
                  Version: %s (from %s)
                  Phase: %s
                  Stale for: %s

                What would you like to do?
                  [1] Resume from %s
                  [2] Abort release %s (full cleanup)
                  [3] Start new release (discard state)\
                """.formatted(
                state.version(),
                state.previousVersion(),
                state.phase(),
                age,
                state.phase(),
                state.version());
    }

    private ResumeDecision buildPromptDecision(
            DetectedState state) {
        List<ResumeOption> options = new ArrayList<>();
        options.add(ResumeOption.RESUME);
        options.add(ResumeOption.ABORT);
        if (hasNewCommits) {
            options.add(ResumeOption.START_NEW);
        }
        return ResumeDecision.promptUser(state, options);
    }
}
