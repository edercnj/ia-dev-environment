package dev.iadev.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OverwriteDetector")
class OverwriteDetectorTest {

    @Nested
    @DisplayName("checkExistingArtifacts")
    class CheckExistingArtifacts {

        @Test
        @DisplayName("returns empty list for empty directory")
        void emptyDirectory_whenCalled_returnsEmptyList(@TempDir Path tempDir) {
            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("returns empty list for non-existing directory")
        void nonExistingDir_forNonExistingdirectory_returnsemptylist(@TempDir Path tempDir) {
            Path nonExisting = tempDir.resolve("does-not-exist");

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(nonExisting);

            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("detects .claude/ directory")
        void detect_whenCalled_detectsClaude(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve(".claude"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly(".claude/");
        }

        @Test
        @DisplayName("detects .agents/ directory")
        void detect_whenCalled_detectsAgents(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve(".agents"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly(".agents/");
        }

        @Test
        @DisplayName("detects steering/ directory")
        void detect_whenCalled_detectsSteering(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve("steering"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly("steering/");
        }

        @Test
        @DisplayName("detects specs/ directory")
        void detect_whenCalled_detectsSpecs(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve("specs"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly("specs/");
        }

        @Test
        @DisplayName("detects results/ directory")
        void detect_whenCalled_detectsResults(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve("results"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly("results/");
        }

        @Test
        @DisplayName("detects contracts/ directory")
        void detect_whenCalled_detectsContracts(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve("contracts"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly("contracts/");
        }

        @Test
        @DisplayName("detects adr/ directory")
        void detect_whenCalled_detectsAdr(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve("adr"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly("adr/");
        }

        @Test
        @DisplayName("detects multiple conflicting directories")
        void multipleConflicts_whenCalled_detected(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(tempDir.resolve(".claude"));
            Files.createDirectories(tempDir.resolve(".agents"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts)
                    .containsExactlyInAnyOrder(".claude/", ".agents/");
        }

        @Test
        @DisplayName("detects all eight artifact directories")
        void allEight_whenCalled_detected(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve(".claude"));
            Files.createDirectories(tempDir.resolve(".agents"));
            Files.createDirectories(tempDir.resolve("steering"));
            Files.createDirectories(tempDir.resolve("specs"));
            Files.createDirectories(tempDir.resolve("plans"));
            Files.createDirectories(tempDir.resolve("results"));
            Files.createDirectories(tempDir.resolve("contracts"));
            Files.createDirectories(tempDir.resolve("adr"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).hasSize(8);
            assertThat(conflicts).containsExactlyInAnyOrder(
                    ".claude/",
                    ".agents/", "steering/", "specs/",
                    "plans/", "results/", "contracts/", "adr/");
        }

        @Test
        @DisplayName("ignores regular files, only detects directories")
        void regularFiles_whenCalled_ignored(@TempDir Path tempDir)
                throws IOException {
            Files.writeString(tempDir.resolve(".claude"), "not a dir");

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("ignores unrelated directories")
        void unrelatedDirs_whenCalled_ignored(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(tempDir.resolve("src"));
            Files.createDirectories(tempDir.resolve("lib"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).isEmpty();
        }
    }

    @Nested
    @DisplayName("formatConflictMessage")
    class FormatConflictMessage {

        @Test
        @DisplayName("returns empty string for empty conflict list")
        void emptyList_whenCalled_returnsEmptyString() {
            String message = OverwriteDetector.formatConflictMessage(
                    Collections.emptyList());

            assertThat(message).isEmpty();
        }

        @Test
        @DisplayName("formats single conflict with --force suggestion")
        void singleConflict_formatsWithForce_succeeds() {
            String message = OverwriteDetector.formatConflictMessage(
                    List.of(".claude/"));

            assertThat(message).contains(".claude/");
            assertThat(message).contains("--force");
            assertThat(message).contains("--output-dir");
        }

        @Test
        @DisplayName("formats multiple conflicts with all listed")
        void multipleConflicts_withAllListed_allListed() {
            String message = OverwriteDetector.formatConflictMessage(
                    List.of(".claude/", ".codex/"));

            assertThat(message).contains(".claude/");
            assertThat(message).contains(".codex/");
            assertThat(message).contains("--force");
        }

        @Test
        @DisplayName("each conflict appears on its own line")
        void conflicts_whenCalled_eachOnOwnLine() {
            String message = OverwriteDetector.formatConflictMessage(
                    List.of(".claude/", ".codex/", "steering/"));

            assertThat(message).contains("  - .claude/ (exists)");
            assertThat(message).contains("  - .codex/ (exists)");
            assertThat(message).contains("  - steering/ (exists)");
        }

        @Test
        @DisplayName("message uses LF line endings only")
        void detect_whenCalled_lfLineEndings() {
            String message = OverwriteDetector.formatConflictMessage(
                    List.of(".claude/"));

            assertThat(message).doesNotContain("\r\n");
            assertThat(message).contains("\n");
        }
    }
}
