package dev.iadev.domain.model;

import java.util.List;

/**
 * Represents the result of a verification comparison against golden files.
 *
 * <p>Contains success status, total file count, mismatches, and missing/extra files.</p>
 *
 * <p>Example:
 * <pre>{@code
 * var result = new VerificationResult(true, 42, List.of(), List.of(), List.of());
 * }</pre>
 * </p>
 *
 * @param success whether verification passed with no differences
 * @param totalFiles the total number of files compared
 * @param mismatches the list of file differences (immutable)
 * @param missingFiles the list of files present in reference but missing in output (immutable)
 * @param extraFiles the list of files present in output but missing from reference (immutable)
 */
public record VerificationResult(
        boolean success,
        int totalFiles,
        List<FileDiff> mismatches,
        List<String> missingFiles,
        List<String> extraFiles) {

    /**
     * Compact constructor enforcing immutability of lists.
     */
    public VerificationResult {
        mismatches = List.copyOf(mismatches);
        missingFiles = List.copyOf(missingFiles);
        extraFiles = List.copyOf(extraFiles);
    }
}
