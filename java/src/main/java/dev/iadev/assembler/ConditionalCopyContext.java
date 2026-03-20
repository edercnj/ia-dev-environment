package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable parameter object for conditional resource copy
 * operations.
 *
 * <p>Groups the five parameters needed by
 * {@link RulesConditionals#copyDatabaseRefs} into a single
 * cohesive record, reducing parameter count from 5 to 1.</p>
 *
 * @param config      the project configuration
 * @param resourceDir the resources root directory
 * @param skillsDir   the skills output directory
 * @param engine      the template engine
 * @param context     the placeholder context map
 */
public record ConditionalCopyContext(
        ProjectConfig config,
        Path resourceDir,
        Path skillsDir,
        TemplateEngine engine,
        Map<String, Object> context) {

    /**
     * Validates that required fields are not null.
     */
    public ConditionalCopyContext {
        Objects.requireNonNull(config,
                "config must not be null");
        Objects.requireNonNull(resourceDir,
                "resourceDir must not be null");
        Objects.requireNonNull(skillsDir,
                "skillsDir must not be null");
        Objects.requireNonNull(engine,
                "engine must not be null");
        Objects.requireNonNull(context,
                "context must not be null");
    }
}
