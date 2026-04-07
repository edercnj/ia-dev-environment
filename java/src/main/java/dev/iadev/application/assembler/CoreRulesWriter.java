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
 * Writes core rules (01-09, plus conditional 09-12)
 * and routes core knowledge pack files during assembly.
 *
 * @see RulesAssembler
 */
public final class CoreRulesWriter {

    private final Path resourcesDir;
    private final AntiPatternsRuleWriter antiPatternsWriter;
    private final PciRuleWriter pciRuleWriter;
    private final SecurityAntiPatternsRuleWriter
            securityAntiPatternsWriter;

    CoreRulesWriter(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
        this.antiPatternsWriter =
                new AntiPatternsRuleWriter(resourcesDir);
        this.pciRuleWriter =
                new PciRuleWriter(resourcesDir);
        this.securityAntiPatternsWriter =
                new SecurityAntiPatternsRuleWriter(
                        resourcesDir);
    }

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

    List<String> routeCoreToKps(
            ProjectConfig config, Path skillsDir) {
        Path coreDir = resourcesDir.resolve("knowledge/core");
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

    String copyDomainTemplate(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path dest = rulesDir.resolve("02-domain.md");
        Path template = resourcesDir.resolve(
                "shared/templates/domain-template.md");
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

    List<String> copyConditionalAntiPatternsRule(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        return antiPatternsWriter
                .copyConditionalAntiPatternsRule(
                        config, rulesDir, engine, context);
    }

    List<String> copyConditionalPciRule(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        return pciRuleWriter
                .copyConditionalPciRule(
                        config, rulesDir, engine, context);
    }

    List<String> copyConditionalSecurityAntiPatternsRule(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        return securityAntiPatternsWriter
                .copyConditionalSecurityAntiPatternsRule(
                        config, rulesDir, engine, context);
    }

    private static final String NONE_VALUE = "none";
}
