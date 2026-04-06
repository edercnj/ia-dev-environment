package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for review templates (story-0024-0003):
 * specialist review, tech lead review, consolidated
 * dashboard, and remediation tracker.
 *
 * <p>Validates that each template file exists in
 * shared/templates/, contains all mandatory sections,
 * uses parseable score format (RULE-005), supports
 * cumulative rounds (RULE-006), and includes
 * standardized headers (RULE-011).</p>
 */
@DisplayName("Review Templates (story-0024-0003)")
class ReviewTemplatesTest {

    private static final Path TEMPLATES_DIR =
            resolveTemplatesDir();

    private static final Pattern SCORE_PATTERN =
            Pattern.compile(
                    "(\\d+)/(\\d+)\\s*\\|\\s*"
                            + "Status:\\s*"
                            + "(Approved|Rejected|Partial)");

    @Nested
    @DisplayName("Specialist Review Template")
    class SpecialistReviewTemplate {

        private static final String FILENAME =
                "_TEMPLATE-SPECIALIST-REVIEW.md";

        @Test
        @DisplayName("template file exists in"
                + " shared/templates/")
        void templateFile_exists_inSharedTemplates() {
            Path path = TEMPLATES_DIR.resolve(FILENAME);

            assertThat(path)
                    .as("Template file must exist: %s",
                            FILENAME)
                    .exists();
        }

        @Test
        @DisplayName("contains standardized header"
                + " with Story ID, Date, Reviewer,"
                + " Engineer Type, Template Version")
        void template_header_containsAllRequiredFields() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("Story ID:")
                    .contains("Date:")
                    .contains("Reviewer:")
                    .contains("Engineer Type:")
                    .contains("Template Version:");
        }

