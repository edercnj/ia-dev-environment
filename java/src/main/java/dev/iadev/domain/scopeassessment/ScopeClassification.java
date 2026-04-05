package dev.iadev.domain.scopeassessment;

import java.util.List;

/**
 * Immutable result of scope assessment classification.
 *
 * <p>Contains the tier, the raw metrics, a human-readable
 * rationale, phases to skip, and additional phases to add.</p>
 *
 * @param tier             the complexity tier
 * @param metrics          the raw metrics used for classification
 * @param rationale        human-readable justification
 * @param phasesToSkip     lifecycle phases to skip (empty for
 *                         STANDARD/COMPLEX)
 * @param additionalPhases extra phases (e.g., stakeholder-review
 *                         for COMPLEX)
 */
public record ScopeClassification(
        StoryComplexityTier tier,
        ScopeMetrics metrics,
        String rationale,
        List<String> phasesToSkip,
        List<String> additionalPhases) {

    /**
     * Compact constructor enforcing immutability.
     */
    public ScopeClassification {
        phasesToSkip = List.copyOf(phasesToSkip);
        additionalPhases = List.copyOf(additionalPhases);
    }
}
