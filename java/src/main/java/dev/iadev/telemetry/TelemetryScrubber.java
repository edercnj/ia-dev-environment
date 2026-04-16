package dev.iadev.telemetry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes credentials, tokens and personally identifiable
 * information (PII) from a {@link TelemetryEvent} before it is
 * persisted to {@code events.ndjson}.
 *
 * <p>The scrubber implements the normative matrix documented in
 * {@code targets/claude/rules/20-telemetry-privacy.md} (rule 20).
 * Every string value inside {@code failureReason}, {@code phase},
 * {@code tool}, {@code skill} and each {@code metadata} entry
 * goes through the ordered list of {@link ScrubRule} defined in
 * {@link #DEFAULT_RULES}; matches are replaced by deterministic
 * markers such as {@code AWS_KEY_REDACTED} or
 * {@code EMAIL_REDACTED} so that downstream analytics can count
 * events without recovering the secret.</p>
 *
 * <p>The scrubber also enforces the metadata whitelist published
 * by {@link MetadataWhitelist}. Keys outside the allow-list are
 * removed (not masked) with an {@code INFO} log line as required
 * by story-0040-0005 §5.3.</p>
 *
 * <p><b>Fail-open semantics.</b> When any regex rule throws at
 * emit time (e.g.,
 * {@link java.util.regex.PatternSyntaxException}) the scrubber
 * logs WARN and returns the original event unchanged. The audit
 * pipeline ({@link PiiAudit}) catches anything the emit-time
 * scrubber missed, so a transient regex failure never drops an
 * event silently.</p>
 */
public final class TelemetryScrubber {

    private static final Logger LOG =
            LoggerFactory.getLogger(TelemetryScrubber.class);

    /**
     * The 8 blocked patterns from rule 20 in canonical application
     * order: Bearer → JWT → AWS key → AWS secret → GitHub token →
     * URL-with-credentials → email → CPF. This order guarantees
     * that a {@code "Bearer eyJ..."} header collapses to a single
     * {@code BEARER_REDACTED} marker instead of producing a nested
     * {@code BEARER_REDACTED JWT_REDACTED} composite.
     */
    public static final List<ScrubRule> DEFAULT_RULES = List.of(
            ScrubRule.of(
                    "bearer",
                    "(?i)bearer\\s+[A-Za-z0-9._-]+",
                    "BEARER_REDACTED"),
            ScrubRule.of(
                    "jwt",
                    "eyJ[A-Za-z0-9_-]+\\."
                            + "eyJ[A-Za-z0-9_-]+\\."
                            + "[A-Za-z0-9_-]+",
                    "JWT_REDACTED"),
            ScrubRule.of(
                    "aws_access_key",
                    "AKIA[0-9A-Z]{16}",
                    "AWS_KEY_REDACTED"),
            ScrubRule.of(
                    "aws_secret",
                    "(?i)aws_secret[^=\\s]*\\s*=\\s*\\S+",
                    "AWS_SECRET_REDACTED"),
            ScrubRule.of(
                    "github_token",
                    "gh[pousr]_[A-Za-z0-9]{36,}",
                    "GITHUB_TOKEN_REDACTED"),
            ScrubRule.of(
                    "url_credentials",
                    "://[^:/\\s]+:[^@/\\s]+@[A-Za-z0-9.-]+",
                    "://USER:PASS_REDACTED@HOST"),
            ScrubRule.of(
                    "email",
                    "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+"
                            + "\\.[A-Za-z]{2,}",
                    "EMAIL_REDACTED"),
            ScrubRule.of(
                    "cpf",
                    "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}",
                    "CPF_REDACTED"));

    private final List<ScrubRule> rules;
    private final MetadataWhitelist whitelist;

    /** Creates a scrubber with the default rule list. */
    public TelemetryScrubber() {
        this(DEFAULT_RULES, new MetadataWhitelist());
    }

    /**
     * Creates a scrubber with a custom rule list. Primarily used
     * by tests that need to inject a failing rule to exercise the
     * fail-open path.
     *
     * @param rules     the ordered rule pipeline; must not be null
     * @param whitelist the metadata key whitelist; must not be
     *                  null
     */
    public TelemetryScrubber(
            List<ScrubRule> rules,
            MetadataWhitelist whitelist) {
        this.rules = List.copyOf(
                Objects.requireNonNull(rules, "rules"));
        this.whitelist = Objects.requireNonNull(
                whitelist, "whitelist");
    }

    /**
     * Produces a scrubbed copy of {@code event}. The returned
     * event is equivalent to the input except that:
     *
     * <ul>
     *   <li>each string field is passed through every
     *       {@link ScrubRule} in order;</li>
     *   <li>metadata keys outside the whitelist are removed;</li>
     *   <li>metadata values are scrubbed when they are
     *       strings.</li>
     * </ul>
     *
     * @param event the event to scrub; must not be null
     * @return a new {@link TelemetryEvent} with PII redacted
     * @throws NullPointerException when {@code event} is null
     */
    public TelemetryEvent scrub(TelemetryEvent event) {
        Objects.requireNonNull(event, "event is required");
        try {
            return scrubUnchecked(event);
        } catch (RuntimeException e) {
            LOG.warn(
                    "telemetry.scrub.failure falling back to"
                            + " original event: {}",
                    e.getMessage());
            return event;
        }
    }

    private TelemetryEvent scrubUnchecked(TelemetryEvent event) {
        String scrubbedFailure = scrubString(
                event.failureReason());
        String scrubbedPhase = scrubString(event.phase());
        String scrubbedTool = scrubString(event.tool());
        String scrubbedSkill = scrubString(event.skill());
        Map<String, Object> scrubbedMetadata =
                scrubMetadata(event.metadata());

        return new TelemetryEvent(
                event.schemaVersion(),
                event.eventId(),
                event.timestamp(),
                event.sessionId(),
                event.epicId(),
                event.storyId(),
                event.taskId(),
                event.type(),
                scrubbedSkill,
                scrubbedPhase,
                scrubbedTool,
                event.durationMs(),
                event.status(),
                scrubbedFailure,
                scrubbedMetadata);
    }

    /**
     * Applies every rule to {@code value} in order.
     *
     * @param value the text to scrub; may be null
     * @return scrubbed text, or {@code null} when {@code value}
     *         is null
     */
    public String scrubString(String value) {
        if (value == null) {
            return null;
        }
        String current = value;
        for (ScrubRule rule : rules) {
            current = rule.apply(current);
        }
        return current;
    }

    private Map<String, Object> scrubMetadata(
            Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return metadata;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry
                : metadata.entrySet()) {
            String key = entry.getKey();
            if (!whitelist.isAllowed(key)) {
                LOG.info(
                        "telemetry.metadata.removed key={}",
                        key);
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof String s) {
                out.put(key, scrubString(s));
            } else {
                out.put(key, value);
            }
        }
        return out;
    }
}
