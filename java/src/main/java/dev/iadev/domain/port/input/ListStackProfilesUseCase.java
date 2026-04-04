package dev.iadev.domain.port.input;

import dev.iadev.domain.model.StackProfile;

import java.util.List;

/**
 * Use case contract for listing available stack profiles.
 *
 * <p>Returns all known technology stack profiles that can be
 * used for environment generation. Each {@link StackProfile}
 * contains the profile name, description, language, and
 * framework.</p>
 *
 * <p>Implementations must return an immutable list. An empty
 * list indicates no profiles are available (never null).</p>
 *
 * @see StackProfile
 */
public interface ListStackProfilesUseCase {

    /**
     * Lists all available stack profiles.
     *
     * @return an immutable list of stack profiles (never null)
     */
    List<StackProfile> listProfiles();
}
