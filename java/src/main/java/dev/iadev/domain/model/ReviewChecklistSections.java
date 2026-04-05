package dev.iadev.domain.model;

/**
 * Generates conditional checklist section text for the
 * x-review-pr skill template.
 *
 * <p>Sections are activated based on project config:</p>
 * <ul>
 *   <li>Section L (Event-Driven): 8 criteria</li>
 *   <li>Section M (PCI-DSS): 7 criteria</li>
 *   <li>Section N (LGPD): 4 criteria</li>
 * </ul>
 */
public final class ReviewChecklistSections {

    private ReviewChecklistSections() {
        // utility class
    }

    /**
     * Builds the rubric table rows for active sections.
     *
     * @param hasEvent  true if event interfaces exist
     * @param hasPciDss true if PCI-DSS compliance active
     * @param hasLgpd   true if LGPD compliance active
     * @return rubric rows or empty string
     */
    public static String buildRubricRows(
            boolean hasEvent,
            boolean hasPciDss,
            boolean hasLgpd) {
        var sb = new StringBuilder();
        if (hasEvent) {
            sb.append(SECTION_L_RUBRIC);
        }
        if (hasPciDss) {
            sb.append(SECTION_M_RUBRIC);
        }
        if (hasLgpd) {
            sb.append(SECTION_N_RUBRIC);
        }
        return sb.toString();
    }

    /**
     * Builds the detailed criteria for active sections.
     *
     * @param hasEvent  true if event interfaces exist
     * @param hasPciDss true if PCI-DSS compliance active
     * @param hasLgpd   true if LGPD compliance active
     * @return criteria text or empty string
     */
    public static String buildDetailedCriteria(
            boolean hasEvent,
            boolean hasPciDss,
            boolean hasLgpd) {
        var sb = new StringBuilder();
        if (hasEvent) {
            sb.append(SECTION_L_CRITERIA);
        }
        if (hasPciDss) {
            sb.append(SECTION_M_CRITERIA);
        }
        if (hasLgpd) {
            sb.append(SECTION_N_CRITERIA);
        }
        return sb.toString();
    }

    private static final String SECTION_L_RUBRIC =
            "| L. Event-Driven Review | 8      "
            + "| Idempotency, ordering, DLQ,"
            + " schema evolution, retry, isolation"
            + "     |\n";

    private static final String SECTION_M_RUBRIC =
            "| M. PCI-DSS             | 7      "
            + "| Card data protection, encryption,"
            + " tokenization, audit trail"
            + "        |\n";

    private static final String SECTION_N_RUBRIC =
            "| N. LGPD                | 4      "
            + "| Consent tracking, data deletion,"
            + " processing log, anonymization"
            + "    |\n";

    private static final String SECTION_L_CRITERIA =
            """

            ### Section L -- Event-Driven Review (8 criteria)

            1. Consumer idempotency (deduplication by event ID)
            2. Ordering guaranteed within partition key
            3. Dead letter strategy configured and tested
            4. Schema evolution with backward compatibility
            5. Retry policy with exponential backoff
            6. Consumer group isolation (no shared groups)
            7. Transactional outbox when applicable
            8. Lag and throughput observability per consumer
            """;

    private static final String SECTION_M_CRITERIA =
            """

            ### Section M -- PCI-DSS (7 criteria)

            1. Card data never logged (PAN, CVV, expiry)
            2. Encryption in transit (TLS 1.2+) and at rest\
             (AES-256)
            3. Tokenization for PAN storage
            4. Audit trail for sensitive data access
            5. Environment segregation (dev/staging/prod)
            6. Key and credential rotation documented
            7. Penetration tests referenced in pipeline
            """;

    private static final String SECTION_N_CRITERIA =
            """

            ### Section N -- LGPD (4 criteria)

            1. Consent traceable per processing operation
            2. Personal data deletion endpoint implemented
            3. Processing operations log with legal basis
            4. Anonymization/pseudonymization applied where\
             personal data not strictly necessary
            """;
}
