package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VersionResolver")
class VersionResolverTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("findVersionDir()")
    class FindVersionDirTests {

        @Test
        @DisplayName("finds exact match directory")
        void findVersionDir_exactMatch_found() throws IOException {
            Files.createDirectory(tempDir.resolve("java-21"));

            var result = VersionResolver.findVersionDir(
                    tempDir, "java", "21");

            assertThat(result).isPresent();
            assertThat(result.get().getFileName().toString())
                    .isEqualTo("java-21");
        }

        @Test
        @DisplayName("falls back to major version wildcard")
        void findVersionDir_majorFallback_found() throws IOException {
            Files.createDirectory(tempDir.resolve("quarkus-3.x"));

            var result = VersionResolver.findVersionDir(
                    tempDir, "quarkus", "3.17.1");

            assertThat(result).isPresent();
            assertThat(result.get().getFileName().toString())
                    .isEqualTo("quarkus-3.x");
        }

        @Test
        @DisplayName("prefers exact match over major fallback")
        void findVersionDir_bothExist_prefersExact() throws IOException {
            Files.createDirectory(tempDir.resolve("java-21"));
            Files.createDirectory(tempDir.resolve("java-21.x"));

            var result = VersionResolver.findVersionDir(
                    tempDir, "java", "21");

            assertThat(result).isPresent();
            assertThat(result.get().getFileName().toString())
                    .isEqualTo("java-21");
        }

        @Test
        @DisplayName("returns empty when neither exists")
        void findVersionDir_noneExist_empty() {
            var result = VersionResolver.findVersionDir(
                    tempDir, "java", "21");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("skips files that are not directories")
        void findVersionDir_fileNotDir_empty() throws IOException {
            Files.createFile(tempDir.resolve("java-21"));

            var result = VersionResolver.findVersionDir(
                    tempDir, "java", "21");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractMajorPart()")
    class ExtractMajorPartTests {

        @Test
        @DisplayName("extracts before first dot")
        void extractMajor_dotted_beforeDot() {
            assertThat(VersionResolver.extractMajorPart("3.17.1"))
                    .isEqualTo("3");
        }

        @Test
        @DisplayName("returns full string when no dot")
        void extractMajor_noDot_fullString() {
            assertThat(VersionResolver.extractMajorPart("21"))
                    .isEqualTo("21");
        }

        @Test
        @DisplayName("returns empty for empty string")
        void extractMajor_empty_empty() {
            assertThat(VersionResolver.extractMajorPart(""))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns null for null")
        void extractMajor_null_null() {
            assertThat(VersionResolver.extractMajorPart(null))
                    .isNull();
        }
    }
}
