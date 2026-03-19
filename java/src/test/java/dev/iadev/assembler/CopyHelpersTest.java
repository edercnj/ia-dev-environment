package dev.iadev.assembler;

import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        void rendersPlaceholders(@TempDir Path tempDir)
                throws IOException {
            Path src = tempDir.resolve("source.md");
            Files.writeString(src,
                    "Project: {{PROJECT_NAME}}\n",
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
        void createsParentDirs(@TempDir Path tempDir)
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
        @DisplayName("returns path when source exists")
        void sourceExists_returnsPath(@TempDir Path tempDir)
                throws IOException {
            Path src = tempDir.resolve("exists.md");
            Files.writeString(src, "hello");
            Path dest = tempDir.resolve("out.md");

            String result = CopyHelpers.copyTemplateFileIfExists(
                    src, dest, new TemplateEngine(), Map.of());

            assertThat(result).isNotNull();
            assertThat(dest).exists();
        }

        @Test
        @DisplayName("returns null when source missing")
        void sourceMissing_returnsNull(@TempDir Path tempDir) {
            Path src = tempDir.resolve("nonexistent.md");
            Path dest = tempDir.resolve("out.md");

            String result = CopyHelpers.copyTemplateFileIfExists(
                    src, dest, new TemplateEngine(), Map.of());

            assertThat(result).isNull();
            assertThat(dest).doesNotExist();
        }
    }

    @Nested
    @DisplayName("copyStaticFile")
    class CopyStaticFile {

        @Test
        @DisplayName("copies file without rendering")
        void copiesWithoutRendering(@TempDir Path tempDir)
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
        void createsParentDirs(@TempDir Path tempDir)
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
        void copiesRecursively(@TempDir Path tempDir)
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
        void createsDirectoryAndParents(@TempDir Path tempDir) {
            Path dir = tempDir.resolve("x/y/z");

            CopyHelpers.ensureDirectory(dir);

            assertThat(dir).isDirectory();
        }

        @Test
        @DisplayName("is idempotent for existing directory")
        void existingDir_noError(@TempDir Path tempDir)
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
        void replacesInMdFiles(@TempDir Path tempDir)
                throws IOException {
            Path mdFile = tempDir.resolve("test.md");
            Files.writeString(mdFile,
                    "Name: {{PROJECT_NAME}}\n");

            Path txtFile = tempDir.resolve("test.txt");
            Files.writeString(txtFile,
                    "Name: {{PROJECT_NAME}}\n");

            Map<String, Object> context =
                    Map.of("project_name", "replaced");

            CopyHelpers.replacePlaceholdersInDir(
                    tempDir, new TemplateEngine(), context);

            assertThat(Files.readString(mdFile))
                    .contains("replaced");
            // .txt files should NOT be replaced
            assertThat(Files.readString(txtFile))
                    .contains("{{PROJECT_NAME}}");
        }

        @Test
        @DisplayName("replaces in nested directories")
        void replacesInNestedDirs(@TempDir Path tempDir)
                throws IOException {
            Path sub = tempDir.resolve("sub");
            Files.createDirectories(sub);
            Path nested = sub.resolve("deep.md");
            Files.writeString(nested,
                    "{{PROJECT_NAME}} value");

            Map<String, Object> context =
                    Map.of("project_name", "deep-val");

            CopyHelpers.replacePlaceholdersInDir(
                    tempDir, new TemplateEngine(), context);

            assertThat(Files.readString(nested))
                    .contains("deep-val");
        }
    }
}
