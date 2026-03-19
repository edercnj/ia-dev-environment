package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.stack.LanguageCommandSet;
import dev.iadev.domain.stack.StackMapping;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles CI/CD pipeline artifacts conditionally based on
 * the project configuration.
 *
 * <p>This is the twenty-first assembler in the pipeline
 * (position 21 of 23 per RULE-005). It generates up to five
 * types of artifacts:
 * <ol>
 *   <li>CI workflow ({@code .github/workflows/ci.yml}) —
 *       always generated</li>
 *   <li>Dockerfile — conditional on
 *       {@code container == "docker"}</li>
 *   <li>Docker Compose — conditional on
 *       {@code container == "docker"}</li>
 *   <li>K8s manifests (deployment, service, configmap) —
 *       conditional on
 *       {@code orchestrator == "kubernetes"}</li>
 *   <li>Smoke test config — conditional on
 *       {@code smokeTests == true}</li>
 * </ol>
 *
 * <p>Uses {@link #buildStackContext(ProjectConfig)} to resolve
 * stack-specific commands (compile, build, test, coverage,
 * lint) from {@link StackMapping} constants.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler cicd = new CicdAssembler();
 * List<String> files = cicd.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see StackMapping
 */
public final class CicdAssembler implements Assembler {

    private static final String CICD_TEMPLATES =
            "cicd-templates";
    private static final String CI_TEMPLATE =
            "ci-workflow/ci.yml.njk";
    private static final String COMPOSE_TEMPLATE =
            "docker-compose/docker-compose.yml.njk";
    private static final String SMOKE_SOURCE =
            "smoke-tests/smoke-config.md";
    private static final String DOCKER_CONDITION =
            "docker";
    private static final String K8S_CONDITION =
            "kubernetes";

    private static final List<String> K8S_MANIFESTS =
            List.of(
                    "deployment.yaml",
                    "service.yaml",
                    "configmap.yaml");

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

    private final Path resourcesDir;

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
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds a stack context, then generates CI/CD
     * artifacts conditionally. Returns the list of generated
     * file paths.</p>
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
        GenerationContext gc = new GenerationContext(
                config, outputDir, resourcesDir,
                engine, fullContext,
                new ArrayList<>(), new ArrayList<>());

        generateCiWorkflow(gc);
        generateDockerfile(gc);
        generateDockerCompose(gc);
        generateK8sManifests(gc);
        generateSmokeTestConfig(gc);

        return gc.files;
    }

    /**
     * Builds a stack-specific template context from
     * {@link StackMapping} constants.
     *
     * <p>Resolves compile, build, test, coverage, and lint
     * commands for the language/build-tool combination.
     * Also resolves framework port, health path, and Docker
     * base image.</p>
     *
     * @param config the project configuration
     * @return the stack context map
     */
    static Map<String, Object> buildStackContext(
            ProjectConfig config) {
        String langKey = config.language().name()
                + "-" + config.framework().buildTool();
        LanguageCommandSet commands =
                StackMapping.LANGUAGE_COMMANDS.get(langKey);
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

        Map<String, Object> ctx =
                new LinkedHashMap<>(16);
        ctx.put("compile_cmd",
                commands != null
                        ? commands.compileCmd() : "");
        ctx.put("build_cmd",
                commands != null
                        ? commands.buildCmd() : "");
        ctx.put("test_cmd",
                commands != null
                        ? commands.testCmd() : "");
        ctx.put("coverage_cmd",
                commands != null
                        ? commands.coverageCmd() : "");
        ctx.put("lint_cmd",
                LINT_COMMANDS.getOrDefault(
                        langKey, DEFAULT_LINT_CMD));
        ctx.put("file_extension",
                commands != null
                        ? commands.fileExtension() : "");
        ctx.put("build_file",
                commands != null
                        ? commands.buildFile() : "");
        ctx.put("package_manager",
                commands != null
                        ? commands.packageManager() : "");
        ctx.put("framework_port", port);
        ctx.put("health_path", healthPath);
        ctx.put("docker_base_image", resolvedImage);
        ctx.put("container",
                config.infrastructure().container());
        return ctx;
    }

    /**
     * CI workflow — always generated.
     *
     * @param gc the generation context
     */
    private void generateCiWorkflow(
            GenerationContext gc) {
        Path dest = gc.outputDir
                .resolve(".github")
                .resolve("workflows")
                .resolve("ci.yml");
        String err = renderAndWrite(
                gc.engine, CI_TEMPLATE, dest, gc.ctx);
        if (err == null) {
            gc.files.add(dest.toString());
        } else {
            gc.warnings.add(err);
        }
    }

    /**
     * Dockerfile — conditional on container == "docker".
     *
     * @param gc the generation context
     */
    private void generateDockerfile(
            GenerationContext gc) {
        if (!DOCKER_CONDITION.equals(
                gc.config.infrastructure().container())) {
            gc.warnings.add(
                    "Dockerfile skipped:"
                            + " container is not docker");
            return;
        }
        String stackKey = gc.config.language().name()
                + "-"
                + gc.config.framework().buildTool();
        String tpl = "dockerfile/Dockerfile."
                + stackKey + ".njk";
        Path srcPath = gc.resourcesDir
                .resolve(CICD_TEMPLATES).resolve(tpl);
        if (!Files.exists(srcPath)) {
            gc.warnings.add(
                    "Dockerfile template not found"
                            + " for stack: " + stackKey);
            return;
        }
        Path dest = gc.outputDir.resolve("Dockerfile");
        String err = renderAndWrite(
                gc.engine, tpl, dest, gc.ctx);
        if (err == null) {
            gc.files.add(dest.toString());
        } else {
            gc.warnings.add(err);
        }
    }

    /**
     * Docker Compose — conditional on
     * container == "docker".
     *
     * @param gc the generation context
     */
    private void generateDockerCompose(
            GenerationContext gc) {
        if (!DOCKER_CONDITION.equals(
                gc.config.infrastructure().container())) {
            gc.warnings.add(
                    "Docker Compose skipped:"
                            + " container is not docker");
            return;
        }
        Path dest = gc.outputDir
                .resolve("docker-compose.yml");
        String err = renderAndWrite(
                gc.engine, COMPOSE_TEMPLATE,
                dest, gc.ctx);
        if (err == null) {
            gc.files.add(dest.toString());
        } else {
            gc.warnings.add(err);
        }
    }

    /**
     * K8s manifests — conditional on
     * orchestrator == "kubernetes".
     *
     * @param gc the generation context
     */
    private void generateK8sManifests(
            GenerationContext gc) {
        if (!K8S_CONDITION.equals(
                gc.config.infrastructure()
                        .orchestrator())) {
            gc.warnings.add(
                    "K8s manifests skipped:"
                            + " orchestrator is not"
                            + " kubernetes");
            return;
        }
        for (String manifest : K8S_MANIFESTS) {
            Path dest = gc.outputDir
                    .resolve("k8s").resolve(manifest);
            String tpl = "k8s/"
                    + manifest.replace(
                    ".yaml", ".yaml.njk");
            String err = renderAndWrite(
                    gc.engine, tpl, dest, gc.ctx);
            if (err == null) {
                gc.files.add(dest.toString());
            } else {
                gc.warnings.add(err);
            }
        }
    }

    /**
     * Smoke test config — conditional on
     * smokeTests == true.
     *
     * @param gc the generation context
     */
    private void generateSmokeTestConfig(
            GenerationContext gc) {
        if (!gc.config.testing().smokeTests()) {
            gc.warnings.add(
                    "Smoke test config skipped:"
                            + " smokeTests is false");
            return;
        }
        Path src = gc.resourcesDir
                .resolve(CICD_TEMPLATES)
                .resolve(SMOKE_SOURCE);
        if (!Files.exists(src)) {
            return;
        }
        Path dest = gc.outputDir
                .resolve("tests")
                .resolve("smoke")
                .resolve("smoke-config.md");
        CopyHelpers.ensureDirectory(dest.getParent());
        copyFile(src, dest);
        gc.files.add(dest.toString());
    }

    /**
     * Renders a template and writes to disk.
     *
     * @param engine          the template engine
     * @param templateRelPath template path relative to
     *                        cicd-templates/
     * @param destPath        the destination file path
     * @param extraContext    additional context variables
     * @return null on success, error message on failure
     */
    private String renderAndWrite(
            TemplateEngine engine,
            String templateRelPath,
            Path destPath,
            Map<String, Object> extraContext) {
        try {
            String content = engine.render(
                    CICD_TEMPLATES + "/"
                            + templateRelPath,
                    extraContext);
            CopyHelpers.ensureDirectory(
                    destPath.getParent());
            Files.writeString(
                    destPath, content,
                    StandardCharsets.UTF_8);
            return null;
        } catch (Exception e) {
            return "Failed to render "
                    + templateRelPath + ": "
                    + e.getMessage();
        }
    }

    private static void copyFile(
            Path src, Path dest) {
        try {
            Files.copy(src, dest,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to copy file: " + src, e);
        }
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
        var url = CicdAssembler.class.getClassLoader()
                .getResource(CICD_TEMPLATES);
        if (url == null) {
            return Path.of("src/main/resources");
        }
        // Go up 1 level: cicd-templates dir -> resources
        return Path.of(url.getPath()).getParent();
    }

    /**
     * Bundled context for generation methods.
     *
     * @param config      the project configuration
     * @param outputDir   the output directory
     * @param resourcesDir the resources directory
     * @param engine      the template engine
     * @param ctx         the merged template context
     * @param files       mutable list of generated files
     * @param warnings    mutable list of warnings
     */
    private record GenerationContext(
            ProjectConfig config,
            Path outputDir,
            Path resourcesDir,
            TemplateEngine engine,
            Map<String, Object> ctx,
            List<String> files,
            List<String> warnings) {
    }
}
