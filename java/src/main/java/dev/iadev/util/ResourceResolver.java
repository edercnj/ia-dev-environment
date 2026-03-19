package dev.iadev.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * Resolves classpath resources to filesystem {@link Path}
 * objects, handling both exploded classpath (IDE/development)
 * and fat JAR (production) scenarios.
 *
 * <p>When running from an exploded classpath, resources are
 * accessed directly via filesystem paths. When running from
 * a JAR, resources are extracted to a temporary directory
 * that is cached for the lifetime of the JVM and cleaned up
 * on shutdown.</p>
 *
 * <p>All assemblers should use
 * {@link #resolveResourcesRoot(String)} or
 * {@link #resolveResourcesRoot(String, int)} to obtain a
 * filesystem path to the resources root directory.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Path root = ResourceResolver.resolveResourcesRoot(
 *     "skills-templates");
 * // root points to src/main/resources/ (or extracted temp)
 * Path skills = root.resolve("skills-templates/core");
 * }</pre>
 *
 * @see ResourceDiscovery
 */
public final class ResourceResolver {

    private static final Object LOCK = new Object();
    private static volatile Path cachedExtractedDir;

    private ResourceResolver() {
        // Utility class
    }

    /**
     * Resolves the resources root directory by probing a
     * known resource name on the classpath.
     *
     * <p>Equivalent to
     * {@code resolveResourcesRoot(probe, 1)} — expects the
     * probe to be a top-level directory whose parent is the
     * resources root.</p>
     *
     * @param probe a resource directory name known to exist
     *              on the classpath (e.g. "skills-templates")
     * @return filesystem path to the resources root
     */
    public static Path resolveResourcesRoot(String probe) {
        return resolveResourcesRoot(probe, 1);
    }

    /**
     * Resolves the resources root directory by probing a
     * known resource on the classpath and navigating up
     * {@code depth} parent levels.
     *
     * <p>For a top-level directory probe (e.g.
     * "skills-templates"), use depth=1. For a nested file
     * probe (e.g. "templates/readme.md"), use depth=2.</p>
     *
     * @param probe a resource path known to exist on the
     *              classpath
     * @param depth number of parent levels to navigate up
     *              from the probe to reach the resources root
     * @return filesystem path to the resources root
     */
    public static Path resolveResourcesRoot(
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

    private static Path resolveFromJar(URL url) {
        synchronized (LOCK) {
            if (cachedExtractedDir != null
                    && Files.exists(cachedExtractedDir)) {
                return cachedExtractedDir;
            }
            cachedExtractedDir = extractJarResources(url);
            registerShutdownHook(cachedExtractedDir);
            return cachedExtractedDir;
        }
    }

    private static Path extractJarResources(URL url) {
        try {
            Path tempDir = Files.createTempDirectory(
                    "ia-dev-env-res-");
            String jarUrl = url.toString();
            int bang = jarUrl.indexOf('!');
            String jarUri = jarUrl.substring(0, bang + 2);
            URI fsUri = URI.create(jarUri);

            try (FileSystem jarFs = FileSystems.newFileSystem(
                    fsUri, Map.of())) {
                Path jarRoot = jarFs.getPath("/");
                Files.walkFileTree(jarRoot,
                        new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path dir,
                            BasicFileAttributes attrs)
                            throws IOException {
                        if (shouldSkip(dir)) {
                            return FileVisitResult
                                    .SKIP_SUBTREE;
                        }
                        Path target = tempDir.resolve(
                                jarRoot.relativize(dir)
                                        .toString());
                        Files.createDirectories(target);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attrs)
                            throws IOException {
                        String rel = jarRoot.relativize(file)
                                .toString();
                        if (rel.endsWith(".class")
                                || rel.startsWith(
                                        "META-INF/")) {
                            return FileVisitResult.CONTINUE;
                        }
                        Path target =
                                tempDir.resolve(rel);
                        Files.createDirectories(
                                target.getParent());
                        try (InputStream is =
                                     Files.newInputStream(
                                             file)) {
                            Files.copy(is, target,
                                    StandardCopyOption
                                            .REPLACE_EXISTING
                            );
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            return tempDir;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to extract JAR resources", e);
        }
    }

    private static boolean shouldSkip(Path dir) {
        String name = dir.getFileName() == null
                ? "" : dir.getFileName().toString();
        return "META-INF".equals(name);
    }

    private static void registerShutdownHook(
            Path extractedDir) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> deleteQuietly(extractedDir),
                        "ia-dev-env-cleanup"));
    }

    private static void deleteQuietly(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walkFileTree(dir,
                        new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attrs)
                            throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult
                    postVisitDirectory(
                            Path d, IOException exc)
                            throws IOException {
                        Files.delete(d);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException ignored) {
            // Best-effort cleanup
        }
    }
}
