package dev.iadev.release.prompt;

/**
 * Actions that the {@link PromptEngine} can resolve from
 * an operator's halt-point response.
 */
public enum PromptAction {

    /** Continue the release flow (next phase). */
    CONTINUE,

    /** Exit the skill, preserving state for later resume. */
    EXIT,

    /** Retry the failed operation. */
    RETRY,

    /** Skip the current step and advance. */
    SKIP,

    /** Abort the release with error code. */
    ABORT,

    /** Hand off to another skill (e.g. /x-pr-fix). */
    HANDOFF
}
