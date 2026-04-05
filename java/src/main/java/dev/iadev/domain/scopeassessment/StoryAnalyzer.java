package dev.iadev.domain.scopeassessment;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes story markdown content to extract classification
 * metrics.
 *
 * <p>Parses tech descriptions, data contracts, frontmatter,
 * and implementation maps to count components, endpoints,
 * schema changes, compliance requirements, and dependents.</p>
 */
public final class StoryAnalyzer {

    private static final Pattern FILE_REFERENCE_PATTERN =
            Pattern.compile("`([\\w./-]+\\.\\w+)`");

    private static final Pattern ENDPOINT_PATTERN =
            Pattern.compile(
                    "(?m)^\\s*(?:POST|GET|PUT|DELETE|PATCH)"
                            + "\\s+/\\S+");

    private static final Pattern SCHEMA_PATTERN =
            Pattern.compile(
                    "(?i)(?:migration\\s+script"
                            + "|ALTER\\s+TABLE"
                            + "|CREATE\\s+TABLE"
                            + "|DROP\\s+TABLE"
                            + "|ADD\\s+COLUMN)");

    private static final Pattern COMPLIANCE_PATTERN =
            Pattern.compile(
                    "(?m)^\\s*compliance:\\s*(.+)$");

    private static final Set<String> COMPLIANCE_NONE_VALUES =
            Set.of("none", "");

    private StoryAnalyzer() {
    }

    /**
     * Counts component references in the story content.
     *
     * @param storyContent the full story markdown
     * @return number of distinct components detected
     */
    public static int countComponents(String storyContent) {
        if (storyContent.isEmpty()) {
            return 0;
        }
        var distinctFiles = new HashSet<String>();
        var matcher = FILE_REFERENCE_PATTERN.matcher(storyContent);
        while (matcher.find()) {
            distinctFiles.add(matcher.group(1));
        }
        return distinctFiles.size();
    }

    /**
     * Counts new endpoint declarations in the story content.
     *
     * @param storyContent the full story markdown
     * @return number of new endpoints detected
     */
    public static int countEndpoints(String storyContent) {
        if (storyContent.isEmpty()) {
            return 0;
        }
        var distinctEndpoints = new HashSet<String>();
        var matcher = ENDPOINT_PATTERN.matcher(storyContent);
        while (matcher.find()) {
            distinctEndpoints.add(matcher.group().trim());
        }
        return distinctEndpoints.size();
    }

    /**
     * Checks if the story mentions schema changes.
     *
     * @param storyContent the full story markdown
     * @return true if schema changes are detected
     */
    public static boolean hasSchemaChanges(String storyContent) {
        if (storyContent.isEmpty()) {
            return false;
        }
        return SCHEMA_PATTERN.matcher(storyContent).find();
    }

    /**
     * Checks if the story has compliance requirements.
     *
     * @param storyContent the full story markdown
     * @return true if compliance requirement is detected
     */
    public static boolean hasCompliance(String storyContent) {
        if (storyContent.isEmpty()) {
            return false;
        }
        Matcher matcher = COMPLIANCE_PATTERN.matcher(storyContent);
        if (matcher.find()) {
            String value = matcher.group(1).trim().toLowerCase();
            return !COMPLIANCE_NONE_VALUES.contains(value);
        }
        return false;
    }

    /**
     * Counts stories that depend on the given story ID.
     *
     * @param storyId                  the story identifier
     * @param implementationMapContent the implementation map
     *                                 markdown (may be empty)
     * @return number of dependent stories
     */
    public static int countDependents(
            String storyId,
            String implementationMapContent) {
        if (implementationMapContent.isEmpty()) {
            return 0;
        }
        int count = 0;
        String[] lines = implementationMapContent.split("\n");
        for (String line : lines) {
            if (isTableRow(line)
                    && containsStoryInBlockedBy(line, storyId)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isTableRow(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("|")
                && !trimmed.startsWith("|--")
                && !trimmed.startsWith("| Story")
                && !trimmed.startsWith("| ---");
    }

    private static boolean containsStoryInBlockedBy(
            String line,
            String storyId) {
        String[] cells = line.split("\\|");
        if (cells.length < 3) {
            return false;
        }
        String blockedByCell = cells[2].trim();
        String storyCell = cells[1].trim();
        if (storyCell.equals(storyId)) {
            return false;
        }
        return blockedByCell.contains(storyId);
    }
}
