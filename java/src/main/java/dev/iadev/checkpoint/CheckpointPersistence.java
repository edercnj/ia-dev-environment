package dev.iadev.checkpoint;

import java.nio.file.Path;

/**
 * Port for checkpoint serialization and deserialization.
 *
 * <p>Abstracts the persistence mechanism so that
 * {@link CheckpointEngine} does not depend on any specific
 * serialization framework (e.g., Jackson, Gson).</p>
 *
 * <p>Implementations must:
 * <ul>
 *   <li>Write atomically (temp file + rename) to avoid corruption</li>
 *   <li>Validate loaded state via {@link CheckpointValidation}</li>
 *   <li>Throw {@link dev.iadev.exception.CheckpointIOException}
 *       on I/O failures</li>
 * </ul>
 */
public interface CheckpointPersistence {

    /**
     * Serializes and persists the execution state to the given path.
     *
     * @param state the execution state to persist
     * @param path  the file path for the JSON file
     * @throws dev.iadev.exception.CheckpointIOException
     *         if writing fails
     */
    void save(ExecutionState state, Path path);

    /**
     * Loads and deserializes the execution state from the given path.
     *
     * @param path the file path to read from
     * @return the deserialized and validated execution state
     * @throws dev.iadev.exception.CheckpointIOException
     *         if reading or parsing fails
     * @throws dev.iadev.exception.CheckpointValidationException
     *         if state validation fails
     */
    ExecutionState load(Path path);
}
