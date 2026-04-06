package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
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
 * Tests for x-container-scan skill: conditional inclusion,
 * SKILL.md content, and scanning selection.
 */
@DisplayName("Container Scan Skill — x-container-scan")
class ContainerScanSkillTest {

    @Nested
    @DisplayName("selectSecurityScanningSkills")
    class SelectSecurityScanningSkills {

        @Test
        @DisplayName("containerScan true includes"
                + " x-container-scan")
        void select_containerScanTrue_includesScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .containerScan(true)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills)
                    .containsExactly("x-container-scan");
        }

        @Test
        @DisplayName("containerScan false returns empty")
        void select_containerScanFalse_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .containerScan(false)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("default config returns empty"
                + " scanning skills")
        void select_defaultConfig_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills).isEmpty();
        }
    }

    @Nested
    @DisplayName("selectConditionalSkills integration")
    class ConditionalIntegration {

        @Test
        @DisplayName("containerScan true appears in"
                + " aggregated conditional skills")
        void select_containerScanTrue_inConditional() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .containerScan(true)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .contains("x-container-scan");
        }

        @Test
        @DisplayName("containerScan false excluded from"
                + " aggregated conditional skills")
        void select_containerScanFalse_notInConditional() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .containerScan(false)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .doesNotContain("x-container-scan");
        }
    }

    @Nested
    @DisplayName("SKILL.md assembly")
    class SkillMdAssembly {

        @Test
        @DisplayName("containerScan true generates"
                + " SKILL.md file")
        void assemble_containerScanTrue_skillMdExists(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .containerScan(true)
                            .build();
            SkillsAssembler assembler =
                    new SkillsAssembler();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path skillMd = outputDir.resolve(
                    "skills/x-container-scan/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("containerScan false does not"
                + " generate SKILL.md")
        void assemble_containerScanFalse_noSkillMd(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .containerScan(false)
                            .build();
            SkillsAssembler assembler =
                    new SkillsAssembler();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path skillMd = outputDir.resolve(
                    "skills/x-container-scan/SKILL.md");
            assertThat(skillMd).doesNotExist();
        }

        @Test
        @DisplayName("generated SKILL.md contains"
                + " x-container-scan name")
        void assemble_skillMd_containsName(
                @TempDir Path tempDir) throws IOException {
            String content =
                    generateContainerScanContent(tempDir);

            assertThat(content)
                    .contains("name: x-container-scan");
        }

        @Test
        @DisplayName("generated SKILL.md contains"
                + " Trivy tool reference")
        void assemble_skillMd_containsTrivy(
                @TempDir Path tempDir) throws IOException {
            String content =
                    generateContainerScanContent(tempDir);

            assertThat(content)
                    .contains("Trivy");
        }

        @Test
        @DisplayName("generated SKILL.md contains"
                + " Grype fallback reference")
        void assemble_skillMd_containsGrype(
                @TempDir Path tempDir) throws IOException {
            String content =
                    generateContainerScanContent(tempDir);

            assertThat(content)
                    .contains("Grype");
        }

        @Test
        @DisplayName("generated SKILL.md contains"
                + " SARIF output format")
        void assemble_skillMd_containsSarif(
                @TempDir Path tempDir) throws IOException {
            String content =
                    generateContainerScanContent(tempDir);

            assertThat(content)
                    .contains("SARIF")
                    .contains("2.1.0");
        }

        @Test
        @DisplayName("generated SKILL.md contains"
                + " Dockerfile checks")
        void assemble_skillMd_containsDockerfileChecks(
                @TempDir Path tempDir) throws IOException {
            String content =
                    generateContainerScanContent(tempDir);

            assertThat(content)
                    .contains("root-user")
                    .contains("secrets-in-layers")
                    .contains("latest-tag")
                    .contains("no-multi-stage")
                    .contains("unnecessary-packages")
                    .contains("excessive-permissions")
                    .contains("missing-healthcheck");
        }

        @Test
        @DisplayName("generated SKILL.md contains"
                + " score and grade assignment")
        void assemble_skillMd_containsScoring(
                @TempDir Path tempDir) throws IOException {
            String content =
                    generateContainerScanContent(tempDir);

            assertThat(content)
                    .contains("Score")
                    .contains("Grade");
        }

        @Test
        @DisplayName("generated SKILL.md contains"
                + " --ignore-unfixed parameter")
        void assemble_skillMd_containsIgnoreUnfixed(
                @TempDir Path tempDir) throws IOException {
            String content =
                    generateContainerScanContent(tempDir);

            assertThat(content)
                    .contains("--ignore-unfixed");
        }

        @Test
        @DisplayName("generated SKILL.md contains"
                + " CI integration section")
        void assemble_skillMd_containsCiIntegration(
                @TempDir Path tempDir) throws IOException {
            String content =
                    generateContainerScanContent(tempDir);

            assertThat(content)
                    .contains("CI Integration");
        }
    }

    private String generateContainerScanContent(
            Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        ProjectConfig config =
                TestConfigBuilder.builder()
                        .containerScan(true)
                        .build();
        SkillsAssembler assembler = new SkillsAssembler();
        assembler.assemble(
                config, new TemplateEngine(), outputDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/x-container-scan/SKILL.md"),
                StandardCharsets.UTF_8);
    }
}
