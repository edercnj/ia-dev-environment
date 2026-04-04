package dev.iadev.assembler;

import dev.iadev.domain.model.McpServerConfig;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.util.JsonHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds copilot-mcp.json content from project config.
 *
 * <p>Extracted from {@link GithubMcpAssembler} to keep
 * both classes under 250 lines per RULE-004.</p>
 *
 * @see GithubMcpAssembler
 */
final class McpJsonBuilder {

    private McpJsonBuilder() {
        // utility class
    }

    /**
     * Builds the copilot-mcp.json content from project
     * configuration.
     *
     * @param config the project configuration
     * @return JSON string with 2-space indentation and
     *         trailing newline
     */
    static String buildCopilotMcpJson(
            ProjectConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(JsonHelpers.indent(1))
                .append("\"mcpServers\": {\n");

        List<McpServerConfig> servers =
                config.mcp().servers();
        for (int i = 0; i < servers.size(); i++) {
            McpServerConfig server = servers.get(i);
            appendServer(sb, server);
            if (i < servers.size() - 1) {
                sb.setLength(sb.length() - 1);
                sb.append(",\n");
            }
        }

        sb.append(JsonHelpers.indent(1)).append("}\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendServer(
            StringBuilder sb,
            McpServerConfig server) {
        appendServerHeader(sb, server);
        appendServerBody(sb, server);
        sb.append(JsonHelpers.indent(2)).append("}\n");
    }

    private static void appendServerHeader(
            StringBuilder sb,
            McpServerConfig server) {
        sb.append(JsonHelpers.indent(2))
                .append('"')
                .append(JsonHelpers.escapeJson(server.id()))
                .append("\": {\n");
    }

    private static void appendServerBody(
            StringBuilder sb,
            McpServerConfig server) {
        boolean hasCaps =
                !server.capabilities().isEmpty();
        boolean hasEnv = !server.env().isEmpty();

        sb.append(JsonHelpers.indent(3))
                .append("\"url\": \"")
                .append(JsonHelpers.escapeJson(server.url()))
                .append('"');
        sb.append(hasCaps || hasEnv ? ",\n" : "\n");

        if (hasCaps) {
            appendCapabilities(
                    sb, server.capabilities(), hasEnv);
        }
        if (hasEnv) {
            appendEnv(sb, server.env());
        }
    }

    private static void appendCapabilities(
            StringBuilder sb,
            List<String> capabilities,
            boolean hasMore) {
        sb.append(JsonHelpers.indent(3))
                .append("\"capabilities\": [");
        for (int i = 0; i < capabilities.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"')
                    .append(JsonHelpers.escapeJson(
                            capabilities.get(i)))
                    .append('"');
        }
        sb.append(']');
        if (hasMore) {
            sb.append(",\n");
        } else {
            sb.append('\n');
        }
    }

    private static void appendEnv(
            StringBuilder sb,
            Map<String, String> env) {
        sb.append(JsonHelpers.indent(3))
                .append("\"env\": {\n");

        Map<String, String> sorted = new TreeMap<>(env);
        List<Map.Entry<String, String>> entries =
                new ArrayList<>(sorted.entrySet());

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, String> entry =
                    entries.get(i);
            sb.append(JsonHelpers.indent(4))
                    .append('"')
                    .append(JsonHelpers.escapeJson(
                            entry.getKey()))
                    .append("\": \"")
                    .append(JsonHelpers.escapeJson(
                            entry.getValue()))
                    .append('"');
            if (i < entries.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(JsonHelpers.indent(3)).append("}\n");
    }
}
