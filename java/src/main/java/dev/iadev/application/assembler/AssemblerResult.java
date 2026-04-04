package dev.iadev.assembler;

import java.util.List;

/**
 * Immutable result of an assembler operation.
 *
 * <p>Shared type replacing the duplicate inner records
 * previously defined in {@code GithubAgentsAssembler},
 * {@code GithubMcpAssembler}, and
 * {@code AssemblerPipeline.NormalizedResult}.</p>
 *
 * @param files    list of generated file paths (never null)
 * @param warnings list of warning messages (never null)
 */
public record AssemblerResult(
        List<String> files,
        List<String> warnings) {

    /** Compact constructor: ensures lists are never null. */
    public AssemblerResult {
        files = files != null ? List.copyOf(files) : List.of();
        warnings = warnings != null
                ? List.copyOf(warnings) : List.of();
    }

    /**
     * Returns an empty result with no files and no warnings.
     *
     * @return an empty AssemblerResult
     */
    public static AssemblerResult empty() {
        return new AssemblerResult(List.of(), List.of());
    }

    /**
     * Factory method for convenience.
     *
     * @param files    list of generated file paths
     * @param warnings list of warning messages
     * @return a new AssemblerResult
     */
    public static AssemblerResult of(
            List<String> files,
            List<String> warnings) {
        return new AssemblerResult(files, warnings);
    }
}
