package dev.iadev.domain.stack;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Version directory lookup with fallback.
 *
 * <p>Resolves version-specific directories by trying exact
 * match first, then falling back to major version wildcard.
 * This is a pure domain utility with no external framework
 * or I/O dependencies (RULE-006).</p>
 *
 * <p>Filesystem access is delegated to a
 * {@link VersionDirectoryProvider} port injected via
 * constructor.</p>
 */
public final class VersionResolver {

    private final VersionDirectoryProvider provider;

    /**
     * Creates a VersionResolver with the given directory
     * provider.
     *
     * @param provider the directory provider port
     */
    public VersionResolver(
            VersionDirectoryProvider provider) {
        this.provider = provider;
    }

    /**
     * Finds a version-specific directory with fallback.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Exact match:
     *       {@code {baseDir}/{name}-{version}}</li>
     *   <li>Major version fallback:
     *       {@code {baseDir}/{name}-{major}.x}</li>
     * </ol>
     *
     * @param baseDir the base directory to search in
     * @param name the component name (e.g. "java")
     * @param version the full version string (e.g. "21")
     * @return the resolved directory path, or empty if
     *         neither exists
     */
    public Optional<Path> findVersionDir(
            Path baseDir, String name, String version) {
        Path exact = baseDir.resolve(name + "-" + version);
        if (provider.exists(exact)) {
            return Optional.of(exact);
        }
        String major = extractMajorPart(version);
        Path fallback =
                baseDir.resolve(name + "-" + major + ".x");
        if (provider.exists(fallback)) {
            return Optional.of(fallback);
        }
        return Optional.empty();
    }

    /**
     * Extracts the major version part from a version string.
     *
     * @param version the version string
     * @return the major version part (before the first dot)
     */
    static String extractMajorPart(String version) {
        if (version == null || version.isEmpty()) {
            return version;
        }
        int dotIndex = version.indexOf('.');
        if (dotIndex < 0) {
            return version;
        }
        return version.substring(0, dotIndex);
    }
}
