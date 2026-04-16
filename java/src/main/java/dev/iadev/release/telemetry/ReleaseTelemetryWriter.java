package dev.iadev.release.telemetry;

import dev.iadev.release.ReleaseContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure formatter that builds a single JSONL line for a
 * release-phase telemetry event (story-0039-0014 TASK-009).
 *
 * <p>The {@code releaseType} field is derived from the
 * {@link ReleaseContext#hotfix()} flag (story-0039-0014 §3.1
 * — no new redundant field is introduced on the state file):
 * {@code "hotfix"} when the flow is a hotfix, {@code "release"}
 * otherwise.</p>
 *
 * <p>Field names align with the canonical JSONL schema emitted by
 * {@code dev.iadev.infrastructure.adapter.output.telemetry.FileTelemetryWriter}
 * and the domain record
 * {@code dev.iadev.domain.model.PhaseMetric} (story-0039-0012 §5.1):
 * {@code releaseVersion}, {@code releaseType}, {@code phase},
 * {@code startedAt}. The subset emitted here is intentionally
 * minimal — the {@code endedAt}, {@code durationSec}, and
 * {@code outcome} fields are populated by the full phase-wrapping
 * writer; this lightweight formatter is used by the interactive
 * flow for early-phase, pre-completion markers that share the same
 * reader/benchmark path.</p>
 *
 * <p>Output is canonical JSON: keys sorted by insertion order,
 * strings escaped per RFC 8259, no trailing whitespace. The
 * writer itself performs no I/O — the caller is responsible
 * for persisting the line.</p>
 */
public final class ReleaseTelemetryWriter {

    private static final String RELEASE = "release";
    private static final String HOTFIX = "hotfix";

    private ReleaseTelemetryWriter() {
        throw new AssertionError("no instances");
    }

    /**
     * Formats a telemetry event as a single JSONL line.
     *
     * <p>Emitted keys (in canonical order): {@code releaseVersion},
     * {@code releaseType}, {@code phase}, {@code startedAt}. This
     * shape is a strict subset of the schema used by
     * {@code FileTelemetryWriter}, so the same JSONL file can be
     * consumed by {@code TelemetryJsonlReader} without special
     * casing.</p>
     *
     * @param phase         phase name (e.g.
     *                      {@code "DETERMINE"}); never null
     * @param version       target version string (mapped to the
     *                      {@code releaseVersion} JSON field);
     *                      never null
     * @param timestampIso  ISO-8601 UTC timestamp (mapped to the
     *                      {@code startedAt} JSON field);
     *                      never null
     * @param ctx           release context; never null
     * @return JSON object encoded as a single line without a
     *         trailing newline
     */
    public static String format(
            String phase,
            String version,
            String timestampIso,
            ReleaseContext ctx) {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(
                timestampIso, "timestampIso");
        Objects.requireNonNull(ctx, "ctx");

        Map<String, String> fields =
                new LinkedHashMap<>();
        fields.put("releaseVersion", version);
        fields.put("releaseType",
                ctx.hotfix() ? HOTFIX : RELEASE);
        fields.put("phase", phase);
        fields.put("startedAt", timestampIso);

        return encode(fields);
    }

    private static String encode(
            Map<String, String> fields) {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry
                : fields.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            appendString(sb, entry.getKey());
            sb.append(':');
            appendString(sb, entry.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

    private static void appendString(
            StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format(
                                "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
