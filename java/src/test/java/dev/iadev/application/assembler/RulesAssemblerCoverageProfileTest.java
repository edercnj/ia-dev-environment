package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
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
 * Coverage tests for RulesAssembler — profile-specific
 * tests for language KPs, framework KPs, and full
 * integration.
 */
@DisplayName("RulesAssembler — coverage profiles")
class RulesAssemblerCoverageProfileTest {

    @Nested
    @DisplayName("copyLanguageKps — edge cases")
    class CopyLanguageKps {

        @Test
        @DisplayName("language dir missing returns empty")
        void copyLanguageKps_noLanguageDir_succeeds(
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
                            .language("unknown-lang", "1.0")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).noneMatch(
                    f -> f.contains("coding-standards"));
        }

        @Test
        @DisplayName("language dir is a file returns empty")
        void copyLanguageKps_languageDir_isFile(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    RulesAssemblerCoverageHelper
                            .setupMinimalRes(tempDir);
            Path langParent =
                    resourceDir.resolve("knowledge/languages");
            Files.createDirectories(langParent);
            Files.writeString(
                    langParent.resolve("java"),
                    "not a dir");
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("common subdir missing still works")
        void copyLanguageKps_noCommonSubdir_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    RulesAssemblerCoverageHelper
                            .setupMinimalRes(tempDir);
            Path langDir =
                    resourceDir.resolve("knowledge/languages/java");
            Files.createDirectories(langDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("common with testing file routes"
                + " to testing refs")
        void copyLanguageKps_whenCalled_testingFileRoutesToTestingRefs(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir =
                    RulesAssemblerCoverageHelper
                            .setupMinimalRes(tempDir);
            Path common = resourceDir.resolve(
                    "knowledge/languages/java/common");
            Files.createDirectories(common);
            Files.writeString(
                    common.resolve("testing-patterns.md"),
                    "testing content");
            Files.writeString(
                    common.resolve("coding-style.md"),
                    "coding content");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(
                    f -> f.contains("testing/references/"
                            + "testing-patterns.md"));
            assertThat(files).anyMatch(
                    f -> f.contains("coding-standards/"
                            + "references/"
                            + "coding-style.md"));
        }

        @Test
        @DisplayName("version dir empty returns no"
                + " version files")
        void copyLanguageKps_noVersionDir_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    RulesAssemblerCoverageHelper
                            .setupMinimalRes(tempDir);
            Path langDir =
                    resourceDir.resolve("knowledge/languages/java");
            Files.createDirectories(langDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "99")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }
    }
}
