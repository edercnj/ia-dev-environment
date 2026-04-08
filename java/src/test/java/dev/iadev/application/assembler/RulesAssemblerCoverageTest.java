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
 * Coverage tests for RulesAssembler — core rules,
 * route-to-KPs, context builder, and defaults.
 */
@DisplayName("RulesAssembler — coverage base")
class RulesAssemblerCoverageTest {

    @Nested
    @DisplayName("copyCoreRules — edge cases")
    class CopyCoreRulesEdgeCases {

        @Test
        @DisplayName("rules dir missing returns"
                + " only identity and domain")
        void assemble_noCoreRulesDir_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path templates =
                    resourceDir.resolve("shared/templates");
            Files.createDirectories(templates);
            Files.writeString(
                    templates.resolve(
                            "domain-template.md"),
                    "Domain template\n");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(
                    f -> f.contains(
                            "01-project-identity.md"));
            assertThat(files).anyMatch(
                    f -> f.contains("02-domain.md"));
        }

        @Test
        @DisplayName("rules is a file not directory"
                + " returns only identity and domain")
        void assemble_coreRules_isFile(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(
                    resourceDir.resolve("targets/claude"));
            Files.writeString(
                    resourceDir.resolve(
                            "targets/claude/rules"),
                    "not a directory");
            Path templates =
                    resourceDir.resolve("shared/templates");
            Files.createDirectories(templates);
            Files.writeString(
                    templates.resolve(
                            "domain-template.md"),
                    "Domain\n");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(
                    f -> f.contains(
                            "01-project-identity.md"));
        }
    }

    @Nested
    @DisplayName("routeCoreToKps — edge cases")
    class RouteCoreToKps {

        @Test
        @DisplayName("core dir missing returns empty"
                + " kp list")
        void routeCoreToKps_noCoreDir_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreRules =
                    resourceDir.resolve("targets/claude/rules");
            Files.createDirectories(coreRules);
            Path templates =
                    resourceDir.resolve("shared/templates");
            Files.createDirectories(templates);
            Files.writeString(
                    templates.resolve(
                            "domain-template.md"),
                    "Domain\n");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files)
                    .noneMatch(
                            f -> f.contains("references"));
        }

        @Test
        @DisplayName("core dir is a file returns empty")
        void routeCoreToKps_coreDir_isFile(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreRules =
                    resourceDir.resolve("targets/claude/rules");
            Files.createDirectories(coreRules);
            Files.createDirectories(
                    resourceDir.resolve("knowledge"));
            Files.writeString(
                    resourceDir.resolve("knowledge/core"),
                    "not a dir");
            Path templates =
                    resourceDir.resolve("shared/templates");
            Files.createDirectories(templates);
            Files.writeString(
                    templates.resolve(
                            "domain-template.md"),
                    "Domain\n");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("core dir present but route source"
                + " files missing triggers continue")
        void routeCoreToKps_whenCalled_routeSourceFileMissing(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir =
                    RulesAssemblerCoverageHelper
                            .setupMinimalRes(tempDir);
            Path coreDir =
                    resourceDir.resolve("knowledge/core");
            Files.createDirectories(coreDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }
    }

}
