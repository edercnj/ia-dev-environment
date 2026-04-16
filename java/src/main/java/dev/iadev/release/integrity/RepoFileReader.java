package dev.iadev.release.integrity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound adapter: reads repository files for {@link IntegrityChecker} (Story 0039-0003 §3.3).
 *
 * <p>Security:
 * <ul>
 *   <li>All paths are resolved against {@code repoRoot} and normalized; any path escaping the
 *       root via {@code ..} or symlinks is rejected with {@link SecurityException} (OWASP A01).</li>
 *   <li>Missing files return {@link Optional#empty()}; the caller decides whether to treat this
 *       as a PASS (no drift) or a hard failure.</li>
 *   <li>UTF-8 is enforced. No character-encoding negotiation.</li>
 * </ul>
 * </p>
 *
 * <p>Error messages never include file content — only the offending path — to avoid CWE-209
 * information disclosure.</p>
 */
public final class RepoFileReader {

    private final Path repoRoot;

    public RepoFileReader(Path repoRoot) {
        Objects.requireNonNull(repoRoot, "repoRoot");
        this.repoRoot = repoRoot.toAbsolutePath().normalize();
    }

    /**
     * Read the content of a repo-relative file as UTF-8.
     *
     * @param relativePath path relative to the repo root (e.g., {@code "CHANGELOG.md"})
     * @return the file content, or {@link Optional#empty()} if the file does not exist
     * @throws SecurityException if the resolved path escapes the repo root
     * @throws UncheckedIOException if the file exists but cannot be read
     */
    public Optional<String> readText(String relativePath) {
        Objects.requireNonNull(relativePath, "relativePath");
        Path resolved = resolveSafely(relativePath);
        if (!Files.exists(resolved)) {
            return Optional.empty();
        }
        rejectSymlinkEscape(resolved, relativePath);
        try {
            return Optional.of(Files.readString(resolved, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + relativePath, e);
        }
    }

    /**
     * Read multiple repo-relative files into a map preserving insertion order.
     * Missing files are omitted; no entry is added for them.
     */
    public Map<String, String> readTexts(List<String> relativePaths) {
        Objects.requireNonNull(relativePaths, "relativePaths");
        Map<String, String> out = new LinkedHashMap<>();
        for (String rel : relativePaths) {
            readText(rel).ifPresent(content -> out.put(rel, content));
        }
        return out;
    }

    private Path resolveSafely(String relativePath) {
        if (relativePath.isBlank()) {
            throw new SecurityException("Blank relative path");
        }
        Path candidate = repoRoot.resolve(relativePath).normalize().toAbsolutePath();
        if (!candidate.startsWith(repoRoot)) {
            throw new SecurityException("Path traversal rejected: " + relativePath);
        }
        return candidate;
    }

    private void rejectSymlinkEscape(Path resolved, String relativePath) {
        try {
            Path realPath = resolved.toRealPath();
            Path realRoot = repoRoot.toRealPath();
            if (!realPath.startsWith(realRoot)) {
                throw new SecurityException("Symlink traversal rejected: " + relativePath);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to resolve real path of " + relativePath, e);
        }
    }
}
