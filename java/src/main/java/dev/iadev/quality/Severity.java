package dev.iadev.quality;

/**
 * Severity tiers for {@link SkillSizeLinter} findings.
 *
 * <p>The three tiers correspond to RULE-047-04 thresholds:
 * <ul>
 *   <li>{@link #INFO}: &lt;250 lines, or &gt;500 with non-empty
 *       {@code references/} sibling.</li>
 *   <li>{@link #WARN}: 250-500 lines (grey zone; advisory).</li>
 *   <li>{@link #ERROR}: &gt;500 lines without a non-empty
 *       {@code references/} sibling (fails CI).</li>
 * </ul>
 *
 * @see LintFinding
 * @see SkillSizeLinter
 */
public enum Severity {
    INFO,
    WARN,
    ERROR
}
