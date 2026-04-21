package dev.iadev.quality;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Walks a skills root directory and emits {@link LintFinding}
 * entries, one per {@code SKILL.md}, classified into three
 * severity tiers (RULE-047-04, story-0047-0003 §5.2).
 *
 * <p>Tiers:
 * <ul>
 *   <li>&lt;250 lines &rarr; {@link Severity#INFO} (silent pass)</li>
 *   <li>250-500 lines &rarr; {@link Severity#WARN} (advisory)</li>
 *   <li>&gt;500 lines without non-empty {@code references/}
 *       &rarr; {@link Severity#ERROR}</li>
 *   <li>&gt;500 lines with non-empty {@code references/}
 *       &rarr; {@link Severity#INFO} (orchestrator carve-out)</li>
 * </ul>
 *
 * <p>Exclusions (story §5.3):
 * <ul>
 *   <li>{@code _shared/**} — not a skill directory.</li>
 *   <li>Non-{@code SKILL.md} markdown files.</li>
 * </ul>
 *
 * <p>Pure function given a filesystem state; fully testable with
 * {@code @TempDir}. No mutable state; no dependencies outside the
 * standard library.
 */
public final class SkillSizeLinter {

    /** Hard threshold above which a SKILL.md must carve out. */
    public static final int ERROR_THRESHOLD_LINES = 500;

    /** Lower bound of the WARN tier (inclusive). */
    public static final int WARN_THRESHOLD_LINES = 250;

    private static final String SKILL_FILENAME = "SKILL.md";
    private static final String REFERENCES_DIR = "references";
    private static final String SHARED_DIR = "_shared";
    private static final String README_FILENAME = "README.md";

    private SkillSizeLinter() {
    }

    /**
     * Walks {@code skillsRoot} and returns one finding per
     * {@code SKILL.md}. Returns an empty list for an empty tree.
     *
     * @param skillsRoot root directory to scan
     *        (e.g., {@code targets/claude/skills/})
     * @return findings, one per SKILL.md encountered
     */
    public static List<LintFinding> lint(Path skillsRoot) {
        if (!Files.isDirectory(skillsRoot)) {
            return List.of();
        }
        List<LintFinding> findings = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(skillsRoot)) {
            stream
                .filter(SkillSizeLinter::isSkillMdCandidate)
                .forEach(p -> findings.add(
                    classify(skillsRoot, p)));
        } catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to walk " + skillsRoot, e);
        }
        return findings;
    }

    /**
     * Filters a list to only ERROR findings. Convenience for
     * acceptance tests that want "fail on any error".
     *
     * @param findings full list from {@link #lint(Path)}
     * @return sub-list of ERROR-severity findings
     */
    public static List<LintFinding> errorFindings(
            List<LintFinding> findings) {
        List<LintFinding> errors = new ArrayList<>();
        for (LintFinding f : findings) {
            if (f.severity() == Severity.ERROR) {
                errors.add(f);
            }
        }
        return errors;
    }

    private static boolean isSkillMdCandidate(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        if (!path.getFileName().toString()
                .equals(SKILL_FILENAME)) {
            return false;
        }
        for (Path segment : path) {
            if (segment.toString().equals(SHARED_DIR)) {
                return false;
            }
        }
        return true;
    }

    private static LintFinding classify(
            Path root, Path skillMd) {
        int lineCount = countLines(skillMd);
        Path skillDir = skillMd.getParent();
        Path refsDir = skillDir.resolve(REFERENCES_DIR);
        boolean hasRefsDir = Files.isDirectory(refsDir);
        boolean refsNonEmpty = hasRefsDir
            && hasNonReadmeMarkdown(refsDir);
        Severity severity = pickSeverity(
            lineCount, refsNonEmpty);
        Path relative = root.relativize(skillMd);
        String message = buildMessage(
            relative, lineCount,
            hasRefsDir, refsNonEmpty, severity);
        return new LintFinding(
            relative, lineCount, severity,
            hasRefsDir, refsNonEmpty, message);
    }

    private static Severity pickSeverity(
            int lineCount, boolean refsNonEmpty) {
        if (lineCount < WARN_THRESHOLD_LINES) {
            return Severity.INFO;
        }
        if (lineCount <= ERROR_THRESHOLD_LINES) {
            return Severity.WARN;
        }
        return refsNonEmpty ? Severity.INFO : Severity.ERROR;
    }

    private static int countLines(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return (int) lines.count();
        } catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to count lines: " + file, e);
        }
    }

    private static boolean hasNonReadmeMarkdown(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .anyMatch(SkillSizeLinter::isNonReadmeMarkdown);
        } catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to list " + dir, e);
        }
    }

    private static boolean isNonReadmeMarkdown(String name) {
        return name.endsWith(".md")
            && !name.equalsIgnoreCase(README_FILENAME);
    }

    private static String buildMessage(
            Path relative, int lineCount,
            boolean hasRefsDir, boolean refsNonEmpty,
            Severity severity) {
        if (severity != Severity.ERROR) {
            return String.format(
                "%s: %d lines, severity=%s",
                relative, lineCount, severity);
        }
        return buildErrorMessage(
            relative, lineCount, hasRefsDir, refsNonEmpty);
    }

    private static String buildErrorMessage(
            Path relative, int lineCount,
            boolean hasRefsDir, boolean refsNonEmpty) {
        String nl = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        appendErrorHeader(builder, relative, lineCount, nl);
        if (hasRefsDir && !refsNonEmpty) {
            builder.append(
                "        references/ must contain >= 1 .md"
                + " file other than README.md.")
                .append(nl);
        }
        appendErrorSuggestions(builder, nl);
        return builder.toString();
    }

    private static void appendErrorHeader(
            StringBuilder builder,
            Path relative, int lineCount, String nl) {
        builder.append("[ERROR] SkillSizeLinter: ")
            .append(relative)
            .append(" exceeds ")
            .append(ERROR_THRESHOLD_LINES)
            .append(" lines (current: ")
            .append(lineCount)
            .append(") without a non-empty references/ sibling.")
            .append(nl);
    }

    private static void appendErrorSuggestions(
            StringBuilder builder, String nl) {
        builder.append("        Suggestions:").append(nl)
            .append("        1. Carve out detail to")
            .append(" references/full-protocol.md")
            .append(" (see ADR-0007).").append(nl)
            .append("        2. Carve out shared content to")
            .append(" _shared/ (see ADR-0011).").append(nl)
            .append("        3. Split into multiple skills")
            .append(" (only if scope justifies).").append(nl)
            .append("        Threshold: <= ")
            .append(ERROR_THRESHOLD_LINES)
            .append(" lines OR > ")
            .append(ERROR_THRESHOLD_LINES)
            .append(" with references/ containing 1+ .md files.");
    }
}
