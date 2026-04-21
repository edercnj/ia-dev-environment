package dev.iadev.application.lifecycle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans a skills directory for lifecycle-integrity violations
 * as defined in Rule 22 and EPIC-0046 story-0046-0007.
 *
 * <p>Three dimensions are enforced:</p>
 * <ul>
 *   <li>{@code ORPHAN_PHASE} — a numbered section documented in
 *       the SKILL.md (e.g. {@code ## Phase 3.8}, {@code ##
 *       Section 1.6b}) that is not referenced by any Core Loop
 *       / Workflow section of the same skill.</li>
 *   <li>{@code WRITE_WITHOUT_COMMIT} — a write to {@code
 *       plans/epic-*&#47;reports/} that is not followed within
 *       20 lines by an invocation of the {@code x-git-commit}
 *       skill (Rule 22 RULE-046-05).</li>
 *   <li>{@code SKIP_IN_HAPPY_PATH} — a {@code --skip-verification}
 *       / {@code --skip-status-sync} flag referenced inside a
 *       Core Loop / Workflow section (not in a Recovery / Error
 *       Handling section).</li>
 * </ul>
 *
 * <p>Lines preceded by the escape hatch {@code
 * <!-- audit-exempt -->} are ignored. Scanner is robust to
 * unreadable files (skipped with no violation rather than
 * fatal).</p>
 */
public final class LifecycleAuditRunner {

    /** Dimension codes surfaced on {@link Violation#dimension()}. */
    public static final String DIM_ORPHAN_PHASE = "ORPHAN_PHASE";
    public static final String DIM_WRITE_WITHOUT_COMMIT =
            "WRITE_WITHOUT_COMMIT";
    public static final String DIM_SKIP_IN_HAPPY_PATH =
            "SKIP_IN_HAPPY_PATH";

    private static final Pattern HEADING =
            Pattern.compile(
                    "^(#{2,4})\\s+(?:Phase|Section|Step|"
                            + "Fase|Etapa|Passo)\\s+"
                            + "([0-9]+(?:\\.[0-9A-Za-z]+)*)"
                            + ".*$");

    private static final Pattern PHASE_REF =
            Pattern.compile(
                    "(?:Phase|Section|Step|Fase|Etapa|Passo)\\s+"
                            + "([0-9]+(?:\\.[0-9A-Za-z]+)*)");

    private static final Pattern WRITE_REPORT =
            Pattern.compile(
                    "(?i)(?:write|save|gravar?|emit)\\s+(?:to\\s+)?"
                            + "[`'\"]?plans/epic-\\S+/reports/");

    private static final Pattern COMMIT_MARK =
            Pattern.compile(
                    "(?:Skill\\(\\s*skill:\\s*\"x-git-commit\""
                            + "|x-git-commit|git commit)");

    private static final Pattern SKIP_FLAG =
            Pattern.compile(
                    "--skip-(?:verification|status-sync)");

    private static final Pattern EXEMPT =
            Pattern.compile("<!--\\s*audit-exempt\\s*-->");

    /** The 6 heading patterns that indicate a Core Loop section. */
    private static final Pattern CORE_LOOP_HEADING =
            Pattern.compile(
                    "(?i)^#{2,4}\\s*.*(core\\s*loop|workflow|"
                            + "phases?\\s+overview|execution\\s+flow"
                            + "|phase\\s+[0-9]|happy\\s*path).*$");

    private static final Pattern RECOVERY_HEADING =
            Pattern.compile(
                    "(?i)^#{2,4}\\s*.*(recovery|error\\s+handling|"
                            + "troubleshoot|resume|fallback).*$");

    /**
     * Scans the given root directory and returns all lifecycle
     * violations. Returns an empty list (never null) when the
     * directory is null, missing, or empty.
     *
     * @param skillsRoot directory to scan (typically
     *     {@code java/src/main/resources/targets/claude/skills})
     * @return the violations found; never null
     */
    public List<Violation> scan(Path skillsRoot) {
        if (skillsRoot == null || !Files.isDirectory(skillsRoot)) {
            return List.of();
        }
        List<Violation> all = new ArrayList<>();
        try {
            Files.walkFileTree(skillsRoot, new SkillVisitor(all));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return all;
    }

    private static final class SkillVisitor
            extends SimpleFileVisitor<Path> {
        private final List<Violation> sink;

        SkillVisitor(List<Violation> sink) {
            this.sink = sink;
        }

        @Override
        public FileVisitResult visitFile(Path f,
                BasicFileAttributes a) {
            if (f.getFileName().toString().equals("SKILL.md")) {
                scanFile(f, sink);
            }
            return FileVisitResult.CONTINUE;
        }
    }

    static void scanFile(Path file, List<Violation> out) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            return;
        }
        detectOrphanPhases(file, lines, out);
        detectWriteWithoutCommit(file, lines, out);
        detectSkipInHappyPath(file, lines, out);
    }

    // Dimension 1
    static void detectOrphanPhases(Path file, List<String> lines,
            List<Violation> out) {
        List<int[]> headings = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        java.util.Set<String> referenced = new java.util.HashSet<>();
        boolean inCoreLoop = false;
        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            if (ln.startsWith("#")) {
                if (CORE_LOOP_HEADING.matcher(ln).matches()) {
                    inCoreLoop = true;
                } else if (RECOVERY_HEADING.matcher(ln).matches()) {
                    inCoreLoop = false;
                } else if (ln.startsWith("## ")) {
                    // generic section boundary
                    inCoreLoop = CORE_LOOP_HEADING
                            .matcher(ln).matches();
                }
            }
            Matcher m = HEADING.matcher(ln);
            if (m.matches()) {
                headings.add(new int[]{i + 1});
                ids.add(m.group(2));
            }
            if (inCoreLoop) {
                Matcher r = PHASE_REF.matcher(ln);
                while (r.find()) {
                    referenced.add(r.group(1));
                }
            }
        }
        // Also collect Phase refs from ANY line (liberal — the
        // heuristic "referenced somewhere in the skill body"
        // reduces false positives for skills that document their
        // core loop implicitly via prose).
        for (String ln : lines) {
            Matcher r = PHASE_REF.matcher(ln);
            while (r.find()) {
                referenced.add(r.group(1));
            }
        }
        for (int idx = 0; idx < ids.size(); idx++) {
            String id = ids.get(idx);
            int line = headings.get(idx)[0];
            if (isExempt(lines, line - 1)) {
                continue;
            }
            // Only flag sub-sections (dotted IDs like "1.6b",
            // "3.8") as orphans. Top-level numbered headings
            // (Phase 1, Step 2) are almost always TOC-style
            // and lack cross-references in many review skills
            // by design — flagging them yields noise, not
            // signal. The story's motivating gap was
            // x-epic-implement Section 1.6b (a dotted
            // sub-section promoted to Phase 1.7).
            if (!id.contains(".")) {
                continue;
            }
            // A heading is considered referenced when its own
            // id (or any prefix) appears in the reference set
            // (e.g. "3" covers "3.8"). Since the heading itself
            // also matches the PHASE_REF pattern on the heading
            // line, we need a cross-line check — exclude its
            // own heading occurrence from the match set.
            if (!hasOtherReference(lines, line - 1, id)) {
                out.add(new Violation(DIM_ORPHAN_PHASE, file,
                        line,
                        "Phase/Section " + id
                                + " documented but not "
                                + "referenced elsewhere"));
            }
        }
    }

    private static boolean hasOtherReference(List<String> lines,
            int headingIdx, String id) {
        for (int i = 0; i < lines.size(); i++) {
            if (i == headingIdx) {
                continue;
            }
            Matcher m = PHASE_REF.matcher(lines.get(i));
            while (m.find()) {
                String found = m.group(1);
                // Exact match OR the found ref is a more
                // specific descendant of this heading
                // (e.g. heading "1.6" referenced by "1.6b").
                // We do NOT count a parent ref ("Phase 1")
                // as referencing a child ("1.6b") — the
                // whole point of the audit is to ensure
                // dotted sub-sections are individually
                // wired into the flow.
                if (found.equals(id)
                        || found.startsWith(id + ".")
                        || found.startsWith(id)
                                && found.length() > id.length()
                                && !Character.isDigit(
                                        found.charAt(
                                                id.length()))) {
                    return true;
                }
            }
        }
        return false;
    }

    // Dimension 2
    static void detectWriteWithoutCommit(Path file,
            List<String> lines, List<Violation> out) {
        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            if (!WRITE_REPORT.matcher(ln).find()) {
                continue;
            }
            if (isExempt(lines, i)) {
                continue;
            }
            boolean committed = false;
            int windowEnd = Math.min(lines.size(), i + 21);
            for (int j = i + 1; j < windowEnd; j++) {
                if (COMMIT_MARK.matcher(lines.get(j)).find()) {
                    committed = true;
                    break;
                }
            }
            if (!committed) {
                out.add(new Violation(DIM_WRITE_WITHOUT_COMMIT,
                        file, i + 1,
                        "Write to plans/epic-*/reports/ "
                                + "without x-git-commit in next "
                                + "20 lines"));
            }
        }
    }

    // Dimension 3
    static void detectSkipInHappyPath(Path file,
            List<String> lines, List<Violation> out) {
        boolean inRecovery = false;
        boolean inFrontmatter = false;
        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            // YAML frontmatter delimited by --- lines at the
            // very top of the file. The argument-hint field
            // legitimately mentions skip flags.
            if (i == 0 && ln.trim().equals("---")) {
                inFrontmatter = true;
                continue;
            }
            if (inFrontmatter) {
                if (ln.trim().equals("---")) {
                    inFrontmatter = false;
                }
                continue;
            }
            if (ln.startsWith("## ") || ln.startsWith("### ")) {
                inRecovery = RECOVERY_HEADING
                        .matcher(ln).matches();
            }
            if (inRecovery) {
                continue;
            }
            Matcher m = SKIP_FLAG.matcher(ln);
            if (!m.find()) {
                continue;
            }
            if (isExempt(lines, i)) {
                continue;
            }
            // Skip flag lines inside dedicated "CLI Arguments"
            // tables or "## Triggers" / "## Examples" (user-
            // facing) are legitimate documentation.
            if (isDocumentationContext(lines, i)) {
                continue;
            }
            // Inline "Recovery-only" marker on the same or
            // previous line documents the flag as
            // recovery-only (Rule 22 RULE-046-04 explicitly
            // allows this — see x-story-implement line ~1110).
            if (isRecoveryOnlyInline(lines, i)) {
                continue;
            }
            out.add(new Violation(DIM_SKIP_IN_HAPPY_PATH, file,
                    i + 1,
                    "Skip flag '" + m.group()
                            + "' on Core Loop / happy path"));
        }
    }

    private static boolean isRecoveryOnlyInline(
            List<String> lines, int idx) {
        String cur = lines.get(idx).toLowerCase();
        // Prose that describes the flag rather than invokes
        // it: "When --skip-verification is active, ..." /
        // "If --skip-verification is passed, ..." etc.
        if (cur.matches(
                ".*\\b(when|if|unless|only when|only if)\\b.*"
                        + "--skip-(verification|status-sync).*"
                        + "(is |is\\s|been\\s).*")) {
            return true;
        }
        for (int j = Math.max(0, idx - 2);
                j <= Math.min(lines.size() - 1, idx); j++) {
            String lo = lines.get(j).toLowerCase();
            if (lo.contains("recovery-only")
                    || lo.contains("recovery only")
                    || lo.contains("skip condition")
                    || lo.contains("not be used in the happy")
                    || lo.contains("skip_in_happy_path")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDocumentationContext(
            List<String> lines, int idx) {
        for (int j = idx; j >= 0 && j >= idx - 40; j--) {
            String ln = lines.get(j);
            if (ln.startsWith("## ") || ln.startsWith("### ")) {
                String h = ln.toLowerCase();
                return h.contains("cli arg")
                        || h.contains("trigger")
                        || h.contains("example")
                        || h.contains("flag")
                        || h.contains("deprecat")
                        || h.contains("argument");
            }
        }
        return false;
    }

    private static boolean isExempt(List<String> lines, int idx) {
        if (idx > 0 && EXEMPT.matcher(lines.get(idx - 1)).find()) {
            return true;
        }
        return EXEMPT.matcher(lines.get(idx)).find();
    }
}
