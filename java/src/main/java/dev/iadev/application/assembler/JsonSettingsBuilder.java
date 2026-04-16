package dev.iadev.application.assembler;

import dev.iadev.util.JsonHelpers;

import java.util.List;

/**
 * Builds JSON content for {@code settings.json} and
 * {@code settings.local.json}.
 *
 * <p>Constructs the permissions and hooks sections as
 * formatted JSON strings. Uses {@link JsonHelpers} for
 * escaping and indentation.</p>
 *
 * @see SettingsAssembler
 * @see JsonHelpers
 */
public final class JsonSettingsBuilder {

    JsonSettingsBuilder() {
        // package-private constructor
    }

    /**
     * Builds the settings.json content as a formatted JSON
     * string.
     *
     * <p>Legacy overload kept for backward compatibility.
     * Delegates to {@link #build(List, HookPresence, boolean)}
     * with telemetry disabled.</p>
     *
     * @param permissions  the list of allowed commands
     * @param hookPresence whether to include hooks section
     * @return formatted JSON string
     */
    String build(
            List<String> permissions,
            HookPresence hookPresence) {
        return build(permissions, hookPresence, false);
    }

    /**
     * Builds the settings.json content as a formatted JSON
     * string with explicit telemetry control.
     *
     * @param permissions the list of allowed commands
     * @param hookPresence whether the legacy
     *     post-compile-check hook is present
     * @param telemetryEnabled whether to emit the 5 telemetry
     *     event entries (story-0040-0004)
     * @return formatted JSON string
     */
    String build(
            List<String> permissions,
            HookPresence hookPresence,
            boolean telemetryEnabled) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(JsonHelpers.indent(1))
                .append("\"permissions\": {\n");
        sb.append(JsonHelpers.indent(2))
                .append("\"allow\": [\n");
        appendPermissions(sb, permissions);
        sb.append(JsonHelpers.indent(2)).append("]\n");
        boolean hasAnyHook =
                hookPresence.hasHooks() || telemetryEnabled;
        if (hasAnyHook) {
            sb.append(JsonHelpers.indent(1))
                    .append("},\n");
            HookConfigBuilder.appendHooksSection(
                    sb, hookPresence.hasHooks(),
                    telemetryEnabled);
        } else {
            sb.append(JsonHelpers.indent(1))
                    .append("}\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Builds the settings.local.json content.
     *
     * @return formatted JSON string with empty permissions
     */
    String buildLocal() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(JsonHelpers.indent(1))
                .append("\"permissions\": {\n");
        sb.append(JsonHelpers.indent(2))
                .append("\"allow\": []\n");
        sb.append(JsonHelpers.indent(1)).append("}\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendPermissions(
            StringBuilder sb, List<String> permissions) {
        for (int i = 0; i < permissions.size(); i++) {
            sb.append(JsonHelpers.indent(3))
                    .append('"')
                    .append(JsonHelpers.escapeJson(
                            permissions.get(i)))
                    .append('"');
            if (i < permissions.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
    }
}
