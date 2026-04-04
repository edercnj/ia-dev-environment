package dev.iadev.application.assembler;

import dev.iadev.util.JsonHelpers;

/**
 * Builds the hooks configuration section for
 * {@code settings.json}.
 *
 * <p>Generates the PostToolUse hook that triggers
 * compilation checks when source files are modified
 * via Write or Edit operations.</p>
 *
 * @see SettingsAssembler
 * @see JsonSettingsBuilder
 */
public final class HookConfigBuilder {

    private static final int HOOK_TIMEOUT = 60;

    HookConfigBuilder() {
        // package-private constructor
    }

    /**
     * Appends the hooks section to the given StringBuilder.
     *
     * <p>Generates the PostToolUse hook configuration block
     * with Write|Edit matcher, the post-compile-check script
     * command, timeout, and status message.</p>
     *
     * @param sb the StringBuilder to append to
     */
    static void appendHooksSection(StringBuilder sb) {
        sb.append(JsonHelpers.indent(1))
                .append("\"hooks\": {\n");
        sb.append(JsonHelpers.indent(2))
                .append("\"PostToolUse\": [\n");
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
                .append("\"\\\"$CLAUDE_PROJECT_DIR\\\"")
                .append("/.claude/hooks/")
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
        sb.append(JsonHelpers.indent(3)).append("}\n");
        sb.append(JsonHelpers.indent(2)).append("]\n");
        sb.append(JsonHelpers.indent(1)).append("}\n");
    }
}
