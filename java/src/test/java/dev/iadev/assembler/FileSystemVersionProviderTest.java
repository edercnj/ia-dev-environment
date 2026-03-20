package dev.iadev.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileSystemVersionProvider")
class FileSystemVersionProviderTest {

    private final FileSystemVersionProvider provider =
            new FileSystemVersionProvider();

    @Nested
    @DisplayName("exists()")
    class ExistsTests {

        @Test
        @DisplayName("returns true for existing directory")
        void exists_existingDir_true(
                @TempDir Path tempDir)
                throws IOException {
            Path dir = tempDir.resolve("java-21");
            Files.createDirectory(dir);

            assertThat(provider.exists(dir)).isTrue();
        }

        @Test
        @DisplayName("returns false for non-existing path")
        void exists_nonExisting_false(
                @TempDir Path tempDir) {
            Path missing = tempDir.resolve("missing");

            assertThat(provider.exists(missing)).isFalse();
        }

        @Test
        @DisplayName("returns false for regular file")
        void exists_regularFile_false(
                @TempDir Path tempDir)
                throws IOException {
            Path file = tempDir.resolve("java-21");
            Files.createFile(file);

            assertThat(provider.exists(file)).isFalse();
        }
    }

    @Nested
    @DisplayName("listVersionDirectories()")
    class ListVersionDirectoriesTests {

        @Test
        @DisplayName("lists subdirectories under base path")
        void listVersionDirs_withSubdirs_returnsDirs(
                @TempDir Path tempDir)
                throws IOException {
            Files.createDirectory(
                    tempDir.resolve("17"));
            Files.createDirectory(
                    tempDir.resolve("21"));
            Files.createDirectory(
                    tempDir.resolve("default"));

            List<Path> result =
                    provider.listVersionDirectories(
                            tempDir);

            assertThat(result).hasSize(3);
            List<String> names = result.stream()
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
            assertThat(names).containsExactly(
                    "17", "21", "default");
        }

        @Test
        @DisplayName("excludes regular files from listing")
        void listVersionDirs_mixedContent_onlyDirs(
                @TempDir Path tempDir)
                throws IOException {
            Files.createDirectory(
                    tempDir.resolve("21"));
            Files.createFile(
                    tempDir.resolve("README.md"));

            List<Path> result =
                    provider.listVersionDirectories(
                            tempDir);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFileName()
                    .toString()).isEqualTo("21");
        }

        @Test
        @DisplayName("returns empty for non-existing path")
        void listVersionDirs_nonExisting_empty(
                @TempDir Path tempDir) {
            Path missing = tempDir.resolve("missing");

            List<Path> result =
                    provider.listVersionDirectories(
                            missing);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for empty directory")
        void listVersionDirs_emptyDir_empty(
                @TempDir Path tempDir) {
            List<Path> result =
                    provider.listVersionDirectories(
                            tempDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when base is a file")
        void listVersionDirs_fileNotDir_empty(
                @TempDir Path tempDir)
                throws IOException {
            Path file = tempDir.resolve("not-a-dir");
            Files.createFile(file);

            List<Path> result =
                    provider.listVersionDirectories(file);

            assertThat(result).isEmpty();
        }
    }
}
