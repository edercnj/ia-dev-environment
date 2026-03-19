package dev.iadev.assembler;

import dev.iadev.domain.stack.CoreKpRouting;
import dev.iadev.domain.stack.StackPackMapping;
import dev.iadev.domain.stack.VersionResolver;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Assembles {@code .claude/rules/} and {@code .claude/skills/}
 * from source knowledge packs and templates.
 *
 * <p>This is the first assembler in the pipeline (position 1 of
 * 23 per RULE-005). It generates 5 core rules unconditionally
 * plus conditional rules based on project features.</p>
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
 */
public final class RulesAssembler implements Assembler {

    private final Path resourcesDir;

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
        this.resourcesDir = resourcesDir;
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
                buildContext(config);

        List<String> generated = new ArrayList<>();
        generated.addAll(
                copyCoreRules(rulesDir, engine, context));
        generated.addAll(
                routeCoreToKps(config, skillsDir));
        generated.addAll(
                copyLanguageKps(config, skillsDir));
        generated.addAll(
                copyFrameworkKps(config, skillsDir));
        generated.add(
                generateProjectIdentity(config, rulesDir));
        generated.add(
                copyDomainTemplate(
                        config, rulesDir, engine, context));
        generated.addAll(
                copyConditionals(
                        config, skillsDir, engine, context));

        return generated;
    }

    /**
     * Builds the placeholder context map from config.
     *
     * @param config the project configuration
     * @return context map for placeholder replacement
     */
    static Map<String, Object> buildContext(
            ProjectConfig config) {
        return Map.ofEntries(
                Map.entry("project_name",
                        config.project().name()),
                Map.entry("project_purpose",
                        config.project().purpose()),
                Map.entry("language_name",
                        config.language().name()),
                Map.entry("language_version",
                        config.language().version()),
                Map.entry("framework_name",
                        config.framework().name()),
                Map.entry("framework_version",
                        config.framework().version()),
                Map.entry("build_tool",
                        config.framework().buildTool()),
                Map.entry("architecture_style",
                        config.architecture().style()),
                Map.entry("domain_driven",
                        String.valueOf(
                                config.architecture()
                                        .domainDriven())),
                Map.entry("event_driven",
                        String.valueOf(
                                config.architecture()
                                        .eventDriven())),
                Map.entry("container",
                        config.infrastructure().container()),
                Map.entry("orchestrator",
                        config.infrastructure()
                                .orchestrator()),
                Map.entry("database_name",
                        config.data().database().name()),
                Map.entry("cache_name",
                        config.data().cache().name()));
    }

    private List<String> copyCoreRules(
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path coreRules =
                resourcesDir.resolve("core-rules");
        if (!Files.exists(coreRules)
                || !Files.isDirectory(coreRules)) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        List<Path> files = listMdFilesSorted(coreRules);
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

    private List<String> routeCoreToKps(
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

    private List<String> copyLanguageKps(
            ProjectConfig config, Path skillsDir) {
        String lang = config.language().name();
        Path langDir =
                resourcesDir.resolve("languages/" + lang);
        if (!Files.exists(langDir)
                || !Files.isDirectory(langDir)) {
            return List.of();
        }
        Path codingRefs = skillsDir.resolve(
                "coding-standards/references");
        Path testingRefs = skillsDir.resolve(
                "testing/references");
        CopyHelpers.ensureDirectory(codingRefs);
        CopyHelpers.ensureDirectory(testingRefs);
        List<String> generated = new ArrayList<>();
        generated.addAll(
                copyLangCommon(langDir,
                        codingRefs, testingRefs));
        generated.addAll(
                copyLangVersion(config,
                        langDir, codingRefs));
        return generated;
    }

    private List<String> copyLangCommon(
            Path langDir,
            Path codingRefs, Path testingRefs) {
        Path common = langDir.resolve("common");
        if (!Files.exists(common)
                || !Files.isDirectory(common)) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        List<Path> files = listMdFilesSorted(common);
        for (Path file : files) {
            String name =
                    file.getFileName().toString();
            Path dest = name.contains("testing")
                    ? testingRefs : codingRefs;
            generated.add(
                    CopyHelpers.copyStaticFile(
                            file,
                            dest.resolve(name)));
        }
        return generated;
    }

    private List<String> copyLangVersion(
            ProjectConfig config,
            Path langDir, Path codingRefs) {
        Optional<Path> versionDir =
                VersionResolver.findVersionDir(
                        langDir,
                        config.language().name(),
                        config.language().version());
        if (versionDir.isEmpty()) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        List<Path> files =
                listMdFilesSorted(versionDir.get());
        for (Path file : files) {
            generated.add(
                    CopyHelpers.copyStaticFile(
                            file,
                            codingRefs.resolve(
                                    file.getFileName()
                                            .toString())));
        }
        return generated;
    }

    private List<String> copyFrameworkKps(
            ProjectConfig config, Path skillsDir) {
        String fw = config.framework().name();
        String packName =
                StackPackMapping.getStackPackName(fw);
        if (packName.isEmpty()) {
            return List.of();
        }
        Path fwDir =
                resourcesDir.resolve("frameworks/" + fw);
        if (!Files.exists(fwDir)
                || !Files.isDirectory(fwDir)) {
            return List.of();
        }
        Path refsDir = skillsDir.resolve(
                packName + "/references");
        CopyHelpers.ensureDirectory(refsDir);
        List<String> generated = new ArrayList<>();
        generated.addAll(copyFwCommon(fwDir, refsDir));
        generated.addAll(
                copyFwVersion(config, fwDir, refsDir));
        return generated;
    }

    private List<String> copyFwCommon(
            Path fwDir, Path refsDir) {
        Path common = fwDir.resolve("common");
        if (!Files.exists(common)
                || !Files.isDirectory(common)) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        List<Path> files = listMdFilesSorted(common);
        for (Path file : files) {
            generated.add(
                    CopyHelpers.copyStaticFile(
                            file,
                            refsDir.resolve(
                                    file.getFileName()
                                            .toString())));
        }
        return generated;
    }

    private List<String> copyFwVersion(
            ProjectConfig config,
            Path fwDir, Path refsDir) {
        Optional<Path> versionDir =
                VersionResolver.findVersionDir(
                        fwDir,
                        config.framework().name(),
                        config.framework().version());
        if (versionDir.isEmpty()) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        List<Path> files =
                listMdFilesSorted(versionDir.get());
        for (Path file : files) {
            generated.add(
                    CopyHelpers.copyStaticFile(
                            file,
                            refsDir.resolve(
                                    file.getFileName()
                                            .toString())));
        }
        return generated;
    }

    private String generateProjectIdentity(
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

    private String copyDomainTemplate(
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

    private List<String> copyConditionals(
            ProjectConfig config,
            Path skillsDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        List<String> generated = new ArrayList<>();
        generated.addAll(
                RulesConditionals.copyDatabaseRefs(
                        config, resourcesDir,
                        skillsDir, engine, context));
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

    private static List<Path> listMdFilesSorted(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(f -> f.toString()
                            .endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: " + dir, e);
        }
    }

    private static Path resolveClasspathResources() {
        var url = RulesAssembler.class.getClassLoader()
                .getResource("core-rules");
        if (url == null) {
            return Path.of("src/main/resources");
        }
        return Path.of(url.getPath()).getParent();
    }
}