        @Test
        @DisplayName("contains Review Scope section")
        void template_containsReviewScopeSection() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Review Scope");
        }

        @Test
        @DisplayName("contains Score Summary with"
                + " parseable XX/YY format")
        void template_scoreSummary_parseableFormat() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Score Summary");
            Matcher matcher =
                    SCORE_PATTERN.matcher(content);
            assertThat(matcher.find())
                    .as("Score must match regex: "
                            + "(\\d+)/(\\d+)\\s*\\|\\s*"
                            + "Status:\\s*(Approved|"
                            + "Rejected|Partial)")
                    .isTrue();
        }

        @Test
        @DisplayName("contains Passed Items section")
        void template_containsPassedItemsSection() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Passed Items");
        }

        @Test
        @DisplayName("contains Failed Items section"
                + " with File, Line, Severity columns")
        void template_failedItems_hasRequiredColumns() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Failed Items")
                    .contains("File")
                    .contains("Line")
                    .contains("Severity")
                    .contains("Description");
        }

        @Test
        @DisplayName("Failed Items includes all"
                + " severity levels")
        void template_failedItems_allSeverityLevels() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("Critical")
                    .contains("High")
                    .contains("Medium")
                    .contains("Low");
        }

        @Test
        @DisplayName("contains Partial Items section")
        void template_containsPartialItemsSection() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Partial Items");
        }

        @Test
        @DisplayName("contains Severity Summary section"
                + " with aggregated counts")
        void template_severitySummary_hasAggregatedCounts() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Severity Summary")
                    .contains("Critical")
                    .contains("High")
                    .contains("Medium")
                    .contains("Low")
                    .contains("Total");
        }

        @Test
        @DisplayName("contains Recommendations section")
        void template_containsRecommendationsSection() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Recommendations");
        }

        @Test
        @DisplayName("has all 8 mandatory sections")
        void template_hasAll8MandatorySections() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .as("Missing: header")
                    .contains("# Specialist Review");
            assertThat(content)
                    .as("Missing: Review Scope")
                    .contains("## Review Scope");
            assertThat(content)
                    .as("Missing: Score Summary")
                    .contains("## Score Summary");
            assertThat(content)
                    .as("Missing: Passed Items")
                    .contains("## Passed Items");
            assertThat(content)
                    .as("Missing: Failed Items")
                    .contains("## Failed Items");
            assertThat(content)
                    .as("Missing: Partial Items")
                    .contains("## Partial Items");
            assertThat(content)
                    .as("Missing: Severity Summary")
                    .contains("## Severity Summary");
            assertThat(content)
                    .as("Missing: Recommendations")
                    .contains("## Recommendations");
        }
    }

    @Nested
    @DisplayName("Tech Lead Review Template")
    class TechLeadReviewTemplate {

        private static final String FILENAME =
                "_TEMPLATE-TECH-LEAD-REVIEW.md";

        @Test
        @DisplayName("template file exists in"
                + " shared/templates/")
        void templateFile_exists_inSharedTemplates() {
            Path path = TEMPLATES_DIR.resolve(FILENAME);

            assertThat(path)
                    .as("Template file must exist: %s",
                            FILENAME)
                    .exists();
        }

        @Test
        @DisplayName("contains standardized header"
                + " with Story ID, PR, Date, Score,"
                + " Template Version")
        void template_header_containsAllRequiredFields() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("Story ID:")
                    .contains("PR:")
                    .contains("Date:")
                    .contains("Score:")
                    .contains("Template Version:");
        }

        @Test
        @DisplayName("contains Decision section with"
                + " GO/NO-GO values")
        void template_decision_hasGoNoGoValues() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Decision")
                    .contains("GO")
                    .contains("NO-GO");
        }

        @Test
        @DisplayName("Section Scores table has exactly"
                + " 11 rows (A through K)")
        void template_sectionScores_has11Rows() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Section Scores");

            assertThat(content)
                    .contains("| Clean Code | A |")
                    .contains("| SOLID | B |")
                    .contains("| Architecture | C |")
                    .contains(
                            "| Framework Conventions | D |")
                    .contains("| Tests | E |")
                    .contains("| TDD Process | F |")
                    .contains("| Security | G |")
                    .contains(
                            "| Cross-File Consistency | H |")
                    .contains("| API Design | I |")
                    .contains("| Events/Messaging | J |")
                    .contains("| Documentation | K |");
        }

        @Test
        @DisplayName("Section Scores table has"
                + " Section, ID, Score, Max Score columns")
        void template_sectionScores_hasRequiredColumns() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("| Section | ID | Score"
                            + " | Max Score |");
        }

        @Test
        @DisplayName("contains Cross-File Consistency"
                + " section")
        void template_containsCrossFileConsistency() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Cross-File Consistency");
        }

        @Test
        @DisplayName("contains Critical Issues section")
        void template_containsCriticalIssues() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Critical Issues");
        }

        @Test
        @DisplayName("contains Medium Issues section")
        void template_containsMediumIssues() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Medium Issues");
        }

        @Test
        @DisplayName("contains Low Issues section")
        void template_containsLowIssues() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Low Issues");
        }

        @Test
        @DisplayName("contains TDD Compliance"
                + " Assessment section")
        void template_containsTddComplianceAssessment() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains(
                            "## TDD Compliance Assessment");
        }

        @Test
        @DisplayName("contains Specialist Review"
                + " Validation section")
        void template_containsSpecialistReviewValidation() {
            String content = readTemplate(FILENAME);

            assertThat(content).contains(
                    "## Specialist Review Validation");
        }

        @Test
        @DisplayName("contains Verdict section")
        void template_containsVerdictSection() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Verdict");
        }

        @Test
        @DisplayName("has all 10 mandatory sections")
        void template_hasAll10MandatorySections() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .as("Missing: header")
                    .contains("# Tech Lead Review");
            assertThat(content)
                    .as("Missing: Decision")
                    .contains("## Decision");
            assertThat(content)
                    .as("Missing: Section Scores")
                    .contains("## Section Scores");
            assertThat(content)
                    .as("Missing: Cross-File Consistency")
                    .contains(
                            "## Cross-File Consistency");
            assertThat(content)
                    .as("Missing: Critical Issues")
                    .contains("## Critical Issues");
            assertThat(content)
                    .as("Missing: Medium Issues")
                    .contains("## Medium Issues");
            assertThat(content)
                    .as("Missing: Low Issues")
                    .contains("## Low Issues");
            assertThat(content)
                    .as("Missing: TDD Compliance")
                    .contains(
                            "## TDD Compliance Assessment");
            assertThat(content)
                    .as("Missing: Specialist Review"
                            + " Validation")
                    .contains(
                            "## Specialist Review "
                                    + "Validation");
            assertThat(content)
                    .as("Missing: Verdict")
                    .contains("## Verdict");
        }

        @Test
        @DisplayName("score is parseable via regex"
                + " XX/YY | Status:")
        void template_score_parseableViaRegex() {
            String content = readTemplate(FILENAME);

            Matcher matcher =
                    SCORE_PATTERN.matcher(content);
            assertThat(matcher.find())
                    .as("Score must match regex: "
                            + "(\\d+)/(\\d+)\\s*\\|\\s*"
                            + "Status:\\s*(Approved|"
                            + "Rejected|Partial)")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Consolidated Review Dashboard Template")
    class ConsolidatedDashboardTemplate {

        private static final String FILENAME =
                "_TEMPLATE-CONSOLIDATED-REVIEW-"
                        + "DASHBOARD.md";

        @Test
        @DisplayName("template file exists in"
                + " shared/templates/")
        void templateFile_exists_inSharedTemplates() {
            Path path = TEMPLATES_DIR.resolve(FILENAME);

            assertThat(path)
                    .as("Template file must exist: %s",
                            FILENAME)
                    .exists();
        }

        @Test
        @DisplayName("contains standardized header"
                + " with Story ID, Epic ID, Date,"
                + " Template Version")
        void template_header_containsAllRequiredFields() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("Story ID:")
                    .contains("Epic ID:")
                    .contains("Date:")
                    .contains("Template Version:");
        }

        @Test
        @DisplayName("contains Overall Score with"
                + " parseable format")
        void template_overallScore_parseableFormat() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Overall Score");
            Matcher matcher =
                    SCORE_PATTERN.matcher(content);
            assertThat(matcher.find())
                    .as("Score must match regex")
                    .isTrue();
        }

        @Test
        @DisplayName("Engineer Scores Table has 8"
                + " specialist types")
        void template_engineerScores_has8Types() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Engineer Scores Table")
                    .contains("Security")
                    .contains("QA")
                    .contains("Performance")
                    .contains("Database")
                    .contains("Observability")
                    .contains("DevOps")
                    .contains("API")
                    .contains("Event");
        }

        @Test
        @DisplayName("contains Tech Lead Score section")
        void template_containsTechLeadScore() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Tech Lead Score");
        }

        @Test
        @DisplayName("contains Critical Issues Summary")
        void template_containsCriticalIssuesSummary() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Critical Issues Summary");
        }

        @Test
        @DisplayName("contains Severity Distribution"
                + " with all 4 levels")
        void template_severityDistribution_allLevels() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Severity Distribution")
                    .contains("Critical")
                    .contains("High")
                    .contains("Medium")
                    .contains("Low");
        }

        @Test
        @DisplayName("contains Remediation Status with"
                + " all 4 statuses")
        void template_remediationStatus_allStatuses() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Remediation Status")
                    .contains("Open")
                    .contains("Fixed")
                    .contains("Deferred")
                    .contains("Accepted");
        }

        @Test
        @DisplayName("Review History supports"
                + " cumulative rounds (RULE-006)")
        void template_reviewHistory_cumulativeRounds() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Review History")
                    .contains("### Round 1")
                    .contains("### Round 2");
        }

        @Test
        @DisplayName("contains Correction Story section")
        void template_containsCorrectionStory() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Correction Story");
        }

        @Test
        @DisplayName("has all 9 mandatory sections")
        void template_hasAll9MandatorySections() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .as("Missing: header")
                    .contains(
                            "# Consolidated Review "
                                    + "Dashboard");
            assertThat(content)
                    .as("Missing: Overall Score")
                    .contains("## Overall Score");
            assertThat(content)
                    .as("Missing: Engineer Scores Table")
                    .contains(
                            "## Engineer Scores Table");
            assertThat(content)
                    .as("Missing: Tech Lead Score")
                    .contains("## Tech Lead Score");
            assertThat(content)
                    .as("Missing: Critical Issues Summary")
                    .contains(
                            "## Critical Issues Summary");
            assertThat(content)
                    .as("Missing: Severity Distribution")
                    .contains(
                            "## Severity Distribution");
            assertThat(content)
                    .as("Missing: Remediation Status")
                    .contains("## Remediation Status");
            assertThat(content)
                    .as("Missing: Review History")
                    .contains("## Review History");
            assertThat(content)
                    .as("Missing: Correction Story")
                    .contains("## Correction Story");
        }
    }

    @Nested
    @DisplayName("Review Remediation Template")
    class ReviewRemediationTemplate {

        private static final String FILENAME =
                "_TEMPLATE-REVIEW-REMEDIATION.md";

        @Test
        @DisplayName("template file exists in"
                + " shared/templates/")
        void templateFile_exists_inSharedTemplates() {
            Path path = TEMPLATES_DIR.resolve(FILENAME);

            assertThat(path)
                    .as("Template file must exist: %s",
                            FILENAME)
                    .exists();
        }

        @Test
        @DisplayName("contains standardized header"
                + " with Story ID, Epic ID, Date,"
                + " Template Version")
        void template_header_containsAllRequiredFields() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("Story ID:")
                    .contains("Epic ID:")
                    .contains("Date:")
                    .contains("Template Version:");
        }

        @Test
        @DisplayName("Findings Tracker has all"
                + " required columns")
        void template_findingsTracker_hasRequiredColumns() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Findings Tracker")
                    .contains("Finding ID")
                    .contains("Engineer")
                    .contains("Severity")
                    .contains("Description")
                    .contains("Status")
                    .contains("Fix Commit SHA")
                    .contains("Notes");
        }

        @Test
        @DisplayName("Findings Tracker has all 4"
                + " status values")
        void template_findingsTracker_allStatusValues() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("Open")
                    .contains("Fixed")
                    .contains("Deferred")
                    .contains("Accepted");
        }

        @Test
        @DisplayName("Findings Tracker uses FIND-NNN"
                + " format for IDs")
        void template_findingsTracker_findIdFormat() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("FIND-001");
        }

        @Test
        @DisplayName("contains Remediation Summary"
                + " with totals per status")
        void template_remediationSummary_hasTotals() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Remediation Summary")
                    .contains("Open")
                    .contains("Fixed")
                    .contains("Deferred")
                    .contains("Accepted")
                    .contains("Total");
        }

        @Test
        @DisplayName("contains Deferred Justifications"
                + " section")
        void template_containsDeferredJustifications() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains(
                            "## Deferred Justifications");
        }

        @Test
        @DisplayName("Deferred Justifications requires"
                + " mandatory justification")
        void template_deferredJustifications_mandatory() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains(
                            "## Deferred Justifications")
                    .containsIgnoringCase("mandatory")
                    .containsIgnoringCase("justification");
        }

        @Test
        @DisplayName("contains Re-review Results"
                + " section")
        void template_containsReReviewResults() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .contains("## Re-review Results");
        }

        @Test
        @DisplayName("has all 5 mandatory sections")
        void template_hasAll5MandatorySections() {
            String content = readTemplate(FILENAME);

            assertThat(content)
                    .as("Missing: header")
                    .contains(
                            "# Review Remediation Tracker");
            assertThat(content)
                    .as("Missing: Findings Tracker")
                    .contains("## Findings Tracker");
            assertThat(content)
                    .as("Missing: Remediation Summary")
                    .contains("## Remediation Summary");
            assertThat(content)
                    .as("Missing: Deferred "
                            + "Justifications")
                    .contains(
                            "## Deferred Justifications");
            assertThat(content)
                    .as("Missing: Re-review Results")
                    .contains("## Re-review Results");
        }
    }

    @Nested
    @DisplayName("Score Format Compliance (RULE-005)")
    class ScoreFormatCompliance {

        @Test
        @DisplayName("specialist review score matches"
                + " RULE-005 regex")
        void specialistReview_score_matchesRule005() {
            String content = readTemplate(
                    "_TEMPLATE-SPECIALIST-REVIEW.md");

            Matcher matcher =
                    SCORE_PATTERN.matcher(content);
            assertThat(matcher.find())
                    .as("Specialist score must match "
                            + "RULE-005 regex")
                    .isTrue();
        }

        @Test
        @DisplayName("tech lead review score matches"
                + " RULE-005 regex")
        void techLeadReview_score_matchesRule005() {
            String content = readTemplate(
                    "_TEMPLATE-TECH-LEAD-REVIEW.md");

            Matcher matcher =
                    SCORE_PATTERN.matcher(content);
            assertThat(matcher.find())
                    .as("Tech Lead score must match "
                            + "RULE-005 regex")
                    .isTrue();
        }

        @Test
        @DisplayName("dashboard overall score matches"
                + " RULE-005 regex")
        void dashboard_overallScore_matchesRule005() {
            String content = readTemplate(
                    "_TEMPLATE-CONSOLIDATED-REVIEW-"
                            + "DASHBOARD.md");

            Matcher matcher =
                    SCORE_PATTERN.matcher(content);
            assertThat(matcher.find())
                    .as("Dashboard score must match "
                            + "RULE-005 regex")
                    .isTrue();
        }
    }

    private static Path resolveTemplatesDir() {
        Path resourcesDir = Path.of(
                "src/main/resources/shared/templates");
        if (!Files.exists(resourcesDir)) {
            resourcesDir = Path.of(
                    "java/src/main/resources/"
                            + "shared/templates");
        }
        return resourcesDir;
    }

    private static String readTemplate(String filename) {
        try {
            return Files.readString(
                    TEMPLATES_DIR.resolve(filename),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read template: "
                            + filename, e);
        }
    }
}
