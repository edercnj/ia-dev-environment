package dev.iadev.domain.scopeassessment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes story markdown content to extract classification
 * metrics: components, endpoints, schema changes, compliance.
 */
final class ContentAnalyzer {

    private static final Pattern COMPONENT_PATTERN =
            Pattern.compile(
                    "\\b\\w+\\.(java|kt|py|ts|go|rs)\\b");

    private static final Pattern ENDPOINT_PATTERN =
            Pattern.compile(
                    "\\b(GET|POST|PUT|DELETE|PATCH)"
                            + "\\s+/[\\w/{}-]+");

    private static final Pattern COMPLIANCE_PATTERN =
            Pattern.compile(
                    "compliance:\\s*(\\S+)",
                    Pattern.CASE_INSENSITIVE);

    private static final List<String> SCHEMA_KEYWORDS =
            List.of("migration script", "ALTER TABLE",
                    "CREATE TABLE", "DROP TABLE",
                    "ADD COLUMN");

    private ContentAnalyzer() {
    }

    static int countComponents(String content) {
        Matcher matcher =
                COMPONENT_PATTERN.matcher(content);
        List<String> found = new ArrayList<>();
        while (matcher.find()) {
            String match = matcher.group();
            if (!found.contains(match)) {
                found.add(match);
            }
        }
        return found.size();
    }

    static int countEndpoints(String content) {
        Matcher matcher =
                ENDPOINT_PATTERN.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    static boolean hasSchemaChanges(String content) {
        String upper = content.toUpperCase();
        return SCHEMA_KEYWORDS.stream()
                .anyMatch(kw ->
                        upper.contains(kw.toUpperCase()));
    }

    static boolean hasCompliance(String content) {
        Matcher matcher =
                COMPLIANCE_PATTERN.matcher(content);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (!"none".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
