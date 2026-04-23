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
 * Tests for story-0013-0024: API Design KP extension
 * with deprecation/versioning sections and FinOps KP
 * conditional generation.
 */
@DisplayName("API Design + FinOps KP")
class ApiDesignFinOpsKpTest {

    @Nested
    @DisplayName("API Design KP — deprecation section")
    class ApiDeprecation {

        @Test
        @DisplayName("contains API Deprecation Strategy"
                + " section")
        void apiDesign_whenGenerated_containsDeprecation(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            new SkillsAssembler().assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String content = readSkill(outputDir,
                    "api-design/SKILL.md");
            assertThat(content)
                    .contains("## API Deprecation Strategy");
        }

        @Test
        @DisplayName("contains Sunset header RFC 8594"
                + " reference")
        void apiDesign_whenGenerated_containsSunsetHeader(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            new SkillsAssembler().assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String content = readSkill(outputDir,
                    "api-design/SKILL.md");
            assertThat(content)
                    .contains("RFC 8594")
                    .contains("Sunset");
        }

        @Test
        @DisplayName("contains 4 deprecation timeline"
                + " phases")
        void apiDesign_whenGenerated_containsTimelinePhases(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            new SkillsAssembler().assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String content = readSkill(outputDir,
                    "api-design/SKILL.md");
            assertThat(content)
                    .contains("Announce")
                    .contains("Warn")
                    .contains("Sunset")
                    .contains("Remove");
        }

        @Test
        @DisplayName("contains deprecation checklist"
                + " reference")
        void apiDesign_whenGenerated_containsChecklistRef(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            new SkillsAssembler().assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String content = readSkill(outputDir,
                    "api-design/SKILL.md");
            assertThat(content).contains(
                    "api-deprecation-checklist.md");
        }
    }

    @Nested
    @DisplayName("API Design KP — versioning section")
    class ApiVersioning {

        @Test
        @DisplayName("contains API Versioning Patterns"
                + " section")
        void apiDesign_whenGenerated_containsVersioning(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            new SkillsAssembler().assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String content = readSkill(outputDir,
                    "api-design/SKILL.md");
            assertThat(content)
                    .contains("## API Versioning Patterns");
        }

        @Test
        @DisplayName("contains 4 versioning patterns")
        void apiDesign_whenGenerated_containsFourPatterns(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            new SkillsAssembler().assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String content = readSkill(outputDir,
                    "api-design/SKILL.md");
            assertThat(content)
                    .contains("URI Versioning")
                    .contains("Header Versioning")
                    .contains("Query Parameter Versioning")
                    .contains("Content Negotiation");
        }
    }

    @Nested
    @DisplayName("API Design KP — backward compatibility")
    class BackwardCompat {

        @Test
        @DisplayName("preserves existing content after"
                + " extension")
        void apiDesign_whenExtended_preservesExisting(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            new SkillsAssembler().assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String content = readSkill(outputDir,
                    "api-design/SKILL.md");
            assertThat(content)
                    .contains("# Knowledge Pack: API Design")
                    .contains("## Purpose")
                    .contains("## Quick Reference")
                    .contains("## Detailed References")
                    .contains("rest-conventions.md")
                    .contains("grpc-conventions.md");
        }
    }

    @Nested
    @DisplayName("API Design KP — reference files")
    class ApiDesignReferences {

        @Test
        @DisplayName("generates deprecation checklist"
                + " reference file")
        void apiDesign_whenGenerated_hasChecklistFile(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            new SkillsAssembler().assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve(
                    "skills/api-design/references/"
                            + "api-deprecation-checklist.md"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("FinOps KP — conditional generation")
    class FinOpsConditional {

        @Test
        @DisplayName("generated when cloud provider is aws")
        void finOps_awsProvider_generated(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder.builder()
                    .cloudProvider("aws")
                    .build();
            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve(
                    "skills/finops/SKILL.md")).exists();
        }

        @Test
        @DisplayName("NOT generated when cloud provider"
                + " is none")
        void finOps_noneProvider_notGenerated(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder.builder()
                    .cloudProvider("none")
                    .build();
            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve(
                    "skills/finops")).doesNotExist();
        }

        @Test
        @DisplayName("contains required FinOps sections")
        void finOps_whenGenerated_containsSections(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder.builder()
                    .cloudProvider("aws")
                    .build();
            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), outputDir);
            String content = readSkill(outputDir,
                    "finops/SKILL.md");
            assertThat(content)
                    .contains("Resource Rightsizing")
                    .contains("Cost Allocation")
                    .contains("Spot")
                    .contains("Reserved Capacity")
                    .contains("Cost Alerting")
                    .contains("FinOps Practices");
        }

        @Test
        @DisplayName("generates cost optimization"
                + " checklist reference")
        void finOps_whenGenerated_hasCostChecklist(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder.builder()
                    .cloudProvider("aws")
                    .build();
            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve(
                    "skills/finops/references/"
                            + "cost-optimization-checklist.md"))
                    .exists();
        }

        @Test
        @DisplayName("generates tagging strategy"
                + " template reference")
        void finOps_whenGenerated_hasTaggingTemplate(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder.builder()
                    .cloudProvider("aws")
                    .build();
            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve(
                    "skills/finops/references/"
                            + "tagging-strategy-template.md"))
                    .exists();
        }

        @Test
        @DisplayName("generated for azure provider too")
        void finOps_azureProvider_generated(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config = TestConfigBuilder.builder()
                    .cloudProvider("azure")
                    .build();
            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve(
                    "skills/finops/SKILL.md")).exists();
        }
    }

    @Nested
    @DisplayName("SkillsSelection — finops pack")
    class FinOpsSelection {

        @Test
        @DisplayName("includes finops when cloud is aws")
        void select_awsCloud_includesFinops() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .cloudProvider("aws")
                    .build();
            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);
            assertThat(packs).contains("finops");
        }

        @Test
        @DisplayName("excludes finops when cloud is none")
        void select_noneCloud_excludesFinops() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .cloudProvider("none")
                    .build();
            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);
            assertThat(packs).doesNotContain("finops");
        }

        @Test
        @DisplayName("includes finops when cloud is gcp")
        void select_gcpCloud_includesFinops() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .cloudProvider("gcp")
                    .build();
            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);
            assertThat(packs).contains("finops");
        }
    }

    private String readSkill(Path outputDir, String path)
            throws IOException {
        return Files.readString(
                outputDir.resolve("skills/" + path),
                StandardCharsets.UTF_8);
    }
}
