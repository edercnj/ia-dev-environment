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
 * Tests for SkillsAssembler — interface contract,
 * core skills, and conditional skills.
 */
@DisplayName("SkillsAssembler — core + conditional")
class SkillsCoreConditionalTest {

    @Nested
    @DisplayName("assemble — implements Assembler")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — core skills generation")
    class CoreSkills {

        @Test
        @DisplayName("generates core skill directories")
        void assemble_whenCalled_generatesCoreSkillsWithSkillMd(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            Path skillsDir = outputDir.resolve("skills");
            assertThat(skillsDir.resolve(
                    "x-story-implement/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-task-implement/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-review/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-review-pr/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-git-push/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-ops-troubleshoot/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-test-plan/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-test-run/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-jira-create-epic/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-jira-create-stories/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-spec-drift/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("generates lib sub-skills")
        void assemble_whenCalled_generatesLibSubSkills(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            TestConfigBuilder.minimal();
            assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve(
                    "skills/lib/x-lib-task-decomposer"
                            + "/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("core skill references are copied")
        void assemble_coreSkillReferences_areCopied(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve(
                    "skills/x-story-implement/references"))
                    .exists();
        }

        @Test
        @DisplayName("returned list is not empty")
        void assemble_whenCalled_returnedListNotEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            List<String> files = assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            assertThat(files).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("assemble — conditional skills")
    class ConditionalSkills {

        @Test
        @DisplayName("REST generates x-review-api")
        void assemble_restConfig_generatesApiReviewSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/x-review-api/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("gRPC generates x-review-grpc")
        void assemble_grpcConfig_generatesGrpcReviewSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("grpc")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/x-review-grpc/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("no REST excludes x-review-api")
        void assemble_noRest_excludesApiReviewSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/x-review-api"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("no database excludes db skills")
        void assemble_noDatabase_excludesDbSkills(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("cli")
                            .smokeTests(false)
                            .performanceTests(false)
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/x-review-api"))
                    .doesNotExist();
            assertThat(outputDir.resolve(
                    "skills/x-review-grpc"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("pentestReadiness true generates"
                + " x-security-pentest")
        void assemble_pentestTrue_generatesPentestSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .pentestReadiness(true)
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/x-security-pentest/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("pentestReadiness false excludes"
                + " x-security-pentest")
        void assemble_pentestFalse_excludesPentestSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .pentestReadiness(false)
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/x-security-pentest"))
                    .doesNotExist();
        }
    }
}
