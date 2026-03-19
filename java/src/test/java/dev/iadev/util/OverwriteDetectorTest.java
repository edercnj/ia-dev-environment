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
        void emptyDirectory_returnsEmptyList(@TempDir Path tempDir) {
            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("returns empty list for non-existing directory")
        void nonExistingDir_returnsEmptyList(@TempDir Path tempDir) {
            Path nonExisting = tempDir.resolve("does-not-exist");

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(nonExisting);

            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("detects .claude/ directory")
        void detectsClaude(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve(".claude"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly(".claude/");
        }

        @Test
        @DisplayName("detects .github/ directory")
        void detectsGithub(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve(".github"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly(".github/");
        }

        @Test
        @DisplayName("detects .codex/ directory")
        void detectsCodex(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve(".codex"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly(".codex/");
        }

        @Test
        @DisplayName("detects .agents/ directory")
        void detectsAgents(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve(".agents"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly(".agents/");
        }

        @Test
        @DisplayName("detects docs/ directory")
        void detectsDocs(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve("docs"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).containsExactly("docs/");
        }

        @Test
        @DisplayName("detects multiple conflicting directories")
        void multipleConflicts_detected(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(tempDir.resolve(".claude"));
            Files.createDirectories(tempDir.resolve(".github"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts)
                    .containsExactlyInAnyOrder(".claude/", ".github/");
        }

        @Test
        @DisplayName("detects all five artifact directories")
        void allFive_detected(@TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve(".claude"));
            Files.createDirectories(tempDir.resolve(".github"));
            Files.createDirectories(tempDir.resolve(".codex"));
            Files.createDirectories(tempDir.resolve(".agents"));
            Files.createDirectories(tempDir.resolve("docs"));

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).hasSize(5);
            assertThat(conflicts).containsExactlyInAnyOrder(
                    ".claude/", ".github/", ".codex/",
                    ".agents/", "docs/");
        }

        @Test
        @DisplayName("ignores regular files, only detects directories")
        void regularFiles_ignored(@TempDir Path tempDir)
                throws IOException {
            Files.writeString(tempDir.resolve(".claude"), "not a dir");

            List<String> conflicts =
                    OverwriteDetector.checkExistingArtifacts(tempDir);

            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("ignores unrelated directories")
        void unrelatedDirs_ignored(@TempDir Path tempDir)
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
        void emptyList_returnsEmptyString() {
            String message = OverwriteDetector.formatConflictMessage(
                    Collections.emptyList());

            assertThat(message).isEmpty();
        }

        @Test
        @DisplayName("formats single conflict with --force suggestion")
        void singleConflict_formatsWithForce() {
            String message = OverwriteDetector.formatConflictMessage(
                    List.of(".claude/"));

            assertThat(message).contains(".claude/");
            assertThat(message).contains("--force");
            assertThat(message).contains("--output-dir");
        }

        @Test
        @DisplayName("formats multiple conflicts with all listed")
        void multipleConflicts_allListed() {
            String message = OverwriteDetector.formatConflictMessage(
                    List.of(".claude/", ".github/"));

            assertThat(message).contains(".claude/");
            assertThat(message).contains(".github/");
            assertThat(message).contains("--force");
        }

        @Test
        @DisplayName("each conflict appears on its own line")
        void conflicts_eachOnOwnLine() {
            String message = OverwriteDetector.formatConflictMessage(
                    List.of(".claude/", ".github/", "docs/"));

            assertThat(message).contains("  - .claude/ (exists)");
            assertThat(message).contains("  - .github/ (exists)");
            assertThat(message).contains("  - docs/ (exists)");
        }

        @Test
        @DisplayName("message uses LF line endings only")
        void lfLineEndings() {
            String message = OverwriteDetector.formatConflictMessage(
                    List.of(".claude/"));

            assertThat(message).doesNotContain("\r\n");
            assertThat(message).contains("\n");
        }
    }
}
