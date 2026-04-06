package dev.iadev.application.assembler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for epic orchestration templates
 * (story-0024-0004):
 * <ul>
 *   <li>{@code _TEMPLATE-EPIC-EXECUTION-PLAN.md}
 *       — 8 mandatory sections</li>
 *   <li>{@code _TEMPLATE-PHASE-COMPLETION-REPORT.md}
 *       — 8 mandatory sections</li>
 * </ul>
 *
 * <p>Validates mandatory sections, table schemas,
 * standardized headers (RULE-011), and that
 * {@code {{PLACEHOLDER}}} markers are preserved
 * verbatim (RULE-003).</p>
 */
@DisplayName("Epic Orchestration Templates"
        + " (story-0024-0004)")
class EpicOrchestrationTemplatesTest {

    private static final String EXECUTION_PLAN_PATH =
            "shared/templates/"
                    + "_TEMPLATE-EPIC-EXECUTION-PLAN.md";
    private static final String PHASE_REPORT_PATH =
            "shared/templates/"
                    + "_TEMPLATE-PHASE-COMPLETION"
                    + "-REPORT.md";

    private static String executionPlanContent;
    private static String phaseReportContent;

    @BeforeAll
    static void loadTemplates() throws IOException {
        executionPlanContent =
                loadResource(EXECUTION_PLAN_PATH);
        phaseReportContent =
                loadResource(PHASE_REPORT_PATH);
    }

