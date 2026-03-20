package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Immutable context shared across CI/CD sub-assemblers.
 *
 * <p>Bundles everything a sub-assembler needs to generate
 * artifacts: project config, output directory, resources
 * directory, template engine, and merged template context.
 * </p>
 *
 * <p>The {@code warnings} list is defensively copied via
 * {@link List#copyOf(java.util.Collection)} in the compact
 * constructor to guarantee immutability (L-007 fix).</p>
 *
 * @param config       the project configuration
 * @param outputDir    the output directory
 * @param resourcesDir the resources directory
 * @param engine       the template engine
 * @param ctx          the merged template context
 */
record CicdContext(
        ProjectConfig config,
        Path outputDir,
        Path resourcesDir,
        TemplateEngine engine,
        Map<String, Object> ctx) {

    CicdContext {
        ctx = Map.copyOf(ctx);
    }
}
