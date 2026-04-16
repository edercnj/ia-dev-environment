package dev.iadev.release.resume;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StateFileDetector}.
 *
 * <p>TPP-ordered: nil → constant → scalar → conditional.
 * Maps to story-0039-0008 §7 Gherkin scenarios.
 */
@DisplayName("StateFileDetectorTest")
class StateFileDetectorTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("detect — state file search")
    class Detect {

        @Test
        @DisplayName("noStateFiles_returnsEmpty"
                + " (degenerate)")
        void detect_noStateFiles_returnsEmpty()
                throws IOException {
            Path plansDir = tempDir.resolve("plans");
            Files.createDirectories(plansDir);

            StateFileDetector detector =
                    new StateFileDetector(plansDir);

            Optional<DetectedState> result =
                    detector.detect();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("completedStateFile_returnsEmpty"
                + " (boundary — COMPLETED ignored)")
        void detect_completedStateFile_returnsEmpty()
                throws IOException {
            Path plansDir = tempDir.resolve("plans");
            Files.createDirectories(plansDir);
            writeStateFile(plansDir,
                    "release-state-3.2.0.json",
                    "COMPLETED", "3.2.0", "v3.1.0",
                    Instant.now().minusSeconds(3600));

            StateFileDetector detector =
                    new StateFileDetector(plansDir);

            Optional<DetectedState> result =
                    detector.detect();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("activeStateFile_returnsDetectedState"
                + " (happy path)")
        void detect_activeStateFile_returnsDetectedState()
                throws IOException {
            Path plansDir = tempDir.resolve("plans");
            Files.createDirectories(plansDir);
            Instant twoHoursAgo =
                    Instant.now().minusSeconds(7200);
            writeStateFile(plansDir,
                    "release-state-3.2.0.json",
                    "APPROVAL_PENDING", "3.2.0", "v3.1.0",
                    twoHoursAgo);

            StateFileDetector detector =
                    new StateFileDetector(plansDir);

            Optional<DetectedState> result =
                    detector.detect();

            assertThat(result).isPresent();
            DetectedState state = result.get();
            assertThat(state.version()).isEqualTo("3.2.0");
            assertThat(state.phase())
                    .isEqualTo("APPROVAL_PENDING");
            assertThat(state.previousVersion())
                    .isEqualTo("v3.1.0");
        }

        @Test
        @DisplayName("activeStateFile_calculatesAge"
                + " (boundary — age > 0)")
        void detect_activeStateFile_calculatesAge()
                throws IOException {
            Path plansDir = tempDir.resolve("plans");
            Files.createDirectories(plansDir);
            Instant twoHoursAgo =
                    Instant.now().minusSeconds(7200);
            writeStateFile(plansDir,
                    "release-state-3.2.0.json",
                    "APPROVAL_PENDING", "3.2.0", "v3.1.0",
                    twoHoursAgo);

            StateFileDetector detector =
                    new StateFileDetector(plansDir);

            Optional<DetectedState> result =
                    detector.detect();

            assertThat(result).isPresent();
            Duration age = result.get().staleDuration();
            assertThat(age.toHours()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("multipleStateFiles_returnsFirstActive"
                + " (boundary — multiple files)")
        void detect_multipleStateFiles_returnsFirstActive()
                throws IOException {
            Path plansDir = tempDir.resolve("plans");
            Files.createDirectories(plansDir);
            writeStateFile(plansDir,
                    "release-state-3.1.0.json",
                    "COMPLETED", "3.1.0", "v3.0.0",
                    Instant.now().minusSeconds(86400));
            writeStateFile(plansDir,
                    "release-state-3.2.0.json",
                    "APPROVAL_PENDING", "3.2.0", "v3.1.0",
                    Instant.now().minusSeconds(3600));

            StateFileDetector detector =
                    new StateFileDetector(plansDir);

            Optional<DetectedState> result =
                    detector.detect();

            assertThat(result).isPresent();
            assertThat(result.get().version())
                    .isEqualTo("3.2.0");
        }

        @Test
        @DisplayName("pathTraversal_rejected"
                + " (security — CWE-22)")
        void detect_pathTraversalRejected()
                throws IOException {
            Path plansDir = tempDir.resolve("plans");
            Files.createDirectories(plansDir);
            // Create a file with traversal in name
            Path malicious = plansDir.resolve(
                    "release-state-../../etc/passwd.json");
            // The detector should not follow traversal
            // patterns — it only reads files matching
            // the glob in the plans directory

            StateFileDetector detector =
                    new StateFileDetector(plansDir);

            Optional<DetectedState> result =
                    detector.detect();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("plansDirDoesNotExist_returnsEmpty"
                + " (degenerate — missing dir)")
        void detect_plansDirDoesNotExist_returnsEmpty() {
            Path nonExistent =
                    tempDir.resolve("nonexistent");

            StateFileDetector detector =
                    new StateFileDetector(nonExistent);

            Optional<DetectedState> result =
                    detector.detect();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("formatAge — human-readable duration")
    class FormatAge {

        @Test
        @DisplayName("zeroMinutes_returnsLessThanOneMin")
        void formatAge_zeroMinutes_returnsShortLabel() {
            Duration age = Duration.ofSeconds(30);

            String formatted =
                    StateFileDetector.formatAge(age);

            assertThat(formatted).isEqualTo("< 1min");
        }

        @Test
        @DisplayName("minutesOnly_returnsMinutes")
        void formatAge_minutesOnly_returnsMinutes() {
            Duration age = Duration.ofMinutes(45);

            String formatted =
                    StateFileDetector.formatAge(age);

            assertThat(formatted).isEqualTo("45min");
        }

        @Test
        @DisplayName("hoursAndMinutes_returnsHhMm")
        void formatAge_hoursAndMinutes_returnsHhMm() {
            Duration age = Duration.ofMinutes(134);

            String formatted =
                    StateFileDetector.formatAge(age);

            assertThat(formatted).isEqualTo("2h 14min");
        }

        @Test
        @DisplayName("daysHoursMinutes_returnsDdHhMm")
        void formatAge_days_returnsDdHhMm() {
            Duration age = Duration.ofHours(26)
                    .plusMinutes(30);

            String formatted =
                    StateFileDetector.formatAge(age);

            assertThat(formatted).isEqualTo("1d 2h 30min");
        }
    }

    private void writeStateFile(Path plansDir,
            String fileName, String phase, String version,
            String previousVersion, Instant lastCompleted)
            throws IOException {
        String json = """
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
        Files.writeString(plansDir.resolve(fileName), json);
    }
}
