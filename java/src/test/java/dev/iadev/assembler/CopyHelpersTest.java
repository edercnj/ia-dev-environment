package dev.iadev.assembler;

import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for CopyHelpers — template copy and rendering utilities.
 */
@DisplayName("CopyHelpers")
class CopyHelpersTest {

    @Nested
    @DisplayName("copyTemplateFile")
    class CopyTemplateFile {

        @Test
        @DisplayName("copies and renders template placeholders")
        void create_whenCalled_rendersPlaceholders(@TempDir Path tempDir)
                throws IOException {
            Path src = tempDir.resolve("source.md");
            Files.writeString(src,
                    "Project: {PROJECT_NAME}\n",
                    StandardCharsets.UTF_8);

            Path dest = tempDir.resolve("output/result.md");
            Map<String, Object> context =
                    Map.of("project_name", "my-app");

            TemplateEngine engine = new TemplateEngine();
            String result = CopyHelpers.copyTemplateFile(
                    src, dest, engine, context);

            assertThat(result).isEqualTo(dest.toString());
            assertThat(Files.readString(dest))
                    .contains("my-app");
        }

        @Test
        @DisplayName("creates parent directories")
        void create_whenCalled_createsParentDirs(@TempDir Path tempDir)
                throws IOException {
            Path src = tempDir.resolve("in.md");
            Files.writeString(src, "content");

            Path dest = tempDir.resolve("a/b/c/out.md");
            CopyHelpers.copyTemplateFile(
                    src, dest, new TemplateEngine(), Map.of());

            assertThat(dest).exists();
        }
    }

    @Nested
    @DisplayName("copyTemplateFileIfExists")
    class CopyTemplateFileIfExists {

        @Test
        @DisplayName("returns present Optional when"
                + " source exists")
        void sourceExists_whenSourceExists_returnsPresent(
                @TempDir Path tempDir)
                throws IOException {
            Path src = tempDir.resolve("exists.md");
            Files.writeString(src, "hello");
            Path dest = tempDir.resolve("out.md");

            var result =
                    CopyHelpers.copyTemplateFileIfExists(
                            src, dest,
                            new TemplateEngine(), Map.of());

            assertThat(result).isPresent();
            assertThat(dest).exists();
        }

        @Test
        @DisplayName("returns empty Optional when"
                + " source missing")
        void sourceMissing_whenSourceMissing_returnsEmpty(
                @TempDir Path tempDir) {
            Path src = tempDir.resolve("nonexistent.md");
            Path dest = tempDir.resolve("out.md");

            var result =
                    CopyHelpers.copyTemplateFileIfExists(
                            src, dest,
                            new TemplateEngine(), Map.of());

            assertThat(result).isEmpty();
            assertThat(dest).doesNotExist();
        }
    }

    @Nested
    @DisplayName("copyStaticFile")
    class CopyStaticFile {

        @Test
        @DisplayName("copies file without rendering")
        void create_withoutRendering_copies(@TempDir Path tempDir)
                throws IOException {
            Path src = tempDir.resolve("static.txt");
            String content = "{{NOT_REPLACED}}";
            Files.writeString(src, content);

            Path dest = tempDir.resolve("copy.txt");
            String result = CopyHelpers.copyStaticFile(
                    src, dest);

            assertThat(Files.readString(dest))
                    .isEqualTo(content);
            assertThat(result).isEqualTo(dest.toString());
        }

        @Test
        @DisplayName("creates parent directories")
        void create_whenCalled_createsParentDirs(@TempDir Path tempDir)
                throws IOException {
            Path src = tempDir.resolve("in.txt");
            Files.writeString(src, "data");

            Path dest = tempDir.resolve("deep/dir/out.txt");
            CopyHelpers.copyStaticFile(src, dest);

            assertThat(dest).exists();
        }
    }

    @Nested
    @DisplayName("copyDirectory")
    class CopyDirectory {

