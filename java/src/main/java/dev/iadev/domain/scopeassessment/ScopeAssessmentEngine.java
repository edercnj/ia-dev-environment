package dev.iadev.domain.scopeassessment;

import java.util.ArrayList;
import java.util.List;

/**
 * Classifies stories into scope tiers (SIMPLE, STANDARD,
 * COMPLEX) based on content analysis.
 *
 * @see ContentAnalyzer
 * @see ScopeAssessmentTier
 * @see ScopeAssessmentResult
 * @see AssessmentFlags
 */
public final class ScopeAssessmentEngine {

    static final int COMPLEX_COMPONENT_THRESHOLD = 4;
    static final int STANDARD_COMPONENT_MIN = 2;
    static final int STANDARD_ENDPOINT_MIN = 1;
    static final int STANDARD_ENDPOINT_MAX = 2;

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
    }

    /**
     * Assesses a story and returns the scope classification.
     *
     * @param storyContent   the markdown content
     * @param dependentCount stories depending on this one
     * @return the scope assessment result
     */
    public static ScopeAssessmentResult assess(
            String storyContent, int dependentCount) {
        int components = ContentAnalyzer
                .countComponents(storyContent);
        int endpoints = ContentAnalyzer
                .countEndpoints(storyContent);
        AssessmentFlags flags = AssessmentFlags.of(
                ContentAnalyzer
                        .hasSchemaChanges(storyContent),
                ContentAnalyzer.hasCompliance(storyContent),
                false);

        var tier = classify(components, endpoints, flags);
        var rationale = buildRationale(
                tier, components, endpoints, flags);

        return buildResult(tier, components, endpoints,
                flags, dependentCount, rationale);
    }

    /**
     * Builds lifecycle phase config from assessment result.
     *
     * @param result the assessment result
     * @param flags  assessment flags; only
     *               {@link AssessmentFlags#fullOverride()} is
     *               consumed here
     * @return the lifecycle phase configuration
     */
    public static LifecyclePhaseConfig buildPhaseConfig(
            ScopeAssessmentResult result,
            AssessmentFlags flags) {
        if (flags.fullOverride()) {
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

    private static ScopeAssessmentTier classify(
            int components, int endpoints,
            AssessmentFlags flags) {
        if (flags.compliance() || flags.schema()) {
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
            int components, int endpoints,
            AssessmentFlags flags) {
        var parts = new ArrayList<String>();
        if (flags.compliance()) {
            parts.add("compliance requirement detected");
        }
        if (flags.schema()) {
            parts.add("schema changes detected");
        }
        addComponentRationale(parts, tier, components);
        if (endpoints == 0
                && tier == ScopeAssessmentTier.SIMPLE) {
            parts.add("no new endpoints");
        } else if (endpoints > 0) {
            parts.add("%d new endpoint(s)"
                    .formatted(endpoints));
        }
        return parts.isEmpty()
                ? "no components detected"
                : String.join(", ", parts);
    }

    private static void addComponentRationale(
            List<String> parts,
            ScopeAssessmentTier tier, int components) {
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

    private static ScopeAssessmentResult buildResult(
            ScopeAssessmentTier tier,
            int components, int endpoints,
            AssessmentFlags flags,
            int dependentCount, String rationale) {
        List<String> skip =
                tier == ScopeAssessmentTier.SIMPLE
                        ? SIMPLE_SKIP : List.of();
        List<String> extra =
                tier == ScopeAssessmentTier.COMPLEX
                        ? COMPLEX_EXTRA : List.of();
        return new ScopeAssessmentResult(
                tier, components, endpoints,
                flags.schema(), flags.compliance(),
                dependentCount, rationale, skip, extra);
    }
}
