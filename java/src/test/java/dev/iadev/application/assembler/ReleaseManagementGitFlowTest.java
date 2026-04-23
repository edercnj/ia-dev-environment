package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0027-0008: Release Management KP
 * with Git Flow as default recommendation.
 *
 * <p>Validates that the release-management knowledge
 * pack positions Git Flow as the recommended branching
 * strategy, maintains alternatives as documented
 * options, and cross-references Rule 09.</p>
 */
@Disabled("EPIC-0051 complete: SkillsAssembler no longer emits KP output under .claude/skills/{kp}/; replaced by KnowledgePackMigrationSmokeTest + KnowledgeAssemblerTest on the new .claude/knowledge/ layout. See ADR-0013.")
@DisplayName("Release Management KP — Git Flow Default")
class ReleaseManagementGitFlowTest {

    @Nested
    @DisplayName("Claude SKILL.md — Git Flow"
            + " as Recommended")
    class GitFlowRecommended {

        @Test
        @DisplayName("GitFlow appears first in strategy"
                + " comparison table")
        void assemble_strategyComparison_gitFlowFirst(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int gitFlowIdx =
                    content.indexOf("GitFlow (Recommended)");
            int trunkIdx =
                    content.indexOf("Trunk-based");
            int relBranchIdx =
                    content.indexOf("Release branches");
            assertThat(gitFlowIdx)
                    .as("GitFlow must appear first")
                    .isGreaterThan(-1)
                    .isLessThan(trunkIdx)
                    .isLessThan(relBranchIdx);
        }

        @Test
        @DisplayName("GitFlow is marked as Recommended"
                + " in strategy table")
        void assemble_strategyComparison_markedRecommended(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("GitFlow (Recommended)");
        }

        @Test
        @DisplayName("Trunk-based is marked as"
                + " Alternative")
        void assemble_strategyComparison_trunkAlternative(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "Alternative for CI/CD-focused teams");
        }

        @Test
        @DisplayName("Release branches is marked as"
                + " simplified Alternative")
        void assemble_strategyComparison_relBranchAlt(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "Simplified alternative");
        }

        @Test
        @DisplayName("strategy table has Recommendation"
                + " column")
        void assemble_strategyTable_hasRecommendation(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("| Recommendation |");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Decision Matrix")
    class DecisionMatrix {

        @Test
        @DisplayName("decision matrix has GitFlow"
                + " as default")
        void assemble_decisionMatrix_gitFlowDefault(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("GitFlow (default)");
        }

        @Test
        @DisplayName("decision matrix marks compliance"
                + " as GitFlow mandatory")
        void assemble_decisionMatrix_complianceMandatory(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("GitFlow (mandatory)");
        }

        @Test
        @DisplayName("decision matrix marks trunk-based"
                + " as alternative")
        void assemble_decisionMatrix_trunkAlternative(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Trunk-based (alternative)");
        }

        @Test
        @DisplayName("decision matrix includes default"
                + " for all new projects")
        void assemble_decisionMatrix_defaultForNew(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "Default for all new projects");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Rule 09"
            + " Cross-Reference")
    class Rule09CrossRef {

        @Test
        @DisplayName("contains cross-reference to"
                + " Rule 09")
        void assemble_crossRef_containsRule09(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Rule 09");
        }

        @Test
        @DisplayName("cross-reference mentions"
                + " 09-branching-model")
        void assemble_crossRef_mentionsBranchingModel(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("09-branching-model");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Alternatives"
            + " Preserved")
    class AlternativesPreserved {

        @Test
        @DisplayName("Trunk-Based Development is"
                + " still documented")
        void assemble_alternatives_trunkDocumented(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Trunk-based");
        }

        @Test
        @DisplayName("Release branches is still"
                + " documented")
        void assemble_alternatives_relBranchDocumented(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Release branches");
        }

        @Test
        @DisplayName("all three strategies are present")
        void assemble_alternatives_allThreePresent(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("GitFlow")
                    .contains("Trunk-based")
                    .contains("Release branches");
        }
    }

    @Nested
    @DisplayName("Branching Guide — Git Flow Default")
    class BranchingGuide {

        @Test
        @DisplayName("branching guide has default"
                + " recommendation section")
        void assemble_guide_hasDefaultRecommendation(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateBranchingGuide(tempDir);
            assertThat(content)
                    .contains("## Default Recommendation")
                    .contains("GitFlow is the recommended");
        }

        @Test
        @DisplayName("branching guide has GitFlow"
                + " as first strategy detail")
        void assemble_guide_gitFlowFirstStrategy(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateBranchingGuide(tempDir);
            int gitFlowIdx = content.indexOf(
                    "### GitFlow (Recommended)");
            int trunkIdx = content.indexOf(
                    "### Trunk-Based Development"
                            + " (Alternative)");
            int relIdx = content.indexOf(
                    "### Release Branches (Alternative)");
            assertThat(gitFlowIdx)
                    .as("GitFlow must appear first"
                            + " in strategy details")
                    .isGreaterThan(-1)
                    .isLessThan(trunkIdx)
                    .isLessThan(relIdx);
        }

        @Test
        @DisplayName("branching guide cross-references"
                + " Rule 09")
        void assemble_guide_crossRefsRule09(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateBranchingGuide(tempDir);
            assertThat(content)
                    .contains("Rule 09")
                    .contains("09-branching-model");
        }

        @Test
        @DisplayName("branching guide decision matrix"
                + " has GitFlow as recommended")
        void assemble_guide_matrixGitFlowRecommended(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateBranchingGuide(tempDir);
            assertThat(content)
                    .contains("GitFlow (Recommended)")
                    .contains("GitFlow (Mandatory)");
        }
    }

    private Path generateClaudeOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return outputDir;
    }

    private String generateClaudeContent(Path tempDir)
            throws IOException {
        Path outputDir = generateClaudeOutput(tempDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/release-management/"
                                + "SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private String generateBranchingGuide(Path tempDir)
            throws IOException {
        Path outputDir = generateClaudeOutput(tempDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/release-management/"
                                + "references/"
                                + "release-branching"
                                + "-guide.md"),
                StandardCharsets.UTF_8);
    }

}
