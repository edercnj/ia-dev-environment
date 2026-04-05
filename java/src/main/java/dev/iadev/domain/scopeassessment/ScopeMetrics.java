package dev.iadev.domain.scopeassessment;

/**
 * Raw metrics extracted from story content analysis.
 *
 * <p>Immutable value object holding the five classification
 * criteria used by {@link ScopeAssessmentEngine}.</p>
 *
 * @param componentCount   number of affected components
 * @param newEndpointCount number of new endpoints declared
 * @param hasSchemaChanges whether migration scripts are mentioned
 * @param hasCompliance    whether compliance requirement exists
 * @param dependentCount   number of stories depending on this one
 */
public record ScopeMetrics(
        int componentCount,
        int newEndpointCount,
        boolean hasSchemaChanges,
        boolean hasCompliance,
        int dependentCount) {

    /**
     * Compact constructor with input validation.
     */
    public ScopeMetrics {
        if (componentCount < 0) {
            throw new IllegalArgumentException(
                    "componentCount must be >= 0, got: %d"
                            .formatted(componentCount));
        }
        if (newEndpointCount < 0) {
            throw new IllegalArgumentException(
                    "newEndpointCount must be >= 0, got: %d"
                            .formatted(newEndpointCount));
        }
        if (dependentCount < 0) {
            throw new IllegalArgumentException(
                    "dependentCount must be >= 0, got: %d"
                            .formatted(dependentCount));
        }
    }
}