        @Test
        @DisplayName("copies directory recursively")
        void create_whenCalled_copiesRecursively(@TempDir Path tempDir)
                throws IOException {
            Path srcDir = tempDir.resolve("srcDir");
            Files.createDirectories(srcDir.resolve("sub"));
            Files.writeString(
                    srcDir.resolve("a.txt"), "file-a");
            Files.writeString(
                    srcDir.resolve("sub/b.txt"), "file-b");

            Path destDir = tempDir.resolve("destDir");
            String result = CopyHelpers.copyDirectory(
                    srcDir, destDir);

            assertThat(result).isEqualTo(destDir.toString());
            assertThat(destDir.resolve("a.txt")).exists();
            assertThat(destDir.resolve("sub/b.txt")).exists();
            assertThat(
                    Files.readString(destDir.resolve("a.txt")))
                    .isEqualTo("file-a");
        }
    }

    @Nested
    @DisplayName("ensureDirectory")
    class EnsureDirectory {

        @Test
        @DisplayName("creates directory and parents")
        void create_whenCalled_createsDirectoryAndParents(@TempDir Path tempDir) {
            Path dir = tempDir.resolve("x/y/z");

            CopyHelpers.ensureDirectory(dir);

            assertThat(dir).isDirectory();
        }

        @Test
        @DisplayName("is idempotent for existing directory")
        void existingDir_forExistingDirectory_noError(@TempDir Path tempDir)
                throws IOException {
            Path dir = tempDir.resolve("existing");
            Files.createDirectories(dir);

            CopyHelpers.ensureDirectory(dir);

            assertThat(dir).isDirectory();
        }
    }

    @Nested
    @DisplayName("replacePlaceholdersInDir")
    class ReplacePlaceholdersInDir {

        @Test
        @DisplayName("replaces placeholders in .md files")
        void create_whenCalled_replacesInMdFiles(@TempDir Path tempDir)
                throws IOException {
            Path mdFile = tempDir.resolve("test.md");
            Files.writeString(mdFile,
                    "Name: {PROJECT_NAME}\n");

            Path txtFile = tempDir.resolve("test.txt");
            Files.writeString(txtFile,
                    "Name: {PROJECT_NAME}\n");

            Map<String, Object> context =
                    Map.of("project_name", "replaced");

            CopyHelpers.replacePlaceholdersInDir(
                    tempDir, new TemplateEngine(), context);

            assertThat(Files.readString(mdFile))
                    .contains("replaced");
            // .txt files should NOT be replaced
            assertThat(Files.readString(txtFile))
                    .contains("{PROJECT_NAME}");
        }

        @Test
        @DisplayName("replaces in nested directories")
        void create_whenCalled_replacesInNestedDirs(@TempDir Path tempDir)
                throws IOException {
            Path sub = tempDir.resolve("sub");
            Files.createDirectories(sub);
            Path nested = sub.resolve("deep.md");
            Files.writeString(nested,
                    "{PROJECT_NAME} value");

            Map<String, Object> context =
                    Map.of("project_name", "deep-val");

            CopyHelpers.replacePlaceholdersInDir(
                    tempDir, new TemplateEngine(), context);

            assertThat(Files.readString(nested))
                    .contains("deep-val");
        }
    }

    @Nested
    @DisplayName("writeFile")
    class WriteFile {

        @Test
        @DisplayName("creates parent dirs and writes UTF-8")
        void writeFile_parentDirsMissing_createsAndWrites(
                @TempDir Path tempDir) throws IOException {
            Path dest = tempDir.resolve("a/b/c/out.txt");

            CopyHelpers.writeFile(dest, "hello UTF-8");

            assertThat(dest).exists();
            assertThat(Files.readString(
                    dest, StandardCharsets.UTF_8))
                    .isEqualTo("hello UTF-8");
        }

        @Test
        @DisplayName("overwrites existing file content")
        void writeFile_existingFile_overwritesContent(
                @TempDir Path tempDir) throws IOException {
            Path dest = tempDir.resolve("file.txt");
            Files.writeString(dest, "old");

            CopyHelpers.writeFile(dest, "new");

            assertThat(Files.readString(
                    dest, StandardCharsets.UTF_8))
                    .isEqualTo("new");
        }

