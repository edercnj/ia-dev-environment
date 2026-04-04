package dev.iadev.application.assembler;

import java.util.List;

/**
 * Immutable result from a CI/CD sub-assembler.
 *
 * <p>Both lists are defensively copied in the compact
 * constructor to guarantee immutability (L-007 fix).</p>
 *
 * @param files    generated file paths
 * @param warnings warning messages
 */
record CicdResult(
        List<String> files,
        List<String> warnings) {

    CicdResult {
        files = List.copyOf(files);
        warnings = List.copyOf(warnings);
    }

    /**
     * Empty result with no files and no warnings.
     *
     * @return an empty CicdResult
     */
    static CicdResult empty() {
        return new CicdResult(
                List.of(), List.of());
    }

    /**
     * Merges multiple results into one.
     *
     * @param results the results to merge
     * @return a combined CicdResult
     */
    static CicdResult merge(List<CicdResult> results) {
        List<String> allFiles = results.stream()
                .flatMap(r -> r.files().stream())
                .toList();
        List<String> allWarnings = results.stream()
                .flatMap(r -> r.warnings().stream())
                .toList();
        return new CicdResult(allFiles, allWarnings);
    }
}
