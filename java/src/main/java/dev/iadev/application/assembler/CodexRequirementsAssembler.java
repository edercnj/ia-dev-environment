package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates {@code .codex/requirements.toml} from template.
 *
 * <p>This is the twentieth assembler in the pipeline
 * (position 20 of 25 per RULE-005). Its target is
 * {@link AssemblerTarget#CODEX}.</p>
 */
public final class CodexRequirementsAssembler implements Assembler {

    private static final String TEMPLATE_PATH =
            "targets/codex/templates/requirements.toml.njk";

    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Map<String, Object> context = new LinkedHashMap<>(
                ContextBuilder.buildContext(config));
        context.put("approval_policy",
                deriveApprovalPolicy(config));
        context.put("sandbox_mode",
                CodexShared.SANDBOX_WORKSPACE_WRITE);

        String rendered = engine.render(
                TEMPLATE_PATH, context);

        CopyHelpers.ensureDirectory(outputDir);
        Path dest = outputDir.resolve("requirements.toml");
        CopyHelpers.writeFile(dest, rendered);
        return List.of(dest.toString());
    }

    static String deriveApprovalPolicy(ProjectConfig config) {
        return config.security().frameworks().isEmpty()
                ? "on-request" : "suggest";
    }
}