        @Test
        @DisplayName("writes empty content")
        void writeFile_emptyContent_writesEmptyFile(
                @TempDir Path tempDir) throws IOException {
            Path dest = tempDir.resolve("empty.txt");

            CopyHelpers.writeFile(dest, "");

            assertThat(dest).exists();
            assertThat(Files.readString(dest)).isEmpty();
        }
    }

    @Nested
    @DisplayName("readFile")
    class ReadFile {

        @Test
        @DisplayName("reads entire file content as UTF-8")
        void readFile_existingFile_returnsContent(
                @TempDir Path tempDir) throws IOException {
            Path src = tempDir.resolve("input.txt");
            Files.writeString(
                    src, "hello world",
                    StandardCharsets.UTF_8);

            String result = CopyHelpers.readFile(src);

            assertThat(result).isEqualTo("hello world");
        }

        @Test
        @DisplayName("throws UncheckedIOException for missing file")
        void readFile_missingFile_throwsUncheckedIO(
                @TempDir Path tempDir) {
            Path missing = tempDir.resolve("no-such-file.txt");

            assertThatThrownBy(
                    () -> CopyHelpers.readFile(missing))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasCauseInstanceOf(
                            NoSuchFileException.class);
        }

        @Test
        @DisplayName("reads empty file as empty string")
        void readFile_emptyFile_returnsEmptyString(
                @TempDir Path tempDir) throws IOException {
            Path src = tempDir.resolve("empty.txt");
            Files.writeString(src, "");

            String result = CopyHelpers.readFile(src);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("listMdFilesSorted")
    class ListMdFilesSorted {

        @Test
        @DisplayName("returns empty list for directory"
                + " without .md files")
        void listMdFilesSorted_noMdFiles_returnsEmptyList(
                @TempDir Path tempDir) throws IOException {
            Files.writeString(
                    tempDir.resolve("file.txt"), "text");
            Files.writeString(
                    tempDir.resolve("file.yaml"), "yaml");

            List<Path> result =
                    CopyHelpers.listMdFilesSorted(tempDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns files sorted by filename")
        void listMdFilesSorted_multipleFiles_sortedByName(
                @TempDir Path tempDir) throws IOException {
            Files.writeString(
                    tempDir.resolve("c-rules.md"), "c");
            Files.writeString(
                    tempDir.resolve("a-rules.md"), "a");
            Files.writeString(
                    tempDir.resolve("b-rules.md"), "b");

            List<Path> result =
                    CopyHelpers.listMdFilesSorted(tempDir);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getFileName()
                    .toString())
                    .isEqualTo("a-rules.md");
            assertThat(result.get(1).getFileName()
                    .toString())
                    .isEqualTo("b-rules.md");
            assertThat(result.get(2).getFileName()
                    .toString())
                    .isEqualTo("c-rules.md");
        }

        @Test
        @DisplayName("filters out non-.md files")
        void listMdFilesSorted_mixedFiles_onlyMd(
                @TempDir Path tempDir) throws IOException {
            Files.writeString(
                    tempDir.resolve("keep.md"), "md");
            Files.writeString(
                    tempDir.resolve("skip.txt"), "txt");
            Files.writeString(
                    tempDir.resolve("skip.yaml"), "yaml");

            List<Path> result =
                    CopyHelpers.listMdFilesSorted(tempDir);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFileName()
                    .toString())
                    .isEqualTo("keep.md");
        }

        @Test
        @DisplayName("excludes subdirectories ending"
                + " in .md")
        void listMdFilesSorted_dirEndingMd_excluded(
                @TempDir Path tempDir) throws IOException {
            Files.createDirectories(
                    tempDir.resolve("subdir.md"));
            Files.writeString(
                    tempDir.resolve("real.md"), "data");

            List<Path> result =
                    CopyHelpers.listMdFilesSorted(tempDir);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFileName()
                    .toString())
                    .isEqualTo("real.md");
        }

        @Test
        @DisplayName("throws UncheckedIOException for"
                + " nonexistent directory")
        void listMdFilesSorted_noDir_throwsUncheckedIO(
                @TempDir Path tempDir) {
            Path noSuch = tempDir.resolve("nonexistent");

            assertThatThrownBy(
                    () -> CopyHelpers
                            .listMdFilesSorted(noSuch))
                    .isInstanceOf(UncheckedIOException.class);
        }
    }

