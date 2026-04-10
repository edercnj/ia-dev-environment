package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies EPIC-0024 documentation requirements:
 * CLAUDE.md template catalog, CHANGELOG.md entry,
 * and README.md artifact counts.
 *
 * <p>These tests validate the actual project root
 * documentation files, not generated output.</p>
 */
@DisplayName("EPIC-0024 Documentation Verification")
class Epic0024DocumentationTest {

    private static final Path PROJECT_ROOT =
            resolveProjectRoot();

    private static final List<String>
            EXPECTED_TEMPLATES = List.of(
            "_TEMPLATE-IMPLEMENTATION-PLAN.md",
            "_TEMPLATE-TEST-PLAN.md",
            "_TEMPLATE-ARCHITECTURE-PLAN.md",
            "_TEMPLATE-TASK-BREAKDOWN.md",
            "_TEMPLATE-SECURITY-ASSESSMENT.md",
            "_TEMPLATE-COMPLIANCE-ASSESSMENT.md",
            "_TEMPLATE-SPECIALIST-REVIEW.md",
            "_TEMPLATE-TECH-LEAD-REVIEW.md",
            "_TEMPLATE-CONSOLIDATED-REVIEW"
                    + "-DASHBOARD.md",
            "_TEMPLATE-REVIEW-REMEDIATION.md",
            "_TEMPLATE-EPIC-EXECUTION-PLAN.md",
            "_TEMPLATE-PHASE-COMPLETION-REPORT.md");

    @Nested
    @DisplayName("CLAUDE.md template catalog")
    class ClaudeMdCatalog {

        @Test
        @DisplayName("contains Plan & Review Templates"
                + " section")
        void claudeMd_containsTemplateSection()
                throws IOException {
            String content = readProjectFile("CLAUDE.md");

            assertThat(content).contains(
                    "## Plan & Review Templates");
        }

        @Test
        @DisplayName("template table has exactly 12 data"
                + " rows")
        void claudeMd_templateTable_has12Rows()
                throws IOException {
            String content = readProjectFile("CLAUDE.md");
            List<String> tableRows =
                    extractTemplateTableRows(content);

            assertThat(tableRows)
                    .as("Template catalog must have"
                            + " exactly 12 entries")
                    .hasSize(12);
        }

        @Test
        @DisplayName("all 12 templates are listed in"
                + " catalog")
        void claudeMd_allTemplatesListed()
                throws IOException {
            String content = readProjectFile("CLAUDE.md");

            for (String template : EXPECTED_TEMPLATES) {
                assertThat(content)
                        .as("CLAUDE.md must list %s",
                                template)
                        .contains(template);
            }
        }

        @Test
        @DisplayName("no duplicate templates in catalog")
        void claudeMd_noDuplicateTemplates()
                throws IOException {
            String content = readProjectFile("CLAUDE.md");
            List<String> tableRows =
                    extractTemplateTableRows(content);

            Set<String> seen = new HashSet<>();
            List<String> duplicates = new ArrayList<>();
            for (String row : tableRows) {
                String templateName =
                        extractTemplateName(row);
                if (!seen.add(templateName)) {
                    duplicates.add(templateName);
                }
            }

            assertThat(duplicates)
                    .as("No duplicate templates allowed")
                    .isEmpty();
        }

        @Test
        @DisplayName("each row has Template, Produced By,"
                + " Saved To, Pre-Check columns")
        void claudeMd_tableRowsHaveAllColumns()
                throws IOException {
            String content = readProjectFile("CLAUDE.md");
            List<String> tableRows =
                    extractTemplateTableRows(content);

            for (String row : tableRows) {
                String[] cols = row.split("\\|");
                assertThat(cols.length)
                        .as("Row must have 4+ columns"
                                + " (plus borders): %s",
                                row)
                        .isGreaterThanOrEqualTo(5);
            }
        }

        @Test
        @DisplayName("contains fallback note")
        void claudeMd_containsFallbackNote()
                throws IOException {
            String content = readProjectFile("CLAUDE.md");

            assertThat(content).contains(
                    "Templates are optional -- skills"
                            + " degrade gracefully"
                            + " without them");
        }

        @Test
        @DisplayName("contains dual-target note")
        void claudeMd_containsDualTargetNote()
                throws IOException {
            String content = readProjectFile("CLAUDE.md");

            assertThat(content).contains(
                    ".claude/templates/")
                    .contains(".github/templates/");
        }

