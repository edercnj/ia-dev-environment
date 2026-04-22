package dev.iadev.quality;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link LintFinding} record.
 *
 * <p>Validates field assignment, equality, and string
 * representation per story-0047-0003 §5.1 data contract.</p>
 */
class LintFindingTest {

    @Test
    void constructor_allSixFields_fieldsExposed() {
        Path path = Path.of("core/ops/x-release/SKILL.md");

        LintFinding finding = new LintFinding(
            path, 1247, Severity.ERROR,
            false, false,
            "msg");

        assertThat(finding.path()).isEqualTo(path);
        assertThat(finding.lineCount()).isEqualTo(1247);
        assertThat(finding.severity()).isEqualTo(Severity.ERROR);
        assertThat(finding.hasReferencesDir()).isFalse();
        assertThat(finding.referencesNonEmpty()).isFalse();
        assertThat(finding.message()).isEqualTo("msg");
    }

    @Test
    void equals_sameFields_returnsTrue() {
        Path path = Path.of("a/SKILL.md");
        LintFinding a = new LintFinding(
            path, 100, Severity.INFO, false, false, "m");
        LintFinding b = new LintFinding(
            path, 100, Severity.INFO, false, false, "m");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentLineCount_returnsFalse() {
        Path path = Path.of("a/SKILL.md");
        LintFinding a = new LintFinding(
            path, 100, Severity.INFO, false, false, "m");
        LintFinding b = new LintFinding(
            path, 101, Severity.INFO, false, false, "m");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toString_containsAllFields() {
        LintFinding finding = new LintFinding(
            Path.of("x/SKILL.md"),
            555, Severity.WARN,
            true, false,
            "review");

        String repr = finding.toString();
        assertThat(repr).contains("x/SKILL.md");
        assertThat(repr).contains("555");
        assertThat(repr).contains("WARN");
        assertThat(repr).contains("review");
    }

    @Test
    void severity_valuesExist_threeConstants() {
        assertThat(Severity.values()).containsExactly(
            Severity.INFO,
            Severity.WARN,
            Severity.ERROR);
    }
}
