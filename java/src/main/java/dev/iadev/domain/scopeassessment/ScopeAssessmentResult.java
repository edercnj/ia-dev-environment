package dev.iadev.domain.scopeassessment;

import java.util.List;

/**
 * Result of a scope assessment classification.
 *
 * @param tier             the classification tier
 * @param componentCount   number of components affected
 * @param newEndpointCount number of new endpoints declared
 * @param hasSchemaChanges whether migration scripts are
 *                         mentioned
 * @param hasCompliance    whether compliance is active
 * @param dependentCount   number of dependent stories
 * @param rationale        textual justification
 * @param phasesToSkip     lifecycle phases to skip
 * @param additionalPhases extra lifecycle phases to add
 */
public record ScopeAssessmentResult(
        ScopeAssessmentTier tier,
        int componentCount,
        int newEndpointCount,
        boolean hasSchemaChanges,
        boolean hasCompliance,
        int dependentCount,
        String rationale,
        List<String> phasesToSkip,
        List<String> additionalPhases) {

    /**
     * Compact constructor enforcing immutability.
     */
    public ScopeAssessmentResult {
        phasesToSkip = List.copyOf(phasesToSkip);
        additionalPhases = List.copyOf(additionalPhases);
    }
}
