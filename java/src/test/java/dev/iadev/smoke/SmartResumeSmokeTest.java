package dev.iadev.smoke;

import dev.iadev.release.resume.DetectedState;
import dev.iadev.release.resume.ResumeAction;
import dev.iadev.release.resume.ResumeDecision;
import dev.iadev.release.resume.ResumeOption;
import dev.iadev.release.resume.SmartResumeOrchestrator;
import dev.iadev.release.resume.StateFileDetector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the complete smart resume flow:
 * detect state file → resolve decision → validate
 * continuation from APPROVAL_PENDING phase.
 *
 * <p>Maps to story-0039-0008 §7 "Cenario: State file
 * presente — escolhe Retomar (happy path)".
 */
@DisplayName("SmartResumeSmokeTest")
class SmartResumeSmokeTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("detect_resolve_resume_happyPath"
            + " — full flow from fixture to continuation")
    void detect_resolve_resume_happyPath()
            throws IOException {
        // Arrange: create state fixture with
        // phase=APPROVAL_PENDING
        Path plansDir = tempDir.resolve("plans");
        Files.createDirectories(plansDir);
        writeApprovalPendingState(plansDir);

        // Act: detect
        StateFileDetector detector =
                new StateFileDetector(plansDir);
        Optional<DetectedState> detected =
                detector.detect();

        // Assert: state detected
        assertThat(detected).isPresent();
        DetectedState state = detected.get();
        assertThat(state.phase())
                .isEqualTo("APPROVAL_PENDING");
        assertThat(state.version()).isEqualTo("3.2.0");

        // Act: resolve (interactive, has new commits)
        SmartResumeOrchestrator orchestrator =
                new SmartResumeOrchestrator(
                        detected, false, true);
        ResumeDecision decision = orchestrator.resolve();

        // Assert: prompt user with 3 options
        assertThat(decision.action())
                .isEqualTo(ResumeAction.PROMPT_USER);
        assertThat(decision.options())
                .containsExactly(
                        ResumeOption.RESUME,
                        ResumeOption.ABORT,
                        ResumeOption.START_NEW);
        assertThat(decision.detectedState()).isPresent();
        assertThat(decision.detectedState().get().phase())
                .isEqualTo("APPROVAL_PENDING");
    }

    @Test
    @DisplayName("detect_noPrompt_stateConflict"
            + " — CI preserves legacy behavior")
    void detect_noPrompt_stateConflict()
            throws IOException {
        Path plansDir = tempDir.resolve("plans");
        Files.createDirectories(plansDir);
        writeApprovalPendingState(plansDir);

        StateFileDetector detector =
                new StateFileDetector(plansDir);
        Optional<DetectedState> detected =
                detector.detect();

        SmartResumeOrchestrator orchestrator =
                new SmartResumeOrchestrator(
                        detected, true, false);
        ResumeDecision decision = orchestrator.resolve();

        assertThat(decision.action())
                .isEqualTo(ResumeAction.STATE_CONFLICT);
        assertThat(decision.errorCode())
                .isEqualTo("STATE_CONFLICT");
    }

    @Test
    @DisplayName("detect_noNewCommits_excludesStartNew"
            + " — only Resume and Abort offered")
    void detect_noNewCommits_excludesStartNew()
            throws IOException {
        Path plansDir = tempDir.resolve("plans");
        Files.createDirectories(plansDir);
        writeApprovalPendingState(plansDir);

        StateFileDetector detector =
                new StateFileDetector(plansDir);
        Optional<DetectedState> detected =
                detector.detect();

        SmartResumeOrchestrator orchestrator =
                new SmartResumeOrchestrator(
                        detected, false, false);
        ResumeDecision decision = orchestrator.resolve();

        assertThat(decision.action())
                .isEqualTo(ResumeAction.PROMPT_USER);
        assertThat(decision.options())
                .containsExactly(
                        ResumeOption.RESUME,
                        ResumeOption.ABORT);
        assertThat(decision.options())
                .doesNotContain(ResumeOption.START_NEW);
    }

    @Test
    @DisplayName("detect_completedState_autoDetect"
            + " — COMPLETED state is ignored")
    void detect_completedState_autoDetect()
            throws IOException {
        Path plansDir = tempDir.resolve("plans");
        Files.createDirectories(plansDir);
        writeCompletedState(plansDir);

        StateFileDetector detector =
                new StateFileDetector(plansDir);
        Optional<DetectedState> detected =
                detector.detect();

        assertThat(detected).isEmpty();

        SmartResumeOrchestrator orchestrator =
                new SmartResumeOrchestrator(
                        detected, false, true);
        ResumeDecision decision = orchestrator.resolve();

        assertThat(decision.action())
                .isEqualTo(ResumeAction.AUTO_DETECT);
    }

    @Test
    @DisplayName("detect_noStateFile_autoDetect"
            + " — degenerate case")
    void detect_noStateFile_autoDetect()
            throws IOException {
        Path plansDir = tempDir.resolve("plans");
        Files.createDirectories(plansDir);

        StateFileDetector detector =
                new StateFileDetector(plansDir);
        Optional<DetectedState> detected =
                detector.detect();

        assertThat(detected).isEmpty();

        SmartResumeOrchestrator orchestrator =
                new SmartResumeOrchestrator(
                        detected, false, true);
        ResumeDecision decision = orchestrator.resolve();

        assertThat(decision.action())
                .isEqualTo(ResumeAction.AUTO_DETECT);
    }

    @Test
    @DisplayName("promptDisplay_matchesStorySpec"
            + " — §5.2 format")
    void promptDisplay_matchesStorySpec()
            throws IOException {
        Path plansDir = tempDir.resolve("plans");
        Files.createDirectories(plansDir);
        writeApprovalPendingState(plansDir);

        StateFileDetector detector =
                new StateFileDetector(plansDir);
        Optional<DetectedState> detected =
                detector.detect();

        assertThat(detected).isPresent();
        String display = SmartResumeOrchestrator
                .buildPromptDisplay(detected.get());

        assertThat(display).contains("3.2.0");
        assertThat(display).contains("v3.1.0");
        assertThat(display).contains("APPROVAL_PENDING");
        assertThat(display).contains("Resume");
        assertThat(display).contains("Abort");
    }

    private void writeApprovalPendingState(Path plansDir)
            throws IOException {
        Instant twoHoursAgo =
                Instant.now().minusSeconds(7200);
        String json = stateJson(
                "APPROVAL_PENDING", "3.2.0", "v3.1.0",
                twoHoursAgo);
        Files.writeString(
                plansDir.resolve(
                        "release-state-3.2.0.json"),
                json);
    }

    private void writeCompletedState(Path plansDir)
            throws IOException {
        String json = stateJson(
                "COMPLETED", "3.1.0", "v3.0.0",
                Instant.now().minusSeconds(86400));
        Files.writeString(
                plansDir.resolve(
                        "release-state-3.1.0.json"),
                json);
    }

    private String stateJson(String phase, String version,
            String previousVersion, Instant lastCompleted) {
        return """
                {
                  "schemaVersion": 2,
                  "version": "%s",
                  "phase": "%s",
                  "branch": "release/%s",
                  "baseBranch": "develop",
                  "hotfix": false,
                  "dryRun": false,
                  "signedTag": false,
                  "interactive": false,
                  "startedAt": "2026-04-15T10:00:00Z",
                  "lastPhaseCompletedAt": "%s",
                  "phasesCompleted": [],
                  "targetVersion": "%s",
                  "previousVersion": "%s",
                  "bumpType": "minor",
                  "prNumber": null,
                  "prUrl": null,
                  "prTitle": null,
                  "changelogEntry": null,
                  "tagMessage": null,
                  "worktreePath": null,
                  "nextActions": [],
                  "waitingFor": null,
                  "phaseDurations": {},
                  "lastPromptAnsweredAt": null,
                  "githubReleaseUrl": null
                }
                """.formatted(version, phase, version,
                lastCompleted.toString(), version,
                previousVersion);
    }
}
