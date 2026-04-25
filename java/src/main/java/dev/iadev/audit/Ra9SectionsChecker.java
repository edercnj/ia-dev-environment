package dev.iadev.audit;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks planning artifacts for the 9 mandatory RA9 section
 * headers (RULE-006 EPIC-0056 — {@code RA9_SECTIONS_MISSING}).
 *
 * <p>Applies to Epic, Story, and Task files. Skips files
 * that start with the {@code <!-- audit-exempt -->} marker.</p>
 */
public final class Ra9SectionsChecker {

    private static final String EXEMPT_MARKER =
            "<!-- audit-exempt -->";

    static final List<String> REQUIRED_SECTIONS = List.of(
            "## 1. Contexto & Escopo",
            "## 2. Packages (Hexagonal)",
            "## 3. Contratos & Endpoints",
            "## 4. Materialização SOLID",
            "## 5. Quality Gates",
            "## 6. Segurança",
            "## 7. Observabilidade",
            "## 8. Decision Rationale",
            "## 9. Dependências & File Footprint"
    );

    /**
     * Checks the given artifact content for missing RA9 sections.
     *
     * @param content  the full markdown content of the artifact
     * @param filename the filename (for error messages)
     * @return list of violation strings; empty when compliant
     */
    public List<String> check(String content, String filename) {
        if (content.contains(EXEMPT_MARKER)) {
            return List.of();
        }

        List<String> violations = new ArrayList<>();
        for (String section : REQUIRED_SECTIONS) {
            if (!content.contains(section)) {
                violations.add(buildViolation(section, filename));
            }
        }
        return violations;
    }

    private String buildViolation(String section, String filename) {
        return "[RA9_SECTIONS_MISSING] " + filename
                + "\n  Missing section: " + section
                + "\n  Fix: add the exact header per _TEMPLATE-EPIC/STORY/TASK.md v2."
                + "\n  Exempt with <!-- audit-exempt --> (last-resort only).";
    }
}
