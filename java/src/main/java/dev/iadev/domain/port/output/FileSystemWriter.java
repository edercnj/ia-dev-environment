package dev.iadev.domain.port.output;

import java.nio.file.Path;

/**
 * Output port for filesystem write operations.
 *
 * <p>Abstracts all file I/O operations needed by the domain.
 * The domain depends on this interface; concrete implementations
 * reside in the infrastructure adapter layer.</p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@link #writeFile(Path, String)} MUST create parent
 *       directories if they do not exist.</li>
 *   <li>{@link #createDirectory(Path)} MUST be idempotent — calling
 *       it on an existing directory must not throw.</li>
 *   <li>{@link #exists(Path)} MUST NOT throw exceptions for
 *       inaccessible paths — it returns false instead.</li>
 *   <li>{@link #copyResource(String, Path)} copies a classpath
 *       resource to the target path.</li>
 * </ul>
 *
 * <h2>Pre-conditions</h2>
 * <ul>
 *   <li>{@code path} parameters must not be null.</li>
 *   <li>{@code content} must not be null (empty string is acceptable).</li>
 *   <li>{@code resourcePath} must not be null or blank.</li>
 * </ul>
 *
 * <h2>Post-conditions</h2>
 * <ul>
 *   <li>After {@link #writeFile(Path, String)}, the file exists with
 *       the specified content encoded in UTF-8.</li>
 *   <li>After {@link #createDirectory(Path)}, the directory exists.</li>
 * </ul>
 *
 * <h2>Exceptions</h2>
 * <ul>
 *   <li>{@link IllegalArgumentException} if any parameter is null
 *       (or blank where applicable).</li>
 *   <li>Implementation-specific unchecked exceptions for I/O failures.</li>
 * </ul>
 */
public interface FileSystemWriter {

    /**
     * Writes content to a file, creating parent directories as needed.
     *
     * @param path    the target file path
     * @param content the file content (UTF-8)
     * @throws IllegalArgumentException if path or content is null
     */
    void writeFile(Path path, String content);

    /**
     * Creates a directory and any necessary parent directories.
     *
     * <p>Idempotent: does not throw if the directory already exists.</p>
     *
     * @param path the directory path to create
     * @throws IllegalArgumentException if path is null
     */
    void createDirectory(Path path);

    /**
     * Checks whether a file or directory exists at the given path.
     *
     * @param path the path to check
     * @return true if the path exists, false otherwise
     * @throws IllegalArgumentException if path is null
     */
    boolean exists(Path path);

    /**
     * Copies a classpath resource to the specified destination path.
     *
     * @param resourcePath the classpath resource path
     * @param destination  the target file path
     * @throws IllegalArgumentException if resourcePath is null/blank
     *                                  or destination is null
     */
    void copyResource(String resourcePath, Path destination);
}
