package dev.iadev.assembler;

import dev.iadev.model.McpServerConfig;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.util.JsonHelpers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Assembles {@code .github/copilot-mcp.json} with MCP
 * server configuration for GitHub Copilot.
 *
 * <p>This is the ninth assembler in the pipeline (position
 * 9 of 23 per RULE-005). It generates a JSON file only
 * when MCP servers are configured; otherwise, it returns
 * an empty result (graceful no-op).</p>
 *
 * <p>The assembler validates that environment variable
 * values use the {@code $VARIABLE} format and emits
 * warnings for literal values.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler mcp = new GithubMcpAssembler();
 * List<String> files = mcp.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see McpServerConfig
 */
public final class GithubMcpAssembler
        implements Assembler {

    /**
     * Creates a GithubMcpAssembler.
     */
    public GithubMcpAssembler() {
        // Default constructor
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates copilot-mcp.json when MCP servers are
     * configured. Returns empty list when no servers are
     * present. Warnings for literal env values are tracked
     * but the Assembler interface returns only file paths;
     * warnings are logged to stderr.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        if (config.mcp().servers().isEmpty()) {
            return List.of();
        }

        List<String> warnings = warnLiteralEnvValues(
                config.mcp().servers());
        for (String warning : warnings) {
            System.err.println("WARNING: " + warning);
        }

        CopyHelpers.ensureDirectory(outputDir);
        String content = buildCopilotMcpJson(config);
        Path dest =
                outputDir.resolve("copilot-mcp.json");
        CopyHelpers.writeFile(dest, content);

        return List.of(dest.toString());
    }

    /**
     * Generates copilot-mcp.json with full result including
     * warnings. This method provides the complete
     * {@link AssemblerResult} for callers that need warning
     * information.
     *
     * @param config    the project configuration
     * @param outputDir the target output directory
     * @return result with generated files and warnings
     */
    public AssemblerResult assembleWithWarnings(
            ProjectConfig config,
            Path outputDir) {
        if (config.mcp().servers().isEmpty()) {
            return AssemblerResult.empty();
        }

        List<String> warnings = warnLiteralEnvValues(
                config.mcp().servers());

        CopyHelpers.ensureDirectory(outputDir);
        String content = buildCopilotMcpJson(config);
        Path dest =
                outputDir.resolve("copilot-mcp.json");
        CopyHelpers.writeFile(dest, content);

        return AssemblerResult.of(
                List.of(dest.toString()), warnings);
    }

    /**
     * Validates that MCP server env var values use the
     * {@code $VARIABLE} format. Emits warnings for values
     * that appear to be literals.
     *
     * @param servers the list of MCP server configurations
     * @return list of warning messages for literal values
     */
    static List<String> warnLiteralEnvValues(
            List<McpServerConfig> servers) {
        List<String> warnings = new ArrayList<>();
        for (McpServerConfig server : servers) {
            for (Map.Entry<String, String> entry
                    : server.env().entrySet()) {
                String value = entry.getValue();
                if (value != null
                        && !value.startsWith("$")) {
                    warnings.add(
                            "MCP server '"
                                    + server.id()
                                    + "': env var '"
                                    + entry.getKey()
                                    + "' uses literal value"
                                    + " instead of"
                                    + " $VARIABLE format");
                }
            }
        }
        return warnings;
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
        sb.append(JsonHelpers.indent(2))
                .append('"')
                .append(JsonHelpers.escapeJson(server.id()))
                .append("\": {\n");

        sb.append(JsonHelpers.indent(3))
                .append("\"url\": \"")
                .append(JsonHelpers.escapeJson(server.url()))
                .append('"');

        boolean hasCaps =
                !server.capabilities().isEmpty();
        boolean hasEnv = !server.env().isEmpty();

        if (hasCaps || hasEnv) {
            sb.append(",\n");
        } else {
            sb.append('\n');
        }

        if (hasCaps) {
            appendCapabilities(sb, server.capabilities(),
                    hasEnv);
        }

        if (hasEnv) {
            appendEnv(sb, server.env());
        }

        sb.append(JsonHelpers.indent(2)).append("}\n");
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
