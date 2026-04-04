package dev.iadev.domain.service;

import dev.iadev.domain.model.StackProfile;
import dev.iadev.domain.port.input.ListStackProfilesUseCase;
import dev.iadev.domain.port.output.StackProfileRepository;

import java.util.List;
import java.util.Objects;

/**
 * Domain service that lists available stack profiles.
 *
 * <p>Implements {@link ListStackProfilesUseCase} by delegating
 * to the {@link StackProfileRepository} output port. Contains
 * no infrastructure dependencies.</p>
 *
 * @see ListStackProfilesUseCase
 * @see StackProfileRepository
 */
public final class ListStackProfilesService
        implements ListStackProfilesUseCase {

    private final StackProfileRepository profileRepository;

    /**
     * Creates a new ListStackProfilesService.
     *
     * @param profileRepository the repository for loading
     *                          stack profiles (must not be null)
     * @throws NullPointerException if profileRepository is null
     */
    public ListStackProfilesService(
            StackProfileRepository profileRepository) {
        this.profileRepository = Objects.requireNonNull(
                profileRepository,
                "profileRepository must not be null");
    }

    /**
     * Lists all available stack profiles by delegating to
     * the repository.
     *
     * @return an immutable list of stack profiles (never null)
     */
    @Override
    public List<StackProfile> listProfiles() {
        return profileRepository.findAll();
    }
}
