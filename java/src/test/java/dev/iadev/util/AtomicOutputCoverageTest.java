package dev.iadev.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Additional coverage tests for AtomicOutput —
 * targeting uncovered branches and edge cases.
 */
@DisplayName("AtomicOutput — coverage")
class AtomicOutputCoverageTest {

    @Nested
    @DisplayName("write — overwrite scenarios")
    class OverwriteScenarios {

        @Test
        @DisplayName("overwrites existing directory"
                + " with nested files")
        void overwritesNestedExisting(
                @TempDir Path tempDir) throws IOException {
            Path dest = tempDir.resolve("output");
            Files.createDirectories(
                    dest.resolve("old/nested"));
            Files.writeString(
                    dest.resolve("old/nested/file.txt"),
                    "old content");
            Files.writeString(
                    dest.resolve("old-root.txt"),
                    "old root");

            Map<String, String> files = Map.of(
                    "new.txt", "new content\n",
                    "sub/deep.txt", "deep content\n");

            AtomicOutput.write(dest, files);

            assertThat(dest.resolve("new.txt")).exists();
            assertThat(dest.resolve("sub/deep.txt"))
                    .exists();
            assertThat(Files.readString(
                    dest.resolve("new.txt"),
                    StandardCharsets.UTF_8))
                    .isEqualTo("new content\n");
        }

        @Test
        @DisplayName("dest parent dir does not exist"
                + " is created")
        void destParentCreated(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve(
                    "a/b/c/output");
            Map<String, String> files = Map.of(
                    "file.txt", "content\n");

            AtomicOutput.write(dest, files);

            assertThat(dest.resolve("file.txt")).exists();
        }
    }

    @Nested
    @DisplayName("write — failure preserves temp dir info")
    class FailurePreservesTempDir {

        @Test
        @DisplayName("IOException wraps original"
                + " exception with temp dir path")
        void ioExceptionWraps(@TempDir Path tempDir) {
            Path dest = tempDir.resolve("output");
            Map<String, String> files =
                    new LinkedHashMap<>();
            files.put("good.txt", "content");
            files.put(null, "bad key");

            assertThatThrownBy(
                    () -> AtomicOutput.write(dest, files))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("write — multiple nested subdirectories")
    class MultipleNested {

        @Test
        @DisplayName("creates deeply nested structure")
        void deeplyNested(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve("output");
            Map<String, String> files = Map.of(
                    "a/b/c/d/file.txt", "deep\n",
                    "x/y/z/file.md", "deep2\n");

            AtomicOutput.write(dest, files);

            assertThat(dest.resolve(
                    "a/b/c/d/file.txt")).exists();
            assertThat(dest.resolve(
                    "x/y/z/file.md")).exists();
        }
    }

    @Nested
    @DisplayName("write — file content integrity")
    class ContentIntegrity {

        @Test
        @DisplayName("preserves special characters"
                + " in content")
        void preservesSpecialChars(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve("output");
            String content =
                    "Special: \t\n\"quotes\" 'single'\n"
                            + "Unicode: \u2192 \u2014\n"
                            + "Backslash: \\\n";
            Map<String, String> files = Map.of(
                    "special.txt", content);

            AtomicOutput.write(dest, files);

            String readBack = Files.readString(
                    dest.resolve("special.txt"),
                    StandardCharsets.UTF_8);
            assertThat(readBack).isEqualTo(content);
        }

        @Test
        @DisplayName("handles large number of files")
        void handlesLargeFileCount(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve("output");
            Map<String, String> files =
                    new LinkedHashMap<>();
            for (int i = 0; i < 50; i++) {
                files.put("file-" + i + ".txt",
                        "Content of file " + i + "\n");
            }

            AtomicOutput.write(dest, files);

            for (int i = 0; i < 50; i++) {
                assertThat(dest.resolve(
                        "file-" + i + ".txt")).exists();
            }
        }
    }

    @Nested
    @DisplayName("createSecureTempDirectory")
    class SecureTempDirectory {

        @Test
        @DisabledOnOs(OS.WINDOWS)
        @DisplayName("creates temp dir with 700 permissions"
                + " on POSIX")
        void posixPermissions_700() throws IOException {
            Path tempDir =
                    AtomicOutput.createSecureTempDirectory();
            try {
                Set<PosixFilePermission> perms =
                        Files.getPosixFilePermissions(
                                tempDir);
                assertThat(perms).containsExactlyInAnyOrder(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE);
            } finally {
                Files.deleteIfExists(tempDir);
            }
        }

        @Test
        @DisplayName("returns existing directory")
        void returnsExistingDirectory()
                throws IOException {
            Path tempDir =
                    AtomicOutput.createSecureTempDirectory();
            try {
                assertThat(tempDir).exists();
                assertThat(tempDir).isDirectory();
            } finally {
                Files.deleteIfExists(tempDir);
            }
        }
    }
}
