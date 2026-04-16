package dev.iadev.release.preflight;

import dev.iadev.release.BumpType;
import dev.iadev.release.CommitCounts;
import dev.iadev.release.SemVer;
import dev.iadev.release.integrity.CheckResult;
import dev.iadev.release.integrity.CheckStatus;
import dev.iadev.release.integrity.IntegrityReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreflightDashboardRendererTest {

    // -- Helpers --

    private static DashboardData minimalData() {
        return new DashboardData(
                new SemVer(3, 2, 0, null),
                Optional.of(new SemVer(3, 1, 0, null)),
                12,
                new CommitCounts(7, 2, 0, 0, 5),
                BumpType.MINOR,
                List.of("### Added", "- EPIC-0036: skill taxonomy",
                        "### Fixed", "- fix: null check"),
                IntegrityReport.aggregate(List.of(
                        CheckResult.pass("changelog_unreleased_non_empty"),
                        CheckResult.pass("version_alignment"),
                        CheckResult.pass("no_new_todos"))),
                "develop");
    }

    private static List<String> generateLines(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> "- line " + i)
                .toList();
    }

    // -- TASK-001: Degenerate (null data) --

    @Nested
    @DisplayName("Degenerate inputs")
    class Degenerate {

        @Test
        @DisplayName("render_nullData_throwsNPE")
        void render_nullData_throwsNPE() {
            assertThatThrownBy(() -> PreflightDashboardRenderer.render(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("render_nullDataWithLimit_throwsNPE")
        void render_nullDataWithLimit_throwsNPE() {
            assertThatThrownBy(
                    () -> PreflightDashboardRenderer.render(null, 10))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -- TASK-002: Renderer sections --

    @Nested
    @DisplayName("Section rendering")
    class Sections {

        @Test
        @DisplayName("render_minimalData_containsAllFiveSections")
        void render_minimalData_containsAllFiveSections() {
            String output = PreflightDashboardRenderer.render(minimalData());

            assertThat(output).contains("=== PRE-FLIGHT");
            assertThat(output).contains("Versao detectada:");
            assertThat(output).contains("Commits desde tag:");
            assertThat(output).contains("CHANGELOG preview");
            assertThat(output).contains("Integrity checks:");
            assertThat(output).contains("Plano de execucao:");
        }

        @Test
        @DisplayName("render_minimalData_showsCorrectVersion")
        void render_minimalData_showsCorrectVersion() {
            String output = PreflightDashboardRenderer.render(minimalData());

            assertThat(output).contains("release v3.2.0");
            assertThat(output).contains("Versao detectada:    3.2.0");
        }

        @Test
        @DisplayName("render_minimalData_showsPreviousTag")
        void render_minimalData_showsPreviousTag() {
            String output = PreflightDashboardRenderer.render(minimalData());

            assertThat(output).contains("Ultima tag:          v3.1.0");
            assertThat(output).contains("12 dias atras");
        }

        @Test
        @DisplayName("render_noPreviousTag_showsNenhuma")
        void render_noPreviousTag_showsNenhuma() {
            DashboardData data = new DashboardData(
                    new SemVer(1, 0, 0, null),
                    Optional.empty(),
                    0,
                    CommitCounts.ZERO,
                    BumpType.MINOR,
                    List.of(),
                    IntegrityReport.aggregate(List.of()),
                    "develop");

            String output = PreflightDashboardRenderer.render(data);

            assertThat(output).contains("Ultima tag:          (nenhuma)");
        }

        @Test
        @DisplayName("render_minimalData_showsCommitCounts")
        void render_minimalData_showsCommitCounts() {
            String output = PreflightDashboardRenderer.render(minimalData());

            assertThat(output).contains(
                    "14 (7 feat, 2 fix, 0 breaking, 5 ignored)");
        }

        @Test
        @DisplayName("render_minimalData_showsIntegrityPass")
        void render_minimalData_showsIntegrityPass() {
            String output = PreflightDashboardRenderer.render(minimalData());

            assertThat(output).contains("Integrity checks: PASS");
            assertThat(output).contains("v changelog_unreleased_non_empty");
            assertThat(output).contains("v version_alignment");
            assertThat(output).contains("v no_new_todos");
        }

        @Test
        @DisplayName("render_minimalData_showsExecutionPlan")
        void render_minimalData_showsExecutionPlan() {
            String output = PreflightDashboardRenderer.render(minimalData());

            assertThat(output).contains("1. Criar branch release/3.2.0");
            assertThat(output).contains("from develop");
            assertThat(output).contains("2. Bump pom.xml -> 3.2.0");
            assertThat(output).contains(
                    "3. CHANGELOG: [Unreleased] -> [3.2.0]");
            assertThat(output).contains("4. Commit + push");
            assertThat(output).contains("5. PR release/3.2.0 -> main");
        }

        @Test
        @DisplayName("render_emptyChangelog_showsVazio")
        void render_emptyChangelog_showsVazio() {
            DashboardData data = new DashboardData(
                    new SemVer(1, 0, 0, null),
                    Optional.empty(),
                    0,
                    CommitCounts.ZERO,
                    BumpType.MINOR,
                    Collections.emptyList(),
                    IntegrityReport.aggregate(List.of()),
                    "develop");

            String output = PreflightDashboardRenderer.render(data);

            assertThat(output).contains("(vazio)");
        }

        @Test
        @DisplayName("render_linesFitWithin80Cols")
        void render_linesFitWithin80Cols() {
            String output = PreflightDashboardRenderer.render(minimalData());

            for (String line : output.split("\n")) {
                assertThat(line.length())
                        .as("line exceeds 80 cols: '%s'", line)
                        .isLessThanOrEqualTo(80);
            }
        }
    }

    // -- TASK-003/004: Truncation --

    @Nested
    @DisplayName("CHANGELOG truncation")
    class Truncation {

        @Test
        @DisplayName("render_50lines_defaultTruncatesTo10")
        void render_50lines_defaultTruncatesTo10() {
            DashboardData data = new DashboardData(
                    new SemVer(3, 2, 0, null),
                    Optional.of(new SemVer(3, 1, 0, null)),
                    12,
                    new CommitCounts(7, 2, 0, 0, 5),
                    BumpType.MINOR,
                    generateLines(50),
                    IntegrityReport.aggregate(List.of(
                            CheckResult.pass("changelog_unreleased_non_empty"))),
                    "develop");

            String output = PreflightDashboardRenderer.render(data);

            assertThat(output).contains("- line 10");
            assertThat(output).doesNotContain("- line 11");
            assertThat(output).contains("(40 linhas omitidas)");
        }

        @Test
        @DisplayName("render_50linesCustomLimit20_truncatesTo20")
        void render_50linesCustomLimit20_truncatesTo20() {
            DashboardData data = new DashboardData(
                    new SemVer(3, 2, 0, null),
                    Optional.of(new SemVer(3, 1, 0, null)),
                    12,
                    new CommitCounts(7, 2, 0, 0, 5),
                    BumpType.MINOR,
                    generateLines(50),
                    IntegrityReport.aggregate(List.of(
                            CheckResult.pass("changelog_unreleased_non_empty"))),
                    "develop");

            String output = PreflightDashboardRenderer.render(data, 20);

            assertThat(output).contains("- line 20");
            assertThat(output).doesNotContain("- line 21");
            assertThat(output).contains("(30 linhas omitidas)");
        }

        @Test
        @DisplayName("render_linesExactlyAtLimit_noTruncationIndicator")
        void render_linesExactlyAtLimit_noTruncationIndicator() {
            DashboardData data = new DashboardData(
                    new SemVer(3, 2, 0, null),
                    Optional.of(new SemVer(3, 1, 0, null)),
                    12,
                    new CommitCounts(7, 2, 0, 0, 5),
                    BumpType.MINOR,
                    generateLines(10),
                    IntegrityReport.aggregate(List.of(
                            CheckResult.pass("changelog_unreleased_non_empty"))),
                    "develop");

            String output = PreflightDashboardRenderer.render(data);

            assertThat(output).contains("- line 10");
            assertThat(output).doesNotContain("linhas omitidas");
        }
    }

    // -- TASK-005/006: Integrity FAIL path --

    @Nested
    @DisplayName("Integrity FAIL rendering")
    class IntegrityFail {

        @Test
        @DisplayName("render_integrityFail_showsFailStatus")
        void render_integrityFail_showsFailStatus() {
            DashboardData data = new DashboardData(
                    new SemVer(3, 2, 0, null),
                    Optional.of(new SemVer(3, 1, 0, null)),
                    12,
                    new CommitCounts(7, 2, 0, 0, 5),
                    BumpType.MINOR,
                    List.of(),
                    IntegrityReport.aggregate(List.of(
                            CheckResult.fail("changelog_unreleased_non_empty",
                                    List.of("CHANGELOG.md")),
                            CheckResult.pass("version_alignment"),
                            CheckResult.pass("no_new_todos"))),
                    "develop");

            String output = PreflightDashboardRenderer.render(data);

            assertThat(output).contains("Integrity checks: FAIL");
            assertThat(output).contains(
                    "x changelog_unreleased_non_empty");
            assertThat(output).contains("v version_alignment");
        }

        @Test
        @DisplayName("render_integrityWarn_showsWarnStatus")
        void render_integrityWarn_showsWarnStatus() {
            DashboardData data = new DashboardData(
                    new SemVer(3, 2, 0, null),
                    Optional.of(new SemVer(3, 1, 0, null)),
                    12,
                    new CommitCounts(7, 2, 0, 0, 5),
                    BumpType.MINOR,
                    List.of("- some entry"),
                    IntegrityReport.aggregate(List.of(
                            CheckResult.pass("changelog_unreleased_non_empty"),
                            CheckResult.warn("no_new_todos",
                                    List.of("src/Main.java:42")))),
                    "develop");

            String output = PreflightDashboardRenderer.render(data);

            assertThat(output).contains("Integrity checks: WARN");
            assertThat(output).contains("~ no_new_todos");
        }
    }

    // -- TASK-007: Input validation / ANSI sanitization --

    @Nested
    @DisplayName("Security — input sanitization")
    class Security {

        @Test
        @DisplayName("clampChangelogLines_belowMin_clampsTo1")
        void clampChangelogLines_belowMin_clampsTo1() {
            assertThat(PreflightDashboardRenderer.clampChangelogLines(0))
                    .isEqualTo(1);
            assertThat(PreflightDashboardRenderer.clampChangelogLines(-5))
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("clampChangelogLines_aboveMax_clampsTo500")
        void clampChangelogLines_aboveMax_clampsTo500() {
            assertThat(PreflightDashboardRenderer.clampChangelogLines(501))
                    .isEqualTo(500);
            assertThat(PreflightDashboardRenderer.clampChangelogLines(9999))
                    .isEqualTo(500);
        }

        @Test
        @DisplayName("clampChangelogLines_withinRange_passesThrough")
        void clampChangelogLines_withinRange_passesThrough() {
            assertThat(PreflightDashboardRenderer.clampChangelogLines(1))
                    .isEqualTo(1);
            assertThat(PreflightDashboardRenderer.clampChangelogLines(250))
                    .isEqualTo(250);
            assertThat(PreflightDashboardRenderer.clampChangelogLines(500))
                    .isEqualTo(500);
        }

        @Test
        @DisplayName("sanitize_stripsAnsiEscapes")
        void sanitize_stripsAnsiEscapes() {
            String input = "\u001b[31mred text\u001b[0m";
            String result = PreflightDashboardRenderer.sanitize(input);

            assertThat(result).isEqualTo("red text");
            assertThat(result).doesNotContain("\u001b");
        }

        @Test
        @DisplayName("sanitize_stripsControlChars")
        void sanitize_stripsControlChars() {
            String input = "hello\u0007world\u0008test";
            String result = PreflightDashboardRenderer.sanitize(input);

            assertThat(result).isEqualTo("helloworldtest");
        }

        @Test
        @DisplayName("sanitize_preservesNewlineTabCR")
        void sanitize_preservesNewlineTabCR() {
            String input = "line1\nline2\ttab\rreturn";
            String result = PreflightDashboardRenderer.sanitize(input);

            assertThat(result).isEqualTo(input);
        }

        @Test
        @DisplayName("sanitize_incompleteAnsiEscape_stripped")
        void sanitize_incompleteAnsiEscape_stripped() {
            // ESC not followed by '[' — the bare ESC is consumed
            String input = "before\u001bafter";
            String result = PreflightDashboardRenderer.sanitize(input);

            assertThat(result).isEqualTo("beforeafter");
        }

        @Test
        @DisplayName("sanitize_nullReturnsEmpty")
        void sanitize_nullReturnsEmpty() {
            assertThat(PreflightDashboardRenderer.sanitize(null)).isEmpty();
        }

        @Test
        @DisplayName("render_changelogWithAnsi_stripped")
        void render_changelogWithAnsi_stripped() {
            DashboardData data = new DashboardData(
                    new SemVer(1, 0, 0, null),
                    Optional.empty(),
                    0,
                    CommitCounts.ZERO,
                    BumpType.MINOR,
                    List.of("\u001b[31m- malicious\u001b[0m"),
                    IntegrityReport.aggregate(List.of()),
                    "develop");

            String output = PreflightDashboardRenderer.render(data);

            assertThat(output).doesNotContain("\u001b");
            assertThat(output).contains("- malicious");
        }
    }
}
