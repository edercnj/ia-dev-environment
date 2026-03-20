package dev.iadev.assembler;

import dev.iadev.model.McpServerConfig;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@code .github/copilot-mcp.json} with MCP
 * server configuration for GitHub Copilot.
 *
 * <p>JSON building is delegated to
 * {@link McpJsonBuilder}.</p>
 *
 * @see Assembler
 * @see McpJsonBuilder
 * @see McpServerConfig
 */
public final class GithubMcpAssembler
        implements Assembler {

    private final Path resourcesDir;

    /**
     * Creates a GithubMcpAssembler using classpath
     * resources.
     */
    public GithubMcpAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a GithubMcpAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public GithubMcpAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /** {@inheritDoc} */
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
        return assembleWithWarnings(config, outputDir);
    }

    /**
     * Generates copilot-mcp.json with full result
     * including warnings.
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
        String content =
                McpJsonBuilder.buildCopilotMcpJson(config);
        Path dest =
                outputDir.resolve("copilot-mcp.json");
        CopyHelpers.writeFile(dest, content);

        return AssemblerResult.of(
                List.of(dest.toString()), warnings);
    }

    /**
     * Validates that MCP server env var values use the
     * {@code $VARIABLE} format.
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
                            ("MCP server '%s': env var"
                                    + " '%s' uses literal"
                                    + " value instead of"
                                    + " $VARIABLE format")
                                    .formatted(server.id(),
                                            entry.getKey()));
                }
            }
        }
        return warnings;
    }

    /**
     * Delegates to {@link McpJsonBuilder}.
     *
     * @param config the project configuration
     * @return JSON string
     */
    static String buildCopilotMcpJson(
            ProjectConfig config) {
        return McpJsonBuilder.buildCopilotMcpJson(config);
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot("core");
    }
}
