package dev.iadev.release.abort;

import java.nio.file.Path;

/**
 * Port for cleanup operations executed during release abort.
 *
 * <p>Each operation is expected to be idempotent and to throw
 * {@link CleanupException} on failure. The orchestrator
 * catches each failure individually and logs a warning
 * (warn-only policy per story-0039-0010 §3.2).
 */
public interface CleanupPort {

    /**
     * Closes a GitHub PR by number.
     *
     * @param prNumber PR number to close
     * @throws CleanupException if gh CLI fails
     */
    void closePr(int prNumber);

    /**
     * Deletes a local git branch.
     *
     * @param branchName branch to delete
     * @throws CleanupException if git fails
     */
    void deleteLocalBranch(String branchName);

    /**
     * Deletes a remote git branch.
     *
     * @param branchName remote branch to delete
     * @throws CleanupException if git push --delete fails
     */
    void deleteRemoteBranch(String branchName);

    /**
     * Deletes the state file from the filesystem.
     *
     * @param stateFilePath path to the state file
     * @throws CleanupException if file deletion fails
     */
    void deleteStateFile(Path stateFilePath);
}
