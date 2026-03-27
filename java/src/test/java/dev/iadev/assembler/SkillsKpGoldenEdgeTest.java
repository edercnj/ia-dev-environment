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
        @DisplayName("disaster-recovery generated"
                + " when container set")
        void assemble_container_generatesDr(
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
                    "skills/disaster-recovery/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("disaster-recovery has reference"
                + " files when container set")
        void assemble_container_drHasReferences(
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
            Path drDir = outputDir.resolve(
                    "skills/disaster-recovery");
            assertThat(drDir.resolve(
                    "references/"
                    + "dr-strategy-decision-tree.md"))
                    .exists();
            assertThat(drDir.resolve(
                    "references/"
                    + "rpo-rto-calculator.md"))
                    .exists();
        }

        @Test
        @DisplayName("disaster-recovery excluded"
                + " when container is none")
        void assemble_noContainer_excludesDr(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/disaster-recovery"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("resilience KP contains chaos"
                + " engineering section")
        void assemble_resilience_containsChaos(
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
                            "skills/resilience/SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("## Chaos Engineering");
            assertThat(content)
                    .contains("### Principles");
            assertThat(content)
                    .contains("### Experiment Types");
            assertThat(content)
                    .contains("### Tools");
            assertThat(content)
                    .contains("### Game Day Planning");
            assertThat(content)
                    .contains("### Experiment Runbook"
                            + " Template");
        }

        @Test
        @DisplayName("resilience KP preserves original"
                + " content after chaos extension")
        void assemble_resilience_preservesOriginal(
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
                            "skills/resilience/SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("circuit-breaker.md");
            assertThat(content)
                    .contains("rate-limiting.md");
            assertThat(content)
                    .contains("bulkhead.md");
            assertThat(content)
                    .contains("timeout-patterns.md");
            assertThat(content)
                    .contains("retry-with-backoff.md");
            assertThat(content)
                    .contains("fallback-degradation.md");
            assertThat(content)
                    .contains("backpressure.md");
            assertThat(content)
                    .contains("resilience-metrics.md");
        }

        @Test
        @DisplayName("resilience KP has chaos"
                + " engineering experiments reference")
        void assemble_resilience_hasChaosRef(
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
                    "skills/resilience/references/"
                    + "chaos-engineering-experiments.md"))
                    .exists();
        }

        @Test
        @DisplayName("disaster-recovery SKILL.md"
                + " contains required sections")
        void assemble_dr_containsRequiredSections(
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
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/disaster-recovery/"
                            + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("## DR Strategies");
            assertThat(content)
                    .contains("## RPO/RTO Definitions");
            assertThat(content)
                    .contains("## Failover Automation");
            assertThat(content)
                    .contains("## DR Testing Cadence");
            assertThat(content)
                    .contains("## Multi-Region Patterns");
            assertThat(content)
                    .contains("## Recovery Procedures"
                            + " per Component");
        }

        @Test
        @DisplayName("disaster-recovery SKILL.md has"
                + " valid frontmatter")
        void assemble_dr_hasValidFrontmatter(
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
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/disaster-recovery/"
                            + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("name: disaster-recovery");
            assertThat(content)
                    .contains("user-invocable: false");
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
