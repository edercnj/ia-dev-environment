package dev.iadev.util;

import dev.iadev.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Discovers and reads resources from the classpath or an external
 * filesystem directory.
 *
 * <p>Search strategy:
 * <ol>
 *   <li>If {@code --resources-dir} is configured (via constructor),
 *       check the filesystem path first.</li>
 *   <li>Fall back to classpath via
 *       {@link ClassLoader#getResource(String)}.</li>
 * </ol>
 *
 * <p>This abstraction enables resources to be accessed identically
 * whether packaged inside a fat JAR or located on the filesystem
 * during development.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Classpath-only discovery
 * var discovery = new ResourceDiscovery();
 * String content = discovery.readResource(
 *     "config-templates/setup-config.java-spring.yaml");
 *
 * // With filesystem override (--resources-dir)
 * var discovery = new ResourceDiscovery(Path.of("/opt/resources"));
 * URL url = discovery.findResource("templates/readme.md.njk");
 * }</pre>
 *
 * @see ResourceNotFoundException
 */
public class ResourceDiscovery {

    private final Path resourcesDir;

    /**
     * Creates a discovery instance that searches classpath only.
     */
    public ResourceDiscovery() {
        this.resourcesDir = null;
    }

    /**
     * Creates a discovery instance with optional filesystem override.
     *
     * <p>When {@code resourcesDir} is non-null, filesystem is checked
     * first before falling back to classpath.</p>
     *
     * @param resourcesDir external resources directory, or null
     */
    public ResourceDiscovery(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * Returns the configured external resources directory, or null.
     *
     * @return the resources directory path, or null if classpath-only
     */
    public Path getResourcesDir() {
        return resourcesDir;
    }

    /**
     * Checks whether a resource exists without reading its content.
     *
     * @param relativePath path relative to the resources root
     * @return true if the resource exists via any strategy
     */
    public boolean resourceExists(String relativePath) {
        if (existsInFilesystem(relativePath)) {
            return true;
        }
        return existsInClasspath(relativePath);
    }

    /**
     * Locates a resource by relative path.
     *
     * <p>Search order: filesystem (if configured) then classpath.
     * Throws {@link ResourceNotFoundException} if not found via
     * any strategy.</p>
     *
     * @param relativePath path relative to the resources root
     * @return URL pointing to the resource
     * @throws ResourceNotFoundException if resource is not found
     */
    public URL findResource(String relativePath) {
        URL url = findInFilesystem(relativePath);
        if (url != null) {
            return url;
        }

        url = findInClasspath(relativePath);
        if (url != null) {
            return url;
        }

        throw new ResourceNotFoundException(
                relativePath, buildStrategiesDescription());
    }

    /**
     * Reads a resource's content as a UTF-8 string.
     *
     * @param relativePath path relative to the resources root
     * @return the resource content as a string
     * @throws ResourceNotFoundException if the resource is not found
     */
    public String readResource(String relativePath) {
        URL url = findResource(relativePath);
        try (InputStream is = url.openStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResourceNotFoundException(
                    relativePath, buildStrategiesDescription());
        }
    }

    /**
     * Lists resource names in a directory.
     *
     * <p>When {@code resourcesDir} is configured and the directory
     * exists on the filesystem, lists filesystem entries. Otherwise
     * returns an empty list (classpath directory listing is not
     * reliably supported across all classloaders).</p>
     *
     * @param directory directory path relative to the resources root
     * @return list of resource names (file names only, not full paths)
     */
    public List<String> listResources(String directory) {
        if (resourcesDir != null) {
            return listFromFilesystem(directory);
        }
        return listFromClasspath(directory);
    }

    private boolean existsInFilesystem(String relativePath) {
        if (resourcesDir == null) {
            return false;
        }
        return Files.exists(resourcesDir.resolve(relativePath));
    }

    private boolean existsInClasspath(String relativePath) {
        return getClassLoader().getResource(relativePath) != null;
    }

    private URL findInFilesystem(String relativePath) {
        if (resourcesDir == null) {
            return null;
        }
        Path filePath = resourcesDir.resolve(relativePath);
        if (Files.exists(filePath)) {
            try {
                return filePath.toUri().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }
        return null;
    }

    private URL findInClasspath(String relativePath) {
        return getClassLoader().getResource(relativePath);
    }

    private List<String> listFromFilesystem(String directory) {
        Path dirPath = resourcesDir.resolve(directory);
        if (!Files.isDirectory(dirPath)) {
            return Collections.emptyList();
        }
        List<String> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(dirPath)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    entries.add(entry.getFileName().toString());
                }
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
        return entries;
    }

    private List<String> listFromClasspath(String directory) {
        // Classpath directory listing is not reliably supported
        // in all environments (JAR vs filesystem). Return empty list
        // when no resourcesDir is configured. Callers needing
        // directory listing should use --resources-dir.
        return Collections.emptyList();
    }

    private String buildStrategiesDescription() {
        if (resourcesDir != null) {
            return "filesystem(%s), classpath".formatted(resourcesDir);
        }
        return "classpath";
    }

    private ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread()
                .getContextClassLoader();
        if (cl == null) {
            cl = ResourceDiscovery.class.getClassLoader();
        }
        return cl;
    }
}
