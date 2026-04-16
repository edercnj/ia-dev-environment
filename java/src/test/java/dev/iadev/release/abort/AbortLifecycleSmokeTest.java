package dev.iadev.release.abort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the abort lifecycle: given a state file
 * with PR, branches, and state — {@code --abort --yes}
 * executes full cleanup and the state file is removed.
 *
 * <p>Covers story-0039-0010 §TASK-004 acceptance criteria:
 * fixture with state + PR mock + local/remote branches;
 * abort cleanup completes.
 */
@DisplayName("AbortLifecycleSmokeTest")
class AbortLifecycleSmokeTest {

    private static final String FULL_STATE_JSON = """
            {
              "schemaVersion": 2,
              "version": "3.2.0",
              "phase": "APPROVAL_PENDING",
              "branch": "release/3.2.0",
              "baseBranch": "develop",
              "hotfix": false,
              "dryRun": false,
              "signedTag": false,
              "interactive": false,
              "startedAt": "2026-04-13T08:00:00Z",
              "lastPhaseCompletedAt": \
            "2026-04-13T08:12:34Z",
              "phasesCompleted": \
            ["INITIALIZED", "DETERMINE", "VALIDATE-DEEP"],
              "targetVersion": "3.2.0",
              "previousVersion": "3.1.0",
              "bumpType": "minor",
              "prNumber": 297,
              "prUrl": \
            "https://github.com/owner/repo/pull/297",
              "prTitle": "release: v3.2.0",
              "changelogEntry": "## [3.2.0]",
              "tagMessage": null,
              "worktreePath": null,
              "nextActions": [
                {
                  "label": "PR merged — continue",
                  "command": "/x-release"
                }
              ],
              "waitingFor": "PR_MERGE",
              "phaseDurations": {"DETERMINE": 5},
              "lastPromptAnsweredAt": \
            "2026-04-13T08:12:35Z",
              "githubReleaseUrl": null
            }
            """;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName(
            "fullAbortLifecycle_forceMode_"
                    + "cleansAllResourcesAndRemovesState")
    void fullAbortLifecycle_forceMode_cleansAll()
            throws IOException {
        // Arrange: create state file in temp dir
        Path stateFile = tempDir.resolve(
                "release-state-3.2.0.json");
        Files.writeString(stateFile, FULL_STATE_JSON);
        assertThat(stateFile).exists();

        InvocationLog log = new InvocationLog();
        AbortOrchestrator orchestrator =
                new AbortOrchestrator(
                        msg -> {
                            throw new AssertionError(
                                    "Should not be called "
                                            + "in force mode");
                        },
                        log);

        // Act: abort in force mode
        AbortResult result = orchestrator.abort(
                stateFile, true);

        // Assert: exit 0, all resources touched
        assertThat(result.exitCode()).isZero();
        assertThat(result.output())
                .contains("v3.2.0")
                .contains("Cleanup complete");

        assertThat(log.closePrCalled).isTrue();
        assertThat(log.closedPrNumber).isEqualTo(297);
        assertThat(log.deleteLocalBranchCalled).isTrue();
        assertThat(log.deletedBranchName)
                .isEqualTo("release/3.2.0");
        assertThat(log.deleteRemoteBranchCalled).isTrue();
        assertThat(log.deleteStateFileCalled).isTrue();
        assertThat(log.deletedStateFilePath)
                .isEqualTo(stateFile);

        // No warnings expected in clean run
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    @DisplayName(
            "fullAbortLifecycle_partialFailures_"
                    + "warnOnlyExitZero")
    void fullAbortLifecycle_partialFailures_warnOnly()
            throws IOException {
        Path stateFile = tempDir.resolve(
                "release-state-fail.json");
        Files.writeString(stateFile, FULL_STATE_JSON);

        PartiallyFailingCleanupPort cleanupPort =
                new PartiallyFailingCleanupPort();
        AbortOrchestrator orchestrator =
                new AbortOrchestrator(
                        msg -> true,
                        cleanupPort);

        AbortResult result = orchestrator.abort(
                stateFile, true);

        assertThat(result.exitCode()).isZero();
        assertThat(result.warnings())
                .hasSize(2)
                .anyMatch(w -> w.contains(
                        "ABORT_PR_CLOSE_FAILED"))
                .anyMatch(w -> w.contains(
                        "ABORT_BRANCH_DELETE_FAILED"));
        // State file deletion still happened
        assertThat(cleanupPort.stateFileDeleted).isTrue();
    }

    // --- Test doubles ---

    private static final class InvocationLog
            implements CleanupPort {
        boolean closePrCalled;
        int closedPrNumber;
        boolean deleteLocalBranchCalled;
        String deletedBranchName;
        boolean deleteRemoteBranchCalled;
        boolean deleteStateFileCalled;
        Path deletedStateFilePath;

        @Override
        public void closePr(int prNumber) {
            closePrCalled = true;
            closedPrNumber = prNumber;
        }

        @Override
        public void deleteLocalBranch(String name) {
            deleteLocalBranchCalled = true;
            deletedBranchName = name;
        }

        @Override
        public void deleteRemoteBranch(String name) {
            deleteRemoteBranchCalled = true;
        }

        @Override
        public void deleteStateFile(Path path) {
            deleteStateFileCalled = true;
            deletedStateFilePath = path;
        }
    }

    private static final class PartiallyFailingCleanupPort
            implements CleanupPort {
        boolean stateFileDeleted;

        @Override
        public void closePr(int prNumber) {
            throw new CleanupException(
                    "ABORT_PR_CLOSE_FAILED",
                    "gh: not logged in");
        }

        @Override
        public void deleteLocalBranch(String name) {
            throw new CleanupException(
                    "ABORT_BRANCH_DELETE_FAILED",
                    "error: branch not found");
        }

        @Override
        public void deleteRemoteBranch(String name) {
            // succeeds
        }

        @Override
        public void deleteStateFile(Path path) {
            stateFileDeleted = true;
        }
    }
}
