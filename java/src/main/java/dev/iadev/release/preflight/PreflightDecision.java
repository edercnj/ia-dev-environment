package dev.iadev.release.preflight;

/**
 * Operator decision at the pre-flight prompt (story-0039-0009 §3.2).
 */
public enum PreflightDecision {
    /** Continue with the release. */
    PROCEED,
    /** Operator wants to change the version; abort with instruction. */
    EDIT_VERSION,
    /** Operator aborts the release cleanly. */
    ABORT
}
