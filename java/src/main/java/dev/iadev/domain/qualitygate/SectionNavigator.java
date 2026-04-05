package dev.iadev.domain.qualitygate;

import java.util.Locale;

/**
 * Shared helpers for navigating markdown sections.
 */
final class SectionNavigator {

    private SectionNavigator() {
    }

    static int skipToSection(
            String[] lines, int start,
            String sectionName) {
        for (int i = start; i < lines.length; i++) {
            if (sectionMatches(lines[i], sectionName)) {
                return i + 1;
            }
        }
        return lines.length;
    }

    static boolean sectionMatches(
            String line, String sectionName) {
        var trimmed = line.trim()
                .toLowerCase(Locale.ROOT);
        return trimmed.startsWith("#")
                && trimmed.contains(sectionName);
    }

    static boolean isNextSection(String line) {
        return line.trim().startsWith("#");
    }
}
