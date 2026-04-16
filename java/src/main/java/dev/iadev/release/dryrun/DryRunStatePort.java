package dev.iadev.release.dryrun;

import java.nio.file.Path;

/**
 * Port managing the dummy state file written during an
 * interactive dry-run (story-0039-0013 §3.1).
 *
 * <p>The state file lives in the OS temporary directory
 * with restrictive permissions such as {@code 0600}
 * where supported, and is always deleted in
 * {@link #delete(Path)}, even when the simulation is
 * aborted.
 */
public interface DryRunStatePort {

    /**
     * Creates the dummy state file.
     *
     * @param version simulated release version
     * @return absolute path to the created file
     */
    Path create(String version);

    /**
     * Deletes the dummy state file. Must be idempotent:
     * calling delete on a non-existent path is a no-op.
     *
     * @param path path previously returned by
     *             {@link #create(String)}; may be null
     */
    void delete(Path path);
}
