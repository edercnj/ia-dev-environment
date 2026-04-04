package dev.iadev.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Detects existing generated artifact directories in the output path.
 *
 * <p>Implements RULE-012 (Overwrite Detection) by checking whether
 * the destination directory already contains directories that the
 * pipeline would generate: {@code .claude/}, {@code .github/},
 * {@code .codex/}, {@code .agents/}, and {@code docs/}.
 *
 * <p>When conflicts are detected and {@code --force} is not provided,
 * the CLI should abort with a formatted message suggesting
 * {@code --force} or a different {@code --output-dir}.
 *
 * <p>Example usage:
 * <pre>{@code
 * List<String> conflicts =
 *     OverwriteDetector.checkExistingArtifacts(destDir);
 * if (!conflicts.isEmpty() && !forceFlag) {
 *     String message =
 *         OverwriteDetector.formatConflictMessage(conflicts);
 *     throw new CliException(message, 1);
 * }
 * }</pre>
 *
 * @see dev.iadev.exception.CliException
 */
public final class OverwriteDetector {

    private static final List<String> ARTIFACT_DIRS = List.of(
            ".claude", ".github", ".codex", ".agents",
            "steering", "specs", "plans", "results", "contracts", "adr"
    );

    private OverwriteDetector() {
        // Utility class — no instantiation
    }

    /**
     * Checks whether the destination directory contains existing
     * generated artifact directories.
     *
     * <p>Only detects actual directories, not regular files with
     * the same name. Returns directory names with trailing slash
     * for display consistency.
     *
     * @param destDir the destination directory to inspect
     * @return list of conflicting directory names with trailing
     *         slash (e.g., {@code ".claude/"}); empty if none found
     */
    public static List<String> checkExistingArtifacts(Path destDir) {
        if (!Files.isDirectory(destDir)) {
            return Collections.emptyList();
        }

        List<String> conflicts = new ArrayList<>();
        for (String dir : ARTIFACT_DIRS) {
            Path candidate = destDir.resolve(dir);
            if (Files.isDirectory(candidate)) {
                conflicts.add("%s/".formatted(dir));
            }
        }
        return Collections.unmodifiableList(conflicts);
    }

    /**
     * Formats a user-facing error message listing conflicting
     * directories and suggesting remediation options.
     *
     * <p>Returns an empty string when the conflict list is empty,
     * allowing callers to check for conflicts with a simple
     * emptiness check.
     *
     * @param conflicts list of conflicting directory names
     * @return formatted multi-line error message, or empty string
     */
    public static String formatConflictMessage(List<String> conflicts) {
        if (conflicts.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        sb.append(
                "Output directory contains existing generated "
                        + "artifacts:\n");
        for (String conflict : conflicts) {
            sb.append("  - ").append(conflict)
                    .append(" (exists)\n");
        }
        sb.append('\n');
        sb.append("Use --force to overwrite existing files,\n");
        sb.append("or specify a different --output-dir.");
        return sb.toString();
    }
}
