package dev.iadev.cli;

/**
 * Formats a {@link ProjectSummary} for terminal display.
 *
 * <p>Extracted from {@link InteractivePrompter} to keep
 * both classes under 250 lines per RULE-004.</p>
 *
 * @see InteractivePrompter
 */
final class ProjectSummaryFormatter {

    private ProjectSummaryFormatter() {
        // utility class
    }

    /**
     * Displays the project summary on the given terminal.
     *
     * @param terminal the terminal provider
     * @param ps the project summary to display
     */
    static void displaySummary(
            TerminalProvider terminal,
            ProjectSummary ps) {
        String langDisplay =
                formatLanguageDisplay(ps.language());
        String text = formatSummaryText(ps, langDisplay);
        terminal.display(text);
    }

    /**
     * Formats the language name with its default version.
     *
     * @param language the language name
     * @return formatted string (e.g., "java 21")
     */
    static String formatLanguageDisplay(String language) {
        String langVersion =
                LanguageFrameworkMapping
                        .defaultVersionFor(language);
        return langVersion.isEmpty()
                ? language
                : language + " " + langVersion;
    }

    /**
     * Builds the full summary text block.
     *
     * @param ps the project summary
     * @param langDisplay the formatted language string
     * @return multi-line summary text
     */
    static String formatSummaryText(
            ProjectSummary ps, String langDisplay) {
        String db = ps.database().isBlank()
                ? "none" : ps.database();
        String ch = ps.cache().isBlank()
                ? "none" : ps.cache();
        String complianceDisplay =
                ps.compliance().isEmpty()
                        ? "none"
                        : String.join(", ",
                        ps.compliance());
        var sb = new StringBuilder();
        sb.append("""

                Project Configuration Summary:
                  Name:          %s
                  Purpose:       %s
                  Architecture:  %s
                  Language:       %s
                  Framework:      %s
                  Build Tool:     %s
                  Interfaces:     %s
                  Database:       %s
                  Cache:          %s
                """.formatted(
                ps.name(), ps.purpose(), ps.archStyle(),
                langDisplay, ps.framework(),
                ps.buildTool(),
                String.join(", ", ps.interfaces()),
                db, ch));
        appendArchPatternSummary(sb, ps);
        sb.append("  Compliance:    %s%n"
                .formatted(complianceDisplay));
        return sb.toString();
    }

    private static void appendArchPatternSummary(
            StringBuilder sb, ProjectSummary ps) {
        if (!ps.archPatternStyle().isEmpty()) {
            sb.append("  Arch Pattern:  %s%n"
                    .formatted(ps.archPatternStyle()));
            if (ps.validateArchUnit()) {
                sb.append("  ArchUnit:      yes%n");
            }
        }
    }
}
