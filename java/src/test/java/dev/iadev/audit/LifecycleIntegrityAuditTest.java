package dev.iadev.audit;

import dev.iadev.application.lifecycle.LifecycleAuditRunner;
import dev.iadev.application.lifecycle.Violation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CI-blocking audit: runs {@link LifecycleAuditRunner} over the
 * production skills tree and asserts there are no
 * Rule 22 / EPIC-0046 regressions.
 *
 * <p>A file-based baseline at
 * {@code audits/lifecycle-integrity-baseline.txt} lists
 * currently accepted {@code dimension|fileRelative|line}
 * triples; these are tolerated while retrofits ripple through
 * the codebase. Any NEW violation (not in the baseline) fails
 * the build with exit code {@code LIFECYCLE_AUDIT_REGRESSION}.</p>
 */
@DisplayName("CI — Lifecycle Integrity Audit (EPIC-0046)")
class LifecycleIntegrityAuditTest {

    private static final Path SKILLS_ROOT = Path.of(
            "src/main/resources/targets/claude/skills");

    private static final Path BASELINE = Path.of(
            "../audits/lifecycle-integrity-baseline.txt");

    @Test
    @DisplayName("skills tree contains no new lifecycle "
            + "regressions beyond baseline")
    void skillsTree_noNewViolations() throws IOException {
        // Probe both possible roots: tests may run from
        // repo root (/Users/.../ia-dev-environment) or the
        // java/ module directory.
        Path root = SKILLS_ROOT;
        if (!Files.isDirectory(root)) {
            root = Path.of(
                    "java/src/main/resources/targets/claude/"
                            + "skills");
        }
        assertThat(Files.isDirectory(root))
                .as("skills root resolvable")
                .isTrue();

        List<Violation> all =
                new LifecycleAuditRunner().scan(root);

        Set<String> baseline = loadBaseline();
        List<Violation> regressions = all.stream()
                .filter(v -> !baseline.contains(key(v))
                        && !baseline.contains(keyNoLine(v)))
                .collect(Collectors.toList());

        if (!regressions.isEmpty()) {
            System.err.println(
                    "LIFECYCLE_AUDIT_REGRESSION: "
                            + regressions.size()
                            + " new violation(s) (baseline "
                            + "has " + baseline.size() + ")");
            for (Violation v : regressions) {
                System.err.println("  " + key(v) + " — "
                        + v.detail());
            }
        } else {
            System.out.println(
                    "Lifecycle integrity audit: 0 new "
                            + "violations (scanned "
                            + countSkills(root)
                            + " SKILL.md; baseline "
                            + baseline.size() + ")");
        }

        assertThat(regressions)
                .as("no new Rule 22 regressions "
                        + "beyond baseline")
                .isEmpty();
    }

    private static String key(Violation v) {
        return v.dimension() + "|" + relPath(v) + "|"
                + v.line();
    }

    private static String keyNoLine(Violation v) {
        return v.dimension() + "|" + relPath(v);
    }

    private static String relPath(Violation v) {
        String rel = v.file().toString();
        int idx = rel.indexOf("targets/claude/skills/");
        if (idx >= 0) {
            rel = rel.substring(idx);
        }
        return rel;
    }

    private static Set<String> loadBaseline()
            throws IOException {
        // Try module-relative path first, then repo-relative.
        Path b = BASELINE;
        if (!Files.isRegularFile(b)) {
            b = Path.of("audits/lifecycle-integrity-"
                    + "baseline.txt");
        }
        if (!Files.isRegularFile(b)) {
            return Set.of();
        }
        return Files.readAllLines(b).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty()
                        && !s.startsWith("#"))
                // baseline entries use only dim|relPath — the
                // line number drifts on formatting changes, so
                // we tolerate any line by matching on the first
                // two columns. We fan out by stripping the line
                // suffix when matching.
                .collect(Collectors.toSet());
    }

    private static long countSkills(Path root)
            throws IOException {
        try (var s = Files.walk(root)) {
            return s.filter(p -> p.getFileName().toString()
                    .equals("SKILL.md")).count();
        }
    }
}
