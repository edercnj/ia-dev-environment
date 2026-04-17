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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Miscellaneous coverage tests targeting remaining
 * uncovered lines across multiple assemblers.
 */
@DisplayName("Assembler misc — coverage")
class AssemblerMiscCoverageTest {

    @Nested
    @DisplayName("PatternsAssembler — edge cases")
    class PatternsEdge {

        @Test
        @DisplayName("custom resourceDir with no"
                + " patterns returns empty")
        void assemble_noPatternsDir_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("pattern category dir missing"
                + " skips category")
        void assemble_whenCalled_categoryDirMissing(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path patternsDir = resourceDir.resolve(
                    "knowledge/patterns");
            Files.createDirectories(patternsDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("DocsAdrAssembler — edge cases")
    class DocsAdrEdge {

        @Test
        @DisplayName("custom resourceDir with no ADR"
                + " templates returns empty")
        void assemble_noAdrTemplatesEmpty_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("HooksAssembler — edge cases")
    class HooksEdge {

        @Test
        @DisplayName("python language returns empty"
                + " hooks")
        void assemble_whenCalled_pythonNoHooks(@TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .telemetryEnabled(false)
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("Full pipeline integration")
    class FullPipelineIntegration {

        @Test
        @DisplayName("full pipeline with all features"
                + " exercises maximum code paths")
        void assemble_whenCalled_fullPipelineAllFeatures(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("coverage-test")
                    .purpose("Coverage testing")
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .archStyle("hexagonal")
                    .domainDriven(true)
                    .eventDriven(true)
                    .database("postgresql", "16")
                    .cache("redis", "7.4")
                    .container("docker")
                    .orchestrator("kubernetes")
                    .iac("terraform")
                    .cloudProvider("aws")
                    .smokeTests(true)
                    .contractTests(true)
                    .securityFrameworks("owasp")
                    .clearInterfaces()
                    .addInterface("rest")
                    .addInterface("grpc")
                    .addInterface("event-consumer",
                            "", "kafka")
                    .build();

            // Run all assemblers from classpath
            new RulesAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new SkillsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new AgentsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new ProtocolsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new HooksAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new SettingsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);
            new PatternsAssembler().assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(outputDir.resolve("rules"))
                    .exists();
            assertThat(outputDir.resolve("skills"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("CopyHelpers — functional paths")
    class CopyHelpersFunc {

        @Test
        @DisplayName("copyTemplateFileIfExists with"
                + " existing file copies it")
        void copy_whenCalled_copyIfExistsWithFile(@TempDir Path tempDir)
                throws IOException {
            Path src = tempDir.resolve("src.md");
            Files.writeString(src,
                    "Template {{KEY}}\n",
                    StandardCharsets.UTF_8);
            Path dest = tempDir.resolve(
                    "output/copied.md");

            var result =
                    CopyHelpers.copyTemplateFileIfExists(
                            src, dest,
                            new TemplateEngine(),
                            Map.of("key", "value"));

            assertThat(result).isPresent();
            assertThat(dest).exists();
        }

        @Test
        @DisplayName("replacePlaceholdersInDir replaces"
                + " in nested md files")
        void copy_whenCalled_replacePlaceholdersInDir(
                @TempDir Path tempDir) throws IOException {
            Path dir = tempDir.resolve("content");
            Files.createDirectories(dir);
            Files.writeString(
                    dir.resolve("test.md"),
                    "Hello {NAME}",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    dir.resolve("skip.txt"),
                    "Not {REPLACED}",
                    StandardCharsets.UTF_8);

            CopyHelpers.replacePlaceholdersInDir(
                    dir, new TemplateEngine(),
                    Map.of("name", "World"));

            String md = Files.readString(
                    dir.resolve("test.md"),
                    StandardCharsets.UTF_8);
            assertThat(md).contains("World");

            String txt = Files.readString(
                    dir.resolve("skip.txt"),
                    StandardCharsets.UTF_8);
            assertThat(txt)
                    .contains("Not {REPLACED}");
        }
    }
}
