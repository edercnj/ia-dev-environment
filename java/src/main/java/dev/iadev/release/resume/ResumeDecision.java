package dev.iadev.release.resume;

import java.util.List;
import java.util.Optional;

/**
 * Immutable result of {@link SmartResumeOrchestrator#resolve()}.
 * Carries the decision action, the available options for user
 * prompt (if applicable), and an optional error code.
 *
 * @param action        the resolved action
 * @param options       prompt options (empty if not PROMPT_USER)
 * @param detectedState the detected state (empty if none)
 * @param errorCode     error code (null unless STATE_CONFLICT)
 */
public record ResumeDecision(
        ResumeAction action,
        List<ResumeOption> options,
        Optional<DetectedState> detectedState,
        String errorCode) {

    /** Factory for AUTO_DETECT (no state found). */
    static ResumeDecision autoDetect() {
        return new ResumeDecision(
                ResumeAction.AUTO_DETECT,
                List.of(),
                Optional.empty(),
                null);
    }

    /** Factory for STATE_CONFLICT (--no-prompt). */
    static ResumeDecision stateConflict() {
        return new ResumeDecision(
                ResumeAction.STATE_CONFLICT,
                List.of(),
                Optional.empty(),
                "STATE_CONFLICT");
    }

    /** Factory for PROMPT_USER with options. */
    static ResumeDecision promptUser(
            DetectedState state,
            List<ResumeOption> options) {
        return new ResumeDecision(
                ResumeAction.PROMPT_USER,
                List.copyOf(options),
                Optional.of(state),
                null);
    }
}
