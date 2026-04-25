package dev.iadev.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks section 2 (Packages Hexagonal) of planning artifacts
 * for RA9 compliance (RULE-006 EPIC-0056 — {@code RA9_PACKAGES_MISSING}).
 *
 * <p>Applies to Epic, Story, and Task files. Section 2 must list
 * at least one layer with content (not just {@code —}).</p>
 */
public final class Ra9PackagesChecker {

    private static final String EXEMPT_MARKER =
            "<!-- audit-exempt -->";

    private static final String SECTION_2_HEADER =
            "## 2. Packages (Hexagonal)";

    /**
     * Bullet-list form: {@code - `path/to/File.java`} or
     * {@code * `path/to/File.java`} — used by Epic and Story templates.
     */
    private static final Pattern BULLET_LAYER_PATTERN =
            Pattern.compile("^[-*]\\s+`[^`]+`",
                    Pattern.MULTILINE);

    /**
     * Markdown-table form (used by the Task template):
     * {@code | <layer> | `path/to/File.java` | <action> |}.
     * The row must contain a backticked path inside a table cell
     * (line starts with `|` and contains a backtick-wrapped token).
     */
    private static final Pattern TABLE_LAYER_PATTERN =
            Pattern.compile("^\\|[^\\n]*`[^`]+`[^\\n]*\\|",
                    Pattern.MULTILINE);

    /**
     * Checks section 2 for at least one non-dash layer entry.
     *
     * <p>Accepts both formats used by RA9 v2 templates:
     * <ul>
     *   <li>Bullet list ({@code - `path`}) — Epic, Story.</li>
     *   <li>Markdown table row with backticked path — Task.</li>
     * </ul>
     *
     * @param content  the full markdown content
     * @param filename filename for error messages
     * @return violation list; empty when compliant
     */
    public List<String> check(String content, String filename) {
        if (content.contains(EXEMPT_MARKER)) {
            return List.of();
        }

        int sectionStart = content.indexOf(SECTION_2_HEADER);
        if (sectionStart < 0) {
            return List.of();
        }

        String sectionBody = extractSectionBody(
                content, sectionStart);

        boolean hasBulletEntry =
                BULLET_LAYER_PATTERN.matcher(sectionBody).find();
        boolean hasTableEntry =
                TABLE_LAYER_PATTERN.matcher(sectionBody).find();

        if (!hasBulletEntry && !hasTableEntry) {
            return List.of(buildViolation(filename));
        }

        return List.of();
    }

    private String extractSectionBody(
            String content, int sectionStart) {
        int bodyStart = content.indexOf('\n', sectionStart);
        if (bodyStart < 0) {
            return "";
        }
        int nextSection = content.indexOf("\n## ", bodyStart + 1);
        return nextSection > 0
                ? content.substring(bodyStart, nextSection)
                : content.substring(bodyStart);
    }

    private String buildViolation(String filename) {
        return "[RA9_PACKAGES_MISSING] " + filename
                + "\n  Section 2 (Packages Hexagonal) has no layer"
                + " entries (all marked '—' or empty)."
                + "\n  Fix: list at least 1 package in any of the"
                + " 5 layers (domain, application, adapter.inbound,"
                + " adapter.outbound, infrastructure)."
                + "\n  Exempt with <!-- audit-exempt --> (last-resort only).";
    }
}