    private static String loadResource(String path)
            throws IOException {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException(
                        "Resource not found: " + path);
            }
            return new String(
                    is.readAllBytes(),
                    StandardCharsets.UTF_8);
        }
    }

    @Nested
    @DisplayName("_TEMPLATE-EPIC-EXECUTION-PLAN.md")
    class ExecutionPlanTemplate {

        /** GK-1: Empty template fails validation. */
        @Nested
        @DisplayName("degenerate — empty content")
        class DegenerateEmpty {

            @Test
            @DisplayName("empty content has no mandatory"
                    + " sections")
            void validate_emptyContent_hasNoSections() {
                assertThat(hasAllExecutionPlanSections(""))
                        .isFalse();
            }
        }

        @Nested
        @DisplayName("mandatory 8 sections present")
        class MandatorySections {

            @Test
            @DisplayName("contains Header section")
            void template_hasHeaderSection() {
                assertThat(executionPlanContent)
                        .contains("# Epic Execution Plan");
            }

            @Test
            @DisplayName("contains Execution Strategy"
                    + " section")
            void template_hasExecutionStrategySection() {
                assertThat(executionPlanContent)
                        .contains("## Execution Strategy");
            }

            @Test
            @DisplayName("contains Phase Timeline section")
            void template_hasPhaseTimelineSection() {
                assertThat(executionPlanContent)
                        .contains("## Phase Timeline");
            }

            @Test
            @DisplayName("contains Story Execution Order"
                    + " section")
            void template_hasStoryExecutionOrderSection() {
                assertThat(executionPlanContent)
                        .contains(
                                "## Story Execution Order");
            }

            @Test
            @DisplayName("contains Pre-flight Analysis"
                    + " Summary section")
            void template_hasPreflightSection() {
                assertThat(executionPlanContent)
                        .contains(
                                "## Pre-flight Analysis"
                                        + " Summary");
            }

            @Test
            @DisplayName("contains Resource Requirements"
                    + " section")
            void template_hasResourceRequirementsSection() {
                assertThat(executionPlanContent)
                        .contains(
                                "## Resource Requirements");
            }

            @Test
            @DisplayName("contains Risk Assessment section")
            void template_hasRiskAssessmentSection() {
                assertThat(executionPlanContent)
                        .contains("## Risk Assessment");
            }

            @Test
            @DisplayName("contains Checkpoint Strategy"
                    + " section")
            void template_hasCheckpointStrategySection() {
                assertThat(executionPlanContent)
                        .contains(
                                "## Checkpoint Strategy");
            }

            @Test
            @DisplayName("all 8 mandatory sections present")
            void template_hasAll8Sections() {
                assertThat(hasAllExecutionPlanSections(
                        executionPlanContent))
                        .isTrue();
            }
        }

        /** GK-4: Missing section fails validation. */
        @Nested
        @DisplayName("missing section — validation fails")
        class MissingSectionFails {

            @Test
            @DisplayName("returns false when Phase Timeline"
                    + " missing")
            void validate_missingPhaseTimeline_fails() {
                String modified = executionPlanContent
                        .replace(
                                "## Phase Timeline",
                                "## Something Else");
                assertThat(hasAllExecutionPlanSections(
                        modified)).isFalse();
            }

            @Test
            @DisplayName("returns false when Risk Assessment"
                    + " missing")
            void validate_missingRiskAssessment_fails() {
                String modified = executionPlanContent
                        .replace(
                                "## Risk Assessment",
                                "## Something Else");
                assertThat(hasAllExecutionPlanSections(
                        modified)).isFalse();
            }
        }

        /** GK-2: Story Execution Order table schema. */
        @Nested
        @DisplayName("Story Execution Order table")
        class StoryExecutionOrderTable {

            @Test
            @DisplayName("contains Order column")
            void table_hasOrderColumn() {
                assertThat(executionPlanContent)
                        .contains("Order");
            }

            @Test
            @DisplayName("contains Story ID column")
            void table_hasStoryIdColumn() {
                assertThat(executionPlanContent)
                        .contains("Story ID");
            }

            @Test
            @DisplayName("contains Title column")
            void table_hasTitleColumn() {
                assertThat(executionPlanContent)
                        .contains("Title");
            }

            @Test
            @DisplayName("contains Phase column")
            void table_hasPhaseColumn() {
                assertThat(executionPlanContent)
                        .contains("Phase");
            }

            @Test
            @DisplayName("contains Dependencies column")
            void table_hasDependenciesColumn() {
                assertThat(executionPlanContent)
                        .contains("Dependencies");
            }

            @Test
            @DisplayName("contains Critical Path column")
            void table_hasCriticalPathColumn() {
                assertThat(executionPlanContent)
                        .contains("Critical Path");
            }

            @Test
            @DisplayName("contains Estimated Effort column")
            void table_hasEstimatedEffortColumn() {
                assertThat(executionPlanContent)
                        .contains("Estimated Effort");
            }
        }

        /** GK-2: Phase Timeline table schema. */
        @Nested
        @DisplayName("Phase Timeline table")
        class PhaseTimelineTable {

            @Test
            @DisplayName("contains Phase column")
            void table_hasPhaseColumn() {
                assertThat(executionPlanContent)
                        .contains("| Phase |");
            }

            @Test
            @DisplayName("contains Name column")
            void table_hasNameColumn() {
                assertThat(executionPlanContent)
                        .contains("Name");
            }

            @Test
            @DisplayName("contains Stories column")
            void table_hasStoriesColumn() {
                assertThat(executionPlanContent)
                        .contains("Stories");
            }

            @Test
            @DisplayName("contains Parallelism column")
            void table_hasParallelismColumn() {
                assertThat(executionPlanContent)
                        .contains("Parallelism");
            }

            @Test
            @DisplayName("contains Estimated Duration"
                    + " column")
            void table_hasEstimatedDurationColumn() {
                assertThat(executionPlanContent)
                        .contains("Estimated Duration");
            }
        }

        /** RULE-011: Standardized header. */
        @Nested
        @DisplayName("standardized header (RULE-011)")
        class StandardizedHeader {

            @Test
            @DisplayName("contains Epic ID placeholder")
            void header_hasEpicId() {
                assertThat(executionPlanContent)
                        .contains("{{EPIC_ID}}");
            }

            @Test
            @DisplayName("contains generation date"
                    + " placeholder")
            void header_hasGenerationDate() {
                assertThat(executionPlanContent)
                        .contains("{{GENERATION_DATE}}");
            }

            @Test
            @DisplayName("contains author role placeholder")
            void header_hasAuthorRole() {
                assertThat(executionPlanContent)
                        .contains("{{AUTHOR_ROLE}}");
            }

            @Test
            @DisplayName("contains template version"
                    + " placeholder")
            void header_hasTemplateVersion() {
                assertThat(executionPlanContent)
                        .contains("{{TEMPLATE_VERSION}}");
            }

            @Test
            @DisplayName("contains epic title placeholder")
            void header_hasEpicTitle() {
                assertThat(executionPlanContent)
                        .contains("{{EPIC_TITLE}}");
            }

            @Test
            @DisplayName("contains total stories"
                    + " placeholder")
            void header_hasTotalStories() {
                assertThat(executionPlanContent)
                        .contains("{{TOTAL_STORIES}}");
            }

            @Test
            @DisplayName("contains total phases"
                    + " placeholder")
            void header_hasTotalPhases() {
                assertThat(executionPlanContent)
                        .contains("{{TOTAL_PHASES}}");
            }
        }

        /** RULE-003: Placeholders preserved verbatim. */
        @Nested
        @DisplayName("placeholders preserved (RULE-003)")
        class PlaceholdersPreserved {

            @Test
            @DisplayName("contains {{PLACEHOLDER}} markers")
            void template_hasPlaceholderMarkers() {
                assertThat(executionPlanContent)
                        .containsPattern(
                                "\\{\\{[A-Z_]+\\}\\}");
            }
        }
    }

    @Nested
    @DisplayName("_TEMPLATE-PHASE-COMPLETION-REPORT.md")
    class PhaseCompletionReportTemplate {

        /** GK-1: Empty template fails validation. */
        @Nested
        @DisplayName("degenerate — empty content")
        class DegenerateEmpty {

            @Test
            @DisplayName("empty content has no mandatory"
                    + " sections")
            void validate_emptyContent_hasNoSections() {
                assertThat(hasAllPhaseReportSections(""))
                        .isFalse();
            }
        }

        @Nested
        @DisplayName("mandatory 8 sections present")
        class MandatorySections {

            @Test
            @DisplayName("contains Header section")
            void template_hasHeaderSection() {
                assertThat(phaseReportContent)
                        .contains(
                                "# Phase Completion Report");
            }

            @Test
            @DisplayName("contains Stories Completed"
                    + " section")
            void template_hasStoriesCompletedSection() {
                assertThat(phaseReportContent)
                        .contains("## Stories Completed");
            }

            @Test
            @DisplayName("contains Integrity Gate Results"
                    + " section")
            void template_hasIntegrityGateSection() {
                assertThat(phaseReportContent)
                        .contains(
                                "## Integrity Gate Results");
            }

            @Test
            @DisplayName("contains Findings Summary section")
            void template_hasFindingsSummarySection() {
                assertThat(phaseReportContent)
                        .contains("## Findings Summary");
            }

            @Test
            @DisplayName("contains TDD Compliance section")
            void template_hasTddComplianceSection() {
                assertThat(phaseReportContent)
                        .contains("## TDD Compliance");
            }

            @Test
            @DisplayName("contains Coverage Delta section")
            void template_hasCoverageDeltaSection() {
                assertThat(phaseReportContent)
                        .contains("## Coverage Delta");
            }

            @Test
            @DisplayName("contains Blockers Encountered"
                    + " section")
            void template_hasBlockersSection() {
                assertThat(phaseReportContent)
                        .contains(
                                "## Blockers Encountered");
            }

            @Test
            @DisplayName("contains Next Phase Readiness"
                    + " section")
            void template_hasNextPhaseReadinessSection() {
                assertThat(phaseReportContent)
                        .contains(
                                "## Next Phase Readiness");
            }

            @Test
            @DisplayName("all 8 mandatory sections present")
            void template_hasAll8Sections() {
                assertThat(hasAllPhaseReportSections(
                        phaseReportContent))
                        .isTrue();
            }
        }

        /** GK-3: Integrity Gate Results table. */
        @Nested
        @DisplayName("Integrity Gate Results table")
        class IntegrityGateResultsTable {

            @Test
            @DisplayName("contains Gate column")
            void table_hasGateColumn() {
                assertThat(phaseReportContent)
                        .contains("Gate");
            }

            @Test
            @DisplayName("contains Result column")
            void table_hasResultColumn() {
                assertThat(phaseReportContent)
                        .contains("Result");
            }

            @Test
            @DisplayName("contains Details column")
            void table_hasDetailsColumn() {
                assertThat(phaseReportContent)
                        .contains("Details");
            }

            @Test
            @DisplayName("contains Duration column")
            void table_hasDurationColumn() {
                assertThat(phaseReportContent)
                        .contains("Duration");
            }

            @Test
            @DisplayName("contains Compilation gate row")
            void table_hasCompilationRow() {
                assertThat(phaseReportContent)
                        .contains("Compilation");
            }

            @Test
            @DisplayName("contains Tests gate row")
            void table_hasTestsRow() {
                assertThat(phaseReportContent)
                        .contains("Tests");
            }

            @Test
            @DisplayName("contains Coverage gate row")
            void table_hasCoverageRow() {
                assertThat(phaseReportContent)
                        .contains("Coverage");
            }
        }

        /** GK-3: Result column values. */
        @Nested
        @DisplayName("Result column accepted values")
        class ResultColumnValues {

            @Test
            @DisplayName("mentions Pass as accepted value")
            void resultColumn_acceptsPass() {
                assertThat(phaseReportContent)
                        .contains("Pass");
            }

            @Test
            @DisplayName("mentions Fail as accepted value")
            void resultColumn_acceptsFail() {
                assertThat(phaseReportContent)
                        .contains("Fail");
            }

            @Test
            @DisplayName("mentions Skip as accepted value")
            void resultColumn_acceptsSkip() {
                assertThat(phaseReportContent)
                        .contains("Skip");
            }
        }

        /** RULE-011: Standardized header. */
        @Nested
        @DisplayName("standardized header (RULE-011)")
        class StandardizedHeader {

            @Test
            @DisplayName("contains Epic ID placeholder")
            void header_hasEpicId() {
                assertThat(phaseReportContent)
                        .contains("{{EPIC_ID}}");
            }

            @Test
            @DisplayName("contains Phase Number"
                    + " placeholder")
            void header_hasPhaseNumber() {
                assertThat(phaseReportContent)
                        .contains("{{PHASE_NUMBER}}");
            }

            @Test
            @DisplayName("contains Phase Name placeholder")
            void header_hasPhaseName() {
                assertThat(phaseReportContent)
                        .contains("{{PHASE_NAME}}");
            }

            @Test
            @DisplayName("contains Start Timestamp"
                    + " placeholder")
            void header_hasStartTimestamp() {
                assertThat(phaseReportContent)
                        .contains("{{START_TIMESTAMP}}");
            }

            @Test
            @DisplayName("contains End Timestamp"
                    + " placeholder")
            void header_hasEndTimestamp() {
                assertThat(phaseReportContent)
                        .contains("{{END_TIMESTAMP}}");
            }

            @Test
            @DisplayName("contains Template Version"
                    + " placeholder")
            void header_hasTemplateVersion() {
                assertThat(phaseReportContent)
                        .contains("{{TEMPLATE_VERSION}}");
            }
        }

        /** GK-5: Phase report reusable for all phases. */
        @Nested
        @DisplayName("reusable for all phases (GK-5)")
        class ReusableForAllPhases {

            @Test
            @DisplayName("Phase Number field is fillable")
            void reusable_phaseNumberFillable() {
                assertThat(phaseReportContent)
                        .contains("{{PHASE_NUMBER}}");
            }

            @Test
            @DisplayName("Phase Name field is fillable")
            void reusable_phaseNameFillable() {
                assertThat(phaseReportContent)
                        .contains("{{PHASE_NAME}}");
            }

            @Test
            @DisplayName("Next Phase Readiness applies to"
                    + " all phases")
            void reusable_nextPhaseApplies() {
                assertThat(phaseReportContent)
                        .contains(
                                "## Next Phase Readiness");
            }

            @Test
            @DisplayName("no conditional logic that"
                    + " prevents reuse")
            void reusable_noConditionalLogic() {
                assertThat(phaseReportContent)
                        .doesNotContain("{%");
                assertThat(phaseReportContent)
                        .doesNotContain("{% if");
                assertThat(phaseReportContent)
                        .doesNotContain("{% endif");
            }
        }

        /** Missing section fails validation. */
        @Nested
        @DisplayName("missing section — validation fails")
        class MissingSectionFails {

            @Test
            @DisplayName("returns false when Integrity Gate"
                    + " Results missing")
            void validate_missingIntegrityGate_fails() {
                String modified = phaseReportContent
                        .replace(
                                "## Integrity Gate Results",
                                "## Something Else");
                assertThat(hasAllPhaseReportSections(
                        modified)).isFalse();
            }

            @Test
            @DisplayName("returns false when Next Phase"
                    + " Readiness missing")
            void validate_missingNextPhaseReadiness_fails() {
                String modified = phaseReportContent
                        .replace(
                                "## Next Phase Readiness",
                                "## Something Else");
                assertThat(hasAllPhaseReportSections(
                        modified)).isFalse();
            }
        }

        /** RULE-003: Placeholders preserved verbatim. */
        @Nested
        @DisplayName("placeholders preserved (RULE-003)")
        class PlaceholdersPreserved {

            @Test
            @DisplayName("contains {{PLACEHOLDER}} markers")
            void template_hasPlaceholderMarkers() {
                assertThat(phaseReportContent)
                        .containsPattern(
                                "\\{\\{[A-Z_]+\\}\\}");
            }
        }
    }

    // --- section validation helpers ---

    private static final List<String>
            EXECUTION_PLAN_SECTIONS = List.of(
            "Execution Strategy",
            "Phase Timeline",
            "Story Execution Order",
            "Pre-flight Analysis Summary",
            "Resource Requirements",
            "Risk Assessment",
            "Checkpoint Strategy"
    );

    private static final List<String>
            PHASE_REPORT_SECTIONS = List.of(
            "Stories Completed",
            "Integrity Gate Results",
            "Findings Summary",
            "TDD Compliance",
            "Coverage Delta",
            "Blockers Encountered",
            "Next Phase Readiness"
    );

    static boolean hasAllExecutionPlanSections(
            String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        return EXECUTION_PLAN_SECTIONS.stream()
                .allMatch(section ->
                        content.contains(
                                "## " + section));
    }

    static boolean hasAllPhaseReportSections(
            String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        return PHASE_REPORT_SECTIONS.stream()
                .allMatch(section ->
                        content.contains(
                                "## " + section));
    }
}
