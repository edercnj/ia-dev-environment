package dev.iadev.infrastructure.adapter.output.filesystem;

import dev.iadev.domain.port.output.FileSystemWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FileSystemWriterAdapter}.
 *
 * <p>Uses JUnit 5 {@link TempDir} for all filesystem
 * operations to ensure isolation and automatic cleanup.</p>
 */
class FileSystemWriterAdapterTest {

    @TempDir
    Path tempDir;

    private FileSystemWriterAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FileSystemWriterAdapter();
    }

    @Nested
    @DisplayName("Interface implementation")
    class InterfaceImplementation {

        @Test
        @DisplayName("adapter_implementsFileSystemWriter")
        void adapter_implementsFileSystemWriter() {
            assertThat(adapter)
                    .isInstanceOf(FileSystemWriter.class);
        }
    }

    @Nested
    @DisplayName("writeFile — degenerate cases")
    class WriteFileDegenerateCases {

        @Test
        @DisplayName("writeFile_nullPath"
                + "_throwsIllegalArgumentException")
        void writeFile_nullPath_throwsIllegalArgument() {
            assertThatThrownBy(
                    () -> adapter.writeFile(null, "content"))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("path");
        }

        @Test
        @DisplayName("writeFile_nullContent"
                + "_throwsIllegalArgumentException")
        void writeFile_nullContent_throwsIllegalArgument() {
            Path target = tempDir.resolve("file.txt");
            assertThatThrownBy(
                    () -> adapter.writeFile(target, null))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("content");
        }
    }

    @Nested
    @DisplayName("writeFile — happy path")
    class WriteFileHappyPath {

        @Test
        @DisplayName("writeFile_simpleFile"
                + "_createsFileWithContent")
        void writeFile_simpleFile_createsFileWithContent()
                throws IOException {
            Path target = tempDir.resolve("hello.txt");

            adapter.writeFile(target, "hello world");

            assertThat(target).exists();
            assertThat(Files.readString(
                    target, StandardCharsets.UTF_8))
                    .isEqualTo("hello world");
        }

        @Test
        @DisplayName("writeFile_withParentDirs"
                + "_createsParentDirectories")
        void writeFile_withParentDirs_createsParentDirs()
                throws IOException {
            Path target = tempDir.resolve(
                    "subdir/nested/file.txt");

            adapter.writeFile(target, "nested content");

            assertThat(target).exists();
            assertThat(target.getParent()).isDirectory();
            assertThat(Files.readString(
                    target, StandardCharsets.UTF_8))
                    .isEqualTo("nested content");
        }

        @Test
        @DisplayName("writeFile_emptyContent"
                + "_createsEmptyFile")
        void writeFile_emptyContent_createsEmptyFile()
                throws IOException {
            Path target = tempDir.resolve("empty.txt");

            adapter.writeFile(target, "");

            assertThat(target).exists();
            assertThat(Files.readString(
                    target, StandardCharsets.UTF_8))
                    .isEmpty();
        }

        @Test
        @DisplayName("writeFile_existingFile"
                + "_overwritesContent")
        void writeFile_existingFile_overwritesContent()
                throws IOException {
            Path target = tempDir.resolve("overwrite.txt");
            Files.writeString(target, "original");

            adapter.writeFile(target, "replaced");

            assertThat(Files.readString(
                    target, StandardCharsets.UTF_8))
                    .isEqualTo("replaced");
        }

        @Test
        @DisplayName("writeFile_utf8Content"
                + "_preservesEncoding")
        void writeFile_utf8Content_preservesEncoding()
                throws IOException {
            Path target = tempDir.resolve("utf8.txt");
            String utf8Content =
                    "Unicode: \u00e9\u00e0\u00fc\u00f1\u4e16\u754c";

            adapter.writeFile(target, utf8Content);

            assertThat(Files.readString(
                    target, StandardCharsets.UTF_8))
                    .isEqualTo(utf8Content);
        }
    }

    @Nested
    @DisplayName("writeFile — path traversal")
    class WriteFilePathTraversal {

        @Test
        @DisplayName("writeFile_pathWithDotDot"
                + "_throwsIllegalArgumentException")
        void writeFile_pathWithDotDot_throws() {
            Path traversal = tempDir.resolve(
                    "safe/../../../etc/passwd");

            assertThatThrownBy(
                    () -> adapter.writeFile(
                            traversal, "malicious"))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("path traversal");
        }

        @Test
        @DisplayName("writeFile_pathWithDotDot"
                + "_doesNotCreateFile")
        void writeFile_pathWithDotDot_doesNotCreateFile() {
            Path traversal = tempDir.resolve(
                    "safe/../../../etc/passwd");

            try {
                adapter.writeFile(traversal, "malicious");
            } catch (IllegalArgumentException ignored) {
                // expected
            }

            assertThat(traversal).doesNotExist();
        }
    }

    @Nested
    @DisplayName("createDirectory — degenerate cases")
    class CreateDirectoryDegenerateCases {

        @Test
        @DisplayName("createDirectory_nullPath"
                + "_throwsIllegalArgumentException")
        void createDirectory_nullPath_throws() {
            assertThatThrownBy(
                    () -> adapter.createDirectory(null))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("path");
        }
    }

    @Nested
    @DisplayName("createDirectory — happy path")
    class CreateDirectoryHappyPath {

        @Test
        @DisplayName("createDirectory_newDirectory"
                + "_createsDirectory")
        void createDirectory_newDir_createsDirectory() {
            Path dir = tempDir.resolve("newdir");

            adapter.createDirectory(dir);

            assertThat(dir).isDirectory();
        }

        @Test
        @DisplayName("createDirectory_nestedDirectories"
                + "_createsAllParents")
        void createDirectory_nestedDirs_createsAllParents() {
            Path dir = tempDir.resolve("a/b/c/d");

            adapter.createDirectory(dir);

            assertThat(dir).isDirectory();
            assertThat(tempDir.resolve("a/b/c"))
                    .isDirectory();
        }

        @Test
        @DisplayName("createDirectory_existingDirectory"
                + "_isIdempotent")
        void createDirectory_existingDir_isIdempotent()
                throws IOException {
            Path dir = tempDir.resolve("existing");
            Files.createDirectory(dir);

            adapter.createDirectory(dir);

            assertThat(dir).isDirectory();
        }
    }

    @Nested
    @DisplayName("exists — degenerate cases")
    class ExistsDegenerateCases {

        @Test
        @DisplayName("exists_nullPath"
                + "_throwsIllegalArgumentException")
        void exists_nullPath_throws() {
            assertThatThrownBy(
                    () -> adapter.exists(null))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("path");
        }
    }

    @Nested
    @DisplayName("exists — happy path")
    class ExistsHappyPath {

        @Test
        @DisplayName("exists_existingFile_returnsTrue")
        void exists_existingFile_returnsTrue()
                throws IOException {
            Path file = tempDir.resolve("exists.txt");
            Files.writeString(file, "content");

            assertThat(adapter.exists(file)).isTrue();
        }

        @Test
        @DisplayName("exists_existingDirectory"
                + "_returnsTrue")
        void exists_existingDirectory_returnsTrue() {
            assertThat(adapter.exists(tempDir)).isTrue();
        }

        @Test
        @DisplayName("exists_nonExistentPath"
                + "_returnsFalse")
        void exists_nonExistentPath_returnsFalse() {
            Path missing = tempDir.resolve("missing.txt");

            assertThat(adapter.exists(missing)).isFalse();
        }
    }

    @Nested
    @DisplayName("copyResource — degenerate cases")
    class CopyResourceDegenerateCases {

        @Test
        @DisplayName("copyResource_nullResourcePath"
                + "_throwsIllegalArgumentException")
        void copyResource_nullResourcePath_throws() {
            Path dest = tempDir.resolve("dest.txt");
            assertThatThrownBy(
                    () -> adapter.copyResource(null, dest))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("resourcePath");
        }

        @Test
        @DisplayName("copyResource_blankResourcePath"
                + "_throwsIllegalArgumentException")
        void copyResource_blankResourcePath_throws() {
            Path dest = tempDir.resolve("dest.txt");
            assertThatThrownBy(
                    () -> adapter.copyResource("  ", dest))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("resourcePath");
        }

        @Test
        @DisplayName("copyResource_nullDestination"
                + "_throwsIllegalArgumentException")
        void copyResource_nullDestination_throws() {
            assertThatThrownBy(
                    () -> adapter.copyResource(
                            "logback.xml", null))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("destination");
        }
    }

    @Nested
    @DisplayName("copyResource — happy path")
    class CopyResourceHappyPath {

        @Test
        @DisplayName("copyResource_existingResource"
                + "_copiesContent")
        void copyResource_existingResource_copiesContent()
                throws IOException {
            Path dest = tempDir.resolve("logback-copy.xml");

            adapter.copyResource("logback.xml", dest);

            assertThat(dest).exists();
            String content = Files.readString(
                    dest, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("logback");
        }

        @Test
        @DisplayName("copyResource_withParentDirs"
                + "_createsParentDirectories")
        void copyResource_withParentDirs_createsParents()
                throws IOException {
            Path dest = tempDir.resolve(
                    "nested/dir/logback-copy.xml");

            adapter.copyResource("logback.xml", dest);

            assertThat(dest).exists();
            assertThat(dest.getParent()).isDirectory();
        }
    }

    @Nested
    @DisplayName("copyResource — error paths")
    class CopyResourceErrorPaths {

        @Test
        @DisplayName("copyResource_missingResource"
                + "_throwsUncheckedIOException")
        void copyResource_missingResource_throws() {
            Path dest = tempDir.resolve("dest.txt");

            assertThatThrownBy(
                    () -> adapter.copyResource(
                            "nonexistent-resource.xyz",
                            dest))
                    .isInstanceOf(
                            UncheckedIOException.class)
                    .hasMessageContaining(
                            "nonexistent-resource.xyz");
        }
    }

    @Nested
    @DisplayName("writeFile — I/O error paths")
    class WriteFileIOErrors {

        @Test
        @DisplayName("writeFile_readOnlyParent"
                + "_throwsUncheckedIOException")
        void writeFile_readOnlyParent_throws()
                throws IOException {
            Path readOnlyDir = tempDir.resolve("readonly");
            Files.createDirectory(readOnlyDir);
            readOnlyDir.toFile().setWritable(false);

            Path target = readOnlyDir.resolve(
                    "subdir/file.txt");

            try {
                assertThatThrownBy(
                        () -> adapter.writeFile(
                                target, "content"))
                        .isInstanceOf(
                                UncheckedIOException.class)
                        .hasMessageContaining(
                                target.toString());
            } finally {
                readOnlyDir.toFile().setWritable(true);
            }
        }
    }

    @Nested
    @DisplayName("createDirectory — path traversal")
    class CreateDirectoryPathTraversal {

        @Test
        @DisplayName("createDirectory_pathWithDotDot"
                + "_throwsIllegalArgumentException")
        void createDirectory_pathWithDotDot_throws() {
            Path traversal = tempDir.resolve(
                    "safe/../../../tmp/evil");

            assertThatThrownBy(
                    () -> adapter.createDirectory(traversal))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("path traversal");
        }
    }

    @Nested
    @DisplayName("copyResource — path traversal")
    class CopyResourcePathTraversal {

        @Test
        @DisplayName("copyResource_destinationWithDotDot"
                + "_throwsIllegalArgumentException")
        void copyResource_destWithDotDot_throws() {
            Path traversal = tempDir.resolve(
                    "safe/../../../tmp/evil.xml");

            assertThatThrownBy(
                    () -> adapter.copyResource(
                            "logback.xml", traversal))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("path traversal");
        }
    }
}
