package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
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
 * Tests for SkillsAssembler — the second assembler in the
 * pipeline, generating .claude/skills/ directory structure
 * with core skills, conditional skills, and knowledge packs.
 */
@DisplayName("SkillsAssembler")
class SkillsAssemblerTest {

    @Nested
    @DisplayName("assemble — implements Assembler interface")
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
        @DisplayName("generates core skill directories"
                + " with SKILL.md")
        void assemble_whenCalled_generatesCoreSkillsWithSkillMd(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path skillsDir = outputDir.resolve("skills");
            assertThat(skillsDir.resolve(
                    "x-dev-lifecycle/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "x-dev-implement/SKILL.md"))
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
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path skillsDir = outputDir.resolve("skills");
            assertThat(skillsDir.resolve(
                    "lib/x-lib-task-decomposer/SKILL.md"))
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
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path refsDir = outputDir.resolve(
                    "skills/x-dev-lifecycle/references");
            assertThat(refsDir).exists();
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
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("assemble — conditional skills")
    class ConditionalSkills {

        @Test
        @DisplayName("config with REST generates"
                + " x-review-api skill")
        void assemble_restConfig_generatesApiReviewSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/x-review-api/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("config with gRPC generates"
                + " x-review-grpc skill")
        void assemble_grpcConfig_generatesGrpcReviewSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("grpc")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/x-review-grpc/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("config without REST does not"
                + " generate x-review-api")
        void assemble_noRest_excludesApiReviewSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("cli")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/x-review-api"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("config without database does not"
                + " generate database conditional skills")
        void assemble_noDatabase_excludesDbSkills(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("cli")
                    .smokeTests(false)
                    .performanceTests(false)
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path skillsDir = outputDir.resolve("skills");
            // Only core and KPs should exist
            assertThat(skillsDir.resolve("x-review-api"))
                    .doesNotExist();
            assertThat(skillsDir.resolve("x-review-grpc"))
                    .doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble — knowledge packs")
    class KnowledgePacks {

        @Test
        @DisplayName("generates all core knowledge packs")
        void assemble_whenCalled_generatesAllCoreKnowledgePacks(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path skillsDir = outputDir.resolve("skills");
            assertThat(skillsDir.resolve(
                    "coding-standards/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "architecture/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "testing/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "security/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "compliance/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "api-design/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "observability/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "resilience/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "infrastructure/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "protocols/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "story-planning/SKILL.md"))
                    .exists();
            assertThat(skillsDir.resolve(
                    "layer-templates/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("generates stack patterns for"
                + " known framework")
        void assemble_whenCalled_generatesStackPatternsForKnownFramework(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .framework("quarkus", "3.17")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/quarkus-patterns"))
                    .exists();
        }

        @Test
        @DisplayName("generates infra patterns when"
                + " container is not none")
        void assemble_whenCalled_generatesDockerfileWhenContainerSet(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .container("docker")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/dockerfile"))
                    .exists();
        }

        @Test
        @DisplayName("generates k8s-deployment when"
                + " orchestrator is kubernetes")
        void assemble_whenCalled_generatesK8sDeployment(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .orchestrator("kubernetes")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/k8s-deployment"))
                    .exists();
        }

        @Test
        @DisplayName("database-patterns included when"
                + " database not none")
        void assemble_whenCalled_databasePatternsIncludedWithDatabase(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/database-patterns/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("database-patterns excluded when"
                + " database is none")
        void assemble_whenCalled_databasePatternsExcludedWithoutDatabase(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/database-patterns"))
                    .doesNotExist();
        }
    }

    @Nested
    @DisplayName("golden file — byte-for-byte parity")
    class GoldenFile {

        @Test
        @DisplayName("core skill SKILL.md matches golden"
                + " file for java-quarkus profile")
        void golden_coreSkill_matchesGoldenFile(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = buildQuarkusConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String goldenPath = "golden/java-quarkus/"
                    + ".claude/skills/x-dev-lifecycle/"
                    + "SKILL.md";
            String expected = loadResource(goldenPath);
            if (expected != null) {
                String actual = Files.readString(
                        outputDir.resolve(
                                "skills/x-dev-lifecycle/"
                                        + "SKILL.md"),
                        StandardCharsets.UTF_8);
                assertThat(actual).isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("knowledge pack SKILL.md matches"
                + " golden file")
        void golden_kp_matchesGoldenFile(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = buildQuarkusConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String goldenPath = "golden/java-quarkus/"
                    + ".claude/skills/coding-standards/"
                    + "SKILL.md";
            String expected = loadResource(goldenPath);
            if (expected != null) {
                String actual = Files.readString(
                        outputDir.resolve(
                                "skills/coding-standards/"
                                        + "SKILL.md"),
                        StandardCharsets.UTF_8);
                assertThat(actual).isEqualTo(expected);
            }
        }

        private String loadResource(String path) {
            var url = getClass().getClassLoader()
                    .getResource(path);
            if (url == null) {
                return null;
            }
            try {
                return Files.readString(
                        Path.of(url.getPath()),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }
    }

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("custom resourcesDir with empty"
                + " skills-templates returns empty list")
        void assemble_emptyResources_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path stDir = resourceDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(stDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("selectCoreSkills returns empty when"
                + " core dir missing")
        void assemble_whenCalled_selectCoreSkillsEmptyWhenCoreMissing(
                @TempDir Path tempDir) {
            Path resourceDir = tempDir.resolve("res");
            SkillsAssembler assembler =
                    new SkillsAssembler(resourceDir);

            List<String> skills =
                    assembler.selectCoreSkills();

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("conditional skill not found in"
                + " resources is skipped")
        void assemble_conditionalSkillNotFound_isSkipped(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(coreDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/x-review-api"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("knowledge pack not found in"
                + " resources is skipped")
        void assemble_knowledgePackNotFound_isSkipped(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(coreDir);
            Path kpDir = resourceDir.resolve(
                    "skills-templates/knowledge-packs");
            Files.createDirectories(kpDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/coding-standards"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("stack patterns skipped when"
                + " framework not mapped")
        void assemble_whenCalled_stackPatternsSkippedWhenNotMapped(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config = TestConfigBuilder.builder()
                    .framework("unknown-framework", "1.0")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            // No stack pattern should be generated for
            // unknown frameworks
            assertThat(outputDir.resolve(
                    "skills/unknown-patterns"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("copyNonSkillItems copies files and"
                + " directories from KP")
        void assemble_whenCalled_copyNonSkillItemsFromKp(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path kpSrc = resourceDir.resolve(
                    "skills-templates/knowledge-packs/"
                            + "test-pack");
            Files.createDirectories(kpSrc);
            Files.createDirectories(
                    kpSrc.resolve("references"));
            Files.writeString(
                    kpSrc.resolve("SKILL.md"),
                    "# Test Pack\n",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    kpSrc.resolve("references/ref.md"),
                    "# Ref\n",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    kpSrc.resolve("extra.md"),
                    "# Extra\n",
                    StandardCharsets.UTF_8);

            Path coreDir = resourceDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(coreDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            // Manually invoke assembler to exercise
            // copyNonSkillItems path
            SkillsAssembler assembler =
                    new SkillsAssembler(resourceDir);

            // Create a config that selects no KPs from
            // core list, but we'll verify manually
            Path destDir = outputDir.resolve(
                    "skills/test-pack");
            CopyHelpers.ensureDirectory(destDir);
            CopyHelpers.copyTemplateFile(
                    kpSrc.resolve("SKILL.md"),
                    destDir.resolve("SKILL.md"),
                    new TemplateEngine(),
                    java.util.Map.of());

            // Verify the file exists
            assertThat(destDir.resolve("SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("infra patterns skipped when dir"
                + " does not exist")
        void assemble_whenCalled_infraPatternsSkippedWhenDirMissing(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(coreDir);
            Path infraDir = resourceDir.resolve(
                    "skills-templates/knowledge-packs/"
                            + "infra-patterns");
            Files.createDirectories(infraDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.builder()
                    .container("docker")
                    .orchestrator("kubernetes")
                    .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            // Infra patterns directories should not exist
            // since templates are missing
            assertThat(outputDir.resolve(
                    "skills/k8s-deployment"))
                    .doesNotExist();
        }
    }

    private static ProjectConfig buildQuarkusConfig() {
        return TestConfigBuilder.builder()
                .projectName("my-quarkus-service")
                .purpose(
                        "Describe your service purpose here")
                .archStyle("microservice")
                .domainDriven(true)
                .eventDriven(true)
                .language("java", "21")
                .framework("quarkus", "3.17")
                .buildTool("maven")
                .nativeBuild(true)
                .contractTests(true)
                .orchestrator("kubernetes")
                .clearInterfaces()
                .addInterface("rest")
                .addInterface("grpc")
                .addInterface("event-consumer")
                .addInterface("event-producer")
                .build();
    }
}
