package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.stack.LanguageCommandSet;
import dev.iadev.domain.stack.StackMapping;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinator that assembles CI/CD pipeline artifacts by
 * delegating to six specialized sub-assemblers.
 *
 * <p>This is the twenty-third assembler in the pipeline
 * (position 21 of 23 per RULE-005). It delegates to:
 * <ol>
 *   <li>{@link CiWorkflowAssembler} — always generated</li>
 *   <li>{@link CdWorkflowAssembler} — release always,
 *       deploy/rollback conditional on container</li>
 *   <li>{@link DockerfileAssembler} — conditional on
 *       {@code container == "docker"}</li>
 *   <li>{@link DockerComposeAssembler} — conditional on
 *       {@code container == "docker"}</li>
 *   <li>{@link K8sManifestAssembler} — conditional on
 *       {@code orchestrator == "kubernetes"}</li>
 *   <li>{@link SmokeTestAssembler} — conditional on
 *       {@code smokeTests == true}</li>
 * </ol>
 *
 * <p>Uses {@link #buildStackContext(ProjectConfig)} to resolve
 * stack-specific commands from {@link StackMapping}.</p>
 *
 * @see Assembler
 * @see StackMapping
 */
public final class CicdAssembler implements Assembler {

    private static final String CICD_TEMPLATES =
            "cicd-templates";

    /**
     * Lint command mapping per language-buildTool key.
     */
    static final Map<String, String> LINT_COMMANDS =
            Map.of(
                    "java-maven",
                    "./mvnw checkstyle:check",
                    "java-gradle",
                    "./gradlew spotlessCheck",
                    "kotlin-gradle",
                    "./gradlew ktlintCheck",
                    "typescript-npm",
                    "npm run lint",
                    "python-pip",
                    "ruff check .",
                    "go-go",
                    "golangci-lint run",
                    "go-go-mod",
                    "golangci-lint run",
                    "rust-cargo",
                    "cargo clippy -- -D warnings");

    private static final String DEFAULT_LINT_CMD =
            "echo 'No linter configured'";

    private static final int INITIAL_CICD_MAP_CAPACITY = 16;

    private final Path resourcesDir;
    private final CiWorkflowAssembler ciWorkflow;
    private final CdWorkflowAssembler cdWorkflow;
    private final DockerfileAssembler dockerfile;
    private final DockerComposeAssembler dockerCompose;
    private final K8sManifestAssembler k8sManifest;
    private final SmokeTestAssembler smokeTest;

    /**
     * Creates a CicdAssembler using classpath resources.
     */
    public CicdAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a CicdAssembler with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    public CicdAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
        this.ciWorkflow = new CiWorkflowAssembler();
        this.cdWorkflow = new CdWorkflowAssembler();
        this.dockerfile = new DockerfileAssembler();
        this.dockerCompose = new DockerComposeAssembler();
        this.k8sManifest = new K8sManifestAssembler();
        this.smokeTest = new SmokeTestAssembler();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds a stack context, then delegates to six
     * specialized sub-assemblers. Returns the merged list
     * of generated file paths.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Map<String, Object> ctx =
                buildStackContext(config);
        Map<String, Object> fullContext =
                mergeContexts(
                        ContextBuilder.buildContext(config),
                        ctx);
        CicdContext cicdCtx = new CicdContext(
                config, outputDir, resourcesDir,
                engine, fullContext);

        CicdResult result = CicdResult.merge(List.of(
                ciWorkflow.assemble(cicdCtx),
                cdWorkflow.assemble(cicdCtx),
                dockerfile.assemble(cicdCtx),
                dockerCompose.assemble(cicdCtx),
                k8sManifest.assemble(cicdCtx),
                smokeTest.assemble(cicdCtx)));

        return result.files();
    }

    /**
     * Builds a stack-specific template context from
     * {@link StackMapping} constants.
     *
     * @param config the project configuration
     * @return the stack context map
     */
    static Map<String, Object> buildStackContext(
            ProjectConfig config) {
        String langKey = config.language().name()
                + "-" + config.framework().buildTool();

        Map<String, Object> ctx =
                new LinkedHashMap<>(
                        INITIAL_CICD_MAP_CAPACITY);
        addCommandEntries(langKey, ctx);
        addEnvironmentEntries(config, ctx);
        return ctx;
    }

    private static void addCommandEntries(
            String langKey,
            Map<String, Object> ctx) {
        LanguageCommandSet commands =
                StackMapping.LANGUAGE_COMMANDS.get(langKey);
        addBuildCommands(commands, ctx);
        addToolingCommands(langKey, commands, ctx);
    }

    private static void addBuildCommands(
            LanguageCommandSet commands,
            Map<String, Object> ctx) {
        ctx.put("compile_cmd",
                cmd(commands, LanguageCommandSet::compileCmd));
        ctx.put("build_cmd",
                cmd(commands, LanguageCommandSet::buildCmd));
        ctx.put("test_cmd",
                cmd(commands, LanguageCommandSet::testCmd));
        ctx.put("coverage_cmd",
                cmd(commands, LanguageCommandSet::coverageCmd));
    }

    private static void addToolingCommands(
            String langKey,
            LanguageCommandSet commands,
            Map<String, Object> ctx) {
        ctx.put("lint_cmd",
                LINT_COMMANDS.getOrDefault(
                        langKey, DEFAULT_LINT_CMD));
        ctx.put("file_extension",
                cmd(commands, LanguageCommandSet::fileExtension));
        ctx.put("build_file",
                cmd(commands, LanguageCommandSet::buildFile));
        ctx.put("package_manager",
                cmd(commands, LanguageCommandSet::packageManager));
    }

    private static String cmd(
            LanguageCommandSet commands,
            java.util.function.Function<
                    LanguageCommandSet, String> getter) {
        return commands != null ? getter.apply(commands) : "";
    }

    private static void addEnvironmentEntries(
            ProjectConfig config,
            Map<String, Object> ctx) {
        int port = StackMapping.FRAMEWORK_PORTS.getOrDefault(
                config.framework().name(),
                StackMapping.DEFAULT_PORT_FALLBACK);
        String healthPath =
                StackMapping.FRAMEWORK_HEALTH_PATHS
                        .getOrDefault(
                                config.framework().name(),
                                StackMapping
                                        .DEFAULT_HEALTH_PATH);
        String baseImage =
                StackMapping.DOCKER_BASE_IMAGES.getOrDefault(
                        config.language().name(),
                        StackMapping.DEFAULT_DOCKER_IMAGE);
        String resolvedImage = baseImage.replace(
                "{version}",
                config.language().version());

        ctx.put("framework_port", port);
        ctx.put("health_path", healthPath);
        ctx.put("docker_base_image", resolvedImage);
        ctx.put("container",
                config.infrastructure().container());
        ctx.put("native_build",
                config.framework().nativeBuild());
    }

    private static Map<String, Object> mergeContexts(
            Map<String, Object> base,
            Map<String, Object> override) {
        Map<String, Object> merged =
                new LinkedHashMap<>(base);
        merged.putAll(override);
        return merged;
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(CICD_TEMPLATES);
    }
}
