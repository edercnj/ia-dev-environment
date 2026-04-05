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
 * Tests for story-0016-0011: PCI-DSS requirements
 * knowledge pack with 12 PCI-DSS v4.0 requirements
 * mapped to Java code practices.
 *
 * <p>Covers Gherkin scenarios @GK-1 through @GK-5.</p>
 */
@DisplayName("PCI-DSS Requirements KP")
class PciDssRequirementsKpTest {

    @Nested
    @DisplayName("KnowledgePackSelection — pci-dss pack")
    class PciDssSelection {

        @Test
        @DisplayName("includes pci-dss-requirements when"
                + " compliance contains pci-dss")
        void select_pciDssCompliance_includesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .contains("pci-dss-requirements");
        }

        @Test
        @DisplayName("includes pci-dss-requirements when"
                + " compliance has pci-dss among others")
        void select_multipleCompliance_includesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks(
                                    "pci-dss", "lgpd")
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .contains("pci-dss-requirements");
        }

        @Test
        @DisplayName("excludes pci-dss-requirements when"
                + " compliance is empty")
        void select_noCompliance_excludesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .doesNotContain("pci-dss-requirements");
        }

        @Test
        @DisplayName("excludes pci-dss-requirements when"
                + " compliance has lgpd only")
        void select_lgpdOnly_excludesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("lgpd")
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .doesNotContain("pci-dss-requirements");
        }
    }

    @Nested
    @DisplayName("@GK-1: No residual template variables")
    class NoResidualVariables {

        @Test
        @DisplayName("rendered output contains no Pebble"
                + " template markers")
        void render_pciDssKp_noResidualVariables(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");
            assertThat(content)
                    .doesNotContain("{{")
                    .doesNotContain("}}")
                    .doesNotContain("{%");
        }
    }

    @Nested
    @DisplayName("@GK-2: All 12 PCI-DSS requirements")
    class TwelveRequirements {

        @Test
        @DisplayName("contains 12 requirement sections")
        void render_pciDssKp_hasTwelveRequirements(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");

            for (int i = 1; i <= 12; i++) {
                assertThat(content)
                        .as("Requirement %d must be present",
                                i)
                        .contains(
                                "Requirement " + i + ":");
            }
        }

        @Test
        @DisplayName("each requirement N is from 1 to 12")
        void render_pciDssKp_requirementsOneToTwelve(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");

            long reqCount = content.lines()
                    .filter(l -> l.matches(
                            "## PCI-DSS v4\\.0.*"
                                    + "Requirement \\d+:.*"))
                    .count();
            assertThat(reqCount)
                    .as("Must have exactly 12 requirement"
                            + " sections")
                    .isEqualTo(12);
        }
    }

    @Nested
    @DisplayName("@GK-3: Code examples per requirement")
    class CodeExamples {

        @Test
        @DisplayName("requirement 3 has prohibited example")
        void render_req3_hasProhibitedExample(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");
            assertThat(content)
                    .contains("PROIBIDO:");
        }

        @Test
        @DisplayName("requirement 3 has correct example")
        void render_req3_hasCorrectExample(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");
            assertThat(content)
                    .contains("CORRETO:");
        }

        @Test
        @DisplayName("every mappable requirement has both"
                + " prohibited and correct examples")
        void render_mappableReqs_haveBothExamples(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");

            long prohibitedCount = content.lines()
                    .filter(l -> l.contains("PROIBIDO:"))
                    .count();
            long correctCount = content.lines()
                    .filter(l -> l.contains("CORRETO:"))
                    .count();
            assertThat(prohibitedCount)
                    .as("Must have >= 10 prohibited"
                            + " examples (reqs 1-8, 10-11)")
                    .isGreaterThanOrEqualTo(10);
            assertThat(correctCount)
                    .as("Must have >= 10 correct examples")
                    .isGreaterThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("@GK-4: Review checklists")
    class ReviewChecklists {

        @Test
        @DisplayName("each mappable requirement has"
                + " reviewer checklist section")
        void render_mappableReqs_haveChecklist(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");

            long checklistSections = content.lines()
                    .filter(l -> l.contains(
                            "code reviewer deve checar"))
                    .count();
            assertThat(checklistSections)
                    .as("Must have >= 10 checklist sections"
                            + " (reqs 1-8, 10-11)")
                    .isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("each checklist has at least 2 items")
        void render_checklists_haveMinTwoItems(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");

            long checklistItems = content.lines()
                    .filter(l -> l.trim().startsWith("- [ ]"))
                    .count();
            assertThat(checklistItems)
                    .as("Must have >= 20 checklist items"
                            + " (2+ per 10 mappable reqs)")
                    .isGreaterThanOrEqualTo(20);
        }
    }

    @Nested
    @DisplayName("@GK-5: Organizational requirements")
    class OrganizationalRequirements {

        @Test
        @DisplayName("requirement 9 has organizational note")
        void render_req9_hasOrganizationalNote(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");

            assertThat(content).contains(
                    "organizacional");
        }

        @Test
        @DisplayName("requirement 12 has organizational note")
        void render_req12_hasOrganizationalNote(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");

            String req12Section = extractRequirement(
                    content, 12);
            assertThat(req12Section)
                    .contains("organizacional");
        }

        @Test
        @DisplayName("requirement 9 does not contain"
                + " code examples")
        void render_req9_noCodeExamples(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");

            String req9Section = extractRequirement(
                    content, 9);
            assertThat(req9Section)
                    .doesNotContain("PROIBIDO:")
                    .doesNotContain("CORRETO:");
        }
    }

    @Nested
    @DisplayName("Frontmatter and structure")
    class FrontmatterAndStructure {

        @Test
        @DisplayName("has correct frontmatter name"
                + " and user-invocable false")
        void render_pciDssKp_hasCorrectFrontmatter(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "pci-dss-requirements/SKILL.md");
            assertThat(content)
                    .contains("name: pci-dss-requirements")
                    .contains("user-invocable: false");
        }

        @Test
        @DisplayName("not generated when compliance"
                + " does not include pci-dss")
        void render_noCompliance_notGenerated(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            Path kpDir = tempDir.resolve(
                    "skills/pci-dss-requirements");
            assertThat(kpDir).doesNotExist();
        }
    }

    private String readSkill(Path outputDir, String path)
            throws IOException {
        return Files.readString(
                outputDir.resolve("skills/" + path),
                StandardCharsets.UTF_8);
    }

    private String extractRequirement(
            String content, int reqNum) {
        String marker = "## PCI-DSS v4.0";
        String reqMarker = "Requirement " + reqNum + ":";
        String nextMarker =
                "## PCI-DSS v4.0";
        int start = content.indexOf(reqMarker);
        if (start < 0) {
            return "";
        }
        int nextStart = content.indexOf(
                nextMarker, start + reqMarker.length());
        if (nextStart < 0) {
            return content.substring(start);
        }
        return content.substring(start, nextStart);
    }
}
