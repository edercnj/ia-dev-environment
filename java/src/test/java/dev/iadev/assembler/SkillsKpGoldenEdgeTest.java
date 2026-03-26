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
 * Tests for SkillsAssembler — knowledge packs,
 * golden file parity, and edge cases.
 */
@DisplayName("SkillsAssembler — KP + golden + edge")
class SkillsKpGoldenEdgeTest {

    @Nested
    @DisplayName("assemble — knowledge packs")
    class KnowledgePacks {

        @Test
        @DisplayName("generates all core KPs")
        void assemble_whenCalled_generatesAllCoreKPs(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            Path s = outputDir.resolve("skills");
            assertThat(s.resolve(
                    "coding-standards/SKILL.md")).exists();
            assertThat(s.resolve(
                    "architecture/SKILL.md")).exists();
            assertThat(s.resolve(
                    "testing/SKILL.md")).exists();
            assertThat(s.resolve(
                    "security/SKILL.md")).exists();
            assertThat(s.resolve(
                    "compliance/SKILL.md")).exists();
            assertThat(s.resolve(
                    "api-design/SKILL.md")).exists();
            assertThat(s.resolve(
                    "observability/SKILL.md")).exists();
            assertThat(s.resolve(
                    "resilience/SKILL.md")).exists();
            assertThat(s.resolve(
                    "infrastructure/SKILL.md")).exists();
            assertThat(s.resolve(
                    "protocols/SKILL.md")).exists();
            assertThat(s.resolve(
                    "story-planning/SKILL.md")).exists();
            assertThat(s.resolve(
                    "sre-practices/SKILL.md")).exists();
            assertThat(s.resolve(
                    "layer-templates/SKILL.md")).exists();
        }

        @Test
        @DisplayName("generates stack patterns for"
                + " known framework")
        void assemble_whenCalled_generatesStackPatterns(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/quarkus-patterns"))
                    .exists();
        }

        @Test
        @DisplayName("generates dockerfile when"
                + " container set")
        void assemble_whenCalled_generatesDockerfile(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/dockerfile")).exists();
        }

        @Test
        @DisplayName("generates k8s when kubernetes")
        void assemble_whenCalled_generatesK8s(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .orchestrator("kubernetes")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/k8s-deployment")).exists();
        }

        @Test
        @DisplayName("database-patterns with database")
        void assemble_whenCalled_dbPatternsWithDb(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build(),
                    new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve(
                    "skills/database-patterns/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("database-patterns excluded"
                + " without database")
        void assemble_whenCalled_dbPatternsExcluded(
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
                    "skills/database-patterns"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("sre-practices has user-invocable false")
        void assemble_srePractices_frontmatterCorrect(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/sre-practices/SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("user-invocable: false");
            assertThat(content)
                    .contains("name: sre-practices");
        }

        @Test
        @DisplayName("sre-practices has all 6 sections")
        void assemble_srePractices_allSixSections(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/sre-practices/SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("## Error Budgets");
            assertThat(content)
                    .contains("## Toil Reduction");
            assertThat(content)
                    .contains("## On-Call Practices");
            assertThat(content)
                    .contains("## Capacity Planning");
            assertThat(content)
                    .contains("## Incident Management Process");
            assertThat(content)
                    .contains("## Change Management");
        }

        @Test
        @DisplayName("sre-practices reference files exist")
        void assemble_srePractices_referenceFilesExist(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            Path refs = outputDir.resolve(
                    "skills/sre-practices/references");
            assertThat(refs.resolve(
                    "error-budget-calculator.md")).exists();
            assertThat(refs.resolve(
                    "on-call-handbook.md")).exists();
            assertThat(refs.resolve(
                    "capacity-planning-template.md")).exists();
        }

        @Test
        @DisplayName("sre-practices does not overlap"
                + " with observability")
        void assemble_srePractices_noOverlapWithObservability(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String sreContent = Files.readString(
                    outputDir.resolve(
                            "skills/sre-practices/SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(sreContent)
                    .doesNotContain("distributed tracing");
            assertThat(sreContent)
                    .doesNotContain("structured logging");
        }
    }

    @Nested
    @DisplayName("golden file parity")
    class GoldenFile {

        @Test
        @DisplayName("core skill matches golden")
        void golden_coreSkill_matchesGoldenFile(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    SkillsTestFixtures
                            .buildQuarkusConfig(),
                    new TemplateEngine(), outputDir);
            String expected = loadResource(
                    "golden/java-quarkus/.claude/"
                            + "skills/x-dev-lifecycle/"
                            + "SKILL.md");
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
        @DisplayName("KP SKILL.md matches golden")
        void golden_kp_matchesGoldenFile(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    SkillsTestFixtures
                            .buildQuarkusConfig(),
                    new TemplateEngine(), outputDir);
            String expected = loadResource(
                    "golden/java-quarkus/.claude/"
                            + "skills/coding-standards/"
                            + "SKILL.md");
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

}
