package dev.iadev.assembler;

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
     * @param permissions  the list of allowed commands
     * @param hookPresence whether to include hooks section
     * @return formatted JSON string
     */
    String build(
            List<String> permissions,
            HookPresence hookPresence) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(JsonHelpers.indent(1))
                .append("\"permissions\": {\n");
        sb.append(JsonHelpers.indent(2))
                .append("\"allow\": [\n");
        appendPermissions(sb, permissions);
        sb.append(JsonHelpers.indent(2)).append("]\n");
        if (hookPresence.hasHooks()) {
            sb.append(JsonHelpers.indent(1))
                    .append("},\n");
            HookConfigBuilder.appendHooksSection(sb);
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
