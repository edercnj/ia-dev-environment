package dev.iadev.application.assembler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownFileScannerTest {

    @Test
    void listMarkdownFilesSorted_withMixedContent_returnsOnlyMdFilesInOrder(
            @TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("b.md"), "b");
        Files.writeString(dir.resolve("a.md"), "a");
        Files.writeString(dir.resolve("note.txt"), "x");
        Files.writeString(dir.resolve("README"), "r");

        List<Path> result = MarkdownFileScanner
                .listMarkdownFilesSorted(dir);

        assertThat(result)
                .extracting(p -> p.getFileName().toString())
                .containsExactly("a.md", "b.md");
    }

    @Test
    void listMarkdownFilesSorted_onEmptyDirectory_returnsEmptyList(
            @TempDir Path dir) {
        List<Path> result = MarkdownFileScanner
                .listMarkdownFilesSorted(dir);

        assertThat(result).isEmpty();
    }

    @Test
    void listMarkdownFilesSorted_onMissingDirectory_returnsEmptyList(
            @TempDir Path parent) {
        Path missing = parent.resolve("does-not-exist");

        List<Path> result = MarkdownFileScanner
                .listMarkdownFilesSorted(missing);

        assertThat(result).isEmpty();
    }

    @Test
    void listMarkdownFiles_excludesDirectoriesEndingInMd(
            @TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("keep.md"), "keep");
        Files.createDirectory(dir.resolve("fake.md"));

        List<Path> result = MarkdownFileScanner
                .listMarkdownFiles(dir);

        assertThat(result)
                .extracting(p -> p.getFileName().toString())
                .containsExactly("keep.md");
    }

    @Test
    void listMarkdownFiles_whenPathIsFile_returnsEmptyList(
            @TempDir Path parent) throws IOException {
        Path file = parent.resolve("a.md");
        Files.writeString(file, "contents");

        List<Path> result = MarkdownFileScanner
                .listMarkdownFiles(file);

        assertThat(result).isEmpty();
    }
}
