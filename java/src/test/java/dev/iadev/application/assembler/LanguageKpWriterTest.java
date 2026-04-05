package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.stack.VersionResolver;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for LanguageKpWriter — copies language-specific
 * knowledge pack files during rules assembly.
 */
@DisplayName("LanguageKpWriter")
class LanguageKpWriterTest {

    @Nested
    @DisplayName("copyLanguageKps")
    class CopyLanguageKps {

        @Test
        @DisplayName("missing language dir returns empty")
        void write_missingDir_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            LanguageKpWriter writer =
                    new LanguageKpWriter(resourceDir,
                            createVersionResolver());
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("unknown", "1.0")
                            .build();

            List<String> files =
                    writer.copyLanguageKps(
                            config, skillsDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("language dir is a file returns empty")
        void write_languageDirIsFile_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path langParent =
                    resourceDir.resolve("languages");
            Files.createDirectories(langParent);
            Files.writeString(
                    langParent.resolve("java"),
                    "not a directory");
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            LanguageKpWriter writer =
                    new LanguageKpWriter(resourceDir,
                            createVersionResolver());
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();

            List<String> files =
                    writer.copyLanguageKps(
                            config, skillsDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("copies common and version files")
        void write_whenCalled_copiesCommonAndVersionFiles(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path common = resourceDir.resolve(
                    "languages/java/common");
            Files.createDirectories(common);
            Files.writeString(
                    common.resolve("coding-style.md"),
                    "coding content",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    common.resolve("testing-patterns.md"),
                    "testing content",
                    StandardCharsets.UTF_8);

            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            LanguageKpWriter writer =
                    new LanguageKpWriter(resourceDir,
                            createVersionResolver());
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();

            List<String> files =
                    writer.copyLanguageKps(
                            config, skillsDir);

            assertThat(files).hasSize(2);
            assertThat(files).anyMatch(
                    f -> f.contains("coding-standards"
                            + "/references/"
                            + "coding-style.md"));
            assertThat(files).anyMatch(
                    f -> f.contains("testing/references/"
                            + "testing-patterns.md"));
        }

        @Test
        @DisplayName("missing common dir still works")
        void write_whenCalled_missingCommonDirStillWorks(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path langDir = resourceDir.resolve(
                    "languages/java");
            Files.createDirectories(langDir);
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            LanguageKpWriter writer =
                    new LanguageKpWriter(resourceDir,
                            createVersionResolver());
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();

            List<String> files =
                    writer.copyLanguageKps(
                            config, skillsDir);

            assertThat(files).isEmpty();
        }
    }

    private static VersionResolver createVersionResolver() {
        return new VersionResolver(
                new FileSystemVersionProvider());
    }
}
