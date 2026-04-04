package dev.iadev.domain.port.output;

import dev.iadev.domain.model.CheckpointState;

import java.util.Optional;

/**
 * Output port for checkpoint persistence operations.
 *
 * <p>Abstracts the storage mechanism for execution checkpoints.
 * The domain depends on this interface; concrete implementations
 * (e.g., file-based JSON persistence) reside in the infrastructure
 * adapter layer.</p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@link #save(CheckpointState)} MUST persist the state
 *       atomically — partial writes must not corrupt existing data.</li>
 *   <li>{@link #load(String)} MUST return {@link Optional#empty()}
 *       when no checkpoint exists — never null.</li>
 *   <li>{@link #clear(String)} MUST be idempotent — clearing a
 *       non-existent checkpoint must not throw.</li>
 * </ul>
 *
 * <h2>Pre-conditions</h2>
 * <ul>
 *   <li>{@code state} must not be null.</li>
 *   <li>{@code executionId} must not be null or blank.</li>
 * </ul>
 *
 * <h2>Post-conditions</h2>
 * <ul>
 *   <li>After {@link #save(CheckpointState)}, a subsequent
 *       {@link #load(String)} with the same executionId returns
 *       the saved state.</li>
 *   <li>After {@link #clear(String)}, a subsequent
 *       {@link #load(String)} returns empty.</li>
 * </ul>
 *
 * <h2>Exceptions</h2>
 * <ul>
 *   <li>{@link IllegalArgumentException} if parameters are null
 *       (or blank where applicable).</li>
 *   <li>Implementation-specific unchecked exceptions for I/O failures.</li>
 * </ul>
 *
 * @see CheckpointState
 */
public interface CheckpointStore {

    /**
     * Persists the given checkpoint state.
     *
     * @param state the checkpoint state to save
     * @throws IllegalArgumentException if state is null
     */
    void save(CheckpointState state);

    /**
     * Loads the checkpoint state for the given execution.
     *
     * @param executionId the unique execution identifier
     * @return the checkpoint state, or empty if not found
     * @throws IllegalArgumentException if executionId is null or blank
     */
    Optional<CheckpointState> load(String executionId);

    /**
     * Removes the checkpoint state for the given execution.
     *
     * <p>Idempotent: does not throw if no checkpoint exists.</p>
     *
     * @param executionId the unique execution identifier
     * @throws IllegalArgumentException if executionId is null or blank
     */
    void clear(String executionId);
}
