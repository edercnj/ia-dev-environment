package dev.iadev.application.assembler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Rules directory auditor -- counts files and bytes, checks
 * thresholds.
 *
 * <p>Uses synchronous {@code java.nio.file} by design. Audit
 * runs once per CLI invocation on a small directory (typically
 * 10 or fewer files). Sync I/O matches the TypeScript/Python
 * predecessor and avoids unnecessary async complexity.</p>
 *
 * <p>This class is read-only: it never modifies files.</p>
 *
 * @see AuditResult
 */
public final class Auditor {

    /** Maximum recommended number of rule files. */
    public static final int MAX_FILE_COUNT = 10;

    /** Maximum recommended total bytes (50 KB). */
    public static final long MAX_TOTAL_BYTES = 51_200L;

    private Auditor() {
        // utility class
    }

    /**
     * Counts rule files and total size, checks against
     * thresholds.
     *
     * <p>Returns an {@link AuditResult} with warnings if
     * thresholds are exceeded. Returns an empty result if
     * the directory does not exist or is not a directory.</p>
     *
     * @param rulesDir the rules directory to audit
     * @return the audit result
     */
    public static AuditResult auditRulesContext(
            Path rulesDir) {
        if (!Files.exists(rulesDir)
                || !Files.isDirectory(rulesDir)) {
            return new AuditResult(0, 0L,
                    List.of(), List.of());
        }
        List<Map.Entry<String, Long>> fileSizes =
                collectFileSizes(rulesDir);
        int totalFiles = fileSizes.size();
        long totalBytes = fileSizes.stream()
                .mapToLong(Map.Entry::getValue)
                .sum();
        List<String> warnings =
                checkThresholds(totalFiles, totalBytes);

        return new AuditResult(
                totalFiles, totalBytes,
                fileSizes, warnings);
    }

    private static List<Map.Entry<String, Long>>
    collectFileSizes(Path rulesDir) {
        List<String> entries = MarkdownFileScanner
                .listMarkdownFilesSorted(rulesDir).stream()
                .map(p -> p.getFileName().toString())
                .toList();

        List<Map.Entry<String, Long>> sizes =
                new ArrayList<>();
        for (String entry : entries) {
            Path fullPath = rulesDir.resolve(entry);
            try {
                long size = Files.size(fullPath);
                sizes.add(Map.entry(entry, size));
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to stat file: %s"
                                .formatted(fullPath), e);
            }
        }

        sizes.sort(Comparator
                .<Map.Entry<String, Long>>comparingLong(
                        Map.Entry::getValue)
                .reversed());
        return sizes;
    }

    private static List<String> checkThresholds(
            int totalFiles, long totalBytes) {
        List<String> warnings = new ArrayList<>();
        if (totalFiles > MAX_FILE_COUNT) {
            warnings.add(
                    ("%d rule files exceeds recommended"
                    + " maximum of %d.")
                            .formatted(totalFiles,
                                    MAX_FILE_COUNT));
        }
        if (totalBytes > MAX_TOTAL_BYTES) {
            long totalKb = totalBytes / 1024;
            warnings.add(
                    ("%dKB total rules exceeds recommended"
                    + " maximum of 50KB.")
                            .formatted(totalKb));
        }
        return warnings;
    }

    /**
     * Result of auditing the generated rules directory.
     *
     * @param totalFiles the number of .md files
     * @param totalBytes the sum of bytes of all .md files
     * @param fileSizes  file name and size pairs, sorted
     *                   by size descending
     * @param warnings   warnings for exceeded thresholds
     */
    public record AuditResult(
            int totalFiles,
            long totalBytes,
            List<Map.Entry<String, Long>> fileSizes,
            List<String> warnings) {

        /**
         * Creates an AuditResult with immutable lists.
         */
        public AuditResult {
            fileSizes = List.copyOf(fileSizes);
            warnings = List.copyOf(warnings);
        }
    }
}
