package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@code specs/_templates/_TEMPLATE-DATA-MIGRATION-PLAN.md}
 * from the Pebble template of the same name.
 *
 * <p>This assembler is <strong>conditional</strong>: the template
 * is only generated when {@code database_name != "none"}. Projects
 * without a database (e.g., CLI tools) skip generation entirely.</p>
 *
 * <p>The template includes conditional sections based on the
 * migration tool (Flyway, Alembic, Mongock, Prisma, golang-migrate,
 * Diesel) and the database type (SQL vs MongoDB) for validation
 * queries.</p>
 *
 * <p>Graceful no-op: if the source template file does not exist
 * in the resources directory, returns an empty list (backward
 * compatibility).</p>
 *
 * @see Assembler
 * @see TemplateEngine#render(String, Map)
 */
public final class DataMigrationPlanAssembler
        implements Assembler {

    private static final String TEMPLATE_PATH =
            "shared/templates/_TEMPLATE-DATA-MIGRATION-PLAN.md";
    private static final String OUTPUT_SUBDIR =
            "specs/_templates";
    private static final String OUTPUT_FILENAME =
            "_TEMPLATE-DATA-MIGRATION-PLAN.md";
    private static final String NO_DATABASE = "none";

    /** The 9 mandatory data migration plan sections. */
    static final List<String> MANDATORY_SECTIONS =
            List.of(
                    "Migration Summary",
                    "Risk Assessment",
                    "Pre-Migration Steps",
                    "Expand Phase",
                    "Migration Phase",
                    "Contract Phase",
                    "Validation Queries",
                    "Rollback Plan",
                    "Post-Migration");

    private final Path resourcesDir;

    /**
     * Creates a DataMigrationPlanAssembler using classpath
     * resources.
     */
    public DataMigrationPlanAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a DataMigrationPlanAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public DataMigrationPlanAssembler(
            Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the data migration plan template with Pebble
     * and writes it to
     * {@code specs/_templates/_TEMPLATE-DATA-MIGRATION-PLAN.md}.
     * Returns empty list if database type is "none" or the
     * source template is missing.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        if (isDatabaseNone(config)) {
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

    private static boolean isDatabaseNone(
            ProjectConfig config) {
        String dbName = config.databaseName();
        return NO_DATABASE.equals(dbName)
                || dbName == null
                || dbName.isEmpty();
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourceDir("shared")
                .getParent();
    }
}
