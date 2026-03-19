package dev.iadev.util;

import dev.iadev.exception.CliException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PathUtils")
class PathUtilsTest {

    @Nested
    @DisplayName("normalizeDirectory")
    class NormalizeDirectory {

        @Test
        @DisplayName("resolves relative path to absolute")
        void relativePath_resolvesToAbsolute() {
            Path result = PathUtils.normalizeDirectory("some/relative/path");

            assertThat(result).isAbsolute();
            assertThat(result.toString()).endsWith("some/relative/path");
        }

        @Test
        @DisplayName("normalizes dots in path")
        void dotsInPath_normalized() {
            Path result = PathUtils.normalizeDirectory(
                    "/tmp/foo/../bar/./baz");

            assertThat(result.toString()).doesNotContain("..");
            assertThat(result.toString()).contains("bar/baz");
        }

        @Test
        @DisplayName("returns absolute path unchanged")
        void absolutePath_returnedNormalized() {
            Path result = PathUtils.normalizeDirectory("/tmp/output");

            assertThat(result).isAbsolute();
            assertThat(result.toString()).isEqualTo("/tmp/output");
        }

        @Test
        @DisplayName("handles current directory marker")
        void currentDirectory_resolved() {
            Path result = PathUtils.normalizeDirectory(".");

            assertThat(result).isAbsolute();
            assertThat(result.toString()).doesNotEndWith("/.");
        }

        @Test
        @DisplayName("handles empty string as current directory")
        void emptyString_resolvesToCurrent() {
            Path result = PathUtils.normalizeDirectory("");

            assertThat(result).isAbsolute();
        }
    }

    @Nested
    @DisplayName("rejectDangerousPath")
    class RejectDangerousPath {

        @Test
        @DisplayName("rejects home directory")
        void homeDirectory_rejected() {
            Path homePath = Path.of(System.getProperty("user.home"));

            assertThatThrownBy(
                    () -> PathUtils.rejectDangerousPath(homePath))
                    .isInstanceOf(CliException.class)
                    .satisfies(ex -> assertThat(
                            ((CliException) ex).getErrorCode())
                            .isEqualTo(1))
                    .hasMessageContaining("dangerous")
                    .hasMessageContaining(homePath.toString());
        }

        @Test
        @DisplayName("rejects root path")
        void rootPath_rejected() {
            Path rootPath = Path.of("/");

            assertThatThrownBy(
                    () -> PathUtils.rejectDangerousPath(rootPath))
                    .isInstanceOf(CliException.class)
                    .satisfies(ex -> assertThat(
                            ((CliException) ex).getErrorCode())
                            .isEqualTo(1))
                    .hasMessageContaining("dangerous");
        }

        @ParameterizedTest
        @ValueSource(strings = {"/usr", "/etc", "/var", "/bin", "/sbin"})
        @DisplayName("rejects system directories")
        void systemDirectories_rejected(String path) {
            Path systemPath = Path.of(path);

            assertThatThrownBy(
                    () -> PathUtils.rejectDangerousPath(systemPath))
                    .isInstanceOf(CliException.class)
                    .satisfies(ex -> assertThat(
                            ((CliException) ex).getErrorCode())
                            .isEqualTo(1))
                    .hasMessageContaining("dangerous")
                    .hasMessageContaining(path);
        }

        @Test
        @DisplayName("accepts valid project directory")
        void validProjectDir_accepted(@TempDir Path tempDir) {
            assertThatCode(
                    () -> PathUtils.rejectDangerousPath(tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts subdirectory of home")
        void subOfHome_accepted() {
            Path subPath = Path.of(
                    System.getProperty("user.home"), "projects", "my-app");

            assertThatCode(
                    () -> PathUtils.rejectDangerousPath(subPath))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts subdirectory of /var")
        void subOfVar_accepted() {
            Path subPath = Path.of("/var/data/my-project");

            assertThatCode(
                    () -> PathUtils.rejectDangerousPath(subPath))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateDestPath")
    class ValidateDestPath {

        @Test
        @DisplayName("accepts valid writable directory")
        void validWritableDir_accepted(@TempDir Path tempDir) {
            Path dest = tempDir.resolve("output");

            assertThatCode(
                    () -> PathUtils.validateDestPath(dest))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rejects dangerous path via validation")
        void dangerousPath_rejected() {
            Path rootPath = Path.of("/");

            assertThatThrownBy(
                    () -> PathUtils.validateDestPath(rootPath))
                    .isInstanceOf(CliException.class)
                    .satisfies(ex -> assertThat(
                            ((CliException) ex).getErrorCode())
                            .isEqualTo(1));
        }

        @Test
        @DisplayName("normalizes before rejecting")
        void normalizesBeforeRejecting() {
            Path sneakyPath = Path.of("/usr/local/../");

            assertThatThrownBy(
                    () -> PathUtils.validateDestPath(sneakyPath))
                    .isInstanceOf(CliException.class);
        }

        @Test
        @DisplayName("accepts non-existing directory under valid parent")
        void nonExistingDir_underValidParent_accepted(
                @TempDir Path tempDir) {
            Path dest = tempDir.resolve("new-output-dir");

            assertThatCode(
                    () -> PathUtils.validateDestPath(dest))
                    .doesNotThrowAnyException();
        }
    }
}
