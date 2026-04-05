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
 * Tests for FrameworkKpWriter — copies framework-specific
 * knowledge pack files during rules assembly.
 */
@DisplayName("FrameworkKpWriter")
class FrameworkKpWriterTest {

    @Nested
    @DisplayName("copyFrameworkKps")
    class CopyFrameworkKps {

        @Test
        @DisplayName("unknown framework returns empty")
        void write_unknownFramework_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            FrameworkKpWriter writer =
                    new FrameworkKpWriter(resourceDir,
                            createVersionResolver());
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("unknown-fw", "1.0")
                            .build();

            List<String> files =
                    writer.copyFrameworkKps(
                            config, skillsDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("framework dir missing returns empty")
        void write_frameworkDirMissing_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            FrameworkKpWriter writer =
                    new FrameworkKpWriter(resourceDir,
                            createVersionResolver());
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files =
                    writer.copyFrameworkKps(
                            config, skillsDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("framework dir is a file returns"
                + " empty")
        void write_frameworkDirIsFile_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path frameworks =
                    resourceDir.resolve("knowledge/frameworks");
            Files.createDirectories(frameworks);
            Files.writeString(
                    frameworks.resolve("quarkus"),
                    "not a directory");
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            FrameworkKpWriter writer =
                    new FrameworkKpWriter(resourceDir,
                            createVersionResolver());
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files =
                    writer.copyFrameworkKps(
                            config, skillsDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("copies common framework files")
        void write_whenCalled_copiesCommonFiles(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path common = resourceDir.resolve(
                    "knowledge/frameworks/quarkus/common");
            Files.createDirectories(common);
            Files.writeString(
                    common.resolve("patterns.md"),
                    "common patterns",
                    StandardCharsets.UTF_8);

            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            FrameworkKpWriter writer =
                    new FrameworkKpWriter(resourceDir,
                            createVersionResolver());
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files =
                    writer.copyFrameworkKps(
                            config, skillsDir);

            assertThat(files).anyMatch(
                    f -> f.contains("patterns.md"));
        }

        @Test
        @DisplayName("missing common dir still works")
        void write_whenCalled_missingCommonDirStillWorks(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path fwDir = resourceDir.resolve(
                    "knowledge/frameworks/quarkus");
            Files.createDirectories(fwDir);
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            FrameworkKpWriter writer =
                    new FrameworkKpWriter(resourceDir,
                            createVersionResolver());
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files =
                    writer.copyFrameworkKps(
                            config, skillsDir);

            assertThat(files).isEmpty();
        }
    }

    private static VersionResolver createVersionResolver() {
        return new VersionResolver(
                new FileSystemVersionProvider());
    }
}
