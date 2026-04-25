package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for SkillsCopyHelper — non-SKILL.md traversal + dir listing utilities.
 *
 * <p>This class closes the coverage gap identified by the EPIC-0055 foundation
 * review (line 12.5% / branch 0% before; targets &gt;=95% / &gt;=90% after).</p>
 */
@DisplayName("SkillsCopyHelper")
class SkillsCopyHelperTest {

    @Nested
    @DisplayName("copyNonSkillItems")
    class CopyNonSkillItems {

        @Test
        @DisplayName("skips SKILL.md entries")
        void copyNonSkillItems_whenSkillMdPresent_skipsIt(
                @TempDir Path tempDir) throws IOException {
            Path src = tempDir.resolve("src");
            Path dest = tempDir.resolve("dest");
            Files.createDirectories(src);
            Files.createDirectories(dest);
            Files.writeString(src.resolve("SKILL.md"), "skill");
            Files.writeString(src.resolve("keep.txt"), "keep");

            SkillsCopyHelper.copyNonSkillItems(src, dest);

            assertThat(dest.resolve("SKILL.md")).doesNotExist();
            assertThat(dest.resolve("keep.txt"))
                    .exists()
                    .content()
                    .isEqualTo("keep");
        }

        @Test
        @DisplayName("copies new file when target is absent")
        void copyNonSkillItems_whenTargetMissing_copiesFile(
                @TempDir Path tempDir) throws IOException {
            Path src = tempDir.resolve("src");
            Path dest = tempDir.resolve("dest");
            Files.createDirectories(src);
            Files.createDirectories(dest);
            Files.writeString(src.resolve("new.txt"), "new");

            SkillsCopyHelper.copyNonSkillItems(src, dest);

            assertThat(dest.resolve("new.txt"))
                    .exists()
                    .content()
                    .isEqualTo("new");
        }

        @Test
        @DisplayName("does not overwrite existing file")
        void copyNonSkillItems_whenFileExists_keepsOriginal(
                @TempDir Path tempDir) throws IOException {
            Path src = tempDir.resolve("src");
            Path dest = tempDir.resolve("dest");
            Files.createDirectories(src);
            Files.createDirectories(dest);
            Files.writeString(src.resolve("x.txt"), "from-src");
            Files.writeString(dest.resolve("x.txt"), "existing");

            SkillsCopyHelper.copyNonSkillItems(src, dest);

            assertThat(dest.resolve("x.txt"))
                    .content()
                    .isEqualTo("existing");
        }

        @Test
        @DisplayName("copies new directory recursively when target absent")
        void copyNonSkillItems_whenDirMissing_copiesDirectory(
                @TempDir Path tempDir) throws IOException {
            Path src = tempDir.resolve("src");
            Path dest = tempDir.resolve("dest");
            Files.createDirectories(src.resolve("refs/sub"));
            Files.createDirectories(dest);
            Files.writeString(
                    src.resolve("refs/a.txt"), "a");
            Files.writeString(
                    src.resolve("refs/sub/b.txt"), "b");

            SkillsCopyHelper.copyNonSkillItems(src, dest);

            assertThat(dest.resolve("refs/a.txt"))
                    .content().isEqualTo("a");
            assertThat(dest.resolve("refs/sub/b.txt"))
                    .content().isEqualTo("b");
        }

        @Test
        @DisplayName("merges existing directory — skips present files, adds new ones recursively")
        void copyNonSkillItems_whenDirExists_mergesRecursively(
                @TempDir Path tempDir) throws IOException {
            Path src = tempDir.resolve("src");
            Path dest = tempDir.resolve("dest");
            Files.createDirectories(src.resolve("refs/deep"));
            Files.createDirectories(dest.resolve("refs/deep"));
            Files.writeString(
                    src.resolve("refs/shared.txt"), "src-shared");
            Files.writeString(
                    dest.resolve("refs/shared.txt"), "dest-shared");
            Files.writeString(
                    src.resolve("refs/new-top.txt"), "new-top");
            Files.writeString(
                    src.resolve("refs/deep/new-inner.txt"), "new-inner");
            Files.writeString(
                    dest.resolve("refs/deep/exists.txt"), "keep");

            SkillsCopyHelper.copyNonSkillItems(src, dest);

            assertThat(dest.resolve("refs/shared.txt"))
                    .content().isEqualTo("dest-shared");
            assertThat(dest.resolve("refs/new-top.txt"))
                    .content().isEqualTo("new-top");
            assertThat(dest.resolve("refs/deep/new-inner.txt"))
                    .content().isEqualTo("new-inner");
            assertThat(dest.resolve("refs/deep/exists.txt"))
                    .content().isEqualTo("keep");
        }

        @Test
        @DisplayName("no-op when dest already has file identical by name (no overwrite)")
        void copyNonSkillItems_whenDirTargetIsFile_skips(
                @TempDir Path tempDir) throws IOException {
            Path src = tempDir.resolve("src");
            Path dest = tempDir.resolve("dest");
            Files.createDirectories(src.resolve("conflict"));
            Files.createDirectories(dest);
            Files.writeString(
                    src.resolve("conflict/inner.txt"), "src");
            // dest has a FILE named 'conflict' (not a directory)
            Files.writeString(dest.resolve("conflict"), "pre-existing file");

            SkillsCopyHelper.copyNonSkillItems(src, dest);

            // existing file preserved; src dir skipped silently
            assertThat(dest.resolve("conflict"))
                    .isRegularFile()
                    .content().isEqualTo("pre-existing file");
        }

