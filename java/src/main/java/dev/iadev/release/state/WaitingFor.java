package dev.iadev.release.state;

/**
 * Halt mode declared by {@code x-release} when it pauses
 * mid-flow waiting for an external event or operator action.
 *
 * <p>Persisted in the {@code waitingFor} field of the
 * release state file ({@code schemaVersion: 2}) introduced
 * by EPIC-0039 story-0039-0002.
 */
public enum WaitingFor {

    /** Not halted — active execution. */
    NONE,

    /** Waiting for the release PR to be reviewed. */
    PR_REVIEW,

    /** Waiting for the release PR to be merged. */
    PR_MERGE,

    /** Waiting for the back-merge PR to be reviewed. */
    BACKMERGE_REVIEW,

    /** Waiting for the back-merge PR to be merged. */
    BACKMERGE_MERGE,

    /** Waiting for explicit operator confirmation. */
    USER_CONFIRMATION
}
