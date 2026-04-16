package dev.iadev.release.prompt;

import dev.iadev.release.state.ReleaseState;

/**
 * Port for persisting release state updates triggered by
 * the {@link PromptEngine}. Decouples the engine from the
 * file-system implementation of the state file.
 */
@FunctionalInterface
public interface StatePort {

    /**
     * Persists the given state snapshot.
     *
     * @param state updated release state
     */
    void update(ReleaseState state);
}
