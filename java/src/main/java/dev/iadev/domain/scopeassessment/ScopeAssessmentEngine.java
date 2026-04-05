package dev.iadev.domain.scopeassessment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Engine that classifies stories into scope assessment
 * tiers (SIMPLE, STANDARD, COMPLEX) based on analysis
 * of story content.
 *
 * <p>Classification criteria:
 * <ol>
 *   <li>Components affected (file/class mentions)</li>
 *   <li>New endpoints declared</li>
 *   <li>Schema changes (migration mentions)</li>
 *   <li>Compliance requirements</li>
 *   <li>Dependent story count</li>
 * </ol>
 *
 * @see ScopeAssessmentTier
 * @see ScopeAssessmentResult
 */
public final class ScopeAssessmentEngine {

    private static final int COMPLEX_COMPONENT_THRESHOLD = 4;
    private static final int STANDARD_COMPONENT_MIN = 2;
    private static final int STANDARD_ENDPOINT_MIN = 1;
    private static final int STANDARD_ENDPOINT_MAX = 2;

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
            List.of(
                    "migration script",
                    "ALTER TABLE",
                    "CREATE TABLE",
                    "DROP TABLE",
                    "ADD COLUMN");

    private static final List<String> SIMPLE_SKIP =
            List.of("1B", "1C", "1D", "1E");

    private static final List<String> COMPLEX_EXTRA =
            List.of("stakeholder-review");

    private static final List<String> ALL_PHASES =
            List.of("1A", "1B", "1C", "1D", "1E",
                    "2", "3", "4", "5", "6");

    private static final List<String> SIMPLE_PHASES =
            List.of("1A", "2", "4", "5", "6");

    private ScopeAssessmentEngine() {
        // utility class
    }

    /**
     * Assesses a story and returns the scope classification.
     *
     * @param storyContent   the markdown content of the story
     * @param dependentCount number of stories depending on
     *                       this one
     * @return the scope assessment result
     */
    public static ScopeAssessmentResult assess(
            String storyContent,
            int dependentCount) {
        int components = countComponents(storyContent);
        int endpoints = countEndpoints(storyContent);
        boolean schemaChanges =
                hasSchemaChanges(storyContent);
        boolean compliance =
                hasCompliance(storyContent);

        ScopeAssessmentTier tier = classify(
                components, endpoints,
                schemaChanges, compliance);

        String rationale = buildRationale(
                tier, components, endpoints,
                schemaChanges, compliance);

        return buildResult(tier, components, endpoints,
                schemaChanges, compliance,
                dependentCount, rationale);
    }

    /**
     * Builds a lifecycle phase configuration from an
     * assessment result, optionally overridden by
     * --full-lifecycle flag.
     *
     * @param result       the assessment result
     * @param fullOverride true if --full-lifecycle was used
     * @return the lifecycle phase configuration
     */
    public static LifecyclePhaseConfig buildPhaseConfig(
            ScopeAssessmentResult result,
            boolean fullOverride) {
        if (fullOverride) {
            return new LifecyclePhaseConfig(
                    ALL_PHASES, List.of(), List.of(),
                    result.tier(), true);
        }
        return switch (result.tier()) {
            case SIMPLE -> new LifecyclePhaseConfig(
                    SIMPLE_PHASES, SIMPLE_SKIP,
                    List.of(), result.tier(), false);
            case STANDARD -> new LifecyclePhaseConfig(
                    ALL_PHASES, List.of(), List.of(),
                    result.tier(), false);
            case COMPLEX -> new LifecyclePhaseConfig(
                    ALL_PHASES, List.of(), COMPLEX_EXTRA,
                    result.tier(), false);
        };
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

    private static ScopeAssessmentTier classify(
            int components,
            int endpoints,
            boolean schemaChanges,
            boolean compliance) {
        if (compliance) {
            return ScopeAssessmentTier.COMPLEX;
        }
        if (schemaChanges) {
            return ScopeAssessmentTier.COMPLEX;
        }
        if (components >= COMPLEX_COMPONENT_THRESHOLD) {
            return ScopeAssessmentTier.COMPLEX;
        }
        if (components >= STANDARD_COMPONENT_MIN
                || (endpoints >= STANDARD_ENDPOINT_MIN
                && endpoints <= STANDARD_ENDPOINT_MAX)) {
            return ScopeAssessmentTier.STANDARD;
        }
        return ScopeAssessmentTier.SIMPLE;
    }

    private static String buildRationale(
            ScopeAssessmentTier tier,
            int components,
            int endpoints,
            boolean schemaChanges,
            boolean compliance) {
        var parts = new ArrayList<String>();
        addComplianceRationale(parts, compliance);
        addSchemaRationale(parts, schemaChanges);
        addComponentRationale(parts, tier, components);
        addEndpointRationale(parts, tier, endpoints);
        return parts.isEmpty()
                ? "no components detected"
                : String.join(", ", parts);
    }

    private static void addComplianceRationale(
            List<String> parts, boolean compliance) {
        if (compliance) {
            parts.add("compliance requirement detected");
        }
    }

    private static void addSchemaRationale(
            List<String> parts, boolean schemaChanges) {
        if (schemaChanges) {
            parts.add("schema changes detected");
        }
    }

    private static void addComponentRationale(
            List<String> parts,
            ScopeAssessmentTier tier,
            int components) {
        if (components >= COMPLEX_COMPONENT_THRESHOLD
                || (components > 0
                && tier == ScopeAssessmentTier.STANDARD)) {
            parts.add("%d components affected"
                    .formatted(components));
        } else if (components <= 1
                && tier == ScopeAssessmentTier.SIMPLE) {
            parts.add(components == 0
                    ? "no components detected"
                    : "single component change");
        }
    }

    private static void addEndpointRationale(
            List<String> parts,
            ScopeAssessmentTier tier,
            int endpoints) {
        if (endpoints == 0
                && tier == ScopeAssessmentTier.SIMPLE) {
            parts.add("no new endpoints");
        } else if (endpoints > 0) {
            parts.add("%d new endpoint(s)"
                    .formatted(endpoints));
        }
    }

    private static ScopeAssessmentResult buildResult(
            ScopeAssessmentTier tier,
            int components, int endpoints,
            boolean schemaChanges, boolean compliance,
            int dependentCount, String rationale) {
        List<String> skip = tier == ScopeAssessmentTier.SIMPLE
                ? SIMPLE_SKIP : List.of();
        List<String> extra = tier == ScopeAssessmentTier.COMPLEX
                ? COMPLEX_EXTRA : List.of();
        return new ScopeAssessmentResult(
                tier, components, endpoints,
                schemaChanges, compliance,
                dependentCount, rationale, skip, extra);
    }
}
