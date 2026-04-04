package dev.iadev.domain.model;

import java.util.List;

/**
 * Represents the result of a pipeline execution.
 *
 * <p>Contains success status, output directory, generated file paths,
 * warnings, and execution duration.</p>
 *
 * <p>Example:
 * <pre>{@code
 * var result = new PipelineResult(true, "/output", List.of("file1.md"), List.of(), 1500);
 * }</pre>
 * </p>
 *
 * @param success whether the pipeline completed successfully
 * @param outputDir the output directory path
 * @param filesGenerated the list of generated file paths (immutable)
 * @param warnings the list of non-fatal warnings (immutable)
 * @param durationMs the execution duration in milliseconds
 */
public record PipelineResult(
        boolean success,
        String outputDir,
        List<String> filesGenerated,
        List<String> warnings,
        long durationMs) {

    /**
     * Compact constructor enforcing immutability of lists.
     */
    public PipelineResult {
        filesGenerated = List.copyOf(filesGenerated);
        warnings = List.copyOf(warnings);
    }
}
