package dev.iadev.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Ra9SectionsChecker}
 * (TASK-0056-0007-001).
 *
 * <p>Verifies detection of missing RA9 section headers
 * in Epic/Story/Task planning artifacts.</p>
 */
@DisplayName("Ra9SectionsCheckerTest")
class Ra9SectionsCheckerTest {

    private static final String COMPLIANT_CONTENT =
            "# Épico\n\n"
            + "## 1. Contexto & Escopo\n"
            + "content\n"
            + "## 2. Packages (Hexagonal)\n"
            + "content\n"
            + "## 3. Contratos & Endpoints\n"
            + "content\n"
            + "## 4. Materialização SOLID\n"
            + "content\n"
            + "## 5. Quality Gates\n"
            + "content\n"
            + "## 6. Segurança\n"
            + "content\n"
            + "## 7. Observabilidade\n"
            + "content\n"
            + "## 8. Decision Rationale\n"
            + "content\n"
            + "## 9. Dependências & File Footprint\n"
            + "content\n";

    private final Ra9SectionsChecker checker =
            new Ra9SectionsChecker();

    @Test
    @DisplayName("check_compliant_returnsNoViolations")
    void check_compliant_returnsNoViolations() {
        List<String> violations = checker.check(
                COMPLIANT_CONTENT, "epic-0060.md");
        assertThat(violations)
                .as("Compliant content with all 9 sections"
                        + " must produce no violations")
                .isEmpty();
    }

    @Test
    @DisplayName("check_degenerateEmpty_returnsNineViolations")
    void check_degenerateEmpty_returnsNineViolations() {
        List<String> violations = checker.check(
                "# Just a title\n", "story-0060-0001.md");
        assertThat(violations)
                .as("Content with zero RA9 sections must"
                        + " flag 9 missing sections")
                .hasSize(9);
    }

    @ParameterizedTest(name = "missing section {0} detected")
    @ValueSource(strings = {
        "## 2. Packages (Hexagonal)",
        "## 8. Decision Rationale"
    })
    @DisplayName("check_missingOneSection_flagsExactlyThat")
    void check_missingOneSection_flagsExactlyThat(
            String missingSection) {
        String content = COMPLIANT_CONTENT.replace(
                missingSection, "## REMOVED");
        List<String> violations = checker.check(
                content, "epic-test.md");
        assertThat(violations)
                .as("Missing %s must produce exactly 1 violation",
                        missingSection)
                .hasSize(1);
        assertThat(violations.get(0))
                .as("Violation message must name the missing section")
                .contains("RA9_SECTIONS_MISSING");
    }

    @Test
    @DisplayName("check_auditExempt_returnsNoViolations")
    void check_auditExempt_returnsNoViolations() {
        String content = "<!-- audit-exempt -->\n"
                + "# Legacy artifact with no RA9 sections\n";
        List<String> violations = checker.check(
                content, "legacy-story.md");
        assertThat(violations)
                .as("audit-exempt marker must suppress all violations")
                .isEmpty();
    }

    @Test
    @DisplayName("check_violation_includesFilename")
    void check_violation_includesFilename() {
        List<String> violations = checker.check(
                "# Title only\n", "plans/epic-0060/story-0060-0001.md");
        assertThat(violations)
                .isNotEmpty()
                .allMatch(v -> v.contains("story-0060-0001.md"),
                        "all violation messages must reference the filename");
    }
}
