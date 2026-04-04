package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates {@code AGENTS.override.md} at project root.
 *
 * <p>This is the twenty-first assembler in the pipeline
 * (position 21 of 25 per RULE-005). Its target is
 * {@link AssemblerTarget#ROOT}.</p>
 */
public final class CodexOverrideAssembler implements Assembler {

    private static final String TEMPLATE_PATH =
            "codex-templates/agents-override.md.njk";

    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Map<String, Object> context = new LinkedHashMap<>(
                ContextBuilder.buildContext(config));
        String rendered = engine.render(
                TEMPLATE_PATH, context);

        CopyHelpers.ensureDirectory(outputDir);
        Path dest = outputDir.resolve("AGENTS.override.md");
        CopyHelpers.writeFile(dest, rendered);
        return List.of(dest.toString());
    }
}
