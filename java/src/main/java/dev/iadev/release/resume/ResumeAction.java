package dev.iadev.release.resume;

/**
 * Possible outcomes of the smart resume decision logic.
 *
 * <p>Each value maps to a distinct control-flow path in
 * the {@code x-release} skill Step 0.5.
 */
public enum ResumeAction {

    /** No active state — proceed with version auto-detect. */
    AUTO_DETECT,

    /** Active state found — prompt user for choice. */
    PROMPT_USER,

    /**
     * Active state found but {@code --no-prompt} is set —
     * preserve legacy {@code STATE_CONFLICT} abort for CI.
     */
    STATE_CONFLICT
}
