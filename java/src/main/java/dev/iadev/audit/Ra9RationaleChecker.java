package dev.iadev.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks section 8 (Decision Rationale) of planning artifacts
 * for RA9 compliance (RULE-006 EPIC-0056 — {@code RA9_RATIONALE_EMPTY}).
 *
 * <p>Applies to Epic and Story files. Task files accept
 * {@code N/A — <reason>} and are not flagged.</p>
 *
 * <p>Assumes {@link Ra9SectionsChecker} already validates section
 * presence; this checker only runs when section 8 exists.</p>
 */
public final class Ra9RationaleChecker {

    private static final String EXEMPT_MARKER =
            "<!-- audit-exempt -->";

    private static final String SECTION_8_HEADER =
            "## 8. Decision Rationale";

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{[^}]+}}");

    private static final String TODO_MARKER = "TODO";

    private static final List<String> REQUIRED_FIELDS = List.of(
            "**Decisão:**",
            "**Motivo:**",
            "**Alternativa descartada:**",
            "**Consequência:**"
    );

    /**
     * Checks section 8 content for valid Decision Rationale.
     *
     * @param content  the full markdown content of the artifact
     * @param filename the filename (for error messages)
     * @return list of violation strings; empty when compliant
     */
    public List<String> check(String content, String filename) {
        if (content.contains(EXEMPT_MARKER)) {
            return List.of();
        }

        int sectionStart = content.indexOf(SECTION_8_HEADER);
        if (sectionStart < 0) {
            return List.of();
        }

        String sectionBody = extractSectionBody(
                content, sectionStart);

        if (isTaskArtifact(filename) && isNaAccepted(sectionBody)) {
            return List.of();
        }

        List<String> violations = new ArrayList<>();

        if (isEmptyOrInvalid(sectionBody)) {
            violations.add(buildViolation(filename));
            return violations;
        }

        boolean hasAllFields = REQUIRED_FIELDS.stream()
                .allMatch(sectionBody::contains);

        if (!hasAllFields) {
            violations.add(buildViolation(filename));
        }

        return violations;
    }

    private String extractSectionBody(
            String content, int sectionStart) {
        int bodyStart = content.indexOf('\n', sectionStart);
        if (bodyStart < 0) {
            return "";
        }
        int nextSection = content.indexOf("\n## ", bodyStart + 1);
        String body = nextSection > 0
                ? content.substring(bodyStart, nextSection)
                : content.substring(bodyStart);
        return body.strip();
    }

    private boolean isNaAccepted(String body) {
        return body.startsWith("N/A") || body.startsWith("n/a");
    }

    /**
     * N/A is accepted only for Task artifacts. Epic and Story
     * MUST provide the full 4-line micro-template (Rule-002 of
     * EPIC-0056). Detection by filename: {@code task-*.md} or
     * any path segment containing {@code /task-}.
     */
    private boolean isTaskArtifact(String filename) {
        String normalized = filename.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String base = lastSlash >= 0
                ? normalized.substring(lastSlash + 1)
                : normalized;
        return base.startsWith("task-");
    }

    private boolean isEmptyOrInvalid(String body) {
        if (body.isBlank()) {
            return true;
        }
        if (PLACEHOLDER_PATTERN.matcher(body).find()) {
            return true;
        }
        if (body.contains(TODO_MARKER)) {
            return true;
        }
        return false;
    }

    private String buildViolation(String filename) {
        return "[RA9_RATIONALE_EMPTY] " + filename
                + "\n  Section 8 (Decision Rationale) is empty,"
                + " contains a placeholder, or TODO."
                + "\n  Required: **Decisão:** / **Motivo:** /"
                + " **Alternativa descartada:** / **Consequência:**"
                + "\n  Task accepts: N/A — <short reason>"
                + "\n  Exempt with <!-- audit-exempt --> (last-resort only).";
    }
}
