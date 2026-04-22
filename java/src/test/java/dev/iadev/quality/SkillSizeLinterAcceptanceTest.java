package dev.iadev.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance test: runs {@link SkillSizeLinter} against the real
 * source-of-truth tree at
 * {@code java/src/main/resources/targets/claude/skills/} and
 * fails the build if a NEW offender (not in
 * {@code audits/skill-size-baseline.txt}) appears.
 *
 * <p>Brownfield policy (story-0047-0003 §4 DoR Local): at the
 * time this story landed, 25 SKILL.md files exceeded 500 lines
 * without a non-empty {@code references/} sibling. These are
 * enumerated in the baseline file and are tolerated. Stories
 * 0047-0002 (flipped orientation) and 0047-0004 (KP sweep) will
 * carve out those offenders; this test is the guard-rail that
 * prevents REGRESSIONS while that work proceeds.
 *
 * <p>Runs in the default {@code mvn test} scope (not a separate
 * profile). Target: &lt;1s for the full corpus.
 */
class SkillSizeLinterAcceptanceTest {

    private static final Path SKILLS_ROOT = Path.of(
        "src", "main", "resources",
        "targets", "claude", "skills");

    private static final Path BASELINE_FILE = Path.of(
        "..", "audits", "skill-size-baseline.txt");

    @Test
    void lint_realCorpus_noNewViolations() throws IOException {
        Path root = SKILLS_ROOT.toAbsolutePath();
        assertThat(Files.isDirectory(root))
            .as("Skills root must exist: %s", root)
            .isTrue();

        List<LintFinding> findings = SkillSizeLinter.lint(root);
        Set<String> baseline = loadBaseline();

        List<String> newViolations = findings.stream()
            .filter(f -> f.severity() == Severity.ERROR)
            .map(f -> normalize(f.path()))
            .filter(p -> !baseline.contains(p))
            .collect(Collectors.toList());

        assertThat(newViolations)
            .as("SKILL_SIZE_REGRESSION: new SKILL.md file(s)"
                + " exceed %d lines without a non-empty"
                + " references/ sibling. Either carve out the"
                + " content (see ADR-0011 / ADR-0007) or add"
                + " the path to audits/skill-size-baseline.txt"
                + " (discouraged -- prefer carve-out).",
                SkillSizeLinter.ERROR_THRESHOLD_LINES)
            .isEmpty();
    }

    @Test
    void baseline_file_existsAndIsNonEmpty() throws IOException {
        Path baseline = BASELINE_FILE.toAbsolutePath();
        assertThat(Files.isRegularFile(baseline))
            .as("Baseline file must exist: %s", baseline)
            .isTrue();

        List<String> entries = Files.readAllLines(
                baseline, StandardCharsets.UTF_8).stream()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .filter(line -> !line.startsWith("#"))
            .collect(Collectors.toList());

        assertThat(entries)
            .as("Baseline file must list at least one"
                + " current offender; if empty, switch the"
                + " acceptance test to pure green-field mode.")
            .isNotEmpty();
    }

    @Test
    void baseline_stillMatchesReality_noStaleEntries()
            throws IOException {
        Path root = SKILLS_ROOT.toAbsolutePath();
        List<LintFinding> findings = SkillSizeLinter.lint(root);
        Set<String> currentErrors = findings.stream()
            .filter(f -> f.severity() == Severity.ERROR)
            .map(f -> normalize(f.path()))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> baseline = loadBaseline();
        Set<String> staleEntries = baseline.stream()
            .filter(entry -> !currentErrors.contains(entry))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(staleEntries)
            .as("Baseline contains entries that no longer"
                + " violate the threshold. Remove them from"
                + " audits/skill-size-baseline.txt to keep"
                + " the guard-rail tight.")
            .isEmpty();
    }

    private static Set<String> loadBaseline()
            throws IOException {
        Path baseline = BASELINE_FILE.toAbsolutePath();
        if (!Files.isRegularFile(baseline)) {
            return Set.of();
        }
        return Files.readAllLines(
                baseline, StandardCharsets.UTF_8).stream()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .filter(line -> !line.startsWith("#"))
            .map(SkillSizeLinterAcceptanceTest::normalize)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalize(Path relative) {
        return relative.toString().replace('\\', '/');
    }

    private static String normalize(String raw) {
        return raw.replace('\\', '/');
    }
}
