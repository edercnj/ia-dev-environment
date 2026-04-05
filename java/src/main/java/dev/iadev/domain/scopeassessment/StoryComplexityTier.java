package dev.iadev.domain.scopeassessment;

/**
 * Classification tiers for story complexity assessment.
 *
 * <p>Each tier determines which lifecycle phases are executed:
 * <ul>
 *   <li>{@link #SIMPLE} — skips parallel planning phases
 *       (1B, 1C, 1D, 1E)</li>
 *   <li>{@link #STANDARD} — executes all phases normally</li>
 *   <li>{@link #COMPLEX} — adds stakeholder review after
 *       phase 4</li>
 * </ul>
 */
public enum StoryComplexityTier {

    /** Single component, no endpoints, no schema, no compliance. */
    SIMPLE,

    /** 2-3 components or 1-2 new endpoints. */
    STANDARD,

    /** 4+ components, schema changes, or compliance requirement. */
    COMPLEX
}
