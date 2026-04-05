package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage tests for RulesAssembler — framework KP
 * edge cases.
 */
@DisplayName("RulesAssembler — coverage framework")
class RulesAssemblerCoverageFrameworkTest {

    @Nested
    @DisplayName("copyFrameworkKps — edge cases")
    class CopyFrameworkKps {

        @Test
        @DisplayName("unknown framework returns empty")
        void copyFrameworkKps_whenCalled_unknownFramework(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    RulesAssemblerCoverageHelper
                            .setupMinimalRes(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("unknown-fw", "1.0")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("framework dir missing returns empty")
        void copyFrameworkKps_whenCalled_frameworkDirMissing(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    RulesAssemblerCoverageHelper
                            .setupMinimalRes(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("framework dir is a file returns"
                + " empty fw files")
        void copyFrameworkKps_frameworkDir_isFile(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    RulesAssemblerCoverageHelper
                            .setupMinimalRes(tempDir);
            Path frameworks =
                    resourceDir.resolve("knowledge/frameworks");
            Files.createDirectories(frameworks);
            Files.writeString(
                    frameworks.resolve("quarkus"),
                    "not a dir");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("framework with common and version"
                + " copies both")
        void copyFrameworkKps_whenCalled_frameworkCommonAndVersion(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir =
                    RulesAssemblerCoverageHelper
                            .setupMinimalRes(tempDir);
            Path fwDir = resourceDir.resolve(
                    "knowledge/frameworks/quarkus");
            Path common = fwDir.resolve("common");
            Files.createDirectories(common);
            Files.writeString(
                    common.resolve("patterns.md"),
                    "common patterns");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(
                    f -> f.contains("patterns.md"));
        }

        @Test
        @DisplayName("framework common missing still works")
        void copyFrameworkKps_whenCalled_frameworkNoCommon(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    RulesAssemblerCoverageHelper
                            .setupMinimalRes(tempDir);
            Path fwDir = resourceDir.resolve(
                    "knowledge/frameworks/quarkus");
            Files.createDirectories(fwDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }
    }
}
