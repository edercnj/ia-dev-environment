package dev.iadev.domain.stack;

import java.nio.file.Path;
import java.util.List;

/**
 * Port for filesystem directory operations needed by
 * version resolution.
 *
 * <p>Abstracts filesystem I/O so that domain logic
 * ({@link VersionResolver}) remains pure and testable
 * without real filesystem access.</p>
 *
 * @see VersionResolver
 */
public interface VersionDirectoryProvider {

    /**
     * Checks whether the given path exists and is a
     * directory.
     *
     * @param path the path to check
     * @return true if the path is an existing directory
     */
    boolean exists(Path path);

    /**
     * Lists subdirectories under the given base path.
     *
     * @param basePath the directory to list
     * @return list of subdirectory paths, or empty list
     *         if the base path does not exist or has no
     *         subdirectories
     */
    List<Path> listVersionDirectories(Path basePath);
}
