package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0024-0011: x-review-pr SKILL.md changes
 * for template reference, dashboard update, remediation
 * update, idempotency pre-check, and fallback behavior.
 *
 * <p>Validates that the x-review-pr SKILL.md source
 * contains all required sections for RULE-002
 * (idempotency), RULE-005 (parseable scores), RULE-006
 * (cumulative dashboard update), RULE-007 (template
 * instruction), RULE-011 (standardized header), and
 * RULE-012 (graceful fallback).</p>
 */
@DisplayName("x-review-pr SKILL.md template integration"
        + " (story-0024-0011)")
class XReviewPrSkillTemplateTest {

    private static final Path CLAUDE_SKILL_PATH =
            resolveClaudeSkillPath();

    @Nested
    @DisplayName("Phase 0: Idempotency Pre-Check"
            + " (RULE-002)")
    class IdempotencyPreCheck {

        @Test
        @DisplayName("SKILL.md contains Phase 0"
                + " pre-check section")
        void skillMd_containsPhase0PreCheck() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("### Step 0 \u2014 Idempotency"
                            + " Pre-Check");
        }

        @Test
        @DisplayName("pre-check verifies report mtime"
                + " vs commit date")
        void preCheck_verifiesMtimeVsCommitDate() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("mtime(report)")
                    .contains("commit_date");
        }

        @Test
        @DisplayName("pre-check logs reuse message"
                + " when report is valid")
        void preCheck_logsReuseMessage() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Reusing existing tech lead"
                            + " review from");
        }

        @Test
        @DisplayName("execution flow shows PRE-CHECK"
                + " as first step")
        void executionFlow_showsPreCheckAsFirstStep() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("0. PRE-CHECK");
        }
    }

    @Nested
    @DisplayName("Template Reference (RULE-007)")
    class TemplateReference {

        @Test
        @DisplayName("Step 2 includes instruction to read"
                + " Tech Lead review template")
        void step2_includesTemplateReadInstruction() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("_TEMPLATE-TECH-LEAD-REVIEW"
                            + ".md");
        }

        @Test
        @DisplayName("template detection check before"
                + " executing review")
        void templateDetection_beforeExecutingReview() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("TL_TEMPLATE_AVAILABLE")
                    .contains("TL_TEMPLATE_MISSING");
        }

        @Test
        @DisplayName("template instruction uses explicit"
                + " RULE-007 wording")
        void templateInstruction_usesExplicitWording() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Read template at"
                            + " `.claude/templates/"
                            + "_TEMPLATE-TECH-LEAD-REVIEW"
                            + ".md` for required output"
                            + " format");
        }
    }

    @Nested
    @DisplayName("Score Format (RULE-005)")
    class ScoreFormat {

        @Test
        @DisplayName("score format is parseable XX/YY"
                + " with GO/NO-GO status")
        void scoreFormat_isParseableWithGoStatus() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("XX/"
                            + "{review_max_score}")
                    .contains("GO")
                    .contains("NO-GO");
        }
    }

    @Nested
    @DisplayName("Standardized Header (RULE-011)")
    class StandardizedHeader {

        @Test
        @DisplayName("report output mentions"
                + " standardized header fields")
        void reportOutput_mentionsHeaderFields() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Story ID")
                    .contains("Date")
                    .contains("Template Version");
        }
    }

    @Nested
    @DisplayName("Step 5: Dashboard Update"
            + " (RULE-006)")
    class DashboardUpdate {

        @Test
        @DisplayName("SKILL.md contains dashboard update"
                + " step")
        void skillMd_containsDashboardUpdateStep() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("### Step 5 \u2014 Update"
                            + " Consolidated Dashboard");
        }

        @Test
        @DisplayName("dashboard update reads existing"
                + " dashboard file")
        void dashboardUpdate_readsExistingDashboard() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("dashboard-story-XXXX-YYYY"
                            + ".md");
        }

        @Test
        @DisplayName("dashboard update adds Tech Lead"
                + " Score row")
        void dashboardUpdate_addsTechLeadScore() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Tech Lead Score")
                    .contains("Tech Lead")
                    .containsPattern(
                            "(?i)add.*tech lead.*score"
                                    + "|update.*tech lead"
                                    + ".*score");
        }

        @Test
        @DisplayName("dashboard update recalculates"
                + " overall status")
        void dashboardUpdate_recalculatesOverallStatus() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .containsPattern(
                            "(?i)overall.*status"
                                    + ".*recalculate"
                                    + "|recalculate.*overall"
                                    + ".*status"
                                    + "|update.*overall"
                                    + ".*status"
                                    + "|status.*updated"
                                    + ".*specialist.*tech");
        }

        @Test
        @DisplayName("dashboard update increments"
                + " round in Review History")
        void dashboardUpdate_incrementsRound() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Review History")
                    .containsPattern(
                            "(?i)round|append.*round"
                                    + "|new.*round"
                                    + "|increment.*round");
        }

        @Test
        @DisplayName("dashboard is cumulative per"
                + " RULE-006")
        void dashboard_isCumulativePerRule006() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("cumulative")
                    .contains("RULE-006");
        }

        @Test
        @DisplayName("creates fresh dashboard when"
                + " x-review dashboard not found")
        void dashboard_createsFreshWhenNotFound() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Dashboard not found,"
                            + " creating fresh dashboard");
        }
    }

    @Nested
    @DisplayName("Step 6: Remediation Update")
    class RemediationUpdate {

        @Test
        @DisplayName("SKILL.md contains remediation"
                + " update step")
        void skillMd_containsRemediationUpdateStep() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("### Step 6 \u2014 Update"
                            + " Remediation Tracking");
        }

        @Test
        @DisplayName("remediation update reads existing"
                + " remediation file")
        void remediationUpdate_readsExistingFile() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("remediation-story-XXXX-"
                            + "YYYY.md");
        }

        @Test
        @DisplayName("remediation marks fixed findings"
                + " as FIXED")
        void remediation_marksFixedFindingsAsFixed() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .containsPattern(
                            "(?i)open.*->.*fixed"
                                    + "|OPEN.*FIXED"
                                    + "|status.*FIXED"
                                    + "|mark.*FIXED");
        }

        @Test
        @DisplayName("remediation adds new Tech Lead"
                + " findings as OPEN")
        void remediation_addsNewFindingsAsOpen() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .containsPattern(
                            "(?i)new.*finding.*open"
                                    + "|add.*finding.*open"
                                    + "|new.*tech lead"
                                    + ".*finding");
        }

        @Test
        @DisplayName("creates fresh remediation when"
                + " not found")
        void remediation_createsFreshWhenNotFound() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .containsPattern(
                            "(?i)remediation.*not.*found"
                                    + ".*creat"
                                    + "|creat.*remediation"
                                    + ".*tech lead");
        }
    }

    @Nested
    @DisplayName("Fallback Behavior (RULE-012)")
    class FallbackBehavior {

        @Test
        @DisplayName("SKILL.md documents fallback for"
                + " missing Tech Lead template")
        void skillMd_documentsFallbackForTemplate() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Template not found,"
                            + " using inline format");
        }

        @Test
        @DisplayName("fallback skips dashboard and"
                + " remediation updates")
        void fallback_skipsDashboardAndRemediation() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .containsPattern(
                            "(?i)dashboard.*remediation"
                                    + ".*not.*update"
                                    + "|dashboard.*remediation"
                                    + ".*skip"
                                    + "|skip.*dashboard"
                                    + ".*remediation");
        }

        @Test
        @DisplayName("fallback preserves current"
                + " inline behavior")
        void fallback_preservesCurrentInlineBehavior() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("inline format");
        }
    }

    @Nested
    @DisplayName("Step Ordering Consistency")
    class StepOrderingConsistency {

        @Test
        @DisplayName("steps ordered: detect, gather,"
                + " template, review, dashboard,"
                + " remediation, result, no-go")
        void steps_orderedCorrectly() {
            String content = readSkill(CLAUDE_SKILL_PATH);
            int posStep1 = content.indexOf(
                    "### Step 1 \u2014 Detect Context");
            int posStep2 = content.indexOf(
                    "### Step 2 \u2014 Gather Context");
            int posStep3 = content.indexOf(
                    "### Step 3 \u2014 Template Detection");
            int posStep4 = content.indexOf(
                    "### Step 4 \u2014 Execute Tech Lead"
                            + " Review");
            int posStep5 = content.indexOf(
                    "### Step 5 \u2014 Update Consolidated"
                            + " Dashboard");
            int posStep6 = content.indexOf(
                    "### Step 6 \u2014 Update Remediation"
                            + " Tracking");
            int posStep7 = content.indexOf(
                    "### Step 7 \u2014 Process Result");
            int posStep8 = content.indexOf(
                    "### Step 8 \u2014 Handle NO-GO");

            assertThat(posStep1)
                    .as("Step 1 must exist")
                    .isGreaterThan(-1);
            assertThat(posStep1)
                    .as("Step 1 before Step 2")
                    .isLessThan(posStep2);
            assertThat(posStep2)
                    .as("Step 2 before Step 3")
                    .isLessThan(posStep3);
            assertThat(posStep3)
                    .as("Step 3 before Step 4")
                    .isLessThan(posStep4);
            assertThat(posStep4)
                    .as("Step 4 before Step 5")
                    .isLessThan(posStep5);
            assertThat(posStep5)
                    .as("Step 5 before Step 6")
                    .isLessThan(posStep6);
            assertThat(posStep6)
                    .as("Step 6 before Step 7")
                    .isLessThan(posStep7);
            assertThat(posStep7)
                    .as("Step 7 before Step 8")
                    .isLessThan(posStep8);
        }
    }

    private static Path resolveClaudeSkillPath() {
        // Hierarchical SoT (story-0036-0002): x-review-pr
        // lives under core/review/. Legacy flat path is
        // retained as a fallback.
        Path[] candidates = new Path[] {
                Path.of(
                        "src/main/resources/targets/claude/"
                                + "skills/core/review/"
                                + "x-review-pr/SKILL.md"),
                Path.of(
                        "java/src/main/resources/targets/"
                                + "claude/skills/core/review/"
                                + "x-review-pr/SKILL.md"),
                Path.of(
                        "src/main/resources/targets/claude/"
                                + "skills/core/x-review-pr/"
                                + "SKILL.md"),
                Path.of(
                        "java/src/main/resources/targets/"
                                + "claude/skills/core/"
                                + "x-review-pr/SKILL.md")};
        for (Path c : candidates) {
            if (Files.exists(c)) {
                return c;
            }
        }
        return candidates[0];
    }

    private static String readSkill(Path path) {
        try {
            return Files.readString(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read skill: " + path, e);
        }
    }
}
