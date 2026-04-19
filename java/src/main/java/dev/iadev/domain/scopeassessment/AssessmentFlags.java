package dev.iadev.domain.scopeassessment;

/**
 * Cohesive flag set used while assessing and configuring a
 * story's scope. Replaces the repeated
 * {@code (schema, compliance, fullOverride)} boolean triple
 * that violated Rule 03 (no boolean flag parameters and the
 * {@code <= 4} parameter limit) across
 * {@link ScopeAssessmentEngine}'s helpers.
 *
 * <ul>
 *   <li>{@code schema} - story touches persistent schema
 *       (e.g., migrations, {@code ALTER TABLE}).</li>
 *   <li>{@code compliance} - story has an explicit
 *       compliance requirement (PCI, LGPD, HIPAA, ...).</li>
 *   <li>{@code fullOverride} - user requested
 *       {@code --full-lifecycle}, forcing execution of every
 *       lifecycle phase regardless of tier.</li>
 * </ul>
 */
public record AssessmentFlags(
        boolean schema,
        boolean compliance,
        boolean fullOverride) {

    private static final AssessmentFlags NONE =
            new AssessmentFlags(false, false, false);

    /**
     * Named factory mirroring the canonical argument order.
     *
     * @param schema       schema-change flag
     * @param compliance   compliance-requirement flag
     * @param fullOverride full-lifecycle override flag
     * @return a new {@code AssessmentFlags}
     */
    public static AssessmentFlags of(
            boolean schema,
            boolean compliance,
            boolean fullOverride) {
        return new AssessmentFlags(
                schema, compliance, fullOverride);
    }

    /**
     * @return a singleton instance with every flag
     *         {@code false}
     */
    public static AssessmentFlags none() {
        return NONE;
    }
}
