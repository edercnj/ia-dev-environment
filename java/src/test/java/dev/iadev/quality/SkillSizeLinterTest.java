package dev.iadev.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SkillSizeLinter} covering the 6+
 * parametric scenarios from story-0047-0003 §7 in TPP order:
 *
 * <ol>
 *   <li>nil      — empty directory</li>
 *   <li>constant — single small skill (INFO)</li>
 *   <li>scalar   — single skill in WARN tier</li>
 *   <li>collection — mixed tiers aggregated</li>
 *   <li>conditional — &gt;500 branch depends on references/ state</li>
 *   <li>iteration — README-only references/ rejected + message
 *       format</li>
 * </ol>
 */
class SkillSizeLinterTest {

    @TempDir
    Path tempRoot;

    @Test
    void lint_emptyDirectory_returnsEmptyList() {
        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).isEmpty();
    }

    @Test
    void lint_missingRoot_returnsEmptyList() {
        Path missing = tempRoot.resolve("does-not-exist");

        List<LintFinding> findings = SkillSizeLinter.lint(missing);

        assertThat(findings).isEmpty();
    }

    @Test
    void lint_sharedSubdirSkillMd_excluded()
            throws IOException {
        Path sharedSkill = tempRoot.resolve("_shared")
            .resolve("x-nested");
        Files.createDirectories(sharedSkill);
        writeLines(sharedSkill.resolve("SKILL.md"), 100);
        createSkill("x-valid", 100, false, false);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).path().toString())
            .contains("x-valid");
    }

    @Test
    void thresholds_constantsHaveExpectedValues() {
        assertThat(SkillSizeLinter.WARN_THRESHOLD_LINES)
            .isEqualTo(250);
        assertThat(SkillSizeLinter.ERROR_THRESHOLD_LINES)
            .isEqualTo(500);
    }


    @Test
    void lint_smallSkillNoRefs_infoSeverity() throws IOException {
        createSkill("x-small", 200, false, false);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).hasSize(1);
        LintFinding finding = findings.get(0);
        assertThat(finding.severity()).isEqualTo(Severity.INFO);
        assertThat(finding.lineCount()).isEqualTo(200);
        assertThat(finding.hasReferencesDir()).isFalse();
    }

    @Test
    void lint_warnTierSkill_warnSeverity() throws IOException {
        createSkill("x-medium", 380, false, false);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).severity())
            .isEqualTo(Severity.WARN);
    }

    @Test
    void lint_mixedTiers_correctAggregation() throws IOException {
        createSkill("x-a", 100, false, false);
        createSkill("x-b", 300, false, false);
        createSkill("x-c", 700, true, true);
        createSkill("x-d", 900, false, false);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).hasSize(4);
        assertThat(findings).extracting(
                LintFinding::severity,
                LintFinding::lineCount)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(
                    Severity.INFO, 100),
                org.assertj.core.groups.Tuple.tuple(
                    Severity.WARN, 300),
                org.assertj.core.groups.Tuple.tuple(
                    Severity.INFO, 700),
                org.assertj.core.groups.Tuple.tuple(
                    Severity.ERROR, 900));
    }

    @Test
    void lint_largeSkillNoRefs_errorSeverity() throws IOException {
        createSkill("x-release", 1247, false, false);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).hasSize(1);
        LintFinding finding = findings.get(0);
        assertThat(finding.severity()).isEqualTo(Severity.ERROR);
        assertThat(finding.message())
            .contains("x-release", "1247", "500", "references/");
    }

    @Test
    void lint_largeSkillWithValidRefs_infoSeverity()
            throws IOException {
        createSkill("x-release", 1247, true, true);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).hasSize(1);
        LintFinding finding = findings.get(0);
        assertThat(finding.severity()).isEqualTo(Severity.INFO);
        assertThat(finding.hasReferencesDir()).isTrue();
        assertThat(finding.referencesNonEmpty()).isTrue();
    }

    @Test
    void lint_largeSkillReadmeOnlyRefs_errorSeverity()
            throws IOException {
        Path skillDir = tempRoot.resolve("x-foo");
        Files.createDirectories(skillDir);
        writeLines(skillDir.resolve("SKILL.md"), 700);
        Path refs = skillDir.resolve("references");
        Files.createDirectories(refs);
        Files.writeString(refs.resolve("README.md"),
            "# references\n",
            StandardCharsets.UTF_8);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).hasSize(1);
        LintFinding finding = findings.get(0);
        assertThat(finding.severity()).isEqualTo(Severity.ERROR);
        assertThat(finding.hasReferencesDir()).isTrue();
        assertThat(finding.referencesNonEmpty()).isFalse();
        assertThat(finding.message()).contains("README.md");
    }

    @Test
    void lint_largeSkillEmptyRefsDir_errorSeverity()
            throws IOException {
        Path skillDir = tempRoot.resolve("x-bar");
        Files.createDirectories(skillDir);
        writeLines(skillDir.resolve("SKILL.md"), 700);
        Files.createDirectories(skillDir.resolve("references"));

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).hasSize(1);
        LintFinding finding = findings.get(0);
        assertThat(finding.severity()).isEqualTo(Severity.ERROR);
        assertThat(finding.hasReferencesDir()).isTrue();
        assertThat(finding.referencesNonEmpty()).isFalse();
    }

    @Test
    void lint_boundaryAt250_warnTier() throws IOException {
        createSkill("x-boundary-low", 250, false, false);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings.get(0).severity())
            .isEqualTo(Severity.WARN);
    }

    @Test
    void lint_boundaryAt500_warnTier() throws IOException {
        createSkill("x-boundary-high", 500, false, false);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings.get(0).severity())
            .isEqualTo(Severity.WARN);
    }

    @Test
    void lint_boundaryAt501NoRefs_errorTier()
            throws IOException {
        createSkill("x-over", 501, false, false);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings.get(0).severity())
            .isEqualTo(Severity.ERROR);
    }

    @Test
    void lint_sharedDirectoryExcluded_notTraversed()
            throws IOException {
        Path shared = tempRoot.resolve("_shared");
        Files.createDirectories(shared);
        Files.writeString(shared.resolve("snippet.md"),
            "content\n", StandardCharsets.UTF_8);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).isEmpty();
    }

    @Test
    void lint_nonSkillMdFiles_ignored() throws IOException {
        Path dir = tempRoot.resolve("x-foo");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("NOTES.md"),
            "notes\n", StandardCharsets.UTF_8);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);

        assertThat(findings).isEmpty();
    }

    @Test
    void errorFindings_extractsOnlyErrors() throws IOException {
        createSkill("x-a", 100, false, false);
        createSkill("x-b", 800, false, false);
        createSkill("x-c", 900, true, true);

        List<LintFinding> findings = SkillSizeLinter.lint(tempRoot);
        List<LintFinding> errors = SkillSizeLinter
            .errorFindings(findings);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).path().toString())
            .contains("x-b");
    }

    private void createSkill(
            String name,
            int lineCount,
            boolean referencesDir,
            boolean referencesNonEmpty) throws IOException {
        Path skillDir = tempRoot.resolve(name);
        Files.createDirectories(skillDir);
        writeLines(skillDir.resolve("SKILL.md"), lineCount);
        if (referencesDir) {
            Path refs = skillDir.resolve("references");
            Files.createDirectories(refs);
            if (referencesNonEmpty) {
                Files.writeString(refs.resolve("full-protocol.md"),
                    "# carve-out\n",
                    StandardCharsets.UTF_8);
            }
        }
    }

    private void writeLines(Path file, int count)
            throws IOException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append("line ").append(i).append('\n');
        }
        Files.writeString(file, builder.toString(),
            StandardCharsets.UTF_8);
    }
}
