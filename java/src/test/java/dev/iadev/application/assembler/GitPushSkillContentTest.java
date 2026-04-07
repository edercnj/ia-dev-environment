package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.template.TemplateEngine;
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
 * Tests for x-git-push SKILL.md content.
 *
 * <p>Validates that the generated skill uses {@code develop}
 * as the default base branch for feature workflows and
 * includes a separate hotfix workflow branching from
 * {@code main}.
 */
@DisplayName("x-git-push — develop base + hotfix")
class GitPushSkillContentTest {

    private static final String SKILL_PATH =
            "skills/x-git-push/SKILL.md";

    private String generateSkillContent(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler = new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return Files.readString(
                outputDir.resolve(SKILL_PATH),
                StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("feature flow uses develop")
    class FeatureFlowUsesDevelop {

        @Test
        @DisplayName("branch creation uses develop")
        void assemble_branchCreation_usesDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("git checkout develop");
            assertThat(content)
                    .contains("git pull origin develop");
        }

        @Test
        @DisplayName("review diff uses develop")
        void assemble_reviewDiff_usesDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("git diff develop...HEAD");
            assertThat(content)
                    .contains(
                            "git log --oneline develop..HEAD");
        }

        @Test
        @DisplayName("PR creation has --base develop")
        void assemble_prCreation_hasBaseDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("--base develop");
        }

        @Test
        @DisplayName("no checkout main in feature flow")
        void assemble_featureFlow_noCheckoutMain(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSkillContent(tempDir);
            String featureSection = extractSection(
                    content,
                    "## Workflow Per Story",
                    "## Hotfix Workflow");
            assertThat(featureSection)
                    .doesNotContain("checkout main");
        }
    }

    @Nested
    @DisplayName("hotfix workflow present")
    class HotfixWorkflow {

        @Test
        @DisplayName("hotfix section exists")
        void assemble_hotfixSection_exists(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("## Hotfix Workflow");
        }

        @Test
        @DisplayName("hotfix branches from main")
        void assemble_hotfix_branchesFromMain(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSkillContent(tempDir);
            String hotfixSection = extractSection(
                    content,
                    "## Hotfix Workflow",
                    "## Pull Request");
            assertThat(hotfixSection)
                    .contains("git checkout main");
            assertThat(hotfixSection)
                    .contains("--base main");
        }

        @Test
        @DisplayName("hotfix includes back-merge")
        void assemble_hotfix_includesBackMerge(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSkillContent(tempDir);
            String hotfixSection = extractSection(
                    content,
                    "## Hotfix Workflow",
                    "## Pull Request");
            assertThat(hotfixSection)
                    .contains("back-merge");
            assertThat(hotfixSection)
                    .contains("--base develop");
        }
    }

    @Nested
    @DisplayName("branch strategy diagram")
    class BranchStrategyDiagram {

        @Test
        @DisplayName("diagram shows develop as integration")
        void assemble_diagram_showsDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content).contains(
                    "develop (integration, always green)");
        }

        @Test
        @DisplayName("diagram shows main as production")
        void assemble_diagram_showsMainAsProduction(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content).contains(
                    "main (production, tagged releases only)");
        }
    }

    @Nested
    @DisplayName("integration notes updated")
    class IntegrationNotes {

        @Test
        @DisplayName("references develop in lifecycle")
        void assemble_integrationNotes_referencesDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSkillContent(tempDir);
            String notesSection = extractSection(
                    content,
                    "## Integration Notes",
                    null);
            assertThat(notesSection)
                    .contains("branch from `develop`");
            assertThat(notesSection)
                    .contains("PR to `develop`");
        }
    }

    private static String extractSection(
            String content, String start, String end) {
        int startIdx = content.indexOf(start);
        if (startIdx < 0) {
            return "";
        }
        if (end == null) {
            return content.substring(startIdx);
        }
        int endIdx = content.indexOf(end, startIdx);
        if (endIdx < 0) {
            return content.substring(startIdx);
        }
        return content.substring(startIdx, endIdx);
    }
}
