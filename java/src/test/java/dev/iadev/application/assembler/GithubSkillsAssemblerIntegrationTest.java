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
 * Tests for GithubSkillsAssembler — copyReferences and
 * full pipeline integration tests.
 */
@DisplayName("GithubSkillsAssembler — integration")
class GithubSkillsAssemblerIntegrationTest {

    @Nested
    @DisplayName("copyReferences — copies references dir")
    class CopyReferences {

        @Test
        @DisplayName("copies references directory"
                + " when present")
        void copyReferences_whenCalled_copiesReferencesWhenPresent(
                @TempDir Path tempDir)
                throws IOException {
            Path srcDir = tempDir.resolve("src");
            Path refsDir = srcDir.resolve(
                    "references/test-skill");
            Files.createDirectories(refsDir);
            Files.writeString(
                    refsDir.resolve("ref.md"),
                    "# Reference {{PROJECT_NAME}}",
                    StandardCharsets.UTF_8);

            Path skillDir = tempDir.resolve(
                    "output/skills/test-skill");
            Files.createDirectories(skillDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler(tempDir);
            TemplateEngine engine = new TemplateEngine();

            SkillRenderContext ctx =
                    new SkillRenderContext(
                            srcDir, skillDir,
                            null, Map.of());
            assembler.copyReferences(
                    ctx, engine, skillDir,
                    "test-skill");

            Path destRef = skillDir.resolve(
                    "references/ref.md");
            assertThat(destRef).exists();
        }

        @Test
        @DisplayName("does nothing when references"
                + " dir absent")
        void copyReferences_whenCalled_doesNothingWhenAbsent(
                @TempDir Path tempDir)
                throws IOException {
            Path srcDir = tempDir.resolve("src");
            Files.createDirectories(srcDir);

            Path skillDir = tempDir.resolve(
                    "output/skills/test-skill");
            Files.createDirectories(skillDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler(tempDir);
            TemplateEngine engine = new TemplateEngine();

            SkillRenderContext ctx =
                    new SkillRenderContext(
                            srcDir, skillDir,
                            null, Map.of());
            assembler.copyReferences(
                    ctx, engine, skillDir,
                    "test-skill");

            Path refs = skillDir.resolve("references");
            assertThat(refs).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble — full pipeline integration")
    class AssembleIntegration {

        @Test
        @DisplayName("generates skills from classpath"
                + " templates")
        void assemble_whenCalled_generatesSkillsFromClasspath(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("none")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isNotEmpty();
            assertThat(files.stream()
                    .filter(f -> f.contains("SKILL.md")))
                    .isNotEmpty();
        }

        @Test
        @DisplayName("generates lib skills in nested"
                + " subdirectory")
        void assemble_whenCalled_generatesLibSkillsNested(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .filter(f -> f.contains(
                            "/lib/")))
                    .isNotEmpty();
        }

        @Test
        @DisplayName("applies infrastructure feature"
                + " gates")
        void assemble_whenCalled_appliesInfraFeatureGates(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("none")
                            .iac("none")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .filter(f -> f.contains(
                            "dockerfile")))
                    .isNotEmpty();
            assertThat(files.stream()
                    .filter(f -> f.contains(
                            "k8s-deployment")))
                    .isEmpty();
        }

        @Test
        @DisplayName("copies references for skills"
                + " that have them")
        void assemble_whenCalled_copiesReferences(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            Path lifecycle = outputDir.resolve(
                    "skills/x-dev-lifecycle/references");
            assertThat(lifecycle).exists();
        }
    }
}
