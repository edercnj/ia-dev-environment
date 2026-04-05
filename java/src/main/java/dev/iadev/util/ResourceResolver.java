package dev.iadev.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Resolves classpath resources to filesystem {@link Path}
 * objects, handling both exploded classpath and fat JAR
 * scenarios.
 *
 * <p>JAR extraction is delegated to
 * {@link JarResourceExtractor}.</p>
 *
 * @see JarResourceExtractor
 * @see ResourceDiscovery
 */
public final class ResourceResolver {

    private static final Object LOCK = new Object();
    static volatile Path cachedExtractedDir;

    private ResourceResolver() {
        // Utility class
    }

    /**
     * Resolves a resource directory by its relative path
     * within the resources root, without depth arithmetic.
     *
     * <p>Locates the first segment of {@code relativePath}
     * on the classpath and derives the resources root,
     * then appends the full relative path. Throws
     * {@link IllegalArgumentException} if the resolved
     * directory does not exist.</p>
     *
     * @param relativePath path relative to resources root
     *                     (e.g. {@code "databases/cache/redis"})
     * @return absolute filesystem path to the directory
     * @throws IllegalArgumentException if the directory
     *         cannot be found
     */
    public static Path resolveResourceDir(
            String relativePath) {
        String firstSegment = relativePath.contains("/")
                ? relativePath.substring(
                        0, relativePath.indexOf('/'))
                : relativePath;

        Path root = doResolveRoot(firstSegment, 1);
        Path resolved = root.resolve(relativePath);

        if (!Files.isDirectory(resolved)) {
            throw new IllegalArgumentException(
                    "Resource directory not found: "
                            + relativePath);
        }
        return resolved;
    }

    /**
     * Resolves the resources root directory by probing a
     * known resource name on the classpath.
     *
     * @param probe a resource directory name known to exist
     * @return filesystem path to the resources root
     * @deprecated Use {@link #resolveResourceDir(String)}
     *     for depth-free resolution. This method will be
     *     removed in a future release.
     */
    @Deprecated(forRemoval = true)
    public static Path resolveResourcesRoot(String probe) {
        return doResolveRoot(probe, 1);
    }

    /**
     * Resolves the resources root directory by probing a
     * known resource on the classpath and navigating up
     * {@code depth} parent levels.
     *
     * @param probe a resource path known to exist
     * @param depth number of parent levels to navigate up
     * @return filesystem path to the resources root
     * @deprecated Use {@link #resolveResourceDir(String)}
     *     for depth-free resolution. This method will be
     *     removed in a future release.
     */
    @Deprecated(forRemoval = true)
    public static Path resolveResourcesRoot(
            String probe, int depth) {
        return doResolveRoot(probe, depth);
    }

    private static Path doResolveRoot(
            String probe, int depth) {
        URL url = findOnClasspath(probe);
        if (url == null) {
            return Path.of("src/main/resources");
        }

        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            return resolveFromFile(url, depth);
        }
        if ("jar".equals(protocol)) {
            return resolveFromJar(url);
        }

        return Path.of("src/main/resources");
    }

    private static URL findOnClasspath(String name) {
        ClassLoader cl = Thread.currentThread()
                .getContextClassLoader();
        if (cl == null) {
            cl = ResourceResolver.class.getClassLoader();
        }
        return cl.getResource(name);
    }

    private static Path resolveFromFile(
            URL url, int depth) {
        try {
            Path path = Path.of(url.toURI());
            for (int i = 0; i < depth; i++) {
                path = path.getParent();
            }
            return path;
        } catch (URISyntaxException e) {
            throw new UncheckedIOException(
                    new IOException(
                            "Invalid resource URI: " + url,
                            e));
        }
    }

    static Path resolveFromJar(URL url) {
        synchronized (LOCK) {
            if (cachedExtractedDir != null
                    && Files.exists(cachedExtractedDir)) {
                return cachedExtractedDir;
            }
            cachedExtractedDir =
                    JarResourceExtractor
                            .extractJarResources(url);
            registerShutdownHook(cachedExtractedDir);
            return cachedExtractedDir;
        }
    }

    /**
     * Delegates to
     * {@link JarResourceExtractor#extractJarResources}.
     *
     * @param url the JAR URL
     * @return the path to extracted resources
     */
    static Path extractJarResources(URL url) {
        return JarResourceExtractor
                .extractJarResources(url);
    }

    /**
     * Creates a temp directory with secure permissions.
     *
     * @param prefix the directory name prefix
     * @return the created temp directory path
     * @throws IOException if creation fails
     */
    static Path createSecureTempDir(String prefix)
            throws IOException {
        if (!isPosixSystem()) {
            return Files.createTempDirectory(prefix);
        }
        FileAttribute<?> perms =
                PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString(
                                "rwx------"));
        return Files.createTempDirectory(prefix, perms);
    }

    private static boolean isPosixSystem() {
        String os = System.getProperty("os.name", "")
                .toLowerCase();
        return !os.contains("win");
    }

    static boolean shouldSkip(Path dir) {
        String name = dir.getFileName() == null
                ? "" : dir.getFileName().toString();
        return "META-INF".equals(name);
    }

    private static void registerShutdownHook(
            Path extractedDir) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        () -> deleteQuietly(extractedDir),
                        "ia-dev-env-cleanup"));
    }

    /**
     * Delegates to
     * {@link dev.iadev.application.assembler.CopyHelpers#deleteQuietly}.
     *
     * @param dir directory to delete recursively
     */
    static void deleteQuietly(Path dir) {
        dev.iadev.application.assembler.CopyHelpers.deleteQuietly(dir);
    }
}
