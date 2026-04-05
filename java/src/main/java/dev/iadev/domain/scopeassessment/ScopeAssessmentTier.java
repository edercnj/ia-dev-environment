package dev.iadev.domain.scopeassessment;

/**
 * Classification tiers for story scope assessment.
 *
 * <p>Determines which lifecycle phases are executed:
 * <ul>
 *   <li>{@link #SIMPLE} — skips parallel planning
 *       (1B-1E)</li>
 *   <li>{@link #STANDARD} — all phases execute
 *       normally</li>
 *   <li>{@link #COMPLEX} — all phases plus stakeholder
 *       review after phase 4</li>
 * </ul>
 *
 * @see ScopeAssessmentEngine
 */
public enum ScopeAssessmentTier {

    /** Single component, no endpoints, no schema, no compliance. */
    SIMPLE,

    /** 2-3 components or 1-2 new endpoints. */
    STANDARD,

    /** 4+ components, schema changes, or compliance requirement. */
    COMPLEX
}
