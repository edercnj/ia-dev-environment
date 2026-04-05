package dev.iadev.application.assembler;

import dev.iadev.domain.stack.CoreKpRouting;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writes core rules (01-08, plus conditional 09) and
 * routes core knowledge pack files during assembly.
 *
 * <p>Handles:
 * <ul>
 *   <li>Copying targets/claude/rules/*.md with placeholder
 *       replacement (rules 01-08)</li>
 *   <li>Routing core docs to knowledge packs</li>
 *   <li>Generating 01-project-identity.md</li>
 *   <li>Generating/copying 02-domain.md</li>
 *   <li>Conditionally generating 09-data-management.md
 *       when database is configured</li>
 *   <li>Copying conditional resources</li>
 * </ul>
 *
 * <p>Extracted from {@link RulesAssembler} per
 * story-0008-0014 to satisfy the 250-line SRP
 * constraint.</p>
 *
 * @see RulesAssembler
 */
public final class CoreRulesWriter {

    private final Path resourcesDir;
    private final AntiPatternsRuleWriter antiPatternsWriter;
    private final PciRuleWriter pciRuleWriter;

    /**
     * Creates a CoreRulesWriter with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    CoreRulesWriter(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
        this.antiPatternsWriter =
                new AntiPatternsRuleWriter(resourcesDir);
        this.pciRuleWriter =
                new PciRuleWriter(resourcesDir);
    }

    /**
     * Copies core rule template files with placeholder
     * replacement.
     *
     * @param rulesDir the rules output directory
     * @param engine   the template engine
     * @param context  the placeholder context
     * @return list of generated file paths
     */
    List<String> copyCoreRules(
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path coreRules =
                resourcesDir.resolve(
                        "targets/claude/rules");
        if (!Files.exists(coreRules)
                || !Files.isDirectory(coreRules)) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        List<Path> files =
                CopyHelpers.listMdFilesSorted(coreRules);
        for (Path file : files) {
            String dest = CopyHelpers.copyTemplateFile(
                    file,
                    rulesDir.resolve(
                            file.getFileName().toString()),
                    engine, context);
            generated.add(dest);
        }
        return generated;
    }

    /**
     * Routes core documentation files to knowledge pack
     * directories based on project configuration.
     *
     * @param config    the project configuration
     * @param skillsDir the skills output directory
     * @return list of generated file paths
     */
    List<String> routeCoreToKps(
            ProjectConfig config, Path skillsDir) {
        Path coreDir = resourcesDir.resolve("core");
        if (!Files.exists(coreDir)
                || !Files.isDirectory(coreDir)) {
            return List.of();
        }
        var routes =
                CoreKpRouting.getActiveRoutes(config);
        List<String> generated = new ArrayList<>();
        for (var route : routes) {
            Path src = coreDir.resolve(route.sourceFile());
            if (!Files.exists(src)
                    || !Files.isRegularFile(src)) {
                continue;
            }
            Path destDir = skillsDir.resolve(
                    route.kpName() + "/references");
            CopyHelpers.ensureDirectory(destDir);
            Path dest = destDir.resolve(route.destFile());
            generated.add(
                    CopyHelpers.copyStaticFile(src, dest));
        }
        return generated;
    }

    /**
     * Generates the 01-project-identity.md rule file.
     *
     * @param config   the project configuration
     * @param rulesDir the rules output directory
     * @return the generated file path
     */
    String generateProjectIdentity(
            ProjectConfig config, Path rulesDir) {
        Path dest =
                rulesDir.resolve("01-project-identity.md");
        String content =
                RulesIdentity.buildContent(config);
        try {
            Files.writeString(
                    dest, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write identity rule", e);
        }
        return dest.toString();
    }

    /**
     * Copies or generates the 02-domain.md rule file.
     *
     * @param config  the project configuration
     * @param rulesDir the rules output directory
     * @param engine  the template engine
     * @param context the placeholder context
     * @return the generated file path
     */
    String copyDomainTemplate(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path dest = rulesDir.resolve("02-domain.md");
        Path template = resourcesDir.resolve(
                "templates/domain-template.md");
        if (Files.exists(template)
                && Files.isRegularFile(template)) {
            return CopyHelpers.copyTemplateFile(
                    template, dest, engine, context);
        }
        try {
            Files.writeString(dest,
                    RulesIdentity.fallbackDomainContent(
                            config),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write domain rule", e);
        }
        return dest.toString();
    }

    /**
     * Copies conditional resource files (database, cache,
     * security, cloud, infrastructure).
     *
     * @param config    the project configuration
     * @param skillsDir the skills output directory
     * @param engine    the template engine
     * @param context   the placeholder context
     * @return list of generated file paths
     */
    List<String> copyConditionals(
            ProjectConfig config,
            Path skillsDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        List<String> generated = new ArrayList<>();
        generated.addAll(
                RulesConditionals.copyDatabaseRefs(
                        new ConditionalCopyContext(
                                config, resourcesDir,
                                skillsDir, engine,
                                context)));
        generated.addAll(
                RulesConditionals.copyCacheRefs(
                        config, resourcesDir, skillsDir));
        generated.addAll(
                RulesConditionals.assembleSecurityRules(
                        config, resourcesDir, skillsDir));
        generated.addAll(
                RulesConditionals.assembleCloudKnowledge(
                        config, resourcesDir, skillsDir));
        generated.addAll(
                RulesConditionals.assembleInfraKnowledge(
                        config, resourcesDir, skillsDir));
        return generated;
    }

    /**
     * Copies the conditional 09-data-management.md rule
     * when database is configured (not "none").
     *
     * @param config   the project configuration
     * @param rulesDir the rules output directory
     * @param engine   the template engine
     * @param context  the placeholder context
     * @return list of generated file paths (0 or 1)
     */
    List<String> copyConditionalDataRule(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        String dbName = config.databaseName();
        if (dbName == null || dbName.isBlank()
                || NONE_VALUE.equals(dbName)) {
            return List.of();
        }
        Path template = resourcesDir.resolve(
                "targets/claude/rules/conditional/"
                        + "09-data-management.md");
        if (!Files.exists(template)
                || !Files.isRegularFile(template)) {
            return List.of();
        }
        Path dest = rulesDir.resolve(
                "09-data-management.md");
        String path = CopyHelpers.copyTemplateFile(
                template, dest, engine, context);
        return List.of(path);
    }

    /**
     * Conditionally generates the 10-anti-patterns.md rule
     * when a matching template exists for the project's
     * language+framework combination.
     *
     * @param config   the project configuration
     * @param rulesDir the rules output directory
     * @param engine   the template engine
     * @param context  the placeholder context
     * @return list of generated file paths (0 or 1)
     */
    List<String> copyConditionalAntiPatternsRule(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        return antiPatternsWriter
                .copyConditionalAntiPatternsRule(
                        config, rulesDir, engine, context);
    }

    /**
     * Conditionally generates the 11-security-pci.md rule
     * when compliance includes pci-dss.
     *
     * @param config   the project configuration
     * @param rulesDir the rules output directory
     * @param engine   the template engine
     * @param context  the placeholder context
     * @return list of generated file paths (0 or 1)
     */
    List<String> copyConditionalPciRule(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        return pciRuleWriter
                .copyConditionalPciRule(
                        config, rulesDir, engine, context);
    }

    private static final String NONE_VALUE = "none";
}
