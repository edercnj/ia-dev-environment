package dev.iadev.domain.model;

import java.nio.file.Path;

/**
 * Immutable context for environment generation.
 *
 * <p>Carries all inputs needed by
 * {@link dev.iadev.domain.port.input.GenerateEnvironmentUseCase}
 * to produce a complete development environment.</p>
 *
 * @param config the validated project configuration
 * @param outputDirectory the target directory for generated files
 * @param verbose whether to enable verbose output
 */
public record GenerationContext(
        ProjectConfig config,
        Path outputDirectory,
        boolean verbose) {
}
