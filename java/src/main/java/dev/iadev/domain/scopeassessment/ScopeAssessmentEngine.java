package dev.iadev.domain.scopeassessment;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine that classifies story complexity into tiers.
 *
 * <p>Uses {@link StoryAnalyzer} to extract metrics and
 * applies classification rules to determine the
 * {@link StoryComplexityTier}.</p>
 *
 * <p>Classification rules (elevation priority):
 * <ol>
 *   <li>Compliance always elevates to COMPLEX</li>
 *   <li>Schema changes always elevate to COMPLEX</li>
 *   <li>4+ components elevate to COMPLEX</li>
 *   <li>2-3 components or 1-2 endpoints = STANDARD</li>
 *   <li>Otherwise SIMPLE</li>
 * </ol>
 */
public final class ScopeAssessmentEngine {

    private static final int COMPLEX_COMPONENT_THRESHOLD = 4;
    private static final int STANDARD_MIN_COMPONENTS = 2;
    private static final int STANDARD_MAX_COMPONENTS = 3;
    private static final int STANDARD_MIN_ENDPOINTS = 1;
    private static final int STANDARD_MAX_ENDPOINTS = 2;

    private static final List<String> SIMPLE_PHASES_TO_SKIP =
            List.of("1B", "1C", "1D", "1E");

    private static final String STAKEHOLDER_REVIEW =
            "stakeholder-review";

    private ScopeAssessmentEngine() {
    }

    /**
     * Assesses story complexity from raw metrics.
     *
     * @param metrics the extracted scope metrics
     * @return the classification result
     */
    public static ScopeClassification classify(
            ScopeMetrics metrics) {
        if (isComplex(metrics)) {
            return buildComplex(metrics);
        }
        if (isStandard(metrics)) {
            return buildStandard(metrics);
        }
        return buildSimple(metrics);
    }

    /**
     * Full assessment from story content and optional
     * implementation map.
     *
     * @param storyContent             story markdown content
     * @param implementationMapContent implementation map content
     *                                 (empty string if unavailable)
     * @param storyId                  the story identifier
     * @return the classification result
     */
    public static ScopeClassification assess(
            String storyContent,
            String implementationMapContent,
            String storyId) {
        var metrics = new ScopeMetrics(
                StoryAnalyzer.countComponents(storyContent),
                StoryAnalyzer.countEndpoints(storyContent),
                StoryAnalyzer.hasSchemaChanges(storyContent),
                StoryAnalyzer.hasCompliance(storyContent),
                StoryAnalyzer.countDependents(
                        storyId, implementationMapContent));
        return classify(metrics);
    }

    private static boolean isComplex(ScopeMetrics metrics) {
        return metrics.hasCompliance()
                || metrics.hasSchemaChanges()
                || metrics.componentCount()
                >= COMPLEX_COMPONENT_THRESHOLD;
    }

    private static boolean isStandard(ScopeMetrics metrics) {
        return isStandardByComponents(metrics)
                || isStandardByEndpoints(metrics);
    }

    private static boolean isStandardByComponents(
            ScopeMetrics metrics) {
        return metrics.componentCount() >= STANDARD_MIN_COMPONENTS
                && metrics.componentCount()
                <= STANDARD_MAX_COMPONENTS;
    }

    private static boolean isStandardByEndpoints(
            ScopeMetrics metrics) {
        return metrics.newEndpointCount()
                >= STANDARD_MIN_ENDPOINTS;
    }

    private static ScopeClassification buildComplex(
            ScopeMetrics metrics) {
        return new ScopeClassification(
                StoryComplexityTier.COMPLEX,
                metrics,
                buildComplexRationale(metrics),
                List.of(),
                List.of(STAKEHOLDER_REVIEW));
    }

    private static ScopeClassification buildStandard(
            ScopeMetrics metrics) {
        return new ScopeClassification(
                StoryComplexityTier.STANDARD,
                metrics,
                buildStandardRationale(metrics),
                List.of(),
                List.of());
    }

    private static ScopeClassification buildSimple(
            ScopeMetrics metrics) {
        return new ScopeClassification(
                StoryComplexityTier.SIMPLE,
                metrics,
                buildSimpleRationale(metrics),
                SIMPLE_PHASES_TO_SKIP,
                List.of());
    }

    private static String buildComplexRationale(
            ScopeMetrics metrics) {
        var reasons = new ArrayList<String>();
        if (metrics.hasCompliance()) {
            reasons.add("compliance requirement detected");
        }
        if (metrics.hasSchemaChanges()) {
            reasons.add("schema changes detected");
        }
        if (metrics.componentCount()
                >= COMPLEX_COMPONENT_THRESHOLD) {
            reasons.add("%d components affected"
                    .formatted(metrics.componentCount()));
        }
        return String.join(", ", reasons);
    }

    private static String buildStandardRationale(
            ScopeMetrics metrics) {
        var reasons = new ArrayList<String>();
        if (metrics.componentCount() >= STANDARD_MIN_COMPONENTS) {
            reasons.add("%d components affected"
                    .formatted(metrics.componentCount()));
        }
        if (metrics.newEndpointCount()
                >= STANDARD_MIN_ENDPOINTS) {
            reasons.add("%d new endpoints"
                    .formatted(metrics.newEndpointCount()));
        }
        return String.join(", ", reasons);
    }

    private static String buildSimpleRationale(
            ScopeMetrics metrics) {
        if (metrics.componentCount() == 0) {
            return "no components detected, no new endpoints, "
                    + "no schema migration";
        }
        return "single component change, no new endpoints, "
                + "no schema migration";
    }
}
