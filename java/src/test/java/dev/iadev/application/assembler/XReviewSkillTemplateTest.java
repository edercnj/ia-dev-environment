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
 * Tests for story-0024-0010: x-review SKILL.md changes
 * for template references, dashboard, remediation, and
 * fallback behavior.
 *
 * <p>Validates that the x-review SKILL.md source contains
 * all required sections for RULE-002 (idempotency),
 * RULE-005 (parseable scores), RULE-006 (cumulative
 * dashboard), RULE-007 (template instruction), and
 * RULE-012 (graceful fallback).</p>
 */
@DisplayName("x-review SKILL.md template integration"
        + " (story-0024-0010)")
class XReviewSkillTemplateTest {

    private static final Path CLAUDE_SKILL_PATH =
            resolveClaudeSkillPath();
    private static final Path GITHUB_SKILL_PATH =
            resolveGithubSkillPath();

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
                    .contains("## Phase 0: Idempotency"
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
                + " when reports are valid")
        void preCheck_logsReuseMessage() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Reusing existing review"
                            + " reports from");
        }

        @Test
        @DisplayName("execution flow shows PRE-CHECK"
                + " as Phase 0")
        void executionFlow_showsPreCheckAsPhase0() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("0. PRE-CHECK");
        }
    }

    @Nested
    @DisplayName("Phase 2: Template Reference"
            + " (RULE-007)")
    class TemplateReference {

        @Test
        @DisplayName("subagent prompt includes Step 1b"
                + " to read template")
        void subagentPrompt_includesStep1bTemplate() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Step 1b")
                    .contains("Read Output Template")
                    .contains("_TEMPLATE-SPECIALIST-REVIEW"
                            + ".md");
        }

        @Test
        @DisplayName("template detection check before"
                + " dispatching subagents")
        void templateDetection_beforeDispatching() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("### Template Detection"
                            + " (before dispatching"
                            + " subagents)")
                    .contains("TEMPLATE_AVAILABLE")
                    .contains("TEMPLATE_MISSING");
        }

        @Test
        @DisplayName("score format instruction includes"
                + " XX/YY | Status pattern (RULE-005)")
        void scoreFormat_includesParseablePattern() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("XX/YY | Status: "
                            + "Approved/Rejected/Partial");
        }

        @Test
        @DisplayName("status includes Partial option"
                + " in addition to Approved/Rejected")
        void status_includesPartialOption() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("STATUS = Partial");
        }
    }

    @Nested
    @DisplayName("Phase 3d: Consolidated Dashboard"
            + " (RULE-006)")
    class ConsolidatedDashboard {

        @Test
        @DisplayName("SKILL.md contains Phase 3d"
                + " dashboard generation section")
        void skillMd_containsPhase3dDashboard() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("### 3d. Generate"
                            + " Consolidated Dashboard");
        }

        @Test
        @DisplayName("dashboard checks for template"
                + " availability")
        void dashboard_checksTemplateAvailability() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("_TEMPLATE-CONSOLIDATED-"
                            + "REVIEW-DASHBOARD.md")
                    .contains("DASHBOARD_TEMPLATE_AVAILABLE")
                    .contains("DASHBOARD_TEMPLATE_MISSING");
        }

        @Test
        @DisplayName("dashboard output path follows"
                + " naming convention")
        void dashboard_outputPathFollowsConvention() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("dashboard-story-XXXX-YYYY"
                            + ".md");
        }

        @Test
        @DisplayName("dashboard is cumulative with"
                + " Review History rounds (RULE-006)")
        void dashboard_isCumulativeWithRounds() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("cumulative")
                    .contains("RULE-006")
                    .contains("Round N");
        }

        @Test
        @DisplayName("Tech Lead Score placeholder"
                + " for x-review-pr update")
        void dashboard_techLeadScorePlaceholder() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("--/{review_max_score}"
                            + " | Status: Pending")
                    .contains("x-review-pr");
        }

        @Test
        @DisplayName("dashboard skip with warning"
                + " when template missing")
        void dashboard_skipWithWarningWhenMissing() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Dashboard template not"
                            + " found, skipping dashboard"
                            + " generation");
        }
    }

    @Nested
    @DisplayName("Phase 3e: Remediation Tracking")
    class RemediationTracking {

        @Test
        @DisplayName("SKILL.md contains Phase 3e"
                + " remediation section")
        void skillMd_containsPhase3eRemediation() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("### 3e. Generate"
                            + " Remediation Tracking");
        }

        @Test
        @DisplayName("remediation checks for template"
                + " availability")
        void remediation_checksTemplateAvailability() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("_TEMPLATE-REVIEW-"
                            + "REMEDIATION.md")
                    .contains("REMEDIATION_TEMPLATE_"
                            + "AVAILABLE")
                    .contains("REMEDIATION_TEMPLATE_"
                            + "MISSING");
        }

        @Test
        @DisplayName("remediation output path follows"
                + " naming convention")
        void remediation_outputPathFollowsConvention() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("remediation-story-XXXX-"
                            + "YYYY.md");
        }

        @Test
        @DisplayName("remediation extracts FAILED"
                + " and PARTIAL findings")
        void remediation_extractsFailedAndPartial() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("FAILED or PARTIAL");
        }

        @Test
        @DisplayName("findings use sequential FIND-NNN"
                + " IDs")
        void remediation_findingsUseSequentialIds() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("FIND-NNN");
        }

        @Test
        @DisplayName("all findings initialized as Open")
        void remediation_findingsInitializedAsOpen() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("initialized as `Open`");
        }

        @Test
        @DisplayName("remediation skip with warning"
                + " when template missing")
        void remediation_skipWithWarningWhenMissing() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Remediation template not"
                            + " found, skipping"
                            + " remediation tracking"
                            + " generation");
        }
    }

    @Nested
    @DisplayName("Fallback Behavior (RULE-012)")
    class FallbackBehavior {

        @Test
        @DisplayName("SKILL.md documents RULE-012"
                + " fallback for specialist template")
        void skillMd_documentsRule012Fallback() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Fallback (RULE-012)")
                    .contains("pre-EPIC-0024 projects");
        }

        @Test
        @DisplayName("fallback uses inline format"
                + " when template absent")
        void fallback_usesInlineFormatWhenAbsent() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("inline format");
        }

        @Test
        @DisplayName("fallback logs warning when"
                + " template not found")
        void fallback_logsWarningWhenNotFound() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Template not found,"
                            + " using inline format");
        }

        @Test
        @DisplayName("integration notes mention fallback"
                + " for dashboard and remediation")
        void integrationNotes_mentionFallbackSkip() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("dashboard/remediation"
                            + " are skipped");
        }
    }

    @Nested
    @DisplayName("GitHub Copilot Variant Consistency")
    class GithubVariantConsistency {

        @Test
        @DisplayName("GitHub skill also contains"
                + " Phase 0 pre-check")
        void githubSkill_containsPhase0PreCheck() {
            String content = readSkill(GITHUB_SKILL_PATH);

            assertThat(content)
                    .contains("Phase 0: Idempotency"
                            + " Pre-Check");
        }

        @Test
        @DisplayName("GitHub skill contains Phase 3d"
                + " dashboard")
        void githubSkill_containsPhase3dDashboard() {
            String content = readSkill(GITHUB_SKILL_PATH);

            assertThat(content)
                    .contains("3d. Generate Consolidated"
                            + " Dashboard");
        }

        @Test
        @DisplayName("GitHub skill contains Phase 3e"
                + " remediation")
        void githubSkill_containsPhase3eRemediation() {
            String content = readSkill(GITHUB_SKILL_PATH);

            assertThat(content)
                    .contains("3e. Generate Remediation"
                            + " Tracking");
        }

        @Test
        @DisplayName("GitHub skill contains template"
                + " detection")
        void githubSkill_containsTemplateDetection() {
            String content = readSkill(GITHUB_SKILL_PATH);

            assertThat(content)
                    .contains("Template Detection")
                    .contains("TEMPLATE_AVAILABLE");
        }

        @Test
        @DisplayName("GitHub skill contains Step 1b"
                + " template reference")
        void githubSkill_containsStep1bTemplate() {
            String content = readSkill(GITHUB_SKILL_PATH);

            assertThat(content)
                    .contains("Step 1b")
                    .contains("Read Output Template");
        }
    }

    @Nested
    @DisplayName("Phase Numbering Consistency")
    class PhaseNumberingConsistency {

        @Test
        @DisplayName("Threat Model Update renumbered"
                + " to 3f after new phases")
        void threatModelUpdate_renumberedTo3f() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("### 3f. Threat Model"
                            + " Update");
        }

        @Test
        @DisplayName("phases ordered: 3c reports,"
                + " 3d dashboard, 3e remediation,"
                + " 3f threat model")
        void phases_orderedCorrectly() {
            String content = readSkill(CLAUDE_SKILL_PATH);
            int pos3c = content.indexOf(
                    "### 3c. Save Individual Reports");
            int pos3d = content.indexOf(
                    "### 3d. Generate Consolidated"
                            + " Dashboard");
            int pos3e = content.indexOf(
                    "### 3e. Generate Remediation"
                            + " Tracking");
            int pos3f = content.indexOf(
                    "### 3f. Threat Model Update");

            assertThat(pos3c)
                    .as("3c must appear before 3d")
                    .isLessThan(pos3d);
            assertThat(pos3d)
                    .as("3d must appear before 3e")
                    .isLessThan(pos3e);
            assertThat(pos3e)
                    .as("3e must appear before 3f")
                    .isLessThan(pos3f);
        }
    }

    private static Path resolveClaudeSkillPath() {
        Path path = Path.of(
                "src/main/resources/targets/claude/"
                        + "skills/core/x-review/SKILL.md");
        if (!Files.exists(path)) {
            path = Path.of(
                    "java/src/main/resources/targets/"
                            + "claude/skills/core/"
                            + "x-review/SKILL.md");
        }
        return path;
    }

    private static Path resolveGithubSkillPath() {
        Path path = Path.of(
                "src/main/resources/targets/"
                        + "github-copilot/skills/"
                        + "review/x-review.md");
        if (!Files.exists(path)) {
            path = Path.of(
                    "java/src/main/resources/targets/"
                            + "github-copilot/skills/"
                            + "review/x-review.md");
        }
        return path;
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
