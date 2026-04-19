package dev.iadev.telemetry;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * One immutable scrubbing rule: a compiled regex pattern bound to
 * its deterministic replacement marker.
 *
 * <p>Rules form an ordered pipeline that is applied by
 * {@link TelemetryScrubber#scrub(TelemetryEvent)} to every
 * string-bearing field of a {@link TelemetryEvent} — notably
 * {@code failureReason} and the string values in {@code metadata}.
 * The ordering in
 * {@link TelemetryScrubber#DEFAULT_RULES} matters because later
 * rules may otherwise match substrings inside earlier markers.</p>
 *
 * <p>All fields are required; the canonical constructor validates
 * them so that a misconfigured rule fails loud at class-load time
 * rather than silently failing at emit-time (see rule 20,
 * {@code FORBIDDEN} section on silent regex regression).</p>
 *
 * @param category      human-readable identifier, useful for log
 *                      lines and audit reports
 * @param pattern       compiled regex; never null
 * @param replacement   fixed marker string that replaces every
 *                      match; never null and never blank
 */
public record ScrubRule(
        String category, Pattern pattern, String replacement) {

    /**
     * Canonical constructor with fail-fast validation.
     *
     * @throws NullPointerException     when any field is null
     * @throws IllegalArgumentException when the replacement is
     *                                  blank
     */
    public ScrubRule {
        Objects.requireNonNull(category, "category is required");
        Objects.requireNonNull(pattern, "pattern is required");
        Objects.requireNonNull(
                replacement, "replacement is required");
        if (replacement.isBlank()) {
            throw new IllegalArgumentException(
                    "replacement must not be blank: category="
                            + category);
        }
    }

    /**
     * Convenience factory that compiles a regex and wraps it as a
     * {@link ScrubRule}. Case sensitivity is controlled inline via
     * the {@code (?i)} flag — callers MUST bake it into the
     * {@code regex} argument, not into a separate bitmask, so the
     * pattern remains the single source of truth.
     *
     * @param category    rule category
     * @param regex       regex source (may include inline flags)
     * @param replacement replacement marker
     * @return the compiled rule
     * @throws PatternSyntaxException when {@code regex} is invalid
     */
    public static ScrubRule of(
            String category, String regex, String replacement) {
        return new ScrubRule(
                category, Pattern.compile(regex), replacement);
    }

    /**
     * Applies this rule to {@code input}, replacing every match
     * with {@link #replacement}.
     *
     * <p>Callers MUST guard against null before invoking this
     * method (Rule 03 — never return null). The scrubber pipeline
     * in {@link TelemetryScrubber#scrubString(String)} applies
     * the outer null check once, so every rule call receives a
     * non-null string.</p>
     *
     * @param input the text to scrub; must not be null
     * @return the scrubbed text
     * @throws NullPointerException when {@code input} is null
     */
    public String apply(String input) {
        Objects.requireNonNull(input, "input");
        return pattern.matcher(input).replaceAll(replacement);
    }
}
