package dev.iadev.application.assembler;

import dev.iadev.util.JsonHelpers;

/**
 * Builds the hooks configuration section for
 * {@code settings.json}.
 *
 * <p>Generates hook entries for:
 * <ul>
 *   <li>{@code PostToolUse} with {@code Write|Edit} matcher
 *       running {@code post-compile-check.sh} (compiled
 *       languages only)</li>
 *   <li>{@code SessionStart}, {@code PreToolUse},
 *       {@code PostToolUse} ({@code *} matcher),
 *       {@code SubagentStop}, {@code Stop} — telemetry
 *       scripts (story-0040-0004, when
 *       {@link ProjectConfig#telemetryEnabled()} is
 *       {@code true})</li>
 * </ul>
 * </p>
 *
 * @see SettingsAssembler
 * @see JsonSettingsBuilder
 */
public final class HookConfigBuilder {

    private static final int HOOK_TIMEOUT = 60;
    private static final int TELEMETRY_TIMEOUT = 5;
    private static final String CLAUDE_PROJECT_DIR_PREFIX =
            "\"\\\"$CLAUDE_PROJECT_DIR\\\"/.claude/hooks/";

    HookConfigBuilder() {
        // package-private constructor
    }

    /**
     * Appends the hooks section to the given StringBuilder.
     *
     * <p>Section content depends on the presence flags: legacy
     * {@code post-compile-check.sh} is emitted when
     * {@code hasLegacy} is {@code true}; five telemetry event
     * entries are emitted when {@code telemetryEnabled} is
     * {@code true}. When both flags are {@code true} the
     * {@code PostToolUse} array contains both matchers
     * ({@code Write|Edit} for post-compile and {@code *} for
     * telemetry).</p>
     *
     * @param sb the StringBuilder to append to
     * @param hasLegacy whether to emit the legacy
     *     post-compile-check entry
     * @param telemetryEnabled whether to emit the five
     *     telemetry event entries (story-0040-0004)
     */
    static void appendHooksSection(
            StringBuilder sb,
            boolean hasLegacy,
            boolean telemetryEnabled) {
        sb.append(JsonHelpers.indent(1))
                .append("\"hooks\": {\n");
        if (telemetryEnabled) {
            appendTelemetryEvent(sb, "SessionStart",
                    "telemetry-session.sh", false, false);
            appendTelemetryEvent(sb, "PreToolUse",
                    "telemetry-pretool.sh", true, false);
        }
        appendPostToolUseArray(
                sb, hasLegacy, telemetryEnabled);
        if (telemetryEnabled) {
            appendTelemetryEvent(sb, "SubagentStop",
                    "telemetry-subagent.sh", false, false);
            appendTelemetryEvent(sb, "Stop",
                    "telemetry-stop.sh", false, true);
        }
        sb.append(JsonHelpers.indent(1)).append("}\n");
    }

    /**
     * Appends the {@code PostToolUse} event array. The array
     * may contain 0, 1, or 2 entries depending on the flags.
     */
    private static void appendPostToolUseArray(
            StringBuilder sb,
            boolean hasLegacy,
            boolean telemetryEnabled) {
        if (!hasLegacy && !telemetryEnabled) {
            return;
        }
        boolean isLastOuterEntry = !telemetryEnabled;
        sb.append(JsonHelpers.indent(2))
                .append("\"PostToolUse\": [\n");
        if (hasLegacy) {
            appendPostCompileEntry(sb, telemetryEnabled);
        }
        if (telemetryEnabled) {
            appendTelemetryPostToolEntry(sb);
        }
        sb.append(JsonHelpers.indent(2)).append("]");
        sb.append(isLastOuterEntry ? "\n" : ",\n");
    }

    private static void appendPostCompileEntry(
            StringBuilder sb, boolean hasSibling) {
        sb.append(JsonHelpers.indent(3)).append("{\n");
        sb.append(JsonHelpers.indent(4))
                .append("\"matcher\": \"Write|Edit\",\n");
        sb.append(JsonHelpers.indent(4))
                .append("\"hooks\": [\n");
        sb.append(JsonHelpers.indent(5)).append("{\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"type\": \"command\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"command\": ")
                .append(CLAUDE_PROJECT_DIR_PREFIX)
                .append("post-compile-check.sh\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"timeout\": ")
                .append(HOOK_TIMEOUT)
                .append(",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"statusMessage\": ")
                .append("\"Checking compilation...\"\n");
        sb.append(JsonHelpers.indent(5)).append("}\n");
        sb.append(JsonHelpers.indent(4)).append("]\n");
        sb.append(JsonHelpers.indent(3)).append("}")
                .append(hasSibling ? ",\n" : "\n");
    }

    private static void appendTelemetryPostToolEntry(
            StringBuilder sb) {
        sb.append(JsonHelpers.indent(3)).append("{\n");
        sb.append(JsonHelpers.indent(4))
                .append("\"matcher\": \"*\",\n");
        sb.append(JsonHelpers.indent(4))
                .append("\"hooks\": [\n");
        sb.append(JsonHelpers.indent(5)).append("{\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"type\": \"command\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"command\": ")
                .append(CLAUDE_PROJECT_DIR_PREFIX)
                .append("telemetry-posttool.sh\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"timeout\": ")
                .append(TELEMETRY_TIMEOUT)
                .append("\n");
        sb.append(JsonHelpers.indent(5)).append("}\n");
        sb.append(JsonHelpers.indent(4)).append("]\n");
        sb.append(JsonHelpers.indent(3)).append("}\n");
    }

    private static void appendTelemetryEvent(
            StringBuilder sb,
            String eventName,
            String scriptName,
            boolean withWildcardMatcher,
            boolean isLastEvent) {
        sb.append(JsonHelpers.indent(2)).append('"')
                .append(eventName).append("\": [\n");
        sb.append(JsonHelpers.indent(3)).append("{\n");
        if (withWildcardMatcher) {
            sb.append(JsonHelpers.indent(4))
                    .append("\"matcher\": \"*\",\n");
        }
        sb.append(JsonHelpers.indent(4))
                .append("\"hooks\": [\n");
        sb.append(JsonHelpers.indent(5)).append("{\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"type\": \"command\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"command\": ")
                .append(CLAUDE_PROJECT_DIR_PREFIX)
                .append(scriptName).append("\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"timeout\": ")
                .append(TELEMETRY_TIMEOUT)
                .append("\n");
        sb.append(JsonHelpers.indent(5)).append("}\n");
        sb.append(JsonHelpers.indent(4)).append("]\n");
        sb.append(JsonHelpers.indent(3)).append("}\n");
        sb.append(JsonHelpers.indent(2)).append("]")
                .append(isLastEvent ? "\n" : ",\n");
    }
}
