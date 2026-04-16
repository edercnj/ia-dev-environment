package dev.iadev.release.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StatusReporter}.
 *
 * <p>TPP ordering: nil (no state) -> constant (valid state)
 * -> scalar (corrupted state). Covers story-0039-0010 §7
 * acceptance criteria for {@code --status}.
 */
@DisplayName("StatusReporterTest")
class StatusReporterTest {

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
              "nextActions": [
                {
                  "label": "PR merged — continue",
                  "command": "/x-release"
                },
                {
                  "label": "Run fix-pr-comments",
                  "command": "/x-pr-fix"
                }
              ],
              "waitingFor": "PR_MERGE",
              "phaseDurations": {},
              "lastPromptAnsweredAt": "2026-04-13T08:12:35Z",
              "githubReleaseUrl": null
            }
            """;

    @Nested
    @DisplayName("when no state file exists")
    class NoStateFile {

        @Test
        @DisplayName("report_noStateFile_returnsNoReleaseMessage")
        void report_noStateFile_returnsNoReleaseMessage() {
            Path nonExistent = Path.of(
                    "/tmp/nonexistent-state-file.json");
            StatusReporter reporter = new StatusReporter();

            StatusResult result = reporter.report(nonExistent);

            assertThat(result.exitCode()).isZero();
            assertThat(result.output()).contains(
                    "No release in progress.");
        }
    }

    @Nested
    @DisplayName("when state file contains invalid JSON")
    class CorruptedStateFile {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName(
                "report_corruptedJson_returnsParseFailedError")
        void report_corruptedJson_returnsParseFailedError()
                throws IOException {
            Path stateFile = tempDir.resolve(
                    "release-state-bad.json");
            Files.writeString(stateFile,
                    "{ this is not valid JSON }}");
            StatusReporter reporter = new StatusReporter();

            StatusResult result = reporter.report(stateFile);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.errorCode())
                    .isEqualTo("STATUS_PARSE_FAILED");
            assertThat(result.output())
                    .contains("Failed to parse state file");
            // Must NOT expose full path (J7 — error hygiene)
            assertThat(result.output())
                    .doesNotContain(tempDir.toString());
        }
    }

    private static final String MINIMAL_STATE_JSON = """
            {
              "schemaVersion": 2,
              "version": "1.0.0",
              "phase": "BRANCH",
              "branch": "release/1.0.0",
              "baseBranch": "develop",
              "hotfix": false,
              "dryRun": false,
              "signedTag": false,
              "interactive": false,
              "startedAt": "2026-04-13T08:00:00Z",
              "lastPhaseCompletedAt": null,
              "phasesCompleted": [],
              "targetVersion": "1.0.0",
              "previousVersion": null,
              "bumpType": "minor",
              "prNumber": null,
              "prUrl": null,
              "prTitle": null,
              "changelogEntry": null,
              "tagMessage": null,
              "worktreePath": null,
              "nextActions": null,
              "waitingFor": null,
              "phaseDurations": {},
              "lastPromptAnsweredAt": null,
              "githubReleaseUrl": null
            }
            """;

    @Nested
    @DisplayName("duration formatting")
    class DurationFormatting {

        @Test
        @DisplayName(
                "formatDuration_lessThanOneHour_showsMinutes")
        void formatDuration_lessThanOneHour_showsMinutes() {
            assertThat(StatusReporter.formatDuration(
                    java.time.Duration.ofMinutes(42)))
                    .isEqualTo("42min");
        }

        @Test
        @DisplayName(
                "formatDuration_moreThanOneHour_showsHoursMin")
        void formatDuration_moreThanOneHour_showsHoursMin() {
            assertThat(StatusReporter.formatDuration(
                    java.time.Duration.ofMinutes(134)))
                    .isEqualTo("2h 14min");
        }

        @Test
        @DisplayName(
                "formatDuration_zeroMinutes_showsZeroMin")
        void formatDuration_zeroMinutes_showsZeroMin() {
            assertThat(StatusReporter.formatDuration(
                    java.time.Duration.ZERO))
                    .isEqualTo("0min");
        }
    }

    @Nested
    @DisplayName("when state has minimal fields (nulls)")
    class MinimalStateFile {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName(
                "report_minimalState_handlesNullFields"
                        + "Gracefully")
        void report_minimalState_handlesNullFields()
                throws IOException {
            Path stateFile = tempDir.resolve(
                    "release-state-1.0.0.json");
            Files.writeString(stateFile, MINIMAL_STATE_JSON);
            StatusReporter reporter = new StatusReporter();

            StatusResult result = reporter.report(stateFile);

            assertThat(result.exitCode()).isZero();
            String output = result.output();
            assertThat(output).contains("1.0.0");
            assertThat(output).contains("BRANCH");
            // No PR info when prNumber is null
            assertThat(output).doesNotContain("#");
            // No waiting-for when null
            assertThat(output)
                    .doesNotContain("Waiting for:");
            // No next actions when null
            assertThat(output)
                    .doesNotContain("Suggested next actions");
        }
    }

    @Nested
    @DisplayName("when valid state file exists")
    class ValidStateFile {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName(
                "report_validState_outputIncludesVersionPhase"
                        + "PrUrlAndNextActions")
        void report_validState_outputIncludesFields()
                throws IOException {
            Path stateFile = tempDir.resolve(
                    "release-state-3.2.0.json");
            Files.writeString(stateFile, VALID_STATE_JSON);
            StatusReporter reporter = new StatusReporter();

            StatusResult result = reporter.report(stateFile);

            assertThat(result.exitCode()).isZero();
            String output = result.output();
            assertThat(output).contains("3.2.0");
            assertThat(output).contains("APPROVAL_PENDING");
            assertThat(output).contains(
                    "https://github.com/owner/repo/pull/297");
            assertThat(output).contains("PR merged");
            assertThat(output).contains("PR_MERGE");
        }
    }
}
