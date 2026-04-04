package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.stack.VersionResolver;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@code .claude/rules/} and {@code .claude/skills/}
 * from source knowledge packs and templates.
 *
 * <p>This is the first assembler in the pipeline (position 1 of
 * 23 per RULE-005). It delegates to specialized writers:
 * {@link CoreRulesWriter} for 8 core rules (plus conditional
 * rule 09), {@link LanguageKpWriter} for language knowledge
 * packs, and {@link FrameworkKpWriter} for framework knowledge
 * packs.</p>
 *
 * <p>Assembly layers:
 * <ol>
 *   <li>Core rules — copy core-rules/*.md with placeholder
 *       replacement</li>
 *   <li>Core KP routing — route core docs to knowledge
 *       packs</li>
 *   <li>Language KPs — language-specific coding standards
 *       and testing conventions</li>
 *   <li>Framework KPs — framework-specific patterns</li>
 *   <li>Project identity — generate 01-project-identity.md
 *       (overwrites template copy)</li>
 *   <li>Domain template — generate/copy 02-domain.md</li>
 *   <li>Conditionals — database, cache, security, cloud,
 *       infrastructure</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler rules = new RulesAssembler();
 * List<String> files = rules.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see RulesIdentity
 * @see RulesConditionals
 * @see CoreRulesWriter
 * @see LanguageKpWriter
 * @see FrameworkKpWriter
 */
public final class RulesAssembler implements Assembler {

    private final CoreRulesWriter coreWriter;
    private final LanguageKpWriter languageWriter;
    private final FrameworkKpWriter frameworkWriter;

    /**
     * Creates a RulesAssembler using classpath resources.
     */
    public RulesAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a RulesAssembler with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    public RulesAssembler(Path resourcesDir) {
        this(resourcesDir,
                new VersionResolver(
                        new FileSystemVersionProvider()));
    }

    /**
     * Creates a RulesAssembler with explicit resources
     * directory and version resolver.
     *
     * @param resourcesDir the base resources directory
     * @param versionResolver the version resolver
     */
    RulesAssembler(Path resourcesDir,
            VersionResolver versionResolver) {
        this.coreWriter =
                new CoreRulesWriter(resourcesDir);
        this.languageWriter =
                new LanguageKpWriter(
                        resourcesDir, versionResolver);
        this.frameworkWriter =
                new FrameworkKpWriter(
                        resourcesDir, versionResolver);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Orchestrates all assembly layers, returning the
     * list of generated file paths.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path rulesDir = outputDir.resolve("rules");
        Path skillsDir = outputDir.resolve("skills");
        CopyHelpers.ensureDirectory(rulesDir);
        CopyHelpers.ensureDirectory(skillsDir);

        Map<String, Object> context =
                ContextBuilder.buildContext(config);

        List<String> generated = new ArrayList<>();
        assembleCoreRulesAndKps(
                config, engine, rulesDir,
                skillsDir, context, generated);
        assembleIdentityAndConditionals(
                config, engine, rulesDir,
                skillsDir, context, generated);
        return generated;
    }

    private void assembleCoreRulesAndKps(
            ProjectConfig config, TemplateEngine engine,
            Path rulesDir, Path skillsDir,
            Map<String, Object> context,
            List<String> generated) {
        generated.addAll(
                coreWriter.copyCoreRules(
                        rulesDir, engine, context));
        generated.addAll(
                coreWriter.routeCoreToKps(
                        config, skillsDir));
        generated.addAll(
                languageWriter.copyLanguageKps(
                        config, skillsDir));
        generated.addAll(
                frameworkWriter.copyFrameworkKps(
                        config, skillsDir));
    }

    private void assembleIdentityAndConditionals(
            ProjectConfig config, TemplateEngine engine,
            Path rulesDir, Path skillsDir,
            Map<String, Object> context,
            List<String> generated) {
        generated.add(
                coreWriter.generateProjectIdentity(
                        config, rulesDir));
        generated.add(
                coreWriter.copyDomainTemplate(
                        config, rulesDir, engine,
                        context));
        generated.addAll(
                coreWriter.copyConditionalDataRule(
                        config, rulesDir, engine,
                        context));
        generated.addAll(
                coreWriter.copyConditionals(
                        config, skillsDir, engine,
                        context));
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot("core-rules");
    }
}
