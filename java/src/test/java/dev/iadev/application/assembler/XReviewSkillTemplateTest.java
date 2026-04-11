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
 * Tests for x-review SKILL.md template integration.
 *
 * <p>Validates the x-review orchestrator SKILL.md source
 * after story-0029-0012 refactoring: inline checklists
 * and KP mapping removed, Phase 2 delegates to individual
 * review skills via Skill tool, Specialist Reference Table
 * added.</p>
 *
 * <p>Validates RULE-002 (idempotency), RULE-005 (parseable
 * scores), RULE-006 (cumulative dashboard), and RULE-012
 * (graceful fallback).</p>
 */
@DisplayName("x-review SKILL.md template integration")
class XReviewSkillTemplateTest {

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
                    .contains("## Phase 0 -- Idempotency"
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
    @DisplayName("Phase 2: Skill Delegation"
            + " (story-0029-0012)")
    class SkillDelegation {

        @Test
        @DisplayName("Phase 2 invokes skills via"
                + " Skill tool, not subagents")
        void phase2_invokesSkillsViaSkillTool() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("Skills via Skill Tool")
                    .contains("SINGLE message for true"
                            + " parallelism");
        }

        @Test
        @DisplayName("Phase 2 references x-review-qa"
                + " skill invocation via Skill tool"
                + " with STORY_ID args")
        void phase2_referencesQaSkill() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .containsPattern(
                            "Skill\\(skill: \"x-review-qa\","
                                    + "\\s+args: \"\\{STORY_ID\\}\"\\)");
        }

        @Test
        @DisplayName("Phase 2 references x-review-perf"
                + " skill invocation via Skill tool"
                + " with STORY_ID args")
        void phase2_referencesPerfSkill() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .containsPattern(
                            "Skill\\(skill: \"x-review-perf\","
                                    + "\\s+args: \"\\{STORY_ID\\}\"\\)");
        }

        @Test
        @DisplayName("Phase 2 references conditional"
                + " x-review-db skill via Skill tool"
                + " with STORY_ID args")
        void phase2_referencesDbSkill() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .containsPattern(
                            "Skill\\(skill: \"x-review-db\","
                                    + "\\s+args: \"\\{STORY_ID\\}\"\\)");
        }

        @Test
        @DisplayName("output format includes standard"
                + " ENGINEER/STORY/SCORE/STATUS")
        void outputFormat_includesStandardFields() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("ENGINEER: {SPECIALIST}")
                    .contains("STORY: {STORY_ID}")
                    .contains("SCORE: XX/YY")
                    .contains("STATUS: Approved |"
                            + " Rejected | Partial");
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
    @DisplayName("Specialist Reference Table"
            + " (story-0029-0012)")
    class SpecialistReferenceTable {

        @Test
        @DisplayName("table contains 9 specialist entries")
        void table_contains9Entries() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("## Specialist Reference"
                            + " Table")
                    .contains("| QA |")
                    .contains("| Performance |")
                    .contains("| Database |")
                    .contains("| Observability |")
                    .contains("| DevOps |")
                    .contains("| Data Modeling |")
                    .contains("| Security |")
                    .contains("| API |")
                    .contains("| Event |");
        }

        @Test
        @DisplayName("each entry has Specialist, Skill,"
                + " Max Score, Condition columns")
        void table_hasRequiredColumns() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("| Specialist | Skill |"
                            + " Max Score | Condition |");
        }

        @Test
        @DisplayName("core skills are always active")
        void table_coreSkillsAlwaysActive() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("| QA | `/x-review-qa`"
                            + " | /36 | Always |")
                    .contains("| Performance |"
                            + " `/x-review-perf`"
                            + " | /26 | Always |");
        }
    }

    @Nested
    @DisplayName("No Inline Checklists"
            + " (story-0029-0012)")
    class NoInlineChecklists {

        @Test
        @DisplayName("SKILL.md does NOT contain"
                + " Engineer Checklists section")
        void skillMd_noEngineerChecklists() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .doesNotContain(
                            "### Engineer Checklists");
        }

        @Test
        @DisplayName("SKILL.md does NOT contain"
                + " Knowledge Pack Mapping section")
        void skillMd_noKpMapping() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .doesNotContain(
                            "### Engineer \u2192 Knowledge"
                                    + " Pack Mapping");
        }

        @Test
        @DisplayName("SKILL.md does NOT contain"
                + " subagent prompt template")
        void skillMd_noSubagentPromptTemplate() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .doesNotContain("{CHECKLIST}")
                    .doesNotContain("{KP_PATHS}");
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
                + " fallback for dashboard and"
                + " remediation templates")
        void skillMd_documentsRule012Fallback() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("RULE-012")
                    .contains("pre-EPIC-0024 projects");
        }

        @Test
        @DisplayName("fallback skips dashboard when"
                + " template absent")
        void fallback_skipsDashboardWhenAbsent() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("dashboard generation"
                            + " skipped");
        }

        @Test
        @DisplayName("fallback skips remediation when"
                + " template absent")
        void fallback_skipsRemediationWhenAbsent() {
            String content = readSkill(CLAUDE_SKILL_PATH);

            assertThat(content)
                    .contains("remediation tracking"
                            + " skipped");
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
