package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.model.McpServerConfig;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * (position 18 of 23 per RULE-005). Its target is
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
        Path hooksDir = outputDir.getParent()
                .resolve(".claude").resolve("hooks");
        boolean hasHooks =
                CodexShared.detectHooks(hooksDir);

        for (McpServerConfig server
                : config.mcp().servers()) {
            if (!CodexShared.isValidTomlBareKey(
                    server.id())) {
                System.err.println(
                        "WARNING: MCP server id \""
                                + server.id()
                                + "\" contains invalid"
                                + " TOML characters");
            }
        }

        Map<String, Object> context =
                buildConfigContext(config, hasHooks);

        String rendered = engine.render(
                TEMPLATE_PATH, context);

        CopyHelpers.ensureDirectory(outputDir);
        Path dest = outputDir.resolve("config.toml");
        writeFile(dest, rendered);

        return List.of(dest.toString());
    }

    /**
     * Builds the template context for {@code config.toml}
     * rendering.
     *
     * @param config   the project configuration
     * @param hasHooks whether hooks were detected
     * @return the template context map
     */
    static Map<String, Object> buildConfigContext(
            ProjectConfig config,
            boolean hasHooks) {
        List<Map<String, Object>> mcpServers =
                CodexShared.mapMcpServers(config);
        Map<String, Object> ctx = new LinkedHashMap<>(
                ContextBuilder.buildContext(config));
        ctx.put("model", CodexShared.DEFAULT_MODEL);
        ctx.put("approval_policy",
                CodexShared.deriveApprovalPolicy(hasHooks));
        ctx.put("sandbox_mode",
                CodexShared.SANDBOX_WORKSPACE_WRITE);
        ctx.put("mcp_servers", mcpServers);
        ctx.put("has_mcp", !mcpServers.isEmpty());
        return ctx;
    }

    private static void writeFile(
            Path dest, String content) {
        try {
            Files.writeString(
                    dest, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write file: " + dest, e);
        }
    }
}
