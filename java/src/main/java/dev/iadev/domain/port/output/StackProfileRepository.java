package dev.iadev.domain.port.output;

import dev.iadev.domain.model.StackProfile;

import java.util.List;
import java.util.Optional;

/**
 * Output port for loading technology stack profiles.
 *
 * <p>Abstracts the persistence mechanism for stack profiles.
 * The domain depends on this interface; concrete implementations
 * (e.g., YAML-based loading) reside in the infrastructure adapter
 * layer.</p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>Implementations MUST return immutable collections.</li>
 *   <li>{@link #findByName(String)} MUST return {@link Optional#empty()}
 *       when no profile matches — never null.</li>
 *   <li>{@link #findAll()} MUST return an empty list (not null)
 *       when no profiles are available.</li>
 * </ul>
 *
 * <h2>Pre-conditions</h2>
 * <ul>
 *   <li>{@code profileName} parameters must not be null or blank.</li>
 * </ul>
 *
 * <h2>Post-conditions</h2>
 * <ul>
 *   <li>Returned {@link StackProfile} instances are always valid
 *       (non-null name, language, framework).</li>
 * </ul>
 *
 * <h2>Exceptions</h2>
 * <ul>
 *   <li>{@link IllegalArgumentException} if profileName is null or blank.</li>
 *   <li>Implementation-specific unchecked exceptions for I/O failures.</li>
 * </ul>
 *
 * @see StackProfile
 */
public interface StackProfileRepository {

    /**
     * Returns all available stack profiles.
     *
     * @return an immutable list of all profiles; empty if none exist
     */
    List<StackProfile> findAll();

    /**
     * Finds a stack profile by its unique name.
     *
     * @param profileName the profile identifier (e.g., "java-spring")
     * @return the matching profile, or empty if not found
     * @throws IllegalArgumentException if profileName is null or blank
     */
    Optional<StackProfile> findByName(String profileName);

    /**
     * Checks whether a profile with the given name exists.
     *
     * @param profileName the profile identifier to check
     * @return true if the profile exists, false otherwise
     * @throws IllegalArgumentException if profileName is null or blank
     */
    boolean exists(String profileName);
}
