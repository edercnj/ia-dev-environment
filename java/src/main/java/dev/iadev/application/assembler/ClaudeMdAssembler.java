package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.stack.StackMapping;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@code CLAUDE.md} at the output root — the
 * executive summary auto-loaded by Claude Code on every
 * conversation.
 *
 * <p>Introduced in EPIC-0048 (v4.0.0) per ADR-0048-B as a
 * <b>single-responsibility</b> assembler dedicated to the
 * root {@code CLAUDE.md} file. Consumes the Pebble
 * template {@code shared/templates/CLAUDE.md} authored in
 * story-0048-0010.
 *
 * <p>Target: {@link AssemblerTarget#ROOT}.
 * Platforms: Claude Code only. Overwrites any existing
 * file unconditionally (generator-owned, per ADR-0048-B).
 * The {@code --no-claude-md} v4.0.0-only feature flag
 * opts out by excluding this assembler from
 * {@link AssemblerFactory#buildAssemblers}.</p>
 *
 * @see Assembler
 * @see AssemblerFactory
 * @see ContextBuilder
 */
public final class ClaudeMdAssembler implements Assembler {

    private static final String TEMPLATE_PATH =
            "shared/templates/CLAUDE.md";
    static final String OUTPUT_FILENAME = "CLAUDE.md";

    private final Path resourcesDir;

    /**
     * Creates a ClaudeMdAssembler using classpath resources.
     */
    public ClaudeMdAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a ClaudeMdAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public ClaudeMdAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path templateFile =
                resourcesDir.resolve(TEMPLATE_PATH);
        if (!Files.exists(templateFile)) {
            return List.of();
        }
        return renderClaudeMd(
                config, engine, outputDir);
    }

    @Override
    public AssemblerResult assembleWithResult(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        return AssemblerResult.of(
                assemble(config, engine, outputDir),
                List.of());
    }

    private List<String> renderClaudeMd(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Map<String, Object> context =
                buildClaudeMdContext(config);
        String rendered = engine.render(
                TEMPLATE_PATH, context);
        Path destFile =
                outputDir.resolve(OUTPUT_FILENAME);
        CopyHelpers.ensureDirectory(outputDir);
        CopyHelpers.writeFile(destFile, rendered);
        return List.of(destFile.toString());
    }

    private Map<String, Object> buildClaudeMdContext(
            ProjectConfig config) {
        Map<String, Object> context =
                new LinkedHashMap<>(
                        ContextBuilder.buildContext(config));

        context.put("PROJECT_NAME",
                config.project().name());
        context.put("LANGUAGE",
                config.language().name());
        context.put("FRAMEWORK",
                config.framework().name());
        context.put("ARCHITECTURE",
                config.architecture().style());
        context.put("DATABASES",
                String.valueOf(
                        context.getOrDefault(
                                "database_name", "")));
        context.put("INTERFACE_TYPES",
                joinInterfaces(config));

        String buildTool = config.framework().buildTool();
        String langTool = config.language().name()
                + "-" + buildTool;
        var cmdSet = StackMapping.LANGUAGE_COMMANDS
                .get(langTool);
        context.put("BUILD_COMMAND",
                cmdSet != null ? cmdSet.buildCmd() : "");
        context.put("TEST_COMMAND",
                cmdSet != null ? cmdSet.testCmd() : "");

        return context;
    }

    private static String joinInterfaces(
            ProjectConfig config) {
        if (config.interfaces() == null
                || config.interfaces().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (var iface : config.interfaces()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(iface.type());
        }
        return sb.toString();
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourceDir("shared")
                .getParent();
    }
}
