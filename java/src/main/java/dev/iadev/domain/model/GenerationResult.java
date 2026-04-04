package dev.iadev.domain.model;

import java.util.List;

/**
 * Immutable result of environment generation.
 *
 * <p>Returned by
 * {@link dev.iadev.domain.port.input.GenerateEnvironmentUseCase}
 * after generating all artifacts.</p>
 *
 * @param success whether generation completed without errors
 * @param filesGenerated list of generated file paths (immutable)
 * @param warnings list of non-fatal warnings (immutable)
 */
public record GenerationResult(
        boolean success,
        List<String> filesGenerated,
        List<String> warnings) {

    /**
     * Compact constructor enforcing immutability.
     */
    public GenerationResult {
        filesGenerated = List.copyOf(filesGenerated);
        warnings = List.copyOf(warnings);
    }
}