    @Nested
    @DisplayName("deleteQuietly")
    class DeleteQuietly {

        @Test
        @DisplayName("returns false for nonexistent path")
        void deleteQuietly_nonexistent_returnsFalse(
                @TempDir Path tempDir) {
            Path noSuch = tempDir.resolve("nonexistent");

            boolean result =
                    CopyHelpers.deleteQuietly(noSuch);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("deletes existing file and"
                + " returns true")
        void deleteQuietly_existingFile_deletesAndTrue(
                @TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("to-delete.txt");
            Files.writeString(file, "content");

            boolean result =
                    CopyHelpers.deleteQuietly(file);

            assertThat(result).isTrue();
            assertThat(file).doesNotExist();
        }

        @Test
        @DisplayName("does not throw for any path")
        void deleteQuietly_anyPath_noException(
                @TempDir Path tempDir) {
            Path noSuch = tempDir.resolve("no-such-file");

            // Should never throw
            boolean result =
                    CopyHelpers.deleteQuietly(noSuch);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("deletes directory with files"
                + " recursively")
        void deleteQuietly_dirWithFiles_deletesAll(
                @TempDir Path tempDir) throws IOException {
            Path subDir = tempDir.resolve("sub");
            Files.createDirectories(subDir);
            Files.writeString(
                    subDir.resolve("a.txt"), "content");
            Files.writeString(
                    subDir.resolve("b.txt"), "content");

            boolean result =
                    CopyHelpers.deleteQuietly(subDir);

            assertThat(result).isTrue();
            assertThat(subDir).doesNotExist();
        }

        @Test
        @DisplayName("deletes nested directory tree")
        void deleteQuietly_nestedTree_deletesAll(
                @TempDir Path tempDir) throws IOException {
            Path root = tempDir.resolve("root");
            Path nested = root.resolve("a/b/c");
            Files.createDirectories(nested);
            Files.writeString(
                    nested.resolve("file.txt"), "data");

            boolean result =
                    CopyHelpers.deleteQuietly(root);

            assertThat(result).isTrue();
            assertThat(root).doesNotExist();
        }
    }

    @Nested
    @DisplayName("hasAllMandatorySections")
    class HasAllMandatorySections {

        @Test
        @DisplayName("returns true when all sections"
                + " present")
        void hasAllMandatorySections_allPresent_true() {
            String content = "# Doc\n\n"
                    + "## Status\nAccepted\n\n"
                    + "## Context\nSome context\n\n"
                    + "## Decision\nWe decided\n";
            List<String> sections =
                    List.of("Status", "Context",
                            "Decision");

            boolean result =
                    CopyHelpers.hasAllMandatorySections(
                            content, sections);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when section missing")
        void hasAllMandatorySections_missing_false() {
            String content = "# Doc\n\n"
                    + "## Status\nAccepted\n\n"
                    + "## Context\nSome context\n";
            List<String> sections =
                    List.of("Status", "Context",
                            "Decision");

            boolean result =
                    CopyHelpers.hasAllMandatorySections(
                            content, sections);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns false for empty content")
        void hasAllMandatorySections_emptyContent_false() {
            List<String> sections =
                    List.of("Status", "Context");

            boolean result =
                    CopyHelpers.hasAllMandatorySections(
                            "", sections);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns true for empty sections"
                + " list")
        void hasAllMandatorySections_emptySections_true() {
            boolean result =
                    CopyHelpers.hasAllMandatorySections(
                            "any content", List.of());

            assertThat(result).isTrue();
        }
    }
}