        @Test
        @DisplayName("Generation Summary includes Plan"
                + " Templates counts")
        void claudeMd_summaryIncludesTemplates()
                throws IOException {
            String content = readProjectFile("CLAUDE.md");

            assertThat(content)
                    .contains("Plan Templates (.claude)")
                    .contains("Plan Templates (.github)");
        }
    }

    @Nested
    @DisplayName("CHANGELOG.md EPIC-0024 entry")
    class ChangelogEntry {

        @Test
        @DisplayName("contains EPIC-0024 references")
        void changelog_containsEpic0024()
                throws IOException {
            String content =
                    readProjectFile("CHANGELOG.md");

            assertThat(content).contains("EPIC-0024");
        }

        @Test
        @DisplayName("has Added section for EPIC-0024")
        void changelog_hasAddedSection()
                throws IOException {
            String content =
                    readProjectFile("CHANGELOG.md");

            assertThat(content)
                    .contains("### Added")
                    .contains("EPIC-0024");
        }

        @Test
        @DisplayName("Added section mentions 12 templates")
        void changelog_addedMentionsTemplates()
                throws IOException {
            String content =
                    readProjectFile("CHANGELOG.md");

            assertThat(content)
                    .contains("12 new plan and review"
                            + " templates");
        }

        @Test
        @DisplayName("Added section mentions"
                + " PlanTemplatesAssembler")
        void changelog_addedMentionsAssembler()
                throws IOException {
            String content =
                    readProjectFile("CHANGELOG.md");

            assertThat(content)
                    .contains("PlanTemplatesAssembler");
        }

        @Test
        @DisplayName("Added section mentions pre-checks")
        void changelog_addedMentionsPreChecks()
                throws IOException {
            String content =
                    readProjectFile("CHANGELOG.md");

            assertThat(content)
                    .contains("Pre-checks in 8 skills");
        }

        @Test
        @DisplayName("Changed section lists modified"
                + " skills")
        void changelog_changedListsSkills()
                throws IOException {
            String content =
                    readProjectFile("CHANGELOG.md");

            assertThat(content)
                    .contains("x-dev-story-implement (EPIC-0024)")
                    .contains("x-dev-implement (EPIC-0024)")
                    .contains("x-review (EPIC-0024)")
                    .contains("x-review-pr (EPIC-0024)");
        }

        @Test
        @DisplayName("follows Keep a Changelog format")
        void changelog_followsKeepAChangelog()
                throws IOException {
            String content =
                    readProjectFile("CHANGELOG.md");

            assertThat(content)
                    .contains("[Keep a Changelog]")
                    .contains("[Semantic Versioning]");
        }
    }

    @Nested
    @DisplayName("README.md artifact references")
    class ReadmeArtifacts {

        @Test
        @DisplayName("includes templates directory in"
                + " .claude/ structure")
        void readme_claudeStructureHasTemplates()
                throws IOException {
            String content =
                    readProjectFile("README.md");

            assertThat(content)
                    .contains("templates/");
        }

        @Test
        @DisplayName("includes templates directory in"
                + " .github/ structure")
        void readme_githubStructureHasTemplates()
                throws IOException {
            String content =
                    readProjectFile("README.md");

            assertThat(content)
                    .contains("templates/");
        }
    }

    // -- Helpers --

    private static String readProjectFile(String name)
            throws IOException {
        return Files.readString(
                PROJECT_ROOT.resolve(name),
                StandardCharsets.UTF_8);
    }

    private static List<String> extractTemplateTableRows(
            String content) {
        List<String> rows = new ArrayList<>();
        boolean inTable = false;
        boolean headerSkipped = false;
        for (String line : content.split("\n")) {
            if (line.contains("| Template |")
                    && line.contains("| Produced By |")) {
                inTable = true;
                headerSkipped = false;
                continue;
            }
            if (inTable && !headerSkipped
                    && line.startsWith("|---")) {
                headerSkipped = true;
                continue;
            }
            if (inTable && headerSkipped) {
                if (line.startsWith("| `_TEMPLATE-")) {
                    rows.add(line);
                } else {
                    break;
                }
            }
        }
        return rows;
    }

    private static String extractTemplateName(String row) {
        int start = row.indexOf('`');
        int end = row.indexOf('`', start + 1);
        if (start >= 0 && end > start) {
            return row.substring(start + 1, end);
        }
        return row;
    }

    private static Path resolveProjectRoot() {
        Path current = Path.of("").toAbsolutePath();
        if (current.getFileName().toString()
                .equals("java")) {
            return current.getParent();
        }
        return current;
    }
}
