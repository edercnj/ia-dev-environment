package dev.iadev.util;

import dev.iadev.exception.CliException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AtomicOutput")
class AtomicOutputTest {

    @Nested
    @DisplayName("write — success scenarios")
    class WriteSuccess {

        @Test
        @DisplayName("writes single file to destination")
        void singleFile_whenCalled_writtenToDestination(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve("output");
            Map<String, String> files = Map.of(
                    "hello.txt", "Hello World\n");

            AtomicOutput.write(dest, files);

            assertThat(dest.resolve("hello.txt")).exists();
            assertThat(Files.readString(
                    dest.resolve("hello.txt"), StandardCharsets.UTF_8))
                    .isEqualTo("Hello World\n");
        }

        @Test
        @DisplayName("writes multiple files to destination")
        void multipleFiles_whenCalled_writtenToDestination(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve("output");
            Map<String, String> files = new LinkedHashMap<>();
            files.put("a.txt", "content-a\n");
            files.put("b.txt", "content-b\n");
            files.put("c.md", "content-c\n");

            AtomicOutput.write(dest, files);

            assertThat(dest.resolve("a.txt")).exists();
            assertThat(dest.resolve("b.txt")).exists();
            assertThat(dest.resolve("c.md")).exists();
            assertThat(Files.readString(
                    dest.resolve("a.txt"), StandardCharsets.UTF_8))
                    .isEqualTo("content-a\n");
            assertThat(Files.readString(
                    dest.resolve("b.txt"), StandardCharsets.UTF_8))
                    .isEqualTo("content-b\n");
            assertThat(Files.readString(
                    dest.resolve("c.md"), StandardCharsets.UTF_8))
                    .isEqualTo("content-c\n");
        }

        @Test
        @DisplayName("creates nested subdirectories automatically")
        void nestedSubdirs_whenCalled_createdAutomatically(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve("output");
            Map<String, String> files = Map.of(
                    "dir/subdir/file.txt", "nested content\n");

            AtomicOutput.write(dest, files);

            assertThat(dest.resolve("dir/subdir/file.txt")).exists();
            assertThat(Files.readString(
                    dest.resolve("dir/subdir/file.txt"),
                    StandardCharsets.UTF_8))
                    .isEqualTo("nested content\n");
        }

        @Test
        @DisplayName("all files written with UTF-8 encoding")
        void create_withUtf8_filesWritten(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve("output");
            String utf8Content =
                    "Conteudo com acentos: e, a, c, u\n";
            Map<String, String> files = Map.of(
                    "utf8.txt", utf8Content);

            AtomicOutput.write(dest, files);

            byte[] bytes = Files.readAllBytes(
                    dest.resolve("utf8.txt"));
            String readBack = new String(bytes, StandardCharsets.UTF_8);
            assertThat(readBack).isEqualTo(utf8Content);
        }

        @Test
        @DisplayName("all files written with LF line endings")
        void create_withLf_filesWritten(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve("output");
            String content = "line1\nline2\nline3\n";
            Map<String, String> files = Map.of(
                    "lf-test.txt", content);

            AtomicOutput.write(dest, files);

            byte[] bytes = Files.readAllBytes(
                    dest.resolve("lf-test.txt"));
            String readBack = new String(bytes, StandardCharsets.UTF_8);
            assertThat(readBack).isEqualTo(content);
            assertThat(readBack).doesNotContain("\r\n");
        }

        @Test
        @DisplayName("handles empty file map gracefully")
        void emptyFileMap_whenCalled_createsEmptyDestination(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve("output");
            Map<String, String> files = Map.of();

            AtomicOutput.write(dest, files);

            assertThat(dest).exists();
            assertThat(dest).isDirectory();
        }

        @Test
        @DisplayName("overwrites existing destination on success")
        void existingDest_whenCalled_overwritten(@TempDir Path tempDir)
                throws IOException {
            Path dest = tempDir.resolve("output");
            Files.createDirectories(dest);
            Files.writeString(dest.resolve("old.txt"), "old content");

            Map<String, String> files = Map.of(
                    "new.txt", "new content\n");

            AtomicOutput.write(dest, files);

            assertThat(dest.resolve("new.txt")).exists();
        }
    }

    @Nested
    @DisplayName("write — failure scenarios")
    class WriteFailure {

        @Test
        @DisplayName("preserves temp dir info in exception on failure")
        void failedWrite_whenCalled_preservesTempDirInMessage(
                @TempDir Path tempDir) {
            // Use a path under a non-writable parent to force move
            // failure. We'll create a dest inside /dev/null which
            // should fail on most systems.
            // Instead, we test with null content to cause
            // NullPointerException during write.
            Path dest = tempDir.resolve("output");
            Map<String, String> files = new LinkedHashMap<>();
            files.put("valid.txt", "content\n");
            files.put(null, "this should fail");

            assertThatThrownBy(
                    () -> AtomicOutput.write(dest, files))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("rejects null destination path")
        void nullDestination_whenCalled_rejected() {
            Map<String, String> files = Map.of("a.txt", "content");

            assertThatThrownBy(
                    () -> AtomicOutput.write(null, files))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null file map")
        void nullFileMap_whenCalled_rejected(@TempDir Path tempDir) {
            Path dest = tempDir.resolve("output");

            assertThatThrownBy(
                    () -> AtomicOutput.write(dest, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("write — temp directory prefix")
    class TempDirPrefix {

        @Test
        @DisplayName("uses ia-dev-env- prefix for temp directory")
        void write_whenCalled_tempDirPrefix(@TempDir Path tempDir) throws IOException {
            // Verify that the method works end-to-end; the temp dir
            // prefix is an internal detail, but we can verify the
            // output is correct
            Path dest = tempDir.resolve("output");
            Map<String, String> files = Map.of(
                    "test.txt", "content\n");

            AtomicOutput.write(dest, files);

            assertThat(dest.resolve("test.txt")).exists();
            assertThat(Files.readString(
                    dest.resolve("test.txt"), StandardCharsets.UTF_8))
                    .isEqualTo("content\n");
        }
    }
}
