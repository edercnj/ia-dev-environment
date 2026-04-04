package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.McpServerConfig;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates {@code .codex/config.toml} from Pebble template.
 *
 * <p>Operates in 2 phases:
 * <ol>
 *   <li>Derivation — computes model, approval_policy,
 *       sandbox_mode, maps MCP servers</li>
 *   <li>Rendering — passes derived values to
 *       {@code config.toml.njk} and writes output</li>
 * </ol>
 *
 * <p>This is the eighteenth assembler in the pipeline
 * (position 18 of 25 per RULE-005). Its target is
 * {@link AssemblerTarget#CODEX}.</p>
 *
 * @see Assembler
 * @see CodexShared
 */
public final class CodexConfigAssembler
        implements Assembler {

    private static final String TEMPLATE_PATH =
            "codex-templates/config.toml.njk";

    /**
     * {@inheritDoc}
     *
     * <p>Generates {@code config.toml} by deriving config
     * values and rendering the template.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        return assembleWithResult(
                config, engine, outputDir).files();
    }

    /** {@inheritDoc} */
    @Override
    public AssemblerResult assembleWithResult(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path hooksDir = outputDir.getParent()
                .resolve(".claude").resolve("hooks");
        Path agentsDir = outputDir.getParent()
                .resolve(".claude").resolve("agents");
        HookPresence hookPresence = HookPresence.of(
                CodexShared.detectHooks(hooksDir));
        List<AgentInfo> agents = CodexScanner.scanAgents(
                agentsDir);

        List<String> warnings =
                collectTomlKeyWarnings(config);

        Map<String, Object> context =
                buildConfigContext(
                        config, hookPresence, agents);

        String rendered = engine.render(
                TEMPLATE_PATH, context);

        CopyHelpers.ensureDirectory(outputDir);
        Path dest = outputDir.resolve("config.toml");
        CopyHelpers.writeFile(dest, rendered);

        return AssemblerResult.of(
                List.of(dest.toString()), warnings);
    }

    /**
     * Validates MCP server IDs for TOML bare key
     * compliance.
     *
     * @param config the project configuration
     * @return list of warning messages for invalid IDs
     */
    static List<String> collectTomlKeyWarnings(
            ProjectConfig config) {
        List<String> warnings = new ArrayList<>();
        for (McpServerConfig server
                : config.mcp().servers()) {
            if (!CodexShared.isValidTomlBareKey(
                    server.id())) {
                warnings.add(
                        ("MCP server id \"%s\" contains"
                                + " invalid TOML characters")
                                .formatted(server.id()));
            }
        }
        return warnings;
    }

    /**
     * Builds the template context for {@code config.toml}
     * rendering.
     *
     * @param config       the project configuration
     * @param hookPresence whether hooks were detected
     * @param agentsList   scanned Claude agents
     * @return the template context map
     */
    static Map<String, Object> buildConfigContext(
            ProjectConfig config,
            HookPresence hookPresence,
            List<AgentInfo> agentsList) {
        List<Map<String, Object>> mcpServers =
                CodexShared.mapMcpServers(config);
        List<Map<String, Object>> agents =
                mapAgents(agentsList);
        Map<String, Object> ctx = new LinkedHashMap<>(
                ContextBuilder.buildContext(config));
        ctx.put("model", CodexShared.DEFAULT_MODEL);
        ctx.put("approval_policy",
                CodexShared.deriveApprovalPolicy(
                        hookPresence));
        ctx.put("sandbox_mode",
                CodexShared.SANDBOX_WORKSPACE_WRITE);
        ctx.put("mcp_servers", mcpServers);
        ctx.put("has_mcp", !mcpServers.isEmpty());
        ctx.put("agents_list", agents);
        return ctx;
    }

    private static List<Map<String, Object>> mapAgents(
            List<AgentInfo> scanned) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentInfo agent : scanned) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", CodexShared.sanitizeTomlBareKey(
                    agent.name()));
            row.put("description", CodexShared.escapeTomlValue(
                    agent.description()));
            result.add(row);
        }
        return result;
    }

}
