package dev.iadev.release.abort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AbortOrchestrator}.
 *
 * <p>TPP ordering: nil (no state) -> collection (dry-run +
 * cancel) -> conditional (--yes, warn-only failures).
 * Covers story-0039-0010 §7 acceptance criteria for
 * {@code --abort}.
 */
@DisplayName("AbortOrchestratorTest")
class AbortOrchestratorTest {

    private static final String VALID_STATE_JSON = """
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
              "lastPhaseCompletedAt": "2026-04-13T08:12:34Z",
              "phasesCompleted": ["INITIALIZED"],
              "targetVersion": "3.2.0",
              "previousVersion": "3.1.0",
              "bumpType": "minor",
              "prNumber": 297,
              "prUrl": "https://github.com/owner/repo/pull/297",
              "prTitle": "release: v3.2.0",
              "changelogEntry": null,
              "tagMessage": null,
              "worktreePath": null,
              "nextActions": [],
              "waitingFor": "PR_MERGE",
              "phaseDurations": {},
              "lastPromptAnsweredAt": null,
              "githubReleaseUrl": null
            }
            """;

    @Nested
    @DisplayName("when no state file exists")
    class NoStateFile {

        @Test
        @DisplayName(
                "abort_noStateFile_returnsAbortNoRelease")
        void abort_noStateFile_returnsAbortNoRelease() {
            Path nonExistent = Path.of(
                    "/tmp/nonexistent-state.json");
            AbortOrchestrator orchestrator =
                    new AbortOrchestrator(
                            msg -> true,
                            new NoOpCleanupPort());

            AbortResult result = orchestrator.abort(
                    nonExistent, false);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.errorCode())
                    .isEqualTo("ABORT_NO_RELEASE");
        }
    }

    @Nested
    @DisplayName("when user cancels first confirmation")
    class CancelFirstConfirmation {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName(
                "abort_cancelFirst_returnsUserCancelled"
                        + "AndNoResourceTouched")
        void abort_cancelFirst_returnsUserCancelled()
                throws IOException {
            Path stateFile = writeStateFile(tempDir);
            RecordingCleanupPort cleanupPort =
                    new RecordingCleanupPort();
            AbortOrchestrator orchestrator =
                    new AbortOrchestrator(
                            msg -> false,
                            cleanupPort);

            AbortResult result = orchestrator.abort(
                    stateFile, false);

            assertThat(result.exitCode()).isEqualTo(2);
            assertThat(result.errorCode())
                    .isEqualTo("ABORT_USER_CANCELLED");
            assertThat(cleanupPort.prClosed).isFalse();
            assertThat(cleanupPort.localBranchDeleted)
                    .isFalse();
            assertThat(cleanupPort.remoteBranchDeleted)
                    .isFalse();
            assertThat(cleanupPort.stateFileDeleted)
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("when user cancels second confirmation")
    class CancelSecondConfirmation {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName(
                "abort_cancelSecond_returnsUserCancelled"
                        + "AndNoResourceTouched")
        void abort_cancelSecond_returnsUserCancelled()
                throws IOException {
            Path stateFile = writeStateFile(tempDir);
            RecordingCleanupPort cleanupPort =
                    new RecordingCleanupPort();
            int[] callCount = {0};
            AbortOrchestrator orchestrator =
                    new AbortOrchestrator(
                            msg -> {
                                callCount[0]++;
                                return callCount[0] == 1;
                            },
                            cleanupPort);

            AbortResult result = orchestrator.abort(
                    stateFile, false);

            assertThat(result.exitCode()).isEqualTo(2);
            assertThat(result.errorCode())
                    .isEqualTo("ABORT_USER_CANCELLED");
            assertThat(callCount[0]).isEqualTo(2);
            assertThat(cleanupPort.prClosed).isFalse();
        }
    }

    @Nested
    @DisplayName("when user confirms both prompts")
    class ConfirmBoth {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName(
                "abort_confirmBoth_executesFullCleanup")
        void abort_confirmBoth_executesFullCleanup()
                throws IOException {
            Path stateFile = writeStateFile(tempDir);
            RecordingCleanupPort cleanupPort =
                    new RecordingCleanupPort();
            AbortOrchestrator orchestrator =
                    new AbortOrchestrator(
                            msg -> true,
                            cleanupPort);

            AbortResult result = orchestrator.abort(
                    stateFile, false);

            assertThat(result.exitCode()).isZero();
            assertThat(cleanupPort.prClosed).isTrue();
            assertThat(cleanupPort.closedPrNumber)
                    .isEqualTo(297);
            assertThat(cleanupPort.localBranchDeleted)
                    .isTrue();
            assertThat(cleanupPort.deletedLocalBranch)
                    .isEqualTo("release/3.2.0");
            assertThat(cleanupPort.remoteBranchDeleted)
                    .isTrue();
            assertThat(cleanupPort.stateFileDeleted).isTrue();
        }
    }

    @Nested
    @DisplayName("when --yes/--force bypasses confirmations")
    class ForceMode {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName(
                "abort_forceMode_skipsConfirmations"
                        + "AndExecutesCleanup")
        void abort_forceMode_skipsConfirmations()
                throws IOException {
            Path stateFile = writeStateFile(tempDir);
            RecordingCleanupPort cleanupPort =
                    new RecordingCleanupPort();
            int[] confirmCalled = {0};
            AbortOrchestrator orchestrator =
                    new AbortOrchestrator(
                            msg -> {
                                confirmCalled[0]++;
                                return false;
                            },
                            cleanupPort);

            AbortResult result = orchestrator.abort(
                    stateFile, true);

            assertThat(result.exitCode()).isZero();
            assertThat(confirmCalled[0]).isZero();
            assertThat(cleanupPort.prClosed).isTrue();
            assertThat(cleanupPort.stateFileDeleted).isTrue();
        }
    }

    @Nested
    @DisplayName("when cleanup operations fail (warn-only)")
    class WarnOnlyFailures {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName(
                "abort_ghCliFails_warnOnlyAndExitZero")
        void abort_ghCliFails_warnOnlyAndExitZero()
                throws IOException {
            Path stateFile = writeStateFile(tempDir);
            FailingPrCleanupPort cleanupPort =
                    new FailingPrCleanupPort();
            AbortOrchestrator orchestrator =
                    new AbortOrchestrator(
                            msg -> true,
                            cleanupPort);

            AbortResult result = orchestrator.abort(
                    stateFile, true);

            assertThat(result.exitCode()).isZero();
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "ABORT_PR_CLOSE_FAILED"));
            assertThat(cleanupPort.localBranchDeleted)
                    .isTrue();
            assertThat(cleanupPort.stateFileDeleted).isTrue();
        }

        @Test
        @DisplayName(
                "abort_branchDeleteFails_warnOnlyAndContinues")
        void abort_branchDeleteFails_warnOnlyAndContinues()
                throws IOException {
            Path stateFile = writeStateFile(tempDir);
            FailingBranchCleanupPort cleanupPort =
                    new FailingBranchCleanupPort();
            AbortOrchestrator orchestrator =
                    new AbortOrchestrator(
                            msg -> true,
                            cleanupPort);

            AbortResult result = orchestrator.abort(
                    stateFile, true);

            assertThat(result.exitCode()).isZero();
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "ABORT_BRANCH_DELETE_FAILED"));
            assertThat(cleanupPort.stateFileDeleted).isTrue();
        }
    }

    @Nested
    @DisplayName("when state file is corrupted JSON")
    class CorruptedState {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName(
                "abort_corruptedJson_returnsParseFailedError")
        void abort_corruptedJson_returnsParseFailedError()
                throws IOException {
            Path stateFile = tempDir.resolve(
                    "release-state-bad.json");
            Files.writeString(stateFile, "{{bad json");
            AbortOrchestrator orchestrator =
                    new AbortOrchestrator(
                            msg -> true,
                            new NoOpCleanupPort());

            AbortResult result = orchestrator.abort(
                    stateFile, false);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.errorCode())
                    .isEqualTo("STATUS_PARSE_FAILED");
        }
    }

    @Nested
    @DisplayName("when state has no PR (null prNumber)")
    class NoPrInState {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName(
                "abort_noPr_skipsClosePrAndCleansRest")
        void abort_noPr_skipsClosePrAndCleansRest()
                throws IOException {
            String noPrState = VALID_STATE_JSON
                    .replace("\"prNumber\": 297",
                            "\"prNumber\": null");
            Path stateFile = tempDir.resolve(
                    "release-state-noPr.json");
            Files.writeString(stateFile, noPrState);
            RecordingCleanupPort cleanupPort =
                    new RecordingCleanupPort();
            AbortOrchestrator orchestrator =
                    new AbortOrchestrator(
                            msg -> true,
                            cleanupPort);

            AbortResult result = orchestrator.abort(
                    stateFile, true);

            assertThat(result.exitCode()).isZero();
            assertThat(cleanupPort.prClosed).isFalse();
            assertThat(cleanupPort.localBranchDeleted)
                    .isTrue();
            assertThat(cleanupPort.stateFileDeleted).isTrue();
        }
    }

    // --- Test doubles ---

    private static Path writeStateFile(Path dir)
            throws IOException {
        Path stateFile = dir.resolve(
                "release-state-3.2.0.json");
        Files.writeString(stateFile, VALID_STATE_JSON);
        return stateFile;
    }

    private static final class NoOpCleanupPort
            implements CleanupPort {
        @Override
        public void closePr(int prNumber) { }
        @Override
        public void deleteLocalBranch(String name) { }
        @Override
        public void deleteRemoteBranch(String name) { }
        @Override
        public void deleteStateFile(Path path) { }
    }

    static class RecordingCleanupPort
            implements CleanupPort {
        boolean prClosed;
        int closedPrNumber;
        boolean localBranchDeleted;
        String deletedLocalBranch;
        boolean remoteBranchDeleted;
        boolean stateFileDeleted;

        @Override
        public void closePr(int prNumber) {
            prClosed = true;
            closedPrNumber = prNumber;
        }

        @Override
        public void deleteLocalBranch(String name) {
            localBranchDeleted = true;
            deletedLocalBranch = name;
        }

        @Override
        public void deleteRemoteBranch(String name) {
            remoteBranchDeleted = true;
        }

        @Override
        public void deleteStateFile(Path path) {
            stateFileDeleted = true;
        }
    }

    private static final class FailingPrCleanupPort
            extends RecordingCleanupPort {
        @Override
        public void closePr(int prNumber) {
            throw new CleanupException(
                    "ABORT_PR_CLOSE_FAILED",
                    "gh CLI returned non-zero exit code");
        }
    }

    private static final class FailingBranchCleanupPort
            extends RecordingCleanupPort {
        @Override
        public void deleteLocalBranch(String name) {
            throw new CleanupException(
                    "ABORT_BRANCH_DELETE_FAILED",
                    "Branch not found: " + name);
        }

        @Override
        public void deleteRemoteBranch(String name) {
            throw new CleanupException(
                    "ABORT_BRANCH_DELETE_FAILED",
                    "Remote branch not found");
        }
    }
}