        @Test
        @DisplayName("handles empty source directory")
        void copyNonSkillItems_whenSrcEmpty_noOp(
                @TempDir Path tempDir) throws IOException {
            Path src = tempDir.resolve("src");
            Path dest = tempDir.resolve("dest");
            Files.createDirectories(src);
            Files.createDirectories(dest);

            SkillsCopyHelper.copyNonSkillItems(src, dest);

            try (var stream = Files.list(dest)) {
                assertThat(stream).isEmpty();
            }
        }

        @Test
        @DisplayName("throws UncheckedIOException when source does not exist")
        void copyNonSkillItems_whenSrcMissing_throwsUnchecked(
                @TempDir Path tempDir) {
            Path src = tempDir.resolve("nope");
            Path dest = tempDir.resolve("dest");

            assertThatThrownBy(() -> SkillsCopyHelper
                    .copyNonSkillItems(src, dest))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasMessageContaining("Failed to list directory");
        }
    }

    @Nested
    @DisplayName("listDirsSorted")
    class ListDirsSorted {

        @Test
        @DisplayName("returns only directories, alphabetically sorted")
        void listDirsSorted_whenMixed_filtersFilesAndSorts(
                @TempDir Path tempDir) throws IOException {
            Files.createDirectories(tempDir.resolve("gamma"));
            Files.createDirectories(tempDir.resolve("alpha"));
            Files.createDirectories(tempDir.resolve("beta"));
            Files.writeString(tempDir.resolve("a-file.txt"), "");

            List<Path> dirs = SkillsCopyHelper
                    .listDirsSorted(tempDir);

            assertThat(dirs)
                    .hasSize(3)
                    .extracting(p -> p.getFileName().toString())
                    .containsExactly("alpha", "beta", "gamma");
        }

        @Test
        @DisplayName("returns empty list for empty directory")
        void listDirsSorted_whenEmpty_returnsEmpty(
                @TempDir Path tempDir) {
            List<Path> dirs = SkillsCopyHelper
                    .listDirsSorted(tempDir);
            assertThat(dirs).isEmpty();
        }

        @Test
        @DisplayName("throws UncheckedIOException when path does not exist")
        void listDirsSorted_whenMissing_throws(
                @TempDir Path tempDir) {
            Path missing = tempDir.resolve("missing-dir");
            assertThatThrownBy(() -> SkillsCopyHelper
                    .listDirsSorted(missing))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasMessageContaining("Failed to list directory");
        }
    }

    @Nested
    @DisplayName("listEntriesSorted")
    class ListEntriesSorted {

        @Test
        @DisplayName("returns all entries (files + dirs), alphabetically sorted")
        void listEntriesSorted_whenMixed_sortsAll(
                @TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("z.txt"), "");
            Files.createDirectories(tempDir.resolve("a-dir"));
            Files.writeString(tempDir.resolve("m.txt"), "");

            List<Path> entries = SkillsCopyHelper
                    .listEntriesSorted(tempDir);

            assertThat(entries)
                    .hasSize(3)
                    .extracting(p -> p.getFileName().toString())
                    .containsExactly("a-dir", "m.txt", "z.txt");
        }

        @Test
        @DisplayName("returns empty list for empty directory")
        void listEntriesSorted_whenEmpty_returnsEmpty(
                @TempDir Path tempDir) {
            List<Path> entries = SkillsCopyHelper
                    .listEntriesSorted(tempDir);
            assertThat(entries).isEmpty();
        }

        @Test
        @DisplayName("throws UncheckedIOException when path does not exist")
        void listEntriesSorted_whenMissing_throws(
                @TempDir Path tempDir) {
            Path missing = tempDir.resolve("nope");
            assertThatThrownBy(() -> SkillsCopyHelper
                    .listEntriesSorted(missing))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasMessageContaining("Failed to list directory");
        }
    }

    @Nested
    @DisplayName("character-encoding preservation")
    class EncodingPreservation {

        @Test
        @DisplayName("UTF-8 content survives a copy round-trip")
        void copyNonSkillItems_whenUtf8Content_preservesBytes(
                @TempDir Path tempDir) throws IOException {
            Path src = tempDir.resolve("src");
            Path dest = tempDir.resolve("dest");
            Files.createDirectories(src);
            Files.createDirectories(dest);
            String content =
                    "Task subject › Phase N › Arch plan — ação";
            Files.writeString(
                    src.resolve("utf8.md"),
                    content,
                    StandardCharsets.UTF_8);

            SkillsCopyHelper.copyNonSkillItems(src, dest);

            assertThat(
                    Files.readString(
                            dest.resolve("utf8.md"),
                            StandardCharsets.UTF_8))
                    .isEqualTo(content);
        }
    }
}
