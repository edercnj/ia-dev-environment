package dev.iadev.assembler;

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
                    "ci-cd-patterns/SKILL.md")).exists();
            assertThat(s.resolve(
                    "sre-practices/SKILL.md")).exists();
            assertThat(s.resolve(
                    "release-management/SKILL.md")).exists();
            assertThat(s.resolve(
                    "performance-engineering/SKILL.md"))
                    .exists();
            assertThat(s.resolve(
                    "layer-templates/SKILL.md")).exists();
        }

        @Test
        @DisplayName("ci-cd-patterns has reference files")
        void assemble_ciCdPatterns_hasReferenceFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            Path kp = outputDir.resolve(
                    "skills/ci-cd-patterns");
            assertThat(kp.resolve("references/"
                    + "github-actions-patterns.md"))
                    .exists();
            assertThat(kp.resolve("references/"
                    + "pipeline-security.md"))
                    .exists();
            assertThat(kp.resolve("references/"
                    + "caching-strategies.md"))
                    .exists();
        }

        @Test
        @DisplayName("ci-cd-patterns SKILL.md has valid"
                + " frontmatter")
        void assemble_ciCdPatterns_hasValidFrontmatter(
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
                            "skills/ci-cd-patterns/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("name: ci-cd-patterns")
                    .contains("user-invocable: false");
        }

        @Test
        @DisplayName("ci-cd-patterns contains pipeline"
                + " patterns section")
        void assemble_ciCdPatterns_hasPipelinePatterns(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .buildTool("maven")
                            .container("docker")
                            .build(),
                    new TemplateEngine(), outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/ci-cd-patterns/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Pipeline Stages")
                    .contains("Cross-Cutting Patterns");
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
        @DisplayName("release-management has valid"
                + " frontmatter")
        void assemble_releaseManagement_hasValidFrontmatter(
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
                            "skills/release-management/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("name: release-management")
                    .contains("user-invocable: false");
        }

        @Test
        @DisplayName("release-management has all 8 sections")
        void assemble_releaseManagement_allEightSections(
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
                            "skills/release-management/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("## Semantic Versioning");
            assertThat(content)
                    .contains("## Version Lifecycle");
            assertThat(content)
                    .contains(
                            "## Release Branching Strategies");
            assertThat(content)
                    .contains(
                            "## Artifact Registry Management");
            assertThat(content)
                    .contains(
                            "## Release Signing & Attestation");
            assertThat(content)
                    .contains("## Hotfix Process");
            assertThat(content)
                    .contains("## Rollback Procedures");
            assertThat(content)
                    .contains("## Release Communication");
        }

        @Test
        @DisplayName("release-management reference files"
                + " exist")
        void assemble_releaseManagement_referenceFilesExist(
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
                    "skills/release-management/references");
            assertThat(refs.resolve(
                    "release-branching-guide.md")).exists();
            assertThat(refs.resolve(
                    "artifact-publishing-matrix.md"))
                    .exists();
            assertThat(refs.resolve(
                    "rollback-decision-tree.md")).exists();
        }

        @Test
        @DisplayName("release-management does not overlap"
                + " with ci-cd-patterns")
        void assemble_releaseManagement_noOverlapWithCiCd(
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
                            "skills/release-management/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain("Matrix Builds");
            assertThat(content)
                    .doesNotContain("Test Stage");
        }

        @Test
        @DisplayName("performance-engineering has valid"
                + " frontmatter")
        void assemble_perfEng_hasValidFrontmatter(
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
                            "skills/performance-engineering/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains(
                            "name: performance-engineering")
                    .contains("user-invocable: false");
        }

        @Test
        @DisplayName("performance-engineering has all"
                + " 7 sections")
        void assemble_perfEng_allSevenSections(
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
                            "skills/performance-engineering/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains(
                            "## Profiling Tools & Patterns");
            assertThat(content)
                    .contains(
                            "## Benchmarking Frameworks");
            assertThat(content)
                    .contains(
                            "## Performance Anti-Patterns");
            assertThat(content)
                    .contains(
                            "## Optimization Strategies");
            assertThat(content)
                    .contains(
                            "## Load Testing Patterns");
            assertThat(content)
                    .contains(
                            "## Performance Regression Detection");
            assertThat(content)
                    .contains(
                            "## Memory Management");
        }

        @Test
        @DisplayName("performance-engineering reference"
                + " files exist")
        void assemble_perfEng_referenceFilesExist(
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
                    "skills/performance-engineering/"
                            + "references");
            assertThat(refs.resolve(
                    "profiling-tools-matrix.md")).exists();
            assertThat(refs.resolve(
                    "load-testing-patterns.md")).exists();
            assertThat(refs.resolve(
                    "performance-metrics-guide.md"))
                    .exists();
        }

        @Test
        @DisplayName("performance-engineering does not"
                + " overlap with resilience")
        void assemble_perfEng_noOverlapWithResilience(
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
                            "skills/performance-engineering/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain("Circuit Breaker");
            assertThat(content)
                    .doesNotContain("Retry Pattern");
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
