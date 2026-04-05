package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@code contracts/api/grpc-reference.md} from the
 * {@code _TEMPLATE-GRPC-REFERENCE.md} Pebble template.
 *
 * <p>This is the fifteenth assembler in the pipeline
 * (position 15 of 23 per RULE-005). It is
 * <strong>interface-aware</strong>: only generates output
 * when the project configuration includes a {@code grpc}
 * interface type.</p>
 *
 * <p>Double graceful no-op:
 * <ol>
 *   <li>If no gRPC interface is configured, returns an
 *       empty list immediately (no I/O).</li>
 *   <li>If gRPC is configured but the template file does
 *       not exist, returns an empty list (backward
 *       compatibility).</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler grpcDocs = new GrpcDocsAssembler();
 * List<String> files = grpcDocs.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see ConditionEvaluator#hasInterface
 * @see TemplateEngine#render(String, Map)
 */
public final class GrpcDocsAssembler implements Assembler {

    private static final String TEMPLATE_PATH =
            "shared/templates/_TEMPLATE-GRPC-REFERENCE.md";
    private static final String OUTPUT_SUBDIR = "contracts/api";
    private static final String OUTPUT_FILENAME =
            "grpc-reference.md";

    private final Path resourcesDir;

    /**
     * Creates a GrpcDocsAssembler using classpath resources.
     */
    public GrpcDocsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a GrpcDocsAssembler with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    public GrpcDocsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks for gRPC interface presence first, then
     * verifies template existence before rendering with
     * Pebble and writing to
     * {@code api/grpc-reference.md}.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        if (!ConditionEvaluator.hasInterface(
                config, "grpc")) {
            return List.of();
        }
        Path templateFile =
                resourcesDir.resolve(TEMPLATE_PATH);
        if (!Files.exists(templateFile)) {
            return List.of();
        }
        Map<String, Object> context =
                ContextBuilder.buildContext(config);
        String rendered = engine.render(
                TEMPLATE_PATH, context);
        Path destDir =
                outputDir.resolve(OUTPUT_SUBDIR);
        CopyHelpers.ensureDirectory(destDir);
        Path destFile =
                destDir.resolve(OUTPUT_FILENAME);
        CopyHelpers.writeFile(destFile, rendered);
        return List.of(destFile.toString());
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATE_PATH, 3);
    }
}
