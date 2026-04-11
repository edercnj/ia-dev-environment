package dev.iadev.domain.port.input;

import dev.iadev.domain.model.GenerationContext;
import dev.iadev.domain.model.GenerationResult;

/**
 * Use case contract for generating a complete development environment.
 *
 * <p>This is the primary use case of the system. Given a
 * {@link GenerationContext} containing validated configuration and
 * output directory, produces all artifacts ({@code .claude/},
 * Dockerfile, CI/CD, etc.) and returns a {@link GenerationResult}
 * summarizing the outcome.</p>
 *
 * <p>Implementations must be stateless and idempotent: calling
 * {@code generate} twice with the same context must produce the
 * same result.</p>
 *
 * @see GenerationContext
 * @see GenerationResult
 */
public interface GenerateEnvironmentUseCase {

    /**
     * Generates a development environment from the given context.
     *
     * @param context the generation context containing configuration
     *                and output directory (must not be null)
     * @return the generation result with file list and status
     */
    GenerationResult generate(GenerationContext context);
}
