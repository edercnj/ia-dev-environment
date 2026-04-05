package dev.iadev.domain.scopeassessment;

import java.util.List;

/**
 * Configuration of active, skipped, and additional phases
 * for a lifecycle execution based on scope assessment.
 *
 * @param activePhases     phases to execute
 * @param skippedPhases    phases to skip
 * @param additionalPhases extra phases added
 * @param tier             the classification tier
 * @param overrideActive   true if --full-lifecycle was used
 */
public record LifecyclePhaseConfig(
        List<String> activePhases,
        List<String> skippedPhases,
        List<String> additionalPhases,
        ScopeAssessmentTier tier,
        boolean overrideActive) {

    /**
     * Compact constructor enforcing immutability.
     */
    public LifecyclePhaseConfig {
        activePhases = List.copyOf(activePhases);
        skippedPhases = List.copyOf(skippedPhases);
        additionalPhases = List.copyOf(additionalPhases);
    }
}
