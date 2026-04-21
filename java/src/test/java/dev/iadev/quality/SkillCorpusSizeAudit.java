package dev.iadev.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corpus-wide invariant: total LoC across all
 * {@code SKILL.md} files under
 * {@code targets/claude/skills/} must remain below the
 * RULE-047-07 threshold (30,000 lines) at the end of EPIC-0047.
 *
 * <p>Enforcement mode (story-0047-0003 §8 escalation note):
 * <ul>
 *   <li><strong>Soft-warn (default).</strong> The audit logs
 *       the current total and the gap vs. target but does NOT
 *       fail the build. This is the state after story-0047-0003
 *       lands and before stories 0047-0002 / 0047-0004 carve out
 *       enough lines to get below 30k.</li>
 *   <li><strong>Hard-fail.</strong> Activate by setting the
 *       system property
 *       {@code -Dskill.corpus.audit.enforce=true} (typically on
 *       the CI job that runs after 0047-0004 merges). Once total
 *       &lt; 30k, a follow-up PR flips the default in source.</li>
 * </ul>
 */
class SkillCorpusSizeAudit {

    private static final Path SKILLS_ROOT = Path.of(
        "src", "main", "resources",
        "targets", "claude", "skills");

    private static final int CORPUS_TARGET_LINES = 30_000;

    private static final String ENFORCE_PROPERTY =
        "skill.corpus.audit.enforce";

    @Test
    void totalCorpus_belowTarget_orSoftWarn()
            throws IOException {
        Path root = SKILLS_ROOT.toAbsolutePath();
        assertThat(Files.isDirectory(root))
            .as("Skills root must exist: %s", root)
            .isTrue();

        long total = totalLines(root);
        boolean enforce = Boolean.getBoolean(ENFORCE_PROPERTY);

        String msg = buildMessage(total, enforce);

        if (enforce) {
            assertThat(total)
                .as(msg)
                .isLessThan(CORPUS_TARGET_LINES);
        } else {
            System.out.println(msg);
            assertThat(total).isPositive();
        }
    }

    private static long totalLines(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString()
                    .equals("SKILL.md"))
                .mapToLong(SkillCorpusSizeAudit::countLines)
                .sum();
        } catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to walk " + root, e);
        }
    }

    private static long countLines(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.count();
        } catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to count lines: " + file, e);
        }
    }

    private static String buildMessage(
            long total, boolean enforce) {
        long gap = total - CORPUS_TARGET_LINES;
        String mode = enforce ? "HARD-FAIL" : "soft-warn";
        return String.format(
            "[SkillCorpusSizeAudit %s] total=%d lines;"
            + " target=%d; gap=%+d. RULE-047-07 caps the"
            + " corpus at %d lines. Pending EPIC-0047"
            + " stories 0002 (flipped orientation) +"
            + " 0004 (KP sweep) to close the gap. Enforce"
            + " mode via -D%s=true.",
            mode, total, CORPUS_TARGET_LINES, gap,
            CORPUS_TARGET_LINES, ENFORCE_PROPERTY);
    }
}
